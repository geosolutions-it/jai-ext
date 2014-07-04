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
package it.geosolutions.jaiext;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * Dummy {@link OperationDescriptor} for the Scale operation used for testing the {@link JAIExt} and {@link ConcurrentOperationRegistry} classes.
 * 
 * @author Nicola Lagomarsini - GeoSolutions
 * 
 */
public class DummyScaleDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = { { "GlobalName", "Scale" },
            { "LocalName", "Scale" }, { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "null" }, { "DocURL", "null" }, { "Version", "0.0" }

    };

    /**
     * Modes supported by the operation
     */
    private static final String[] supportedModes = { "rendered" };

    /** Constructor. */
    public DummyScaleDescriptor() {
        super(resources, supportedModes, 1, null, null, null, null);
    }

    /** Returns <code>false</code> since renderable operation is supported but never tested. */
    public boolean isRenderableSupported() {
        return false;
    }

    /**
     * Resizes an image.
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
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Scale", RenderedRegistryMode.MODE_NAME);
        // Setting of the source
        pb.setSource("source0", source0);
        // Execution of the operation.
        return JAI.create("Scale", pb, hints);
    }
}
