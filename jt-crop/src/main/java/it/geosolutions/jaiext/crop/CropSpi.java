package it.geosolutions.jaiext.crop;

import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * OperationRegistrySpi implementation to register the "Crop" operation and its associated image
 * factories.
 * 
 * @author Andrea Aime
 * @since 2.7.2
 *
 * @source $URL$
 */
public class CropSpi implements OperationRegistrySpi {

    /** The name of the product to which these operations belong. */
    private String productName = "it.geosolutions.jaiext.roiaware";

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
        registry.registerDescriptor(op);
        String descName = op.getName();

        RenderedImageFactory rif = new CropCRIF();

        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName, productName, rif);
    }
}
