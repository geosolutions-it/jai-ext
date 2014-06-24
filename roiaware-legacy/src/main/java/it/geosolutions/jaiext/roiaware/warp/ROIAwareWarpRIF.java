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
package it.geosolutions.jaiext.roiaware.warp;
import it.geosolutions.jaiext.resources.image.ImageUtilities;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.ROI;
import javax.media.jai.Warp;

import com.sun.media.jai.mlib.MlibWarpRIF;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "Warp" operation in the rendered
 * image layer.
 *
 * @since EA2
 * @see javax.media.jai.operator.WarpDescriptor
 * @see GeneralWarpOpImage
 *
 */
public class ROIAwareWarpRIF implements RenderedImageFactory {

    /** Constructor. */
    public ROIAwareWarpRIF() {}

    /**
     * Creates a new instance of warp operator according to the warp object
     * and interpolation method.
     *
     * @param paramBlock  The warp and interpolation objects.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);
        Warp warp = (Warp)paramBlock.getObjectParameter(0);
        Interpolation interp = (Interpolation)paramBlock.getObjectParameter(1);

        double[] backgroundValues = (double[])paramBlock.getObjectParameter(2);
        
        ROI roi = null;
        Object roi_= paramBlock.getObjectParameter(3);
        if(roi_ instanceof ROI) {
            roi= (ROI) roi_;
        }
        
        try {
        	 // check if we can use the native operation instead
            Rectangle sourceBounds = new Rectangle(source.getMinX(), source.getMinY(), 
                    source.getWidth(), source.getHeight());
	        if(roi == null || (ImageUtilities.isMediaLibAvailable() && (roi.getBounds().isEmpty() || roi.contains(sourceBounds)))) {
	            RenderedImage accelerated = new MlibWarpRIF().create(paramBlock, renderHints);
	            if(accelerated != null) {
	                return accelerated;
	            }
	        }
        } catch (Exception e){
        	//Eat exception and proceed with pure java approach
        }
        
        if (interp instanceof InterpolationNearest) {
            return new ROIAwareWarpNearestOpImage(source,
                                          renderHints,
                                          layout,
                                          warp,
                                          interp,
                                          backgroundValues,
                                          roi);
        } else if (interp instanceof InterpolationBilinear) {
            return new ROIAwareWarpBilinearOpImage(source, extender, renderHints,
                                           layout, warp, interp,
                                           backgroundValues,
                                           roi);
        } else {
            return new ROIAwareWarpGeneralOpImage(source, extender, renderHints,
                                          layout, warp, interp,
                                          backgroundValues,
                                          roi);
        }
    }
}
