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
package it.geosolutions.jaiext.scale;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ScaleOpImage;

import com.sun.media.jai.mlib.MlibScaleRIF;
import com.sun.media.jai.opimage.CopyOpImage;
import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.translate.TranslateIntOpImage;
import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * @see ScaleOpImage
 */
public class ScaleCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public ScaleCRIF() {
        super("scale");
    }

    /**
     * Creates a new instance of ScaleOpImage in the rendered layer. This method satisfies the implementation of RIF.
     * 
     * @param paramBlock The source image, the X and Y scale factor, and the interpolation method for resampling.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        RenderedImage source = paramBlock.getRenderedSource(0);
        float xScale = paramBlock.getFloatParameter(0);
        float yScale = paramBlock.getFloatParameter(1);
        float xTrans = paramBlock.getFloatParameter(2);
        float yTrans = paramBlock.getFloatParameter(3);
        Interpolation interp = (Interpolation) paramBlock.getObjectParameter(4);
        // Get the Nodata Range
        Range nodata = (Range) paramBlock.getObjectParameter(7);
        nodata = RangeFactory.convert(nodata, source.getSampleModel().getDataType());

        // Get the backgroundValues
        double[] backgroundValues = (double[]) paramBlock.getObjectParameter(8);
        // SG make sure we use the ROI
        Object property = paramBlock.getObjectParameter(5);
        ROI roi = null;
        boolean useRoiAccessor = false;
        if (property instanceof ROI) {
            roi = (ROI) property;

            PlanarImage temp = PlanarImage.wrapRenderedImage(source);
            temp.setProperty("ROI", roi);
            source = temp;
            useRoiAccessor = (Boolean) paramBlock.getObjectParameter(6);
        }

        Rectangle sourceBounds = new Rectangle(source.getMinX(), source.getMinY(),
                source.getWidth(), source.getHeight());

        // Check and see if we are scaling by 1.0 in both x and y and no
        // translations. If so call the copy operation. This operation can
        // be executed only if ROI data are not defined or contain all the
        // image or are empty

        if (xScale == 1.0F && yScale == 1.0F && xTrans == 0.0F && yTrans == 0.0F
                && (roi == null || (roi.getBounds().isEmpty() || roi.contains(sourceBounds)))) {
            return new CopyOpImage(source, renderHints, layout);
        }

        // Check to see whether the operation specified is a pure
        // integer translation. If so call translate
        // If the hints contain an ImageLayout hint, then we can't use
        // TranslateIntOpImage since that can't deal with the ImageLayout hint.
        // This operation can be executed only if ROI data are not defined or
        // contain all the image or are empty
        if (xScale == 1.0F && yScale == 1.0F && (Math.abs(xTrans - (int) xTrans) < TOLERANCE)
                && (Math.abs(yTrans - (int) yTrans) < TOLERANCE) && layout == null
                && (roi == null || (roi.getBounds().isEmpty() || roi.contains(sourceBounds)))) {
            // It's an integer translate.
            return new TranslateIntOpImage(source, renderHints, (int) xTrans, (int) yTrans);
        }

        try {
            // check if we can use the native operation instead
            // Rectangle sourceBounds = new Rectangle(source.getMinX(),
            // source.getMinY(), source.getWidth(), source.getHeight());
            if ((roi == null 
                    || (ImageUtilities.isMediaLibAvailable() && (roi.getBounds().isEmpty() || roi
                            .contains(sourceBounds)))) && (nodata == null)) {
                RenderedImage accelerated = new MlibScaleRIF().create(paramBlock, renderHints);
                if (accelerated != null) {
                    return accelerated;
                }
            }
        } catch (Exception e) {
            // Eat exception and proceed with pure java approach
        }
        
        SampleModel sm = source.getSampleModel();

        boolean isBinary =  (sm instanceof MultiPixelPackedSampleModel)
                && (sm.getSampleSize(0) == 1)
                && (sm.getDataType() == DataBuffer.TYPE_BYTE
                        || sm.getDataType() == DataBuffer.TYPE_USHORT || sm.getDataType() == DataBuffer.TYPE_INT);
        
        // Check which kind of interpolation we are using
        boolean nearestInterp = interp instanceof InterpolationNearest
                || interp instanceof javax.media.jai.InterpolationNearest;
        boolean bilinearInterp = interp instanceof InterpolationBilinear
                || interp instanceof javax.media.jai.InterpolationBilinear;
        boolean bicubicInterp = interp instanceof InterpolationBicubic
                || interp instanceof javax.media.jai.InterpolationBicubic
                || interp instanceof javax.media.jai.InterpolationBicubic2;
        
        // Transformation of the interpolators JAI-->JAI-EXT
		int dataType = source.getSampleModel().getDataType();
		double destinationNoData = (backgroundValues != null && backgroundValues.length > 0)?
				backgroundValues[0] : nodata != null? nodata.getMin().doubleValue() : 0;
		if (interp instanceof javax.media.jai.InterpolationNearest) {
			interp = new InterpolationNearest(nodata, useRoiAccessor, destinationNoData,
					dataType);
		} else if (interp instanceof javax.media.jai.InterpolationBilinear) {
			interp = new InterpolationBilinear(interp.getSubsampleBitsH(), nodata,
					useRoiAccessor, destinationNoData, dataType);
		} else if (interp instanceof javax.media.jai.InterpolationBicubic ) {
			javax.media.jai.InterpolationBicubic bic = (javax.media.jai.InterpolationBicubic) interp;
			interp = new InterpolationBicubic(bic.getSubsampleBitsH(), nodata,
					useRoiAccessor, destinationNoData, dataType,true, bic.getPrecisionBits());
		} else if (interp instanceof javax.media.jai.InterpolationBicubic2 ) {
			javax.media.jai.InterpolationBicubic2 bic = (javax.media.jai.InterpolationBicubic2) interp;
			interp = new InterpolationBicubic(bic.getSubsampleBitsH(), nodata,
					useRoiAccessor, destinationNoData, dataType,false, bic.getPrecisionBits());
		}
        
		if (nearestInterp && isBinary) {
			return new ScaleGeneralOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale,
					xTrans, yTrans, useRoiAccessor, nodata, backgroundValues);
		} else if (nearestInterp && !isBinary) {
			return new ScaleNearestOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale, xTrans, yTrans,
					useRoiAccessor, nodata, backgroundValues);
		} else if (bilinearInterp && !isBinary) {
			return new ScaleBilinearOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale, xTrans, yTrans,
					useRoiAccessor, nodata, backgroundValues);
		} else if (bilinearInterp && isBinary) {
			return new ScaleGeneralOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale,
					xTrans, yTrans, useRoiAccessor, nodata, backgroundValues);
		} else if (bicubicInterp && !isBinary) {
			return new ScaleBicubicOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale, xTrans, yTrans,
					useRoiAccessor, nodata, backgroundValues);
		} else if (bicubicInterp && isBinary) {
			return new ScaleGeneralOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale,
					xTrans, yTrans, useRoiAccessor, nodata, backgroundValues);
		} else {
			return new ScaleGeneralOpImage(source, layout, renderHints,
					extender, interp, xScale, yScale, xTrans, yTrans,
					useRoiAccessor, nodata, backgroundValues);
		}
    }

    /**
     * Creates a new instance of <code>AffineOpImage</code> in the renderable layer. This method satisfies the implementation of CRIF.
     */
    public RenderedImage create(RenderContext renderContext, ParameterBlock paramBlock) {
        return paramBlock.getRenderedSource(0);
    }

    /**
     * Maps the output RenderContext into the RenderContext for the ith source. This method satisfies the implementation of CRIF.
     * 
     * @param i The index of the source image.
     * @param renderContext The renderContext being applied to the operation.
     * @param paramBlock The ParameterBlock containing the sources and the translation factors.
     * @param image The RenderableImageOp from which this method was called.
     */
    public RenderContext mapRenderContext(int i, RenderContext renderContext,
            ParameterBlock paramBlock, RenderableImage image) {

        float scale_x = paramBlock.getFloatParameter(0);
        float scale_y = paramBlock.getFloatParameter(1);
        float trans_x = paramBlock.getFloatParameter(2);
        float trans_y = paramBlock.getFloatParameter(3);

        AffineTransform scale = new AffineTransform(scale_x, 0.0, 0.0, scale_y, trans_x, trans_y);

        RenderContext RC = (RenderContext) renderContext.clone();
        AffineTransform usr2dev = RC.getTransform();
        usr2dev.concatenate(scale);
        RC.setTransform(usr2dev);
        return RC;
    }

    /**
     * Gets the bounding box for the output of <code>ScaleOpImage</code>. This method satisfies the implementation of CRIF.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {

        RenderableImage source = paramBlock.getRenderableSource(0);

        float scale_x = paramBlock.getFloatParameter(0);
        float scale_y = paramBlock.getFloatParameter(1);
        float trans_x = paramBlock.getFloatParameter(2);
        float trans_y = paramBlock.getFloatParameter(3);

        // Get the source dimensions
        float x0 = source.getMinX();
        float y0 = source.getMinY();
        float w = source.getWidth();
        float h = source.getHeight();

        // Forward map the source using x0, y0, w and h
        float d_x0 = x0 * scale_x + trans_x;
        float d_y0 = y0 * scale_y + trans_y;
        float d_w = w * scale_x;
        float d_h = h * scale_y;

        return new Rectangle2D.Float(d_x0, d_y0, d_w, d_h);
    }

}
