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
package it.geosolutions.jaiext.convolve;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;


public class ConvolveRIF implements RenderedImageFactory {

    public ConvolveRIF() {
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting the Layout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);
        // Get BorderExtender from renderHints if present.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);
        // Getting source
        RenderedImage img = pb.getRenderedSource(0);
        // Getting parameters
        KernelJAI kernel = (KernelJAI) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range) pb.getObjectParameter(2);
        double destinationNoData = pb.getDoubleParameter(3);
        boolean skipNoData = (Boolean) pb.getObjectParameter(4);

        kernel = kernel.getRotatedKernel();

        int dataType = pb.getRenderedSource(0).getSampleModel().getDataType();
        boolean dataTypeOk = (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_INT);
        if (kernel.getWidth() == 3 && kernel.getHeight() == 3 && kernel.getXOrigin() == 1
                && kernel.getYOrigin() == 1 && dataTypeOk) {
            return new Convolve3x3OpImage(img, extender, hints, l, kernel, roi, nodata,
                    destinationNoData, skipNoData);
        }

        return new ConvolveGeneralOpImage(img, extender, hints, l, kernel, roi, nodata,
                destinationNoData, skipNoData);
        // if (kernel.getWidth() == 3 && kernel.getHeight() == 3 &&
        // kernel.getXOrigin() == 1 && kernel.getYOrigin() == 1 &&
        // dataTypeOk) {
        // return new Convolve3x3OpImage(img,
        // extender,
        // hints,
        // l,
        // kernel);
        // } else if (kernel.isSeparable()) {
        // return new SeparableConvolveOpImage(img,
        // extender,
        // hints,
        // l,
        // kernel);
        //
        // } else {
        // return new ConvolveOpImage(img,
        // extender,
        // hints,
        // l,
        // kernel);
        // }
    }
}
