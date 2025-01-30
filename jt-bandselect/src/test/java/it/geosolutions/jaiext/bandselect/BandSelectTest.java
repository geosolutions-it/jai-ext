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

import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.RenderingHints;
import java.awt.image.*;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;

import org.junit.Test;

import com.sun.media.jai.opimage.CopyOpImage;

import static org.junit.Assert.*;

/**
 * Testing the new BandSelect operation
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class BandSelectTest extends TestBase {

    @Test
    public void baseTest() {
        
        // create image with 2 bands
        RenderedImage twoBands=ConstantDescriptor.create(512f, 512f, new Double[]{1d,0d}, null);
        
        // now select second band
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect");
        pb.addSource(twoBands);
        pb.setParameter("bandIndices", new int[]{1});
        RenderedOp oneBand = JAI.create("BandSelect", pb);
        
        
        // make sure we got the right band
        assertEquals(1,oneBand.getSampleModel().getNumBands());
        assertEquals(0,oneBand.getData().getSample(0, 0, 0),1E-11);
        
    }

    @Test
    public void copyTest() {
        // Layout, Definition of a Layout with a SinglePixelPackedSampleModel
        ImageLayout layout = new ImageLayout();
        int[] bitMask = new int[] { 0, 1 };
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, 512, 512,
                bitMask);
        layout.setSampleModel(sampleModel);
        layout.setColorModel(null);

        // Creation of the RenderingHints
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // create image with 2 bands
        RenderedImage twoBands = ConstantDescriptor.create(512f, 512f, new Byte[] { 1, 0 }, null);
        // Force the SampleModel to be th one defiend by the layout
        RenderedImage twoBandsPacked = new CopyOpImage(twoBands, hints, layout);

        // now select second band
        RenderedOp oneBand = BandSelectDescriptor.create(twoBandsPacked, new int[] { 1 }, hints);

        // make sure we got the right band
        assertEquals(1, oneBand.getSampleModel().getNumBands());
        assertEquals(0, oneBand.getData().getSample(0, 0, 0), 1E-11);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testZeroIndexes(){
        // create image with 2 bands
        RenderedImage twoBands=ConstantDescriptor.create(1f, 1f, new Double[]{1d,0d}, null);
        
        // now select second band
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect");
        pb.addSource(twoBands);
        pb.setParameter("bandIndices", new int[0]);
        RenderedOp oneBand = JAI.create("BandSelect", pb);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNegativeIndex(){
        // create image with 2 bands
        RenderedImage twoBands=ConstantDescriptor.create(1f, 1f, new Double[]{1d,0d}, null);
        
        // now select second band
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect");
        pb.addSource(twoBands);
        pb.setParameter("bandIndices", new int[]{-1});
        RenderedOp oneBand = JAI.create("BandSelect", pb);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGreaterIndex(){
        // create image with 2 bands
        RenderedImage twoBands=ConstantDescriptor.create(1f, 1f, new Double[]{1d,0d}, null);
        
        // now select second band
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect");
        pb.addSource(twoBands);
        pb.setParameter("bandIndices", new int[]{2});
        RenderedOp oneBand = JAI.create("BandSelect", pb);
    }

    @Test
    public void testColorModelDouble() throws Exception {
        // create image with 4 double bands
        RenderedImage source = ConstantDescriptor.create(512f, 512f, new Double[] {1d,2d,3d,4d}, null);
        assertEquals(DataBuffer.TYPE_DOUBLE, source.getSampleModel().getDataType());

        checkFloatingPointColorModel(source);
    }

    @Test
    public void testColorModelFloat() throws Exception {
        // create image with 4 float bands
        RenderedImage source = ConstantDescriptor.create(512f, 512f, new Float[] {1f,2f,3f,4f}, null);
        assertEquals(DataBuffer.TYPE_FLOAT, source.getSampleModel().getDataType());

        checkFloatingPointColorModel(source);
    }

    private static void checkFloatingPointColorModel(RenderedImage source) {
        // go through the various options that might trigger
        for (int bandCount = 1; bandCount <= 4; bandCount++) {
            int[] bandIndices = new int[bandCount];
            for (int j = 0; j < bandCount; j++) {
                bandIndices[j] = j;
            }

            ParameterBlockJAI pb =  new ParameterBlockJAI("BandSelect");
            pb.addSource(source);
            pb.setParameter("bandIndices", bandIndices);
            RenderedOp op = JAI.create("BandSelect", pb);
            assertFalse("Has alpha with " + bandCount + " bands", op.getColorModel().hasAlpha());
            assertEquals(ColorModel.OPAQUE, op.getColorModel().getTransparency());
        }
    }

    @Test
    public void testColorModelByte() throws Exception {
        // create image with 4 byte bands
        RenderedImage source = ConstantDescriptor.create(512f, 512f, new Byte[] {1,2,3,4}, null);
        assertEquals(DataBuffer.TYPE_BYTE, source.getSampleModel().getDataType());

        // go through the various options that might trigger
        for (int bandCount = 1; bandCount <= 4; bandCount++) {
            int[] bandIndices = new int[bandCount];
            for (int j = 0; j < bandCount; j++) {
                bandIndices[j] = j;
            }

            ParameterBlockJAI pb =  new ParameterBlockJAI("BandSelect");
            pb.addSource(source);
            pb.setParameter("bandIndices", bandIndices);
            RenderedOp op = JAI.create("BandSelect", pb);

            // for 2 and 4 band it's fair to expect an alpha channel
            if (bandCount == 2 || bandCount == 4) {
                assertTrue(op.getColorModel().hasAlpha());
                assertEquals(ColorModel.TRANSLUCENT, op.getColorModel().getTransparency());
            } else {
                assertFalse("Has alpha with " + bandCount + " bands", op.getColorModel().hasAlpha());
                assertEquals(ColorModel.OPAQUE, op.getColorModel().getTransparency());
            }
        }
    }

}
