/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2015 GeoSolutions


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
import it.geosolutions.jaiext.testclasses.TestData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAIEXTInitializationTest {

    private final static String SCALE = "Scale";
    
    private final static String CRIF = "it.geosolutions.jaiext.DummyScaleCRIF";

    private static File newJAIFile;

    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        final File inputJAIFile = TestData.file(JAIEXTInitializationTest.class, "META-INF" + File.separator
                + "registryFile2.jaiext");
        newJAIFile = new File(inputJAIFile.getParentFile().getParentFile().getParentFile()
                .getParentFile().getParentFile().getParentFile(), "META-INF" + File.separator
                + "registryFile.jaiext");
        FileUtils.copyFile(inputJAIFile, newJAIFile);

    }

    @Test
    public void testJAIEXTReInit() {
        // Initialize JAIExt with no JAI-Ext ops

        JAIExt.initJAIEXT(false, true);
        // Getting the registry
        ConcurrentOperationRegistry registry = JAIExt.getRegistry();
        // Ensure that the "Scale" operation is described by the dummy descriptor
        OperationCollection operations = registry.getOperationCollection();
        OperationItem operationItem = operations.get(SCALE);
        // First check that the scale operation is present
        assertTrue(operationItem != null);
        assertTrue(operationItem.getVendor().equalsIgnoreCase(ConcurrentOperationRegistry.JAI_PRODUCT));

        // Also check that the associated RIF is not an instance of the JAI-Ext DummyScaleCRIF class
        assertTrue(!operationItem.getCurrentFactory().getClass().getName().equalsIgnoreCase(CRIF));

        // Reinitialize the JAIExt registry
        JAIExt.initJAIEXT(true, true);
        // Getting the registry
        registry = JAIExt.getRegistry();
        // Ensure that the "Scale" operation is described by the dummy descriptor
        operations = registry.getOperationCollection();
        operationItem = operations.get(SCALE);
        // First check that the scale operation is present
        assertTrue(operationItem != null);
        assertTrue(operationItem.getVendor().equalsIgnoreCase(ConcurrentOperationRegistry.JAIEXT_PRODUCT));

        // Also check that the associated RIF is an instance of the DummyScaleCRIF class
        assertTrue(operationItem.getCurrentFactory().getClass().getName().equalsIgnoreCase(CRIF));
    }

    @AfterClass
    public static void fileDisposal() {
        FileUtils.deleteQuietly(newJAIFile);
        FileUtils.deleteQuietly(newJAIFile.getParentFile());
    }
}
