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
package it.geosolutions.jaiext.rlookup;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * Describes the "RangeLookup" operation.
 * <p>
 * This is a variation on the JAI Lookup operation. It works with a {@linkplain RangeLookupTable} object in which each entry maps a source image value
 * range to a destination image value.
 * 
 * <p>
 * Users may also define a {@link ROI} object to use for masking image areas.
 * 
 * <p>
 * In the example below, double data values from a source image are mapped to integer values in a destination image.
 * 
 * <pre>
 * <code>
 * RenderedImage srcImage = ...
 * 
 * // RangeLookupTable is an immutable class. Use the associated Builder class
 * // to construct a new table. The type parameters define source data type 
 * // and destination type respectively
 * RangeLookupTable.Builder&lt;Double, Integer&gt; builder =
 *         new RangeLookupTable.Builder&lt;Double, Integer&gt;();
 * 
 * // Map all source values less than zero to -1
 * Range&lt;Double&gt; r = Range.create(Double.NEGATIVE_INFINITY, false, 0.0, false);
 * builder.add(r, -1);
 * 
 * // Map all source values from 0.0 (inclusive) to 1.0 (exclusive) to 1
 * r = Range.create(0.0, true, 1.0, false);
 * builder.add(r, 1);
 * 
 * // Map all source values from 1.0 (inclusive) to 2.0 (exclusive) to 2
 * r = Range.create(1.0, true, 2.0, false);
 * builder.add(r, 2);
 * 
 * // Map all source values greater than or equal to 2.0 to 3
 * r = Range.create(2.0, true, Double.POSITIVE_INFINITY, false);
 * builder.add(r, 3);
 * 
 * // Create the lookup table and the JAI operation
 * RangeLookupTable&lt;Double, Integer&gt; table = builder.build();
 * 
 * ParameterBlockJAI pb = new ParameterBlockJAI("rangelookup");
 * pb.setSource("source0", srcImage);
 * pb.setParameter("table", table);
 * RenderedImage destImage = JAI.create("rangelookup", pb);
 * </code>
 * </pre>
 * 
 * The example above uses a table with complete coverage of all source image values. It is also allowed to have a table that only covers parts of the
 * source domain. In this case, a default destination value can be specified via the "default" parameter to RangeLookup, and this will be returned for
 * all unmatched source values. If the "default" parameter is null (which is its default setting) unmatched source values will be passed through to
 * the destination image. Note that this may produce surprising results when converting a float or double source image to an integral destination
 * image due to value truncation and overflow.
 * 
 * <p>
 * <b>Parameters</b>
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * <th>Default value</th>
 * </tr>
 * <tr>
 * <td>table</td>
 * <td>RangeLookupTable</td>
 * <td>Table mapping source value ranges to destination values</td>
 * <td>NO DEFAULT</td>
 * </tr>
 * <tr>
 * <td>default</td>
 * <td>Number</td>
 * <td>Specifies the value to return for source values that do not map to any ranges in the lookup table. If null, unmatched source values will be
 * passed through to the destination image.</td>
 * <td>null (pass-through)</td>
 * </tr>
 * <tr>
 * <td>roi</td>
 * <td>javax.media.jai.ROI</td>
 * <td>Specifies a ROI to use for reducing computation area</td>
 * <td>null</td>
 * </tr>
 * </table>
 * 
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 */
public class RangeLookupDescriptor extends OperationDescriptorImpl {

    /** serialVersionUID */
    private static final long serialVersionUID = 6435703646431578734L;

    static final int TABLE_ARG = 0;

    static final int DEFAULT_ARG = 1;

    static final int ROI_ARG = 2;

    private static final String[] paramNames = { "table", "default", "roi" };

    private static final Class<?>[] paramClasses = { RangeLookupTable.class, Number.class,
            javax.media.jai.ROI.class };

    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT, (Number) null, null };

    /** Constructor. */
    public RangeLookupDescriptor() {
        super(new String[][] {
                { "GlobalName", "RLookup" },
                { "LocalName", "RLookup" },
                { "Vendor", "it.geosolutions.jaiext" },
                { "Description", "Maps source image value ranges to destination image values" },
                { "DocURL", "" },
                { "Version", "1.0" },

                {
                        "arg0Desc",
                        String.format("%s - table holding source value ranges mapped to "
                                + "destination values", paramNames[TABLE_ARG]) },
                {
                        "arg1Desc",
                        String.format("%s - value to use for unmatched source values "
                                + "(default: null to pass through source values)",
                                paramNames[DEFAULT_ARG]) },
                {
                        "arg2Desc",
                        String.format("%s - ROI used for reducing the image computations "
                                + "(default: all the image is valid)", paramNames[ROI_ARG]) }, },

        new String[] { RenderedRegistryMode.MODE_NAME }, // supported modes

                1, // number of sources

                paramNames, paramClasses, paramDefaults,

                null // valid values (none defined)
        );

    }

    /**
     * Creates a new {@link RenderedOp} with the RLookup operation applied.
     * 
     * @param table input {@link RangeLookupTable}
     * @param defaultValue Value to set for pixels outside ROI or outside of the Table Range
     * @param roi Input {@link ROI} to use in computation
     * @param hints Configuration hints
     */
    public static RenderedOp create(RenderedImage source, RangeLookupTable table,
            Number defaultValue, ROI roi, RenderingHints hints) {
        // Definition of the ParameterBlock
        ParameterBlockJAI pb = new ParameterBlockJAI("RLookup");
        // Setting the source
        pb.setSource(source, 0);
        // Setting the parameters
        pb.setParameter("table", table);
        pb.setParameter("default", defaultValue);
        pb.setParameter("roi", roi);
        // Calling the operation
        return JAI.create("RLookup", pb, hints);
    }

}
