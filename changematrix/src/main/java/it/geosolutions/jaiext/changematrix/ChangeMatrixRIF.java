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
package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * The image factory for the {@link ChangeMatrixOpImage} operation.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @since 9.0
 */
public class ChangeMatrixRIF implements RenderedImageFactory {

    /** Constructor */
    public ChangeMatrixRIF() {
    }

    /**
     * Create a new instance of {@link ChangeMatrixOpImage} in the rendered layer.
     * 
     * @param paramBlock specifies the source image and the parameters
     * @param renderHints mostly useless with this image
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        RenderedImage reference = paramBlock.getRenderedSource(0);
        if (reference.getSampleModel().getNumBands() > 1) {
            throw new IllegalArgumentException(
                    "Unable to process image with more than one band (source[0])");
        }
        final int referenceDataType = reference.getSampleModel().getDataType();
        if (referenceDataType != DataBuffer.TYPE_BYTE && referenceDataType != DataBuffer.TYPE_INT
                && referenceDataType != DataBuffer.TYPE_SHORT
                && referenceDataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException(
                    "Unable to process image (source[0]) as it has a non integer data type");
        }
        RenderedImage now = paramBlock.getRenderedSource(1);
        if (now.getSampleModel().getNumBands() > 1) {
            throw new IllegalArgumentException(
                    "Unable to process image with more than one band (source[1])");
        }
        final int nowDataType = now.getSampleModel().getDataType();
        if (nowDataType != DataBuffer.TYPE_BYTE && nowDataType != DataBuffer.TYPE_INT
                && nowDataType != DataBuffer.TYPE_SHORT && nowDataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException(
                    "Unable to process image (source[1]) as it has a non integer data type");
        }
        // same data type
        if (referenceDataType != nowDataType) {
            throw new IllegalArgumentException("Unable to process images with different data type");
        }

        // same size
        if (now.getWidth() != reference.getWidth() || now.getHeight() != reference.getHeight()) {
            throw new IllegalArgumentException(
                    "Unable to process images with different raster dimensions");
        }

        // Get the Area Image
        RenderedImage area = (RenderedImage) paramBlock
                .getObjectParameter(ChangeMatrixDescriptor.AREA_MAP_INDEX);
        if(area != null){
            if (area.getSampleModel().getNumBands() > 1) {
                throw new IllegalArgumentException(
                        "Unable to process area image with more than one band (source[0])");
            }
            final int areaDataType = area.getSampleModel().getDataType();
            if (areaDataType != DataBuffer.TYPE_DOUBLE) {
                throw new IllegalArgumentException(
                        "Unable to process area image as it has a non double data type");
            }
        }
        
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        if (layout == null){
            layout = new ImageLayout();
        }

        // result
        final ChangeMatrix result = (ChangeMatrix) paramBlock
                .getObjectParameter(ChangeMatrixDescriptor.RESULT_ARG_INDEX);

        // checks on ROI
        ROI roi = (ROI) paramBlock.getObjectParameter(ChangeMatrixDescriptor.ROI_ARG_INDEX);
        if (roi != null) {
            // ok, does the ROI intersects the reference image? if not we should throw an error
            final Rectangle bounds = PlanarImage.wrapRenderedImage(reference).getBounds();
            if (!roi.intersects(bounds)) {
                throw new IllegalArgumentException("ROI does not intersect reference image");
            } else {
                // in case the ROI intersect the reference image, let's crop it
                // but let's also check if it contains the entire image, which means, it is useless!
                if (roi.contains(bounds)) {
                    roi = null;
                } else {
                    // ROI does not contain the reference image while it intersects it, hence let's r
                    // massage the ROI to capture so.
                    roi = roi.intersect(new ROIShape(bounds));
                }

            }
        }
        
        //Pixel Multiplier value
        int pixelMultiplier = paramBlock.getIntParameter(ChangeMatrixDescriptor.PIXEL_MULTY_ARG_INDEX);
        
        return new ChangeMatrixOpImage(reference, now, area, renderHints, layout, roi, pixelMultiplier, result);
    }
}
