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
package it.geosolutions.jaiext.clamp;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

public class ClampDescriptor extends OperationDescriptorImpl {

    static final Logger LOGGER = Logger.getLogger(ClampDescriptor.class.getName());

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Clampop" },
            { "LocalName", "Clampop" },
            { "Vendor", "it.geosolutions.jaiext" },
            {
                    "Description",
                    "Operation used for sets all the pixels whose value is below a low value to that low value and all the pixels whose value is above a high value to that high value" },
            { "DocURL", "Not Defined" }, { "Version", "1.0" }, { "arg0Desc", "noData values" },
            { "arg1Desc", "Destination No Data value" }, { "arg2Desc", "ROI object to use" },
            { "arg3Desc", "The lower boundary for each band" },
            { "arg4Desc", "The upper boundary for each band" }

    };

    /**
     * Input Parameter name
     */
    private static final String[] paramNames = { "noData", "destinationNoData", "roi", "low",
            "high" };

    /**
     * Input Parameter class
     */
    private static final Class[] paramClasses = { it.geosolutions.jaiext.range.Range.class,
            Double.class, javax.media.jai.ROI.class, double[].class, double[].class };

    /**
     * Input Parameter default values
     */
    private static final Object[] paramDefaults = { null, 0d, null, new double[] { 0.0 },
            new double[] { 255.0 } };

    /** Constructor. */
    public ClampDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);

    }

    protected boolean validateParameters(ParameterBlock args, StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        double[] low = (double[]) args.getObjectParameter(3);
        double[] high = (double[]) args.getObjectParameter(4);

        if (low.length < 1 || high.length < 1) {
            msg.append(getName() + " wrong parameters number");
            return false;
        }

        // each "low" value must be less than or equal to the corresponding "high" value
        int length = Math.min(low.length, high.length);
        for (int i = 0; i < length; i++) {
            if (low[i] > high[i]) {
                msg.append(getName() + " wrong parameters");
                return false;
            }
        }

        // all arrays have the same number of elements that matches the number of bands of the source image
        // or all arrays contain 1 element
        int numBands = ((RenderedImage) args.getSource(0)).getSampleModel().getNumBands();
        if (!((numBands == low.length && numBands == high.length) || (low.length == 1 && high.length == 1))) {
            msg.append(getName() + " wrong parameters number");
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param noData Array of input No Data Ranges.
     * @param destinationNoData value used by the RenderedOp for setting the output no data value.
     * @param roi
     * @param low array of low values
     * @param high array of high values
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @param sources Array of source <code>RenderedImage</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>sources</code> is <code>null</code>.
     * @throws IllegalArgumentException if a <code>source</code> is <code>null</code>.
     */
    public static RenderedOp create(Range noData, double destinationNoData, ROI roi, double[] low,
            double[] high, RenderingHints hints, RenderedImage sources) {
        // register();

        ParameterBlockJAI pb = new ParameterBlockJAI("Clampop", RenderedRegistryMode.MODE_NAME);
        if (sources == null)
            throw new IllegalArgumentException("This resource is null");

        // Setting of sources
        pb.setSource(sources, 0);

        // Setting of the parameters
        pb.setParameter("high", high);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);
        pb.setParameter("roi", roi);
        pb.setParameter("low", low);
        pb.setParameter("high", high);

        // Creation of the RenderedOp
        return JAI.create("Clampop", pb, hints);
    }

}