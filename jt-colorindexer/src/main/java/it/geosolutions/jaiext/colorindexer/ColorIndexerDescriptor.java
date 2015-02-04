/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
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
     * 
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
                        it.geosolutions.jaiext.range.Range.class, Integer.class}, // Parameter
                // classes
                new Object[] { null, null, null, 0}, // Default
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
