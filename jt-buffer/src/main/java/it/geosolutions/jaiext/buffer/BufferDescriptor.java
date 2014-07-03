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
package it.geosolutions.jaiext.buffer;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

public class BufferDescriptor extends OperationDescriptorImpl {

    // private final static Logger LOGGER = Logger.getLogger(BufferDescriptor.class.toString());

    public static final BorderExtender DEFAULT_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /**
     * The resource strings that provide the general documentation and specify the parameter list for the "Buffer" operation.
     */
    private static final String[][] resources = { { "GlobalName", "Buffer" },
            { "LocalName", "Buffer" }, { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "Calculates sum on a buffer for each pixels." },
            { "DocURL", "Not Defined" }, { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("BufferDescriptor0") },
            { "arg1Desc", JaiI18N.getString("BufferDescriptor1") },
            { "arg2Desc", JaiI18N.getString("BufferDescriptor2") },
            { "arg3Desc", JaiI18N.getString("BufferDescriptor3") },
            { "arg4Desc", JaiI18N.getString("BufferDescriptor4") },
            { "arg5Desc", JaiI18N.getString("BufferDescriptor5") },
            { "arg6Desc", JaiI18N.getString("BufferDescriptor6") },
            { "arg7Desc", JaiI18N.getString("BufferDescriptor7") },
            { "arg8Desc", JaiI18N.getString("BufferDescriptor8") },
            { "arg9Desc", JaiI18N.getString("BufferDescriptor9") },
            { "arg10Desc", JaiI18N.getString("BufferDescriptor10") }};

    /** The parameter names for the "Warp" operation. */
    private static final String[] paramNames = { "extender", "leftP", "rightP", "topP", "bottomP",
            "rois", "nodata", "destNoData", "valueToCount", "type", "pixelArea" };

    /** The parameter class types for the "Warp" operation. */
    private static final Class[] paramClasses = { javax.media.jai.BorderExtender.class,
            java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.util.List.class,
            it.geosolutions.jaiext.range.Range.class, java.lang.Double.class,
            java.lang.Double.class, java.lang.Integer.class, java.lang.Double.class};

    /** The parameter default values for the "Warp" operation. */
    private static final Object[] paramDefaults = { DEFAULT_EXTENDER, 0, 0, 0, 0, null, null, 0d,
            null, null, 1d};

    /** Constructor. */
    public BufferDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Calculates the buffer on an Image
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param warp The warp object.
     * @param interpolation The interpolation method. May be <code>null</code>.
     * @param backgroundValues The user-specified background values. May be <code>null</code>.
     * @param sourceROI ROI object used in calculations. May be <code>null</code>.
     * @param noData NoData Range used in calculations. May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>warp</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source, BorderExtender extender, int leftPad,
            int rightPad, int topPad, int bottomPad, List<ROI> rois, Range nodata,
            double destinationNoData, Double valueToCount, Integer type, double pixelArea, RenderingHints hints) {
        
        ParameterBlockJAI pb = new ParameterBlockJAI("Buffer", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source);
        // Extender
        pb.setParameter("extender", extender);
        // Padding params
        pb.setParameter("leftP", leftPad);
        pb.setParameter("rightP", rightPad);
        pb.setParameter("topP", topPad);
        pb.setParameter("bottomP", bottomPad);
        // Rois
        pb.setParameter("rois", rois);
        // noData
        if (nodata != null) {
            pb.setParameter("nodata", nodata);
        }
        // DestinationNoData
        pb.setParameter("destNoData", destinationNoData);
        // Value to count
        if (valueToCount != null) {
            pb.setParameter("valueToCount", valueToCount);
        }
        // Change final image to double
        pb.setParameter("pixelArea", pixelArea);
        // Image data type
        if (type != null) {
            pb.setParameter("type", type);
        }

        return JAI.create("Buffer", pb, hints);
    }

}
