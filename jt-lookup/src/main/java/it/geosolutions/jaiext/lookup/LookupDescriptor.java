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
package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.PropertyGeneratorImpl;

/**
 * This class is used for retrieving an eventual ROI object passed to the source image by calling the getProperty() method.
 * This method checks if the ROI is present and if so, its bounds are intersected with the source and destination images bounds,
 * and then passed as a result. If no property was found an Undefined Property object is returned. 
 *  */

class LookupPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public LookupPropertyGenerator() {
        super(new String[] { "ROI" }, new Class[] { ROI.class }, new Class[] { RenderedOp.class });
    }

    /**
     * Returns the ROI saved as a property.
     */
    public Object getProperty(String name, Object opNode) {
        validate(name, opNode);

        if (opNode instanceof RenderedOp && name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp) opNode;

            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            RenderedImage src = pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null || property.equals(java.awt.Image.UndefinedProperty)
                    || !(property instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }

            ROI srcROI = (ROI) property;
            // Determine the effective source bounds.
            Rectangle srcBounds = null;

            srcBounds = new Rectangle(src.getMinX(), src.getMinY(), src.getWidth(), src.getHeight());

            // If necessary, clip the ROI to the effective source bounds.
            if (!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Saves the destination ROI.
            ROI dstROI = srcROI;

            // Retrieve the destination bounds.
            Rectangle dstBounds = op.getBounds();

            // If necessary, clip the warped ROI to the destination bounds.
            if (!dstBounds.contains(dstROI.getBounds())) {
                dstROI = dstROI.intersect(new ROIShape(dstBounds));
            }

            // Return the warped and possibly clipped ROI.
            return dstROI;
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Lookup" operation.
 * 
 * <p>
 * The Lookup operation takes a rendered image and a lookup table, and performs general table lookup by passing the source image through the table. If
 * ROI or No Data values are set then the lookupTable takes in account this 2 parameters. The out-of-ROI values or No Data values are set to
 * destination no data.
 * 
 * <p>
 * The source may be a single- or multi-banded image of data types <code>byte</code>, <code>ushort</code>, <code>short</code>, or <code>int</code>.
 * The lookup table may be single- or multi-banded and of any DataBuffer supported data types. The destination image must have the same data type as
 * the lookup table, and its number of bands is determined based on the number of bands of the source and the table. If the source is single-banded,
 * the destination has the same number of bands as the lookup table; otherwise, the destination has the same number of bands as the source.
 * 
 * <p>
 * If either the source or the table is single-banded and the other one is multi-banded, then the single band is applied to every band of the
 * multi-banded object. If both are multi-banded, then their corresponding bands are matched up.
 * 
 * <p>
 * The table may have a set of offset values, one for each band. This value is subtracted from the source pixel values before indexing into the table
 * data array.
 * 
 * <p>
 * It is the user's responsibility to make certain the lookup table supplied is suitable for the source image. Specifically, the table data covers the
 * entire range of the source data. Otherwise, the result of this operation is undefined.
 * 
 * <p >
 * By the nature of this operation, the destination may have a different number of bands and/or data type from the source. The
 * <code>SampleModel</code> of the destination is created in accordance with the actual lookup table used in a specific case.
 * 
 * <p>
 * The destination pixel values are defined by the pseudocode:
 * <ul>
 * <li>If the source image is single-banded and the lookup table is single- or multi-banded, then the destination image has the same number of bands
 * as the lookup table:
 * 
 * <pre>
 * dst[x][y][b] = table[b][src[x][y][0] - offsets[b]]
 * </pre>
 * 
 * </li>
 * 
 * <li>If the source image is multi-banded and the lookup table is single-banded, then the destination image has the same number of bands as the
 * source image:
 * 
 * <pre>
 * dst[x][y][b] = table[0][src[x][y][b] - offsets[0]]
 * </pre>
 * 
 * </li>
 * 
 * <li>If the source image is multi-banded and the lookup table is multi-banded, with the same number of bands as the source image, then the
 * destination image will have the same number of bands as the source image:
 * 
 * <pre>
 * dst[x][y][b] = table[b][src[x][y][b] - offsets[b]]
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * <p>
 * <table border=1>
 * <caption>Resource List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>GlobalName</td>
 * <td>Lookup</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Lookup</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Lookup operation supporting ROI and No Data.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>Not defined</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The lookup table to use.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Destination No Data used for ROI or No Data.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>ROI object used.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>No Data Range used.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>Boolean checking if ROI RasterAccessor is used.</td>
 * </tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border=1>
 * <caption>Parameter List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Class Type</th>
 * <th>Default Value</th>
 * </tr>
 * <tr>
 * <td>table</td>
 * <td>it.geosolutions.jaiext.lookup.LookupTable</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>destinationNoData</td>
 * <td>Double</td>
 * <td>0</td>
 * <tr>
 * <td>ROI</td>
 * <td>javax.media.jai.ROI</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>NoData</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>useRoiAccessor</td>
 * <td>Boolean</td>
 * <td>false</td>
 * </table>
 * </p>
 * 
 */
public class LookupDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = { { "GlobalName", "Lookup" },
            { "LocalName", "Lookup" }, { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "Lookup operation supporting ROI and No Data" },
            { "DocURL", "Not defined" }, { "Version", "1.0" },
            { "arg0Desc", "The lookup table to use" },
            { "arg1Desc", "Destination No Data used for ROI or No Data" },
            { "arg2Desc", "ROI object used" }, { "arg3Desc", "No Data Range used" },
            { "arg4Desc", "Boolean checking if ROI RasterAccessor is used" }

    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { it.geosolutions.jaiext.lookup.LookupTable.class,
            java.lang.Double.class, javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class,
            java.lang.Boolean.class };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "table", "destinationNoData", "ROI", "NoData",
            "useRoiAccessor" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { null, 0.0d, null, null, false };

    public LookupDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Lookup" operation
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new LookupPropertyGenerator();
        return pg;
    }

    /**
     * Performs a lookup operation on an integral image.
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
     * @param table The lookuptable used.
     * @param destinationNoData Destination no data used for ROI or No Data.
     * @param ROI Roi object on which the calculation are performed.
     * @param NoData No Data range used for calculation.
     * @param useRoiAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, LookupTable table,
            double destinationNoData, ROI roi, Range noData, boolean useRoiAccessor,
            RenderingHints hints) {
        // Creation of a parameterBlockJAI containing all the operation parameters
        ParameterBlockJAI pb = new ParameterBlockJAI("Lookup", RenderedRegistryMode.MODE_NAME);
        // Source image
        pb.setSource("source0", source0);
        // Image parameters
        pb.setParameter("table", table);
        pb.setParameter("destinationNoData", destinationNoData);
        pb.setParameter("ROI", roi);
        pb.setParameter("NoData", noData);
        pb.setParameter("useRoiAccessor", useRoiAccessor);
        // RenderedImage creation
        return JAI.create("Lookup", pb, hints);
    }

}
