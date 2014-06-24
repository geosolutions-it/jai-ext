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
package it.geosolutions.jaiext.translate;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import com.sun.media.jai.opimage.RIFUtil;

public class TranslateCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public TranslateCRIF() {
        super("translate");
    }

    @Override
    public RenderedImage create(ParameterBlock parameterBlock, RenderingHints hints) {

        RenderedImage source = parameterBlock.getRenderedSource(0);
        float xTrans = parameterBlock.getFloatParameter(0);
        float yTrans = parameterBlock.getFloatParameter(1);

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);

        // If there is a layout hint, TranslateIntOpImage can't deal with it
        if ((Math.abs(xTrans - (int) xTrans) < TOLERANCE)
                && (Math.abs(yTrans - (int) yTrans) < TOLERANCE) && layout == null) {
            return new TranslateIntOpImage(source, hints, (int) xTrans, (int) yTrans);
        }

        return null;

    }

}
