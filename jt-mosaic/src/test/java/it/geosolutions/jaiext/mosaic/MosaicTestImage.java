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
package it.geosolutions.jaiext.mosaic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.NullDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

import org.junit.Test;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;


/**
 * Simple class for displaying a mosaic of 2 images with the first image is translated by half of his dimension. The mosaic operation used can be the
 * old or the new version. The purpose of this class is to help the reader to understand the behavior of the Mosaic operation. For displaying the
 * mosaic, the RenderedImageBrowser clas is used. There are no ROIs, Alpha channels or No Data. For selecting the new MosaicDescriptor, the
 * JAI.Ext.NewDescriptor boolean must be set to true, fale for the old descriptor. For printing the result to the screen the JAI.Ext.Interactive
 * parameter must be set to true.
 * 
 */
public class MosaicTestImage extends TestBase{

    private static final boolean INTERACTIVE = Boolean.getBoolean("JAI.Ext.Interactive");

    private static final boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");

    public static RenderedImage getSyntheticByte(byte value) {
        final float width = 256;
        final float height = 256;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[] { value };
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);

    }

    @Test
    public void testOldMosaicOperation() {
        if (!OLD_DESCRIPTOR) {
            testSimpleMosaicOperation(OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewMosaicOperation() {
        if (OLD_DESCRIPTOR) {
            testSimpleMosaicOperation(!OLD_DESCRIPTOR);
        }
    }

    public void testSimpleMosaicOperation(boolean oldDescriptor) {
        // image creation
        RenderedImage image1 = getSyntheticByte((byte) 99);
        RenderedImage image2 = getSyntheticByte((byte) 50);
        int width = image1.getWidth();
        // layout creation (same height of the source images, doubled width)
        ImageLayout layout = new ImageLayout(0, 0, image1.getWidth() + image2.getWidth(),
                image1.getHeight());

        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        // background values and threshold
        double[] background = { 0 };
        double[][] threshold = { { 0 }, { 0 } };
        // translation of the first image
        RenderedImage image3 = TranslateDescriptor.create(image1, width * 0.1F, 0F, null, hints);
        // No op for the second image
        RenderedImage image4 = NullDescriptor.create(image2, hints);
        // array creation
        RenderedImage[] sources = { image4, image3 };
        RenderedImage image5;
        if (!oldDescriptor) {

            image5 = MosaicDescriptor.create(sources, 
                    javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, 
                    null,null,null,background,null,
                    hints);
        } else {
            image5 = javax.media.jai.operator.MosaicDescriptor.create(sources,
                    javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null,
                    threshold, background, hints);
        }
        // Operations for showing the mosaic image
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(image5, false, false);
            try {
                System.out.println("Press a Key when done");
                System.in.read();
            } catch (IOException e) {
                // Ignore
            }
        }
        // Final Image disposal
        if (image5 instanceof RenderedOp) {
            ((RenderedOp) image5).dispose();
        }

    }

    @Test
    public void testAggregatedNoData() {
        final int w = 32;
        final int h = 64;
        final int halfValue = 128;
        final int quarterValue = 64;

        // we are going to mosaic 2 images.
        
        // First image:
        // Upper half: A rectangle with third band = 0
        // Lower half: A Fully black rectangle
        int [] topLeftAreaPixels = new int[] {quarterValue, halfValue, 0};
        int [] bottomLeftAreaPixels = new int[] {0, 0, 0};
        BufferedImage image1 = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = image1.getRaster();
        fillRaster(raster, 0, h/2, w, h/2, bottomLeftAreaPixels);
        fillRaster(raster, 0, 0, w, h/2, topLeftAreaPixels);

        // Second image:
        // Upper half: A rectangle with second and third band = 0
        // Lower half: A rectangle with first band = 0
        int [] topRightAreaPixels = new int[] {halfValue, 0, 0};
        int [] bottomRightAreaPixels = new int[] {0, quarterValue, halfValue};
        BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster2 = bi2.getRaster();
        fillRaster(raster2, 0, 0, w, h/2, topRightAreaPixels);
        fillRaster(raster2, 0, h/2, w, h/2, bottomRightAreaPixels);
        
        // Shift the second image beside (to the right) of the first image
        RenderedImage image2 = TranslateDescriptor.create(bi2, (float) w, 0F, null, null);
        
        // prepare the mosaicking params
        RenderedImage[] sources = { image1, image2 };
        final double[] background = new double[] { 255, 255, 255 };
 
        // set noData to zero
        Range noDataByte = RangeFactory.create(0, 0);
        Range[] noData = new Range[] { noDataByte, noDataByte };

        RenderedImage mosaic = MosaicDescriptor.create(sources,
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                background, noData, null);
        int[] destPixel = new int[3];

        // Operations for showing the mosaic image
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(mosaic, false, false);
            try {
                System.out.println("Press a Key when done");
                System.in.read();
            } catch (IOException e) {
                // Ignore
            }
        }
        Raster rasterDest = mosaic.getData();
     
        // Bottom left mosaicked rectangle should have been set to background values
        // since all bands were noData
        for (int k = 0; k < 3; k++) {
            rasterDest.getPixel(0, h / 2, destPixel);
            assertTrue((int) background[k] == destPixel[k]);
        }
        
        // All other regions should have preserved the original data since
        // not all the bands was noData.
        checkRasterArea(rasterDest, 0, 0, w, h/2, topLeftAreaPixels, destPixel);
        checkRasterArea(rasterDest, w, h/2, w, h/2, bottomRightAreaPixels, destPixel);
        checkRasterArea(rasterDest, w, 0, w, h/2, topRightAreaPixels, destPixel);
    }

    private void checkRasterArea(Raster rasterDest, int x, int y, int w, int h, int[] expectedPixel,
            int[] destPixel) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                rasterDest.getPixel(i, j, destPixel);
                assertArrayEquals(expectedPixel, destPixel);
            }
        }
    }

    private void fillRaster(WritableRaster raster, int x, int y, int w, int h, int[] pixel) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                raster.setPixel(i, j, pixel);
            }
        }
    }

}
