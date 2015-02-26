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
package it.geosolutions.jaiext.algebra.constant;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * {@link OperationDescriptorImpl} describing the OperationConst operation
 * 
 * @author Nicola Lagomarsini geosolutions
 *
 */
public class OperationConstDescriptor extends OperationDescriptorImpl {

    public final static int OPERATION_INDEX = 0;

    public final static int ROI_INDEX = 1;

    public final static int RANGE_INDEX = 2;

    public final static int DEST_NODATA_INDEX = 4;

    public final static int CONSTANT_INDEX = 3;

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "operationConst" },
            { "LocalName", "operationConst" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description",
                    "This class executes the operation selected by the user on each pixel of the source images " },
            { "DocURL", "Not Defined" }, { "Version", "1.0" },
            { "arg0Desc", "Constant Values to Add" }, { "arg1Desc", "Operation to execute" },
            { "arg2Desc", "ROI object used" }, { "arg3Desc", "No Data Range used" },
            { "arg4Desc", "Output value for No Data" } };

    /**
     * Input Parameter name
     */
    private static final String[] paramNames = {"constants",  "operation", "roi", "noData",
            "destinationNoData" };

    /**
     * Input Parameter class
     */
    private static final Class[] paramClasses = { double[].class, Operator.class,
            javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class, Double.class };

    /**
     * Input Parameter default values
     */
    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, null, null, 0d };

    /** Constructor. */
    public OperationConstDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    @Override
    protected boolean validateParameters(String modeName, ParameterBlock args, StringBuffer msg) {
        if(modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)){
            // Check for the constants
            double[] constants = null;
            Object param = args.getObjectParameter(0);
            if (param != null) {
                if (param instanceof double[]) {
                    return true;
                } else if (param instanceof int[]) {
                    int[] paramInt = (int[]) param;
                    constants = new double[paramInt.length];
                    for (int i = 0; i < paramInt.length; i++) {
                        constants[i] = paramInt[i];
                    }
                    args.set(constants, 0);
                    return true;
                }
            }
            return false;
            
        }
        return true;
    }
    /**
     * Executes the selected operation with a constant on the input image.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source <code>RenderedImage</code> source.
     * @param constants the constants array to apply to the source
     * @param op operation to execute
     * @param roi optional ROI object
     * @param optional nodata range for checking nodata
     * @param destinationNoData value to set for destination NoData
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     */
    public static RenderedOp create(RenderedImage source, double[] constants, Operator op, ROI roi,
            Range noData, double destinationNoData, RenderingHints hints) {

        ParameterBlockJAI pb = new ParameterBlockJAI("operationConst", RenderedRegistryMode.MODE_NAME);

        pb.setSource(source, 0);

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("constants", constants);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.create("operationConst", pb, hints);
    }

    /**
     * Executes the selected operation with a constant on the input image.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     * 
     * @param source <code>RenderedImage</code> source.
     * @param constants the constants array to apply to the source
     * @param op operation to execute
     * @param roi optional ROI object
     * @param optional nodata range for checking nodata
     * @param destinationNoData value to set for destination NoData
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     */
    public static RenderableOp createRenderable(RenderableImage source, double[] constants,
            Operator op, ROI roi, Range noData, double destinationNoData, RenderingHints hints) {

        ParameterBlockJAI pb = new ParameterBlockJAI("operationConst", RenderableRegistryMode.MODE_NAME);

        pb.setSource(source, 0);

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("constants", constants);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.createRenderable("operationConst", pb, hints);
    }
}
