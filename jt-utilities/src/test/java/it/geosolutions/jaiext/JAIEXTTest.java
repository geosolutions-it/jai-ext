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
package it.geosolutions.jaiext;

import static org.junit.Assert.assertTrue;
import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationCollection;
import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;
import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.testclasses.TestData;

import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.operator.ScaleDescriptor;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.mlib.MlibScaleRIF;
import com.sun.media.jai.opimage.ScaleCRIF;

public class JAIEXTTest {

    private final static String SCALE = "Scale";

    private static File newJAIFile;

    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        final File inputJAIFile = TestData.file(JAIEXTTest.class, "META-INF" + File.separator
                + "registryFile2.jaiext");
        newJAIFile = new File(inputJAIFile.getParentFile().getParentFile().getParentFile()
                .getParentFile().getParentFile().getParentFile(), "META-INF" + File.separator
                + "registryFile.jaiext");
        FileUtils.copyFile(inputJAIFile, newJAIFile);

        // Setting of the operation registry
        JAIExt.initJAIEXT();
    }

    @Test
    public void testJAIEXT() {
        // Getting the registry
        ConcurrentOperationRegistry registry = JAIExt.getRegistry();
        // Ensure that the "Scale" operation is described by the dummy descriptor
        OperationCollection operations = registry.getOperationCollection();
        OperationItem operationItem = operations.get(SCALE);
        // First check that the scale operation is present
        assertTrue(operationItem != null);
        // Using JAI-EXT for changing the descriptor from JAI to JAI-EXT
        JAIExt.registerJAIEXTDescriptor(SCALE);
        // Ensure that the "Scale" operation is described by the dummy descriptor
        operations = registry.getOperationCollection();
        operationItem = operations.get(SCALE);
        // Then check that the descriptor is an instance of the DummyScaleDescriptor class
        assertTrue(operationItem.getDescriptor().getClass()
                .isAssignableFrom(DummyScaleDescriptor.class));
        // Also check that the associated RIF is an instance of the DummyScaleCRIF class
        assertTrue(operationItem.getCurrentFactory().getClass()
                .isAssignableFrom(DummyScaleCRIF.class));

        // Using JAI-EXT for changing the descriptor from JAI-EXT to JAI
        JAIExt.registerJAIDescriptor(SCALE);
        // Then check that the descriptor is an instance of the ScaleDescriptor class
        operations = registry.getOperationCollection();
        operationItem = operations.get(SCALE);
        assertTrue(operationItem.getDescriptor().getClass().isAssignableFrom(ScaleDescriptor.class));
        // Also check that the associated RIF is an instance of the ScaleCRIF class
        assertTrue(operationItem.getCurrentFactory().getClass().isAssignableFrom(ScaleCRIF.class));

        // Using JAI-EXT class for setting the MediaLib Factory
        // Setting the JAI operation
        JAIExt.registerJAIDescriptor(SCALE);
        // Check if the Medialib acceleration is present otherwise no test is done
        if (JAIExt.isMedialibavailable()) {
            // Set the acceleration
            JAIExt.setJAIAcceleration(SCALE, true);
            // Then check that the descriptor is an instance of the ScaleDescriptor class
            operations = registry.getOperationCollection();
            operationItem = operations.get(SCALE);
            // Also check that the associated RIF is an instance of the ScaleCRIF class
            assertTrue(operationItem.getCurrentFactory().getClass()
                    .isAssignableFrom(MlibScaleRIF.class));

            // Unset the acceleration
            JAIExt.setJAIAcceleration(SCALE, false);
            // Then check that the descriptor is an instance of the ScaleDescriptor class
            operations = registry.getOperationCollection();
            operationItem = operations.get(SCALE);
            // Also check that the associated RIF is an instance of the ScaleCRIF class
            assertTrue(operationItem.getCurrentFactory().getClass()
                    .isAssignableFrom(ScaleCRIF.class));
        }
    }

    @Test
    public void testInterpolation() {
        // Getting the registry
        ConcurrentOperationRegistry registry = JAIExt.getRegistry();
        // Using JAI-EXT for changing the descriptor from JAI-EXT to JAI
        JAIExt.registerJAIDescriptor(SCALE);
        
        // NEAREST INTERPOLATION
        
        // Trying to execute the Scale operation by passing JAI-EXT interpolation objects
        // and checking if the registry is able to convert them to JAI interpolation objects
        Object[] args = new Object[1];
        ParameterBlock block = new ParameterBlock();
        // Setting of a JAIEXT interpolation object
        block.set(new InterpolationNearest(null, false, 0, 0), 0);
        // Setting of the parameterblock
        args[0] = block;
        registry.checkInterpolation(SCALE, args);
        
        // Ensure that the modified parameterblock contains a JAI interpolation object
        Object interp = block.getObjectParameter(0);
        assertTrue(interp.getClass().isAssignableFrom(javax.media.jai.InterpolationNearest.class));
        
        // BILINEAR INTERPOLATION
        
        // Trying to execute the Scale operation by passing JAI-EXT interpolation objects
        // and checking if the registry is able to convert them to JAI interpolation objects
        args = new Object[1];
        block = new ParameterBlock();
        // Setting of a JAIEXT interpolation object
        int subsampleBits = 8;
        block.set(new InterpolationBilinear(subsampleBits, null, false, 0, 0), 0);
        // Setting of the parameterblock
        args[0] = block;
        registry.checkInterpolation(SCALE, args);
        
        // Ensure that the modified parameterblock contains a JAI interpolation object
        interp = block.getObjectParameter(0);
        assertTrue(interp.getClass().isAssignableFrom(javax.media.jai.InterpolationBilinear.class));
        assertTrue(((javax.media.jai.InterpolationBilinear)interp).getSubsampleBitsH() == subsampleBits);
        
        // BICUBIC INTERPOLATION
        
        // Trying to execute the Scale operation by passing JAI-EXT interpolation objects
        // and checking if the registry is able to convert them to JAI interpolation objects
        args = new Object[1];
        block = new ParameterBlock();
        // Setting of a JAIEXT interpolation object
        block.set(new InterpolationBicubic(subsampleBits, null, false, 0, 0, true, subsampleBits), 0);
        // Setting of the parameterblock
        args[0] = block;
        registry.checkInterpolation(SCALE, args);
        
        // Ensure that the modified parameterblock contains a JAI interpolation object
        interp = block.getObjectParameter(0);
        assertTrue(interp.getClass().isAssignableFrom(javax.media.jai.InterpolationBicubic.class));
        assertTrue(((javax.media.jai.InterpolationBicubic)interp).getSubsampleBitsH() == subsampleBits);
        assertTrue(((javax.media.jai.InterpolationBicubic)interp).getPrecisionBits() == subsampleBits);
        
        // BICUBIC 2 INTERPOLATION
        
        // Trying to execute the Scale operation by passing JAI-EXT interpolation objects
        // and checking if the registry is able to convert them to JAI interpolation objects
        args = new Object[1];
        block = new ParameterBlock();
        // Setting of a JAIEXT interpolation object
        block.set(new InterpolationBicubic(subsampleBits, null, false, 0, 0, false, subsampleBits), 0);
        // Setting of the parameterblock
        args[0] = block;
        registry.checkInterpolation(SCALE, args);
        
        // Ensure that the modified parameterblock contains a JAI interpolation object
        interp = block.getObjectParameter(0);
        assertTrue(interp.getClass().isAssignableFrom(javax.media.jai.InterpolationBicubic2.class));
        assertTrue(((javax.media.jai.InterpolationBicubic2)interp).getSubsampleBitsH() == subsampleBits);
        assertTrue(((javax.media.jai.InterpolationBicubic2)interp).getPrecisionBits() == subsampleBits);
    }

    @AfterClass
    public static void fileDisposal() {
        FileUtils.deleteQuietly(newJAIFile);
        FileUtils.deleteQuietly(newJAIFile.getParentFile());
    }
}
