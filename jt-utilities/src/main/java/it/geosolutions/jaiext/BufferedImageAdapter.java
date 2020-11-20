/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2016 GeoSolutions


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
package it.geosolutions.jaiext;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;

import javax.media.jai.RenderedImageAdapter;

/**
 * a {@link RenderedImageAdapter} alternative that works around a bug in {@link BufferedImage}
 * sub-images
 *
 * @author Andrea Aime - GeoSolutions
 */
public class BufferedImageAdapter implements RenderedImage {

    private BufferedImage image;

    public BufferedImageAdapter(BufferedImage image) {
        this.image = image;
    }

    @Override
    public int getNumXTiles() {
        return 1;
    }

    @Override
    public int getNumYTiles() {
        return 1;
    }

    @Override
    public int getMinTileX() {
        return 0;
    }

    @Override
    public int getMinTileY() {
        return 0;
    }

    @Override
    public int getTileWidth() {
        return image.getWidth();
    }

    @Override
    public int getTileHeight() {
        return image.getHeight();
    }

    @Override
    public int getTileGridXOffset() {
        return 0;
    }

    @Override
    public int getTileGridYOffset() {
        return 0;
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            return null;
        } else {
            return image.getTile(tileX, tileY);
        }
    }

    public Raster getData() {
        final WritableRaster raster = image.getRaster();
        if (raster.getParent() != null) {
            // have to force a copy, otherwise we are breaking the RenderedImage contract just
            // like BufferedImage does (the PNGJ writer actually has special code to work
            // around that bug)
            return image.getData(new Rectangle(0, 0, raster.getWidth(), raster.getHeight()));
        } else {
            return raster;
        }
    }

    public Raster getData(Rectangle rect) {
        return image.getData(rect);
    }

    public WritableRaster copyData(WritableRaster raster) {
        return image.copyData(raster);
    }

    @Override
    public Vector<RenderedImage> getSources() {
        // pretend it does not have sources, make it behave like BufferedImage
        return new Vector<>();
    }

    @Override
    public Object getProperty(String name) {
        return image.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
        return image.getPropertyNames();
    }

    @Override
    public ColorModel getColorModel() {
        return image.getColorModel();
    }

    @Override
    public SampleModel getSampleModel() {
        return image.getSampleModel();
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getMinX() {
        return image.getMinX();
    }

    @Override
    public int getMinY() {
        return image.getMinY();
    }
}
