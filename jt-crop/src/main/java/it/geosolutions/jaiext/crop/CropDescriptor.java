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

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * Describes the "Crop" operation which performs a crop on an image, like the standard JAI Crop,
 * but does so respecting the tile scheduler and tile cache specified in the rendering hints
 * 
 * @author Andrea Aime
 */
public class CropDescriptor extends OperationDescriptorImpl {
    
    static final Logger LOGGER = Logger.getLogger(CropDescriptor.class.getName());

    private static final long serialVersionUID = -2995031215260355215L;

    static final int X_ARG = 0;

    static final int Y_ARG = 1;

    static final int WIDTH_ARG = 2;

    static final int HEIGHT_ARG = 3;
    
    static final int ROI_ARG = 4;
    
    static final int NO_DATA_ARG = 5;
    
    static final int DEST_NO_DATA_ARG = 6;

    private static final String[] paramNames = { "x", "y", "width", "height", "ROI", "NoData", "destNoData"};

    private static final Class[] paramClasses = { Float.class, Float.class, Float.class,
            Float.class, javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class, double[].class };

    private static final Object[] paramDefaults = { Float.valueOf(0), Float.valueOf(0),
            NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT,null,null, new double[]{0}};

    public CropDescriptor() {
        super(new String[][] { { "GlobalName", "Crop" }, { "LocalName", "Crop" },
                { "Vendor", "it.geosolutions.jaiext" },
                { "Description", "Crops the image to the specified bounds" },
                { "DocURL", "Not Defined" }, { "Version", "1.0.0" },

                { "arg0Desc", paramNames[0] + " (Integer, default = 0) min image X" },

                { "arg1Desc", paramNames[1] + " (Integer, default = 0) min image Y" },

                { "arg2Desc", paramNames[2] + " (Integer) image width" },

                { "arg3Desc", paramNames[3] + " (Integer) image height" },

                { "arg4Desc", paramNames[4] + " (ROI) eventual ROI object used" },

                { "arg5Desc", paramNames[5] + " (Range) eventual NoData Range used" },

                { "arg6Desc", paramNames[6] + " (double[]) eventual destination NoData values" },

        },
                new String[] { RenderedRegistryMode.MODE_NAME }, // supported modes
                1, // number of sources
                paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Manually registers the operation in the registry in case it's not already there
     */
    public static void register() {
        try {
            final OperationRegistry opr = JAI.getDefaultInstance().getOperationRegistry();
            if (opr.getDescriptor(RenderedRegistryMode.MODE_NAME, "Crop") == null) {

                final OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();

                OperationDescriptorImpl descriptor = new CropDescriptor();

                final String descName = descriptor.getName();

                registry.registerDescriptor(descriptor);
                registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName,
                        "it.geosolutions.jaiext", new CropCRIF());
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getLocalizedMessage());
            }
        }
    }

    /**
     * Performs cropping to a specified bounding box.
     * 
     * @param source <code>RenderedImage</code> source 0.
     * @param x The x origin of the cropping operation.
     * @param y The y origin of the cropping operation.
     * @param width The width of the cropping operation.
     * @param height The height of the cropping operation.
     * @param roi Eventual ROI object used for performing the crop operation.
     * @param noData Eventual No Data Range object used for checking if the No Data are present.
     * @param hints The <code>RenderingHints</code> to use, may be null
     */
    public static RenderedOp create(RenderedImage source0,
                                    Float x,
                                    Float y,
                                    Float width,
                                    Float height,
                                    ROI roi,
                                    Range noData,
                                    double[] destNoData,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb = new ParameterBlockJAI("Crop", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("x", x);
        pb.setParameter("y", y);
        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("ROI", roi);
        pb.setParameter("NoData", noData);
        pb.setParameter("destNoData", destNoData);

        return JAI.create("Crop", pb, hints);
    }

}
