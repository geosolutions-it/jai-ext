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
package it.geosolutions.jaiext.bandselect;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;

import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandSelect" operation.
 *
 * <p>This <code>OpImage</code> copies the specified bands of the source
 * image to the destination image in the order that is specified.
 *
 * @see javax.media.jai.operator.BandSelectDescriptor
 * @see BandSelectCRIF
 *
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class BandSelectOpImage extends PointOpImage {

    // Set if the source has a SinglePixelPackedSampleModel and
    // bands.length < 3.
    private boolean triggerCopyOperation;

    private int[] bands;

    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            int[] bandIndices) {
        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Create a sub-banded SampleModel.
        SampleModel sourceSM = source.getSampleModel();
        int numBands = bandIndices.length;

        // The only ColorModel compatible with a SinglePixelPackedSampleModel
        // in the J2SE is a DirectColorModel which is by definition of
        // ColorSpace.TYPE_RGB. Therefore if there are fewer than 3 bands
        // a data copy is obligatory if a ColorModel will be possible.
        SampleModel sm = null;
        if(sourceSM instanceof SinglePixelPackedSampleModel && numBands < 3) {
            sm = new PixelInterleavedSampleModel(
                     DataBuffer.TYPE_BYTE,
                     sourceSM.getWidth(), sourceSM.getHeight(),
                     numBands, sourceSM.getWidth()*numBands,
                     numBands == 1 ? new int[] {0} : new int[] {0, 1});
        } else {
            sm = sourceSM.createSubsetSampleModel(bandIndices);
        }
        il.setSampleModel(sm);

        // Clear the ColorModel mask if needed.
        ColorModel cm = il.getColorModel(null);
        if(cm != null &&
           !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            // Clear the mask bit if incompatible.
            il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
        }

        // Force the tile grid to be identical to that of the source.
        il.setTileGridXOffset(source.getTileGridXOffset());
        il.setTileGridYOffset(source.getTileGridYOffset());
        il.setTileWidth(source.getTileWidth());
        il.setTileHeight(source.getTileHeight());

        return il;
    }

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param bands  The selected band indices of the source.
     *                     The number of bands of the destination is
     *                     determined by <code>bands.length</code>.
     */
    public BandSelectOpImage(RenderedImage source,
                             Map<?,?> config,
                             ImageLayout layout,
                             int[] bandIndices) {
        super(vectorize(source),
              layoutHelper(layout, source, bandIndices),
              config, true);

        this.triggerCopyOperation =
            source.getSampleModel() instanceof SinglePixelPackedSampleModel &&
            bandIndices.length < 3;
        this.bands = (int[])bandIndices.clone();
    }

    public boolean computesUniqueTiles() {
        return triggerCopyOperation;
    }

    public Raster computeTile(int tileX, int tileY) {
        Raster tile = getSourceImage(0).getTile(tileX, tileY);

        if (triggerCopyOperation) {
            // Copy the data as there is no concrete ColorModel for
            // a SinglePixelPackedSampleModel with numBands < 3.
            tile = tile.createChild(tile.getMinX(), tile.getMinY(),
                                    tile.getWidth(), tile.getHeight(),
                                    tile.getMinX(), tile.getMinY(),
                                    bands);
            WritableRaster raster = createTile(tileX, tileY);
            raster.setRect(tile);

            return raster;
        } else {
            // Simply return a child of the corresponding source tile.
            return tile.createChild(tile.getMinX(), tile.getMinY(),
                                    tile.getWidth(), tile.getHeight(),
                                    tile.getMinX(), tile.getMinY(),
                                    bands);
        }
    }

    public Raster getTile(int tileX, int tileY) {

        // if we have to perform a copy, caching is needed!
        if (triggerCopyOperation) {
            return super.getTile(tileX, tileY);
        }
        // Just to return computeTile() result so as to avoid caching.
        return computeTile(tileX, tileY);
    }
}
