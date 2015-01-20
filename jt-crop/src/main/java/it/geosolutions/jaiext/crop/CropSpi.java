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
package it.geosolutions.jaiext.crop;

import java.awt.image.renderable.RenderedImageFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * OperationRegistrySpi implementation to register the "Crop" operation and its associated image
 * factories.
 * 
 * @author Andrea Aime
 */
public class CropSpi implements OperationRegistrySpi {
    
    /** Logger class used for Log any exception thrown*/
    private static final Logger LOGGER = Logger.getLogger(CropSpi.class.toString());

    /** The name of the product to which these operations belong. */
    private String productName = "it.geosolutions.jaiext";

    /** Default constructor. */
    public CropSpi() {
    }

    /**
     * Registers the Crop operation and its associated image factories across all
     * supported operation modes.
     * 
     * @param registry
     *            The registry with which to register the operations and their factories.
     */
    public void updateRegistry(OperationRegistry registry) {
        OperationDescriptor op = new CropDescriptor();
        RenderedImageFactory rif = new CropCRIF();
        // Check if the operation has already been registered
        String[] desc = registry.getOperationNames();
        boolean found = false;
        for (int i = 0; i < desc.length; i++) {
            if (desc[i].equalsIgnoreCase(op.getName())) {
                found = true;
                break;
            }
        }
        // Operation not registered
        if (!found) {
            registry.registerDescriptor(op);
            String descName = op.getName();
            registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName, productName, rif);
        }
    }
}
