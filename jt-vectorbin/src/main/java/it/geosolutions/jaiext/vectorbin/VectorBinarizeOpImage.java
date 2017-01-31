/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 GeoSolutions


 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.geosolutions.jaiext.vectorbin;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFactory;
import javax.media.jai.SourcelessOpImage;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jtsexample.geom.ExtendedCoordinate;
import com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequence;

import it.geosolutions.jaiext.utilities.shape.LiteShape;

/**
 * Creates a binary image based on tests of pixel inclusion in a polygonal {@code Geometry}. See {@link VectorBinarizeDescriptor} for details.
 * 
 * @author Michael Bedward
 * @author Andrea Aime
 */
public class VectorBinarizeOpImage extends SourcelessOpImage {

    private final PreparedGeometry geom;

    private final Shape shape;

    private Raster solidTile;

    private Raster blankTile;

    /** Default setting for anti-aliasing (false). */
    public static final boolean DEFAULT_ANTIALIASING = false;

    private boolean antiAliasing = DEFAULT_ANTIALIASING;

    private GeometryFactory gf = new GeometryFactory();

    /**
     * Constructor.
     * 
     * @param sm the {@code SampleModel} used to create tiles
     * @param configuration rendering hints
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * @param width image width
     * @param height image height
     * @param geom reference polygonal geometry
     * @param antiAliasing whether to use anti-aliasing when rendering the reference geometry
     */
    public VectorBinarizeOpImage(SampleModel sm, Map configuration, int minX, int minY, int width,
            int height, PreparedGeometry geom, boolean antiAliasing) {
        super(buildLayout(minX, minY, width, height, sm), configuration, sm, minX, minY, width,
                height);

        this.geom = geom;
        this.shape = new LiteShape(geom.getGeometry());
        this.antiAliasing = antiAliasing;
    }

    /**
     * Builds an {@code ImageLayout} for this image. The {@code width} and {@code height} arguments are requested tile dimensions which will only be
     * used if they are smaller than this operator's default tile dimension.
     * 
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * @param width requested tile width
     * @param height requested tile height
     * @param sm sample model
     * 
     * @return the {@code ImageLayout} object
     */
    static ImageLayout buildLayout(int minX, int minY, int width, int height, SampleModel sm) {
        // build a sample model for the single tile
        ImageLayout il = new ImageLayout();
        il.setMinX(minX);
        il.setMinY(minY);
        il.setWidth(width);
        il.setHeight(height);
        il.setTileWidth(sm.getWidth());
        il.setTileHeight(sm.getHeight());
        il.setSampleModel(sm);

        if (!il.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            il.setTileGridXOffset(il.getMinX(null));
        }
        if (!il.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            il.setTileGridYOffset(il.getMinY(null));
        }

        return il;
    }

    /**
     * Returns the specified tile.
     * 
     * @param tileX tile X index
     * @param tileY tile Y index
     * 
     * @return the requested tile
     */
    @Override
    public Raster computeTile(int tileX, int tileY) {
        final int x = tileXToX(tileX);
        final int y = tileYToY(tileY);

        // get the raster tile
        Raster tile = getTileRaster(x, y);

        // create a read only child in the right location
        Raster result = tile.createChild(0, 0, tileWidth, tileHeight, x, y, null);
        return result;
    }

    /**
     * Gets the data for the requested tile. If the tile is either completely within or outside of the reference {@code PreparedGeometry} a cached
     * constant {@code Raster} with 1 or 0 values is returned. Otherwise tile pixels are checked for inclusion and set individually.
     * 
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * 
     * @return the requested tile
     */
    protected Raster getTileRaster(int minX, int minY) {
        // check relationship between geometry and the tile we're computing
        Polygon testRect = getTestRect(minX, minY);
        try {
            // RasterOp need to be thread safe
            synchronized (geom) {
                if (geom.contains(testRect)) {
                    return getSolidTile();
                } else if (geom.disjoint(testRect)) {
                    return getBlankTile();
                }
            }
        } catch (TopologyException tpe) {
            // In case a Topology Exception have been raised,
            // use the standard rasterization instead of leveraging
            // on the shared tiles
        }

        return drawGeometry(minX, minY);
    }

    /**
     * Draw the geometry using Java2D
     * 
     * @return the binarized geometry
     */
    private Raster drawGeometry(final int minX, final int minY) {
        final int offset = antiAliasing ? 2 : 0;
        SampleModel tileSampleModel = sampleModel
                .createCompatibleSampleModel(tileWidth, tileHeight);

        WritableRaster raster = RasterFactory.createWritableRaster(tileSampleModel,
                new java.awt.Point(0, 0));
        BufferedImage bi = new BufferedImage(colorModel, raster, false, null);
        Graphics2D graphics = null;
        try {
            graphics = bi.createGraphics();

            graphics.setClip(-offset, -offset, tileWidth + offset * 2, tileHeight + offset * 2);
            graphics.translate(-minX, -minY);
            if (antiAliasing) {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }

            // draw the shape
            graphics.setColor(Color.WHITE);
            graphics.fill(shape);
        } finally {
            if (graphics != null) {
                graphics.dispose();
            }
        }

        return raster;

    }

    /**
     * Returns (creating and caching if the first call) a constant tile with 1 values
     * 
     * @return the constant tile
     */
    private Raster getSolidTile() {
        if (solidTile == null) {
            solidTile = constantTile(1);
        }
        return solidTile;
    }

    /**
     * Returns (creating and caching if the first call) a constant tile with 0 values
     * 
     * @return the constant tile
     */
    private Raster getBlankTile() {
        if (blankTile == null) {
            blankTile = constantTile(0);
        }
        return blankTile;
    }

    /**
     * Builds a tile with constant value
     * 
     * @param value the constant value
     * 
     * @return the new tile
     */
    private Raster constantTile(int value) {
        // build the raster
        WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, new java.awt.Point(
                0, 0));

        // sanity checks
        int dataType = sampleModel.getTransferType();
        int numBands = sampleModel.getNumBands();
        if (dataType != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException(
                    "The code works only if the sample model data type is BYTE");
        }
        if (numBands != 1) {
            throw new IllegalArgumentException("The code works only for single band rasters!");
        }

        // flood fill
        int w = sampleModel.getWidth();
        int h = sampleModel.getHeight();
        int[] data = new int[w * h];
        Arrays.fill(data, value);
        raster.setSamples(0, 0, w, h, 0, data);

        return raster;
    }

    /**
     * Builds the bounds of the rectangle used to test inclusion in the reference {@code PreparedGeometry}.
     * 
     * @param x origin X ordinate
     * @param y origin Y ordinate
     */
    private Polygon getTestRect(int x, int y) {
        ExtendedCoordinate[] copyCoords = new ExtendedCoordinate[5];
        for (int i = 0; i < 5; i++) {
            int xx = x;
            int yy = y;
            if (i == 1 || i == 2) {
                yy = y + tileHeight;
            }
            if (i == 2 || i == 3) {
                xx = x + tileWidth;
            }
            copyCoords[i] = new ExtendedCoordinate(xx, yy, 0, 0);
        }

        ExtendedCoordinateSequence seq = new ExtendedCoordinateSequence(copyCoords);

        return gf.createPolygon(gf.createLinearRing(seq), null);
    }
}
