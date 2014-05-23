/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008-2011 TOPP - www.openplans.org.
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

        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        if (layout == null)
            layout = new ImageLayout();

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
        
        return new ChangeMatrixOpImage(reference, now, renderHints, layout, roi, pixelMultiplier, result);
    }
}
