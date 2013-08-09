/*
 * $RCSfile: ScaleCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:42 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.scale;

import it.geosolutions.jaiext.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.interpolators.InterpolationNearestNew;
import it.geosolutions.jaiext.utilities.ImageUtilities;

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
import javax.media.jai.InterpolationNearest;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ScaleOpImage;

import com.sun.media.jai.mlib.MlibScaleRIF;
import com.sun.media.jai.opimage.CopyOpImage;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * @see ScaleOpImage
 */
public class ScaleDataCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public ScaleDataCRIF() {
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
            if (roi == null
                    || (ImageUtilities.isMediaLibAvailable() && (roi.getBounds().isEmpty() || roi
                            .contains(sourceBounds)))) {
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
        
        
        if (interp instanceof InterpolationNearestNew && isBinary) {
            return new ScaleDataOpImage(source, layout, renderHints, extender,
                    (InterpolationNearestNew) interp, xScale, yScale, xTrans, yTrans,
                    useRoiAccessor);
        }else if((interp instanceof InterpolationNearestNew)&& !isBinary){
            return new ScaleNearestOpImage(source, layout, renderHints, extender, 
                    interp, xScale, yScale, xTrans, yTrans, useRoiAccessor);
        }else if (interp instanceof InterpolationBilinearNew) {
            return new ScaleDataOpImage(source, layout, renderHints, extender,
                    (InterpolationBilinearNew) interp, xScale, yScale, xTrans, yTrans,
                    useRoiAccessor);
        } else if (interp instanceof InterpolationBicubicNew) {
            return new ScaleDataOpImage(source, layout, renderHints, extender,
                    (InterpolationBicubicNew) interp, xScale, yScale, xTrans, yTrans,
                    useRoiAccessor);
        } else {
            return new ScaleDataOpImage(source, layout, renderHints, extender, interp, xScale,
                    yScale, xTrans, yTrans, useRoiAccessor);
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
        float x0 = (float) source.getMinX();
        float y0 = (float) source.getMinY();
        float w = (float) source.getWidth();
        float h = (float) source.getHeight();

        // Forward map the source using x0, y0, w and h
        float d_x0 = x0 * scale_x + trans_x;
        float d_y0 = y0 * scale_y + trans_y;
        float d_w = w * scale_x;
        float d_h = h * scale_y;

        return new Rectangle2D.Float(d_x0, d_y0, d_w, d_h);
    }

}
