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
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;

import org.junit.Assert;
import org.junit.Test;

import com.sun.media.jai.opimage.CopyOpImage;

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
        Assert.assertEquals(1,oneBand.getSampleModel().getNumBands());
        Assert.assertEquals(0,oneBand.getData().getSample(0, 0, 0),1E-11);
        
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
        Assert.assertEquals(1, oneBand.getSampleModel().getNumBands());
        Assert.assertEquals(0, oneBand.getData().getSample(0, 0, 0), 1E-11);
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
}
