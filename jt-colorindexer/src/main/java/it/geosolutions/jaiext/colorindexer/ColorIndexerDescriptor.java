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
package it.geosolutions.jaiext.colorindexer;

// J2SE dependencies
import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * Clone of GeoTools color invertion made work against a {@link ColorIndexer}
 * 
 * @source $URL$
 */
public class ColorIndexerDescriptor extends OperationDescriptorImpl {

    static final Logger LOGGER = Logger.getLogger(ColorIndexerCRIF.class.toString());

    /**
     * UID
     */
    private static final long serialVersionUID = 4951347100540806326L;

    /**
     * The operation name, which is {@value} .
     */
    public static final String OPERATION_NAME = "ColorIndexer";

    /**
     * Constructs the descriptor.
     */
    /**
     * Constructs the descriptor.
     */
    public ColorIndexerDescriptor() {
        super(
                new String[][] {
                        { "GlobalName", OPERATION_NAME },
                        { "LocalName", OPERATION_NAME },
                        { "Vendor", "it.geosolutions.jaiext" },
                        { "Description",
                                "Produce a paletted image from an RGB or RGBA image using a provided palette." },
                        { "DocURL", "http://www.geo-solutions.it/" }, // TODO:
                        // provides more accurate URL
                        { "Version", "1.0" }, { "arg0Desc", "Indexer." }, { "arg1Desc", "ROI." },
                        { "arg2Desc", "NoData." }, { "arg3Desc", "DestinationNoData." } },
                new String[] { RenderedRegistryMode.MODE_NAME }, 1, // Supported
                // modes
                new String[] { "Indexer", "roi", "nodata", "destNoData" }, // Parameter
                // names
                new Class[] { ColorIndexer.class, javax.media.jai.ROI.class,
                        it.geosolutions.jaiext.range.Range.class, Integer.class }, // Parameter
                // classes
                new Object[] { null, null, null, 0 }, // Default
                // values
                null // Valid parameter values
        );
    }

    /**
     * Returns {@code true} if this operation supports the specified mode, and is capable of handling the given input source(s) for the specified
     * mode.
     * 
     * @param modeName The mode name (usually "Rendered").
     * @param param The parameter block for the operation to performs.
     * @param message A buffer for formatting an error message if any.
     */
    protected boolean validateSources(final String modeName, final ParameterBlock param,
            final StringBuffer message) {
        if (super.validateSources(modeName, param, message)) {
            for (int i = param.getNumSources(); --i >= 0;) {
                final Object source = param.getSource(i);
                if (!(source instanceof RenderedImage)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the parameters are valids. This implementation check that the number of bands in the source src1 is equals to the
     * number of bands of source src2.
     * 
     * @param modeName The mode name (usually "Rendered").
     * @param param The parameter block for the operation to performs.
     * @param message A buffer for formatting an error message if any.
     */
    protected boolean validateParameters(final String modeName, final ParameterBlock param,
            final StringBuffer message) {
        if (!super.validateParameters(modeName, param, message)) {
            return false;
        }
        if (!(param.getObjectParameter(0) instanceof ColorIndexer))
            return false;
        return true;
    }

    /**
     * Create a new {@link RenderedOp} instance based on the "ColorIndexer" operation
     * 
     * @param source Input image
     * @param indexer {@link ColorIndexer} instance to use for the operation
     * @param roi Input {@link ROI} used for reducing computation area
     * @param nodata NoData {@link Range} used for masking NoData values
     * @param destNoData Value to set for the background
     * @param hints Optional configuration hints
     * @return A new {@link RenderedOp} instance after executing ColorIndexer operation
     */
    public static RenderedOp create(RenderedImage source, ColorIndexer indexer, ROI roi,
            Range nodata, Integer destNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI(OPERATION_NAME, RenderedRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source);
        // Setting parameters
        pb.setParameter("Indexer", indexer);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);

        return JAI.create(OPERATION_NAME, pb, hints);
    }
}
