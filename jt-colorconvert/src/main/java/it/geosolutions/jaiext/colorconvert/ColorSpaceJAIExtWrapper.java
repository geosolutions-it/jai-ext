package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.ROI;

public class ColorSpaceJAIExtWrapper extends ColorSpaceJAIExt {

    /** Input ColorSpace Provided */
    private ColorSpaceJAI cs;

    /** Input Colorspace JAIEXt used if the input colorspace is JaiEXT */
    private ColorSpaceJAIExt csJE;

    /** Boolean used for checking if the input ColorSpace is a {@link ColorSpaceJAIExt} instance */
    boolean isJAIExt = false;

    protected ColorSpaceJAIExtWrapper(ColorSpaceJAI cs) {
        super(cs.getType(), cs.getNumComponents(), cs.isRGBPreferredIntermediary());
        this.cs = cs;
        if (cs instanceof ColorSpaceJAIExt) {
            isJAIExt = true;
            csJE = (ColorSpaceJAIExt) cs;
        }
    }

    @Override
    public WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata) {
        if (isJAIExt) {
            return csJE.fromCIEXYZ(src, srcComponentSize, dest, dstComponentSize, roi, nodata,
                    destNodata);
        }
        return cs.fromCIEXYZ(src, srcComponentSize, dest, dstComponentSize);
    }

    @Override
    public WritableRaster fromRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata) {
        if (isJAIExt) {
            return csJE.fromRGB(src, srcComponentSize, dest, dstComponentSize, roi, nodata,
                    destNodata);
        }
        return cs.fromRGB(src, srcComponentSize, dest, dstComponentSize);
    }

    @Override
    public WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata) {
        if (isJAIExt) {
            return csJE.toCIEXYZ(src, srcComponentSize, dest, dstComponentSize, roi, nodata,
                    destNodata);
        }
        return cs.toCIEXYZ(src, srcComponentSize, dest, dstComponentSize);
    }

    @Override
    public WritableRaster toRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata) {
        if (isJAIExt) {
            return csJE.toRGB(src, srcComponentSize, dest, dstComponentSize, roi, nodata,
                    destNodata);
        }
        return cs.toRGB(src, srcComponentSize, dest, dstComponentSize);
    }

    @Override
    public float[] fromCIEXYZ(float[] src) {
        return cs.fromCIEXYZ(src);
    }

    @Override
    public float[] fromRGB(float[] src) {
        return cs.fromRGB(src);
    }

    @Override
    public float[] toCIEXYZ(float[] src) {
        return cs.toCIEXYZ(src);
    }

    @Override
    public float[] toRGB(float[] src) {
        return cs.toRGB(src);
    }
}
