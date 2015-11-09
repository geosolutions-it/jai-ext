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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.IntegerSequence;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.WarpOpImage;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.Rational;

import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;

/**
 * A class extending <code>WarpOpImage</code> for use by further extension classes that perform image scaling. Image scaling operations require
 * rectilinear backwards mapping and padding by the resampling filter dimensions.
 * 
 * <p>
 * When applying scale factors of scaleX, scaleY to a source image with the upper left pixel at (srcMinX, srcMinY) and width of srcWidth and height of
 * srcHeight, the resulting image is defined to have the following bounds:
 * 
 * <code>
 *       dstMinX = ceil(A), where A = srcMinX * scaleX - 0.5 + transX,
 *       dstMinY = ceil(B), where B = srcMinY * scaleY - 0.5 + transY,
 *       dstMaxX = ceil(C), where C = (srcMaxX + 1) * scaleX - 1.5 + transX
 *                          and srcMaxX = srcMinX + srcWidth - 1    
 *       dstMaxY = ceil(D), where D = (srcMaxY + 1) * scaleY - 1.5 + transY
 *                          and srcMaxY = srcMinY + srcHeight - 1    
 *       dstWidth = dstMaxX - dstMinX + 1
 *       dstHeight = dstMaxY - dstMinY + 1
 * </code>
 * 
 * <p>
 * In the case where source's upper left pixel is located is (0, 0), the formulae simplify to
 * 
 * <code>
 *       dstMinX = 0
 *       dstMinY = 0
 *       dstWidth = ceil (srcWidth * scaleX - 0.5 + transX)
 *       dstHeight = ceil (srcHeight * scaleY - 0.5 + transY)
 * </code>
 * 
 * <p>
 * In the case where the source's upper left pixel is located at (0, 0) and the scaling factors are integers, the formulae further simplify to
 * 
 * <code>
 *       dstMinX = 0
 *       dstMinY = 0
 *       dstWidth = ceil (srcWidth * scaleX + transX)
 *       dstWidth = ceil (srcHeight * scaleY + transY)
 * </code>
 * 
 * <p>
 * When interpolations which require padding the source such as Bilinear or Bicubic interpolation are specified, the source needs to be extended such
 * that it has the extra pixels needed to compute all the destination pixels. This extension is performed via the <code>BorderExtender</code> class.
 * The type of border extension can be specified as a <code>RenderingHint</code> to the <code>JAI.create</code> method.
 * 
 * <p>
 * If no <code>BorderExtender</code> is specified, the source will not be extended. The scaled image size is still calculated according to the formula
 * specified above. However since there is not enough source to compute all the destination pixels, only that subset of the destination image's pixels
 * which can be computed, will be written in the destination. The rest of the destination will be set to zeros.
 * 
 * <p>
 * It may be noted that the minX, minY, width and height hints as specified through the <code>JAI.KEY_IMAGE_LAYOUT</code> hint in the
 * <code>RenderingHints</code> object are not honored, as this operator calculates the destination image bounds itself. The other
 * <code>ImageLayout</code> hints, like tileWidth and tileHeight, however are honored.
 * 
 * It should be noted that the superclass <code>GeometricOpImage</code> automatically adds a value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given <code>configuration</code> and passes it up to its superclass constructor so that
 * geometric operations are performed on the pixel values instead of being performed on the indices into the color map for those operations whose
 * source(s) have an <code>IndexColorModel</code>. This addition will take place only if a value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been provided by the user. Note that the <code>configuration</code> Map is cloned
 * before the new hint is added to it. Regarding the value for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> <code>RenderingHints</code>, the
 * operator itself can be smart based on the parameters, i.e. while the default value for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code> for operations that extend this class, in some cases the operator could set the default.
 * 
 * @see WarpOpImage
 * @see OpImage
 * 
 */
public abstract class ScaleOpImage extends GeometricOpImage {

    /** The horizontal scale factor. */
    protected float scaleX;

    /** The vertical scale factor. */
    protected float scaleY;

    /** Thee horizontal translation factor */
    protected float transX;

    /** The vertical translation factor */
    protected float transY;

    /*** Rational representations */
    protected Rational scaleXRational, scaleYRational;

    protected long scaleXRationalNum, scaleXRationalDenom;

    protected long scaleYRationalNum, scaleYRationalDenom;

    protected Rational invScaleXRational, invScaleYRational;

    protected long invScaleXRationalNum, invScaleXRationalDenom;

    protected long invScaleYRationalNum, invScaleYRationalDenom;

    protected Rational transXRational, transYRational;

    protected long transXRationalNum, transXRationalDenom;

    protected long transYRationalNum, transYRationalDenom;

    protected static float rationalTolerance = 0.000001F;

    // Padding
    private int lpad, rpad, tpad, bpad;

    /** Source ROI */
    protected final ROI srcROI;

    /** ROI image */
    protected final PlanarImage srcROIImage;

    /** Boolean indicating if a ROI object is used */
    protected final boolean hasROI;

    /** Rectangle containing ROI bounds */
    protected final Rectangle roiBounds;

    /** Value indicating if roi RasterAccessor should be used on computations */
    protected boolean useRoiAccessor;

    /** Inverse scale value X */
    protected long invScaleXInt;

    /** Inverse scale fractional value X */
    protected long invScaleXFrac;

    /** Inverse scale value Y */
    protected long invScaleYInt;

    /** Inverse scale fractional value Y */
    protected long invScaleYFrac;

    /** Interpolator provided to the Scale operator */
    protected Interpolation interpolator = null;

    /** Boolean for checking if the image is binary or not */
    protected boolean isBinary;

    /** Subsample bits used for binary and bicubic interpolation */
    protected int subsampleBits;

    /** Value used for calculating the fractional part of the y position */
    protected int one;

    /** Interpolation kernel width */
    protected int interp_width;

    /** Interpolation kernel heigth */
    protected int interp_height;

    /** Interpolation kernel left padding */
    protected int interp_left;

    /** Interpolation kernel top padding */
    protected int interp_top;

    /** Value used for calculating the bilinear interpolation for integer dataTypes */
    protected int shift;

    /** Value used for calculating the bilinear interpolation */
    protected int shift2;

    /** The value of 0.5 scaled by 2^subsampleBits */
    protected int round2;

    /** Precision bits used for bicubic interpolation */
    protected int precisionBits;

    /** The value of 0.5 scaled by 2^precisionBits */
    protected int round;

    /** Image dataType */
    protected int dataType;

    /** No Data Range */
    protected Range noData;

    /** Boolean for checking if no data range is present */
    protected boolean hasNoData = false;

    /** Destination value for No Data byte */
    protected byte[] destinationNoDataByte;

    /** Destination value for No Data ushort */
    protected short[] destinationNoDataUShort;

    /** Destination value for No Data short */
    protected short[] destinationNoDataShort;

    /** Destination value for No Data int */
    protected int[] destinationNoDataInt;

    /** Destination value for No Data float */
    protected float[] destinationNoDataFloat;

    /** Destination value for No Data double */
    protected double[] destinationNoDataDouble;

    /** Boolean for checking if the no data is negative infinity */
    protected boolean isNegativeInf = false;

    /** Boolean for checking if the no data is positive infinity */
    protected boolean isPositiveInf = false;

    /** Boolean for checking if the no data is NaN */
    protected boolean isRangeNaN = false;

    /** Boolean for checking if the interpolator is Nearest */
    protected boolean isNearestNew = false;

    /** Boolean for checking if the interpolator is Bilinear */
    protected boolean isBilinearNew = false;

    /** Boolean for checking if the interpolator is Bicubic */
    protected boolean isBicubicNew = false;

    /** Boolean indicating if No Data and ROI are not used */
    protected boolean caseA;

    /** Boolean indicating if only the ROI is used */
    protected boolean caseB;

    /** Boolean indicating if only the No Data are used */
    protected boolean caseC;

    /** Extended ROI image*/
    protected RenderedOp srcROIImgExt;

    /** Extended source Image*/
    protected RenderedOp extendedIMG;

    /** The extended bounds used by the roi iterator  */
    protected Rectangle roiRect;

    /** ROI Border Extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    // FORMULAE FOR FORWARD MAP are derived as follows
    // Nearest
    // Minimum:
    // srcMin = floor ((dstMin + 0.5 - trans) / scale)
    // srcMin <= (dstMin + 0.5 - trans) / scale < srcMin + 1
    // srcMin*scale <= dstMin + 0.5 - trans < (srcMin + 1)*scale
    // srcMin*scale - 0.5 + trans
    // <= dstMin < (srcMin + 1)*scale - 0.5 + trans
    // Let A = srcMin*scale - 0.5 + trans,
    // Let B = (srcMin + 1)*scale - 0.5 + trans
    //
    // dstMin = ceil(A)
    //
    // Maximum:
    // Note that srcMax is defined to be srcMin + dimension - 1
    // srcMax = floor ((dstMax + 0.5 - trans) / scale)
    // srcMax <= (dstMax + 0.5 - trans) / scale < srcMax + 1
    // srcMax*scale <= dstMax + 0.5 - trans < (srcMax + 1)*scale
    // srcMax*scale - 0.5 + trans
    // <= dstMax < (srcMax+1) * scale - 0.5 + trans
    // Let float A = (srcMax + 1) * scale - 0.5 + trans
    //
    // dstMax = floor(A), if floor(A) < A, else
    // dstMax = floor(A) - 1
    // OR dstMax = ceil(A - 1)
    //
    // Other interpolations
    //
    // First the source should be shrunk by the padding that is
    // required for the particular interpolation. Then the
    // shrunk source should be forward mapped as follows:
    //
    // Minimum:
    // srcMin = floor (((dstMin + 0.5 - trans)/scale) - 0.5)
    // srcMin <= ((dstMin + 0.5 - trans)/scale) - 0.5 < srcMin+1
    // (srcMin+0.5)*scale <= dstMin+0.5-trans <
    // (srcMin+1.5)*scale
    // (srcMin+0.5)*scale - 0.5 + trans
    // <= dstMin < (srcMin+1.5)*scale - 0.5 + trans
    // Let A = (srcMin+0.5)*scale - 0.5 + trans,
    // Let B = (srcMin+1.5)*scale - 0.5 + trans
    //
    // dstMin = ceil(A)
    //
    // Maximum:
    // srcMax is defined as srcMin + dimension - 1
    // srcMax = floor (((dstMax + 0.5 - trans) / scale) - 0.5)
    // srcMax <= ((dstMax + 0.5 - trans)/scale) - 0.5 < srcMax+1
    // (srcMax+0.5)*scale <= dstMax + 0.5 - trans <
    // (srcMax+1.5)*scale
    // (srcMax+0.5)*scale - 0.5 + trans
    // <= dstMax < (srcMax+1.5)*scale - 0.5 + trans
    // Let float A = (srcMax+1.5)*scale - 0.5 + trans
    //
    // dstMax = floor(A), if floor(A) < A, else
    // dstMax = floor(A) - 1
    // OR dstMax = ceil(A - 1)
    //

    private static ImageLayout layoutHelper(RenderedImage source, float scaleX, float scaleY,
            float transX, float transY, Interpolation interp, ImageLayout il) {

        // Represent the scale factors as Rational numbers.
        // Since a value of 1.2 is represented as 1.200001 which
        // throws the forward/backward mapping in certain situations.
        // Convert the scale and translation factors to Rational numbers
        Rational scaleXRational = Rational.approximate(scaleX, rationalTolerance);

        Rational scaleYRational = Rational.approximate(scaleY, rationalTolerance);

        long scaleXRationalNum = (long) scaleXRational.num;
        long scaleXRationalDenom = (long) scaleXRational.denom;
        long scaleYRationalNum = (long) scaleYRational.num;
        long scaleYRationalDenom = (long) scaleYRational.denom;

        Rational transXRational = Rational.approximate(transX, rationalTolerance);

        Rational transYRational = Rational.approximate(transY, rationalTolerance);

        long transXRationalNum = (long) transXRational.num;
        long transXRationalDenom = (long) transXRational.denom;
        long transYRationalNum = (long) transYRational.num;
        long transYRationalDenom = (long) transYRational.denom;

        ImageLayout layout = (il == null) ? new ImageLayout() : (ImageLayout) il.clone();

        int x0 = source.getMinX();
        int y0 = source.getMinY();
        int w = source.getWidth();
        int h = source.getHeight();

        // Variables to store the calculated destination upper left coordinate
        long dx0Num, dx0Denom, dy0Num, dy0Denom;

        // Variables to store the calculated destination bottom right
        // coordinate
        long dx1Num, dx1Denom, dy1Num, dy1Denom;

        // Start calculations for destination

        dx0Num = x0;
        dx0Denom = 1;

        dy0Num = y0;
        dy0Denom = 1;

        // Formula requires srcMaxX + 1 = (x0 + w - 1) + 1 = x0 + w
        dx1Num = x0 + w;
        dx1Denom = 1;

        // Formula requires srcMaxY + 1 = (y0 + h - 1) + 1 = y0 + h
        dy1Num = y0 + h;
        dy1Denom = 1;

        dx0Num *= scaleXRationalNum;
        dx0Denom *= scaleXRationalDenom;

        dy0Num *= scaleYRationalNum;
        dy0Denom *= scaleYRationalDenom;

        dx1Num *= scaleXRationalNum;
        dx1Denom *= scaleXRationalDenom;

        dy1Num *= scaleYRationalNum;
        dy1Denom *= scaleYRationalDenom;

        // Equivalent to subtracting 0.5
        dx0Num = 2 * dx0Num - dx0Denom;
        dx0Denom *= 2;

        dy0Num = 2 * dy0Num - dy0Denom;
        dy0Denom *= 2;

        // Equivalent to subtracting 1.5
        dx1Num = 2 * dx1Num - 3 * dx1Denom;
        dx1Denom *= 2;

        dy1Num = 2 * dy1Num - 3 * dy1Denom;
        dy1Denom *= 2;

        // Adding translation factors

        // Equivalent to float dx0 += transX
        dx0Num = dx0Num * transXRationalDenom + transXRationalNum * dx0Denom;
        dx0Denom *= transXRationalDenom;

        // Equivalent to float dy0 += transY
        dy0Num = dy0Num * transYRationalDenom + transYRationalNum * dy0Denom;
        dy0Denom *= transYRationalDenom;

        // Equivalent to float dx1 += transX
        dx1Num = dx1Num * transXRationalDenom + transXRationalNum * dx1Denom;
        dx1Denom *= transXRationalDenom;

        // Equivalent to float dy1 += transY
        dy1Num = dy1Num * transYRationalDenom + transYRationalNum * dy1Denom;
        dy1Denom *= transYRationalDenom;

        // Get the integral coordinates
        int l_x0, l_y0, l_x1, l_y1;

        l_x0 = Rational.ceil(dx0Num, dx0Denom);
        l_y0 = Rational.ceil(dy0Num, dy0Denom);

        l_x1 = Rational.ceil(dx1Num, dx1Denom);
        l_y1 = Rational.ceil(dy1Num, dy1Denom);

        // Set the top left coordinate of the destination
        layout.setMinX(l_x0);
        layout.setMinY(l_y0);

        int width = l_x1 - l_x0 + 1;
        int height = l_y1 - l_y0 + 1;

        if (width < 1)
            width = 1;
        if (height < 1)
            height = 1;
        // Width and height
        layout.setWidth(width);
        layout.setHeight(height);

        return layout;
    }

    // This method precompute the integer and fractional position of every pixel
    protected final void preComputePositionsInt(Rectangle destRect, int srcRectX, int srcRectY,
            int srcPixelStride, int srcScanlineStride, int xpos[], int ypos[], int[] xfracvalues,
            int[] yfracvalues, int roiScanlineStride, int[] yposRoi) {

        // Destination Rectangle position
        final int dwidth = destRect.width;
        final int dheight = destRect.height;

        // Loop variables based on the destination rectangle to be calculated.
        final int dx = destRect.x;
        final int dy = destRect.y;

        // Initially the y source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long syNum = dy, syDenom = 1;

        // Subtract the X translation factor sy -= transY
        syNum = syNum * transYRationalDenom - transYRationalNum * syDenom;
        syDenom *= transYRationalDenom;

        // Add 0.5
        syNum = 2 * syNum + syDenom;
        syDenom *= 2;

        // Multply by invScaleX
        syNum *= invScaleYRationalNum;
        syDenom *= invScaleYRationalDenom;

        if (isBilinearNew || isBicubicNew) {
            // Subtract 0.5
            syNum = 2 * syNum - syDenom;
            syDenom *= 2;
        }

        // Separate the y source coordinate into integer and fractional part
        int srcYInt = Rational.floor(syNum, syDenom);
        long srcYFrac = syNum % syDenom;
        if (srcYInt < 0) {
            srcYFrac = syDenom + srcYFrac;
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleY
        final long commonYDenom = syDenom * invScaleYRationalDenom;
        srcYFrac *= invScaleYRationalDenom;
        final long newInvScaleYFrac = invScaleYFrac * syDenom;

        // Initially the x source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long sxNum = dx, sxDenom = 1;

        // Subtract the X translation factor sx -= transX
        sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
        sxDenom *= transXRationalDenom;

        // Add 0.5
        sxNum = 2 * sxNum + sxDenom;
        sxDenom *= 2;

        // Multply by invScaleX
        sxNum *= invScaleXRationalNum;
        sxDenom *= invScaleXRationalDenom;

        if (isBilinearNew || isBicubicNew) {
            // Subtract 0.5
            sxNum = 2 * sxNum - sxDenom;
            sxDenom *= 2;
        }

        // Separate the x source coordinate into integer and fractional part
        int srcXInt = Rational.floor(sxNum, sxDenom);
        long srcXFrac = sxNum % sxDenom;
        if (srcXInt < 0) {
            srcXFrac = sxDenom + srcXFrac;
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleX
        final long commonXDenom = sxDenom * invScaleXRationalDenom;
        srcXFrac *= invScaleXRationalDenom;
        final long newInvScaleXFrac = invScaleXFrac * sxDenom;

        // Store of the x positions
        for (int i = 0; i < dwidth; i++) {
            if (isBinary) {
                xpos[i] = srcXInt;
            } else {
                xpos[i] = (srcXInt - srcRectX) * srcPixelStride;
            }

            // Calculate the yfrac value
            if (isBilinearNew || isBicubicNew) {
                xfracvalues[i] = (int) (((1.0f * srcXFrac) / commonXDenom) * one);
            }
            // Move onto the next source pixel.

            // Add the integral part of invScaleX to the integral part
            // of srcX
            srcXInt += invScaleXInt;

            // Add the fractional part of invScaleX to the fractional part
            // of srcX
            srcXFrac += newInvScaleXFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcXFrac >= commonXDenom) {
                srcXInt += 1;
                srcXFrac -= commonXDenom;
            }
        }
        // Store of the y positions
        for (int i = 0; i < dheight; i++) {

            // Calculate the source position in the source data array.
            if (isBinary) {
                ypos[i] = srcYInt;
            } else {
                ypos[i] = (srcYInt - srcRectY) * srcScanlineStride;
            }

            // If roi is present, the y position roi value is calculated
            if (useRoiAccessor) {
                if (isBinary) {
                    yposRoi[i] = srcYInt;
                } else {
                    yposRoi[i] = (srcYInt - srcRectY) * roiScanlineStride;
                }
            }

            // Calculate the yfrac value
            if (isBilinearNew || isBicubicNew) {
                yfracvalues[i] = (int) ((1.0f * srcYFrac / commonYDenom) * one);
            }
            // yfracvalues[i] = (int) ((1.0f *srcYFrac / commonYDenom) * one);
            // Move onto the next source pixel.

            // Add the integral part of invScaleY to the integral part
            // of srcY
            srcYInt += invScaleYInt;

            // Add the fractional part of invScaleY to the fractional part
            // of srcY
            srcYFrac += newInvScaleYFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcYFrac >= commonYDenom) {
                srcYInt += 1;
                srcYFrac -= commonYDenom;
            }
        }
    }

    protected final void preComputePositionsFloat(Rectangle destRect, int srcRectX, int srcRectY,
            int srcPixelStride, int srcScanlineStride, int xpos[], int ypos[], float[] xfracvalues,
            float[] yfracvalues, int roiScanlineStride, int[] yposRoi) {

        // Destination Rectangle position
        final int dwidth = destRect.width;
        final int dheight = destRect.height;

        // Loop variables based on the destination rectangle to be calculated.
        final int dx = destRect.x;
        final int dy = destRect.y;

        // Initially the y source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long syNum = dy, syDenom = 1;

        // Subtract the X translation factor sy -= transY
        syNum = syNum * transYRationalDenom - transYRationalNum * syDenom;
        syDenom *= transYRationalDenom;

        // Add 0.5
        syNum = 2 * syNum + syDenom;
        syDenom *= 2;

        // Multply by invScaleX
        syNum *= invScaleYRationalNum;
        syDenom *= invScaleYRationalDenom;

        if (isBilinearNew || isBicubicNew) {
            // Subtract 0.5
            syNum = 2 * syNum - syDenom;
            syDenom *= 2;
        }

        // Separate the y source coordinate into integer and fractional part
        int srcYInt = Rational.floor(syNum, syDenom);
        long srcYFrac = syNum % syDenom;
        if (srcYInt < 0) {
            srcYFrac = syDenom + srcYFrac;
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleY
        final long commonYDenom = syDenom * invScaleYRationalDenom;
        srcYFrac *= invScaleYRationalDenom;
        final long newInvScaleYFrac = invScaleYFrac * syDenom;

        // Initially the x source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long sxNum = dx, sxDenom = 1;

        // Subtract the X translation factor sx -= transX
        sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
        sxDenom *= transXRationalDenom;

        // Add 0.5
        sxNum = 2 * sxNum + sxDenom;
        sxDenom *= 2;

        // Multply by invScaleX
        sxNum *= invScaleXRationalNum;
        sxDenom *= invScaleXRationalDenom;

        if (isBilinearNew || isBicubicNew) {
            // Subtract 0.5
            sxNum = 2 * sxNum - sxDenom;
            sxDenom *= 2;
        }

        // Separate the x source coordinate into integer and fractional part
        int srcXInt = Rational.floor(sxNum, sxDenom);
        long srcXFrac = sxNum % sxDenom;
        if (srcXInt < 0) {
            srcXFrac = sxDenom + srcXFrac;
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleX
        final long commonXDenom = sxDenom * invScaleXRationalDenom;
        srcXFrac *= invScaleXRationalDenom;
        final long newInvScaleXFrac = invScaleXFrac * sxDenom;

        // Store of the x positions
        for (int i = 0; i < dwidth; i++) {

            xpos[i] = (srcXInt - srcRectX) * srcPixelStride;

            // Calculate the yfrac value
            if (isBilinearNew || isBicubicNew) {
                xfracvalues[i] = (1.0f * srcXFrac) / commonXDenom;
            }
            // xfracvalues[i] = (1.0f * srcXFrac) / commonXDenom;
            // Move onto the next source pixel.

            // Add the integral part of invScaleX to the integral part
            // of srcX
            srcXInt += invScaleXInt;

            // Add the fractional part of invScaleX to the fractional part
            // of srcX
            srcXFrac += newInvScaleXFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcXFrac >= commonXDenom) {
                srcXInt += 1;
                srcXFrac -= commonXDenom;
            }
        }
        // Store of the y positions
        for (int i = 0; i < dheight; i++) {

            // Calculate the source position in the source data array.
            ypos[i] = (srcYInt - srcRectY) * srcScanlineStride;

            // If roi is present, the y position roi value is calculated
            if (useRoiAccessor) {
                yposRoi[i] = (srcYInt - srcRectY) * roiScanlineStride;

            }

            // Calculate the yfrac value
            if (isBilinearNew || isBicubicNew) {
                yfracvalues[i] = 1.0f * srcYFrac / commonYDenom;
            }
            // yfracvalues[i] = (float) srcYFrac / (float) commonYDenom;

            // Move onto the next source pixel.

            // Add the integral part of invScaleY to the integral part
            // of srcY
            srcYInt += invScaleYInt;

            // Add the fractional part of invScaleY to the fractional part
            // of srcY
            srcYFrac += newInvScaleYFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcYFrac >= commonYDenom) {
                srcYInt += 1;
                srcYFrac -= commonYDenom;
            }
        }
    }

    private static Map<Object, Object> configHelper(RenderedImage source,
            Map<Object, Object> configuration, Interpolation interp) {

        Map<Object, Object> config = configuration;

        // If source image is binary and the interpolation is either nearest
        // or bilinear, do not expand
        if (ImageUtil.isBinary(source.getSampleModel())
                && (interp == null || interp instanceof InterpolationNearest || interp instanceof InterpolationBilinear)) {

            // Set to false
            if (configuration == null) {
                config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
            } else {

                // If the user specified a value for this hint, we don't
                // want to change that
                if (!config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL)) {
                    RenderingHints hints = new RenderingHints(null);
                    // This is effectively a clone of configuration
                    hints.putAll(configuration);
                    config = hints;
                    config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.TRUE);
                }
            }
        }

        return config;
    }

    /**
     * Constructs a <code>ScaleOpImage</code> from a <code>RenderedImage</code> source, an optional <code>BorderExtender</code>, x and y scale and
     * translation factors, and an <code>Interpolation</code> object. The image dimensions are determined by forward-mapping the source bounds, and
     * are passed to the superclass constructor by means of the <code>layout</code> parameter. Other fields of the layout are passed through
     * unchanged. If <code>layout</code> is <code>null</code>, a new <code>ImageLayout</code> will be constructor to hold the bounds information.
     * 
     * Note that the scale factors are represented internally as Rational numbers in order to workaround inexact device specific representation of
     * floating point numbers. For instance the floating point number 1.2 is internally represented as 1.200001, which can throw the calculations off
     * during a forward/backward map.
     * 
     * <p>
     * The Rational approximation is valid upto the sixth decimal place.
     * 
     * @param layout an <code>ImageLayout</code> optionally containing the tile grid layout, <code>SampleModel</code>, and <code>ColorModel</code>, or
     *        <code>null</code>.
     * @param source a <code>RenderedImage</code>.
     * @param configuration Configurable attributes of the image including configuration variables indexed by <code>RenderingHints.Key</code>s and
     *        image properties indexed by <code>String</code>s or <code>CaselessStringKey</code>s. This is simply forwarded to the superclass
     *        constructor.
     * @param cobbleSources a boolean indicating whether <code>computeRect</code> expects contiguous sources.
     * @param extender a <code>BorderExtender</code>, or <code>null</code>.
     * @param interp an <code>Interpolation</code> object to use for resampling.
     * @param scaleX scale factor along x axis.
     * @param scaleY scale factor along y axis.
     * @param transX translation factor along x axis.
     * @param transY translation factor along y axis.
     * 
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     * @throws IllegalArgumentException if combining the source bounds with the layout parameter results in negative output width or height.
     * 
     * @since JAI 1.1
     */
    public ScaleOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            boolean cobbleSources, BorderExtender extender, Interpolation interp, float scaleX,
            float scaleY, float transX, float transY, boolean useRoiAccessor, double[] backgroundValues) {
        super(vectorize(source), // vectorize() checks for null source.
                layoutHelper(source, scaleX, scaleY, transX, transY, interp, layout), configHelper(
                        source, configuration, interp), cobbleSources, extender, interp, backgroundValues);

        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.transX = transX;
        this.transY = transY;

        // Represent the scale factors as Rational numbers.
        // Since a value of 1.2 is represented as 1.200001 which
        // throws the forward/backward mapping in certain situations.
        // Convert the scale and translation factors to Rational numbers
        this.scaleXRational = Rational.approximate(scaleX, rationalTolerance);
        this.scaleYRational = Rational.approximate(scaleY, rationalTolerance);

        this.scaleXRationalNum = (long) this.scaleXRational.num;
        this.scaleXRationalDenom = (long) this.scaleXRational.denom;
        this.scaleYRationalNum = (long) this.scaleYRational.num;
        this.scaleYRationalDenom = (long) this.scaleYRational.denom;

        this.transXRational = Rational.approximate(transX, rationalTolerance);
        this.transYRational = Rational.approximate(transY, rationalTolerance);

        this.transXRationalNum = (long) this.transXRational.num;
        this.transXRationalDenom = (long) this.transXRational.denom;
        this.transYRationalNum = (long) this.transYRational.num;
        this.transYRationalDenom = (long) this.transYRational.denom;

        // Inverse scale factors as Rationals
        invScaleXRational = new Rational(scaleXRational);
        invScaleXRational.invert();
        invScaleYRational = new Rational(scaleYRational);
        invScaleYRational.invert();
        invScaleXRationalNum = invScaleXRational.num;
        invScaleXRationalDenom = invScaleXRational.denom;
        invScaleYRationalNum = invScaleYRational.num;
        invScaleYRationalDenom = invScaleYRational.denom;

        lpad = interp.getLeftPadding();
        rpad = interp.getRightPadding();
        tpad = interp.getTopPadding();
        bpad = interp.getBottomPadding();

        if (extender == null) {

            // Get the source dimensions
            int x0 = source.getMinX();
            int y0 = source.getMinY();
            int w = source.getWidth();
            int h = source.getHeight();

            // The first source pixel (x0, y0)
            long dx0Num, dx0Denom, dy0Num, dy0Denom;

            // The first pixel (x1, y1) that is just outside the source
            long dx1Num, dx1Denom, dy1Num, dy1Denom;

            if (interp instanceof InterpolationNearest) {

                // First point inside the source
                dx0Num = x0;
                dx0Denom = 1;

                dy0Num = y0;
                dy0Denom = 1;

                // First point outside source
                // for nearest, x1 = x0 + w, y1 = y0 + h
                // since anything >= a but < (a+1) maps to a for nearest

                // Equivalent to float d_x1 = x0 + w
                dx1Num = x0 + w;
                dx1Denom = 1;

                // Equivalent to float d_y1 = y0 + h
                dy1Num = y0 + h;
                dy1Denom = 1;

            } else {

                // First point inside the source
                dx0Num = 2 * x0 + 1; // x0 + 0.5
                dx0Denom = 2;

                dy0Num = 2 * y0 + 1; // y0 + 0.5
                dy0Denom = 2;

                // for other interpolations, x1 = x0+w+0.5, y1 = y0+h+0.5
                // as derived in the formulae derivation above.
                dx1Num = 2 * x0 + 2 * w + 1;
                dx1Denom = 2;

                dy1Num = 2 * y0 + 2 * h + 1;
                dy1Denom = 2;

                // Equivalent to x0 += lpad;
                dx0Num += dx0Denom * lpad;
                // Equivalent to y0 += tpad;
                dy0Num += dy0Denom * tpad;

                // Equivalent to x1 -= rpad;
                dx1Num -= dx1Denom * rpad;
                // Equivalent to y1 += bpad;
                dy1Num -= dy1Denom * bpad;
            }

            // Forward map the first and last source points

            // Equivalent to float d_x0 = x0 * scaleX;
            dx0Num *= scaleXRationalNum;
            dx0Denom *= scaleXRationalDenom;

            // Add the X translation factor d_x0 += transX
            dx0Num = dx0Num * transXRationalDenom + transXRationalNum * dx0Denom;
            dx0Denom *= transXRationalDenom;

            // Equivalent to float d_y0 = y0 * scaleY;
            dy0Num *= scaleYRationalNum;
            dy0Denom *= scaleYRationalDenom;

            // Add the Y translation factor, float d_y0 += transY
            dy0Num = dy0Num * transYRationalDenom + transYRationalNum * dy0Denom;
            dy0Denom *= transYRationalDenom;

            // Equivalent to float d_x1 = x1 * scaleX;
            dx1Num *= scaleXRationalNum;
            dx1Denom *= scaleXRationalDenom;

            // Add the X translation factor d_x1 += transX
            dx1Num = dx1Num * transXRationalDenom + transXRationalNum * dx1Denom;
            dx1Denom *= transXRationalDenom;

            // Equivalent to float d_y1 = y1 * scaleY;
            dy1Num *= scaleYRationalNum;
            dy1Denom *= scaleYRationalDenom;

            // Add the Y translation factor, float d_y1 += transY
            dy1Num = dy1Num * transYRationalDenom + transYRationalNum * dy1Denom;
            dy1Denom *= transYRationalDenom;

            // Get the integral coordinates

            int l_x0, l_y0, l_x1, l_y1;

            // Subtract 0.5 from dx0, dy0
            dx0Num = 2 * dx0Num - dx0Denom;
            dx0Denom *= 2;

            dy0Num = 2 * dy0Num - dy0Denom;
            dy0Denom *= 2;

            l_x0 = Rational.ceil(dx0Num, dx0Denom);
            l_y0 = Rational.ceil(dy0Num, dy0Denom);

            // Subtract 0.5 from dx1, dy1
            dx1Num = 2 * dx1Num - dx1Denom;
            dx1Denom *= 2;

            dy1Num = 2 * dy1Num - dy1Denom;
            dy1Denom *= 2;

            l_x1 = (int) Rational.floor(dx1Num, dx1Denom);
            // l_x1 must be less than but not equal to (dx1Num/dx1Denom)
            if ((l_x1 * dx1Denom) == dx1Num) {
                l_x1 -= 1;
            }

            l_y1 = (int) Rational.floor(dy1Num, dy1Denom);
            if (l_y1 * dy1Denom == dy1Num) {
                l_y1 -= 1;
            }

            computableBounds = new Rectangle(l_x0, l_y0, (l_x1 - l_x0 + 1), (l_y1 - l_y0 + 1));
        } else {
            // If extender is present we can write the entire destination.
            computableBounds = getBounds();
            // Padding of the input image in order to avoid the call of the getExtendedData() method.
            // Extend the Source image
            ParameterBlock pb = new ParameterBlock();
            pb.setSource(source, 0);
            pb.set(lpad, 0);
            pb.set(rpad, 1);
            pb.set(tpad, 2);
            pb.set(bpad, 3);
            pb.set(extender, 4);
            extendedIMG = JAI.create("border", pb);
        }

        // SG Retrieve the rendered source image and its ROI.
        Object property = source.getProperty("ROI");
        if (property instanceof ROI) {

            srcROI = (ROI) property;
            srcROIImage = srcROI.getAsImage();

            // FIXME can we use smaller bounds here?
            roiRect = new Rectangle(srcROIImage.getMinX() - lpad,
                    srcROIImage.getMinY() - tpad, srcROIImage.getWidth() + lpad + rpad,
                    srcROIImage.getHeight() + tpad + bpad);
            // Padding of the input ROI image in order to avoid the call of the getExtendedData() method
            // Calculate the padding between the ROI and the source image padded
            roiBounds = srcROIImage.getBounds();
            Rectangle srcRect = new Rectangle(source.getMinX()  - lpad , source.getMinY() - tpad,
                    source.getWidth() + lpad + rpad, source.getHeight() + tpad + bpad);
            int deltaX0 = (roiBounds.x - srcRect.x);
            int leftP = deltaX0 > 0 ? deltaX0 : 0;
            int deltaY0 = (roiBounds.y - srcRect.y);
            int topP = deltaY0 > 0 ? deltaY0 : 0;
            int deltaX1 = (srcRect.x + srcRect.width - roiBounds.x - roiBounds.width);
            int rightP = deltaX1 > 0 ? deltaX1 : 0;
            int deltaY1 = (srcRect.y + srcRect.height - roiBounds.y - roiBounds.height);
            int bottomP = deltaY1 > 0 ? deltaY1 : 0;
            // Extend the ROI image
            ParameterBlock pb = new ParameterBlock();
            pb.setSource(srcROIImage, 0);
            pb.set(leftP, 0);
            pb.set(rightP, 1);
            pb.set(topP, 2);
            pb.set(bottomP, 3);
            pb.set(roiExtender, 4);
            srcROIImgExt = JAI.create("border", pb);
            hasROI = true;
            this.useRoiAccessor = useRoiAccessor;
        } else {
            srcROI = null;
            srcROIImage = null;
            roiBounds = null;
            hasROI = false;
        }
    }

    /**
     * Computes the position in the specified source that best matches the supplied destination image position.
     * 
     * <p>
     * The implementation in this class returns the value of <code>pt</code> in the following code snippet:
     * 
     * <pre>
     * Point2D pt = (Point2D) destPt.clone();
     * pt.setLocation((destPt.getX() - transX + 0.5) / scaleX - 0.5, (destPt.getY() - transY + 0.5)
     *         / scaleY - 0.5);
     * </pre>
     * 
     * Subclasses requiring different behavior should override this method.
     * </p>
     * 
     * @param destPt the position in destination image coordinates to map to source image coordinates.
     * @param sourceIndex the index of the source image.
     * 
     * @return a <code>Point2D</code> of the same class as <code>destPt</code>.
     * 
     * @throws IllegalArgumentException if <code>destPt</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is non-zero.
     * 
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex != 0) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Point2D pt = (Point2D) destPt.clone();

        pt.setLocation((destPt.getX() - transX + 0.5) / scaleX - 0.5,
                (destPt.getY() - transY + 0.5) / scaleY - 0.5);

        return pt;
    }

    /**
     * Computes the position in the destination that best matches the supplied source image position.
     * 
     * <p>
     * The implementation in this class returns the value of <code>pt</code> in the following code snippet:
     * 
     * <pre>
     * Point2D pt = (Point2D) sourcePt.clone();
     * pt.setLocation(scaleX * (sourcePt.getX() + 0.5) + transX - 0.5, scaleY * (sourcePt.getY() + 0.5)
     *         + transY - 0.5);
     * </pre>
     * 
     * Subclasses requiring different behavior should override this method.
     * </p>
     * 
     * @param sourcePt the position in source image coordinates to map to destination image coordinates.
     * @param sourceIndex the index of the source image.
     * 
     * @return a <code>Point2D</code> of the same class as <code>sourcePt</code>.
     * 
     * @throws IllegalArgumentException if <code>sourcePt</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is non-zero.
     * 
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex != 0) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Point2D pt = (Point2D) sourcePt.clone();

        pt.setLocation(scaleX * (sourcePt.getX() + 0.5) + transX - 0.5, scaleY
                * (sourcePt.getY() + 0.5) + transY - 0.5);

        return pt;
    }

    /**
     * Returns the minimum bounding box of the region of the destination to which a particular <code>Rectangle</code> of the specified source will be
     * mapped.
     * 
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     * 
     * @return a <code>Rectangle</code> indicating the destination bounding box, or <code>null</code> if the bounding box is unknown.
     * 
     * @throws IllegalArgumentException if <code>sourceIndex</code> is negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is <code>null</code>.
     * 
     * @since JAI 1.1
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {

        if (sourceRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        // Get the source dimensions
        int x0 = sourceRect.x;
        int y0 = sourceRect.y;
        int w = sourceRect.width;
        int h = sourceRect.height;

        // Variables to represent the first pixel inside the destination.
        long dx0Num, dx0Denom, dy0Num, dy0Denom;

        // Variables to represent the last destination pixel.
        long dx1Num, dx1Denom, dy1Num, dy1Denom;

        if (interp instanceof InterpolationNearest) {

            // First point inside the source
            dx0Num = x0;
            dx0Denom = 1;

            dy0Num = y0;
            dy0Denom = 1;

            // First point outside source
            // for nearest, x1 = x0 + w, y1 = y0 + h
            // since anything >= a and < a+1 maps to a for nearest, since
            // we use floor to calculate the integral source position

            // Equivalent to float d_x1 = x0 + w
            dx1Num = x0 + w;
            dx1Denom = 1;

            // Equivalent to float d_y1 = y0 + h
            dy1Num = y0 + h;
            dy1Denom = 1;

        } else {

            // First point inside the source (x0 + 0.5, y0 + 0.5)
            dx0Num = 2 * x0 + 1;
            dx0Denom = 2;

            dy0Num = 2 * y0 + 1;
            dy0Denom = 2;

            // for other interpolations, x1 = x0 + w + 0.5, y1 = y0 + h + 0.5
            // as derived in the formulae derivation above.
            dx1Num = 2 * x0 + 2 * w + 1;
            dx1Denom = 2;

            dy1Num = 2 * y0 + 2 * h + 1;
            dy1Denom = 2;
        }

        // Forward map first and last source positions

        // Equivalent to float d_x0 = x0 * scaleX;
        dx0Num = dx0Num * scaleXRationalNum;
        dx0Denom *= scaleXRationalDenom;

        // Equivalent to float d_y0 = y0 * scaleY;
        dy0Num = dy0Num * scaleYRationalNum;
        dy0Denom *= scaleYRationalDenom;

        // Equivalent to float d_x1 = x1 * scaleX;
        dx1Num = dx1Num * scaleXRationalNum;
        dx1Denom *= scaleXRationalDenom;

        // Equivalent to float d_y1 = y1 * scaleY;
        dy1Num = dy1Num * scaleYRationalNum;
        dy1Denom *= scaleYRationalDenom;

        // Add the translation factors.

        // Equivalent to float d_x0 += transX
        dx0Num = dx0Num * transXRationalDenom + transXRationalNum * dx0Denom;
        dx0Denom *= transXRationalDenom;

        // Equivalent to float d_y0 += transY
        dy0Num = dy0Num * transYRationalDenom + transYRationalNum * dy0Denom;
        dy0Denom *= transYRationalDenom;

        // Equivalent to float d_x1 += transX
        dx1Num = dx1Num * transXRationalDenom + transXRationalNum * dx1Denom;
        dx1Denom *= transXRationalDenom;

        // Equivalent to float d_y1 += transY
        dy1Num = dy1Num * transYRationalDenom + transYRationalNum * dy1Denom;
        dy1Denom *= transYRationalDenom;

        // Get the integral coordinates
        int l_x0, l_y0, l_x1, l_y1;

        // Subtract 0.5 from dx0, dy0
        dx0Num = 2 * dx0Num - dx0Denom;
        dx0Denom *= 2;

        dy0Num = 2 * dy0Num - dy0Denom;
        dy0Denom *= 2;

        l_x0 = Rational.ceil(dx0Num, dx0Denom);
        l_y0 = Rational.ceil(dy0Num, dy0Denom);

        // Subtract 0.5 from dx1, dy1
        dx1Num = 2 * dx1Num - dx1Denom;
        dx1Denom *= 2;

        dy1Num = 2 * dy1Num - dy1Denom;
        dy1Denom *= 2;

        l_x1 = (int) Rational.floor(dx1Num, dx1Denom);
        if ((l_x1 * dx1Denom) == dx1Num) {
            l_x1 -= 1;
        }

        l_y1 = (int) Rational.floor(dy1Num, dy1Denom);
        if ((l_y1 * dy1Denom) == dy1Num) {
            l_y1 -= 1;
        }

        return new Rectangle(l_x0, l_y0, (l_x1 - l_x0 + 1), (l_y1 - l_y0 + 1));
    }

    /**
     * Returns the minimum bounding box of the region of the specified source to which a particular <code>Rectangle</code> of the destination will be
     * mapped.
     * 
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     * 
     * @return a <code>Rectangle</code> indicating the source bounding box, or <code>null</code> if the bounding box is unknown.
     * 
     * @throws IllegalArgumentException if <code>sourceIndex</code> is negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is <code>null</code>.
     * 
     * @since JAI 1.1
     */
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {

        if (destRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        // Get the destination rectangle coordinates and dimensions
        int x0 = destRect.x;
        int y0 = destRect.y;
        int w = destRect.width;
        int h = destRect.height;

        // Variables that will eventually hold the source pixel
        // positions which are the result of the backward map
        long sx0Num, sx0Denom, sy0Num, sy0Denom;

        // First destination point that will be backward mapped
        // will be dx0 + 0.5, dy0 + 0.5
        sx0Num = (x0 * 2 + 1);
        sx0Denom = 2;

        sy0Num = (y0 * 2 + 1);
        sy0Denom = 2;

        // The last destination pixel to be backward mapped will be
        // dx0 + w - 1 + 0.5, dy0 + h - 1 + 0.5 i.e.
        // dx0 + w - 0.5, dy0 + h - 0.5
        long sx1Num, sx1Denom, sy1Num, sy1Denom;

        // Equivalent to float sx1 = dx0 + dw - 0.5;
        sx1Num = 2 * x0 + 2 * w - 1;
        sx1Denom = 2;

        // Equivalent to float sy1 = dy0 + dh - 0.5;
        sy1Num = 2 * y0 + 2 * h - 1;
        sy1Denom = 2;

        // Subtract the translation factors.
        sx0Num = sx0Num * transXRationalDenom - transXRationalNum * sx0Denom;
        sx0Denom *= transXRationalDenom;

        sy0Num = sy0Num * transYRationalDenom - transYRationalNum * sy0Denom;
        sy0Denom *= transYRationalDenom;

        sx1Num = sx1Num * transXRationalDenom - transXRationalNum * sx1Denom;
        sx1Denom *= transXRationalDenom;

        sy1Num = sy1Num * transYRationalDenom - transYRationalNum * sy1Denom;
        sy1Denom *= transYRationalDenom;

        // Backward map both the destination positions

        // Equivalent to float sx0 = x0 / scaleX;
        sx0Num *= invScaleXRationalNum;
        sx0Denom *= invScaleXRationalDenom;

        sy0Num *= invScaleYRationalNum;
        sy0Denom *= invScaleYRationalDenom;

        sx1Num *= invScaleXRationalNum;
        sx1Denom *= invScaleXRationalDenom;

        sy1Num *= invScaleYRationalNum;
        sy1Denom *= invScaleYRationalDenom;

        int s_x0 = 0, s_y0 = 0, s_x1 = 0, s_y1 = 0;
        if (interp instanceof InterpolationNearest) {

            // Floor sx0, sy0
            s_x0 = Rational.floor(sx0Num, sx0Denom);
            s_y0 = Rational.floor(sy0Num, sy0Denom);

            // Equivalent to (int)Math.floor(sx1)
            s_x1 = Rational.floor(sx1Num, sx1Denom);

            // Equivalent to (int)Math.floor(sy1)
            s_y1 = Rational.floor(sy1Num, sy1Denom);

        } else {
            // For all other interpolations

            // Equivalent to (int) Math.floor(sx0 - 0.5)
            s_x0 = Rational.floor(2 * sx0Num - sx0Denom, 2 * sx0Denom);

            // Equivalent to (int) Math.floor(sy0 - 0.5)
            s_y0 = Rational.floor(2 * sy0Num - sy0Denom, 2 * sy0Denom);

            // Calculate the last source point
            s_x1 = Rational.floor(2 * sx1Num - sx1Denom, 2 * sx1Denom);

            // Equivalent to (int)Math.ceil(sy1 - 0.5)
            s_y1 = Rational.floor(2 * sy1Num - sy1Denom, 2 * sy1Denom);
        }

        return new Rectangle(s_x0, s_y0, (s_x1 - s_x0 + 1), (s_y1 - s_y0 + 1));
    }

    /**
     * Computes a tile. If source cobbling was requested at construction time, the source tile boundaries are overlayed onto the destination, cobbling
     * is performed for areas that intersect multiple source tiles, and <code>computeRect(Raster[], WritableRaster, Rectangle)</code> is called for
     * each of the resulting regions. Otherwise, <code>computeRect(PlanarImage[], WritableRaster,
     * Rectangle)</code> is called once to compute the entire active area of the tile.
     * 
     * <p>
     * The image bounds may be larger than the bounds of the source image. In this case, samples for which there are no corresponding sources are set
     * to zero.
     * 
     * <p>
     * The following steps are performed in order to compute the tile:
     * <ul>
     * <li>The destination tile is backward mapped to compute the needed source.
     * <li>This source is then split on tile boundaries to produce rectangles that do not cross tile boundaries.
     * <li>These source rectangles are then forward mapped to produce destination rectangles, and the computeRect method is called for each
     * corresponding pair of source and destination rectangles.
     * <li>For higher order interpolations, some source cobbling across tile boundaries does occur.
     * </ul>
     * 
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * 
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {

        if (!cobbleSources) {
            return super.computeTile(tileX, tileY);
        }

        // X and Y coordinate of the pixel pixel of the tile.
        int orgX = tileXToX(tileX);
        int orgY = tileYToY(tileY);

        // Create a new WritableRaster to represent this tile.
        WritableRaster dest = createWritableRaster(sampleModel, new Point(orgX, orgY));

        Rectangle rect = new Rectangle(orgX, orgY, tileWidth, tileHeight);

        // Clip dest rectangle against the part of the destination
        // rectangle that can be written.
        Rectangle destRect = rect.intersection(computableBounds);
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            // If empty rectangle, return empty tile.
            return dest;
        }

        // Get the source rectangle required to compute the destRect
        Rectangle srcRect = mapDestRect(destRect, 0);
        Raster[] sources = new Raster[1];

        // Split the source on tile boundaries.
        // Get the new pairs of src & dest Rectangles

        // The tileWidth and tileHeight of the source image
        // may differ from this tileWidth and tileHeight.
        PlanarImage source0 = getSourceImage(0);

        IntegerSequence srcXSplits = new IntegerSequence();
        IntegerSequence srcYSplits = new IntegerSequence();
        source0.getSplits(srcXSplits, srcYSplits, srcRect);

        // Note that the getExtendedData() method is not called because the input images are padded.
        // For each image there is a check if the rectangle is contained inside the source image;
        // if this not happen, the data is taken from the padded image.

        if (srcXSplits.getNumElements() == 1 && srcYSplits.getNumElements() == 1) {

            // If the source is fully contained within
            // a tile there is no need to split it any further.
            if (extender == null) {
                sources[0] = source0.getData(srcRect);
            } else {
                if(source0.getBounds().contains(srcRect)){
                    sources[0] = source0.getData(srcRect);
                }else{
                    sources[0] = extendedIMG.getData(srcRect);
                }

            }
            // Compute the destination tile.
            computeRect(sources, dest, destRect);
        } else {
            // Source Rect straddles 2 or more tiles

            // Get Source Tilewidth & height
            int srcTileWidth = source0.getTileWidth();
            int srcTileHeight = source0.getTileHeight();

            srcYSplits.startEnumeration();
            while (srcYSplits.hasMoreElements()) {
                // Along Y TileBoundaries
                int ysplit = srcYSplits.nextElement();

                srcXSplits.startEnumeration();
                while (srcXSplits.hasMoreElements()) {
                    // Along X TileBoundaries
                    int xsplit = srcXSplits.nextElement();

                    // Construct a pseudo tile for intersection purposes
                    Rectangle srcTile = new Rectangle(xsplit, ysplit, srcTileWidth, srcTileHeight);

                    // Intersect the tile with the source Rectangle
                    Rectangle newSrcRect = srcRect.intersection(srcTile);

                    //
                    // The new source rect could be of size less than or equal
                    // to the interpolation kernel dimensions. In which case
                    // the forward map produces null destination rectangles.
                    // Hence we need to deal with these cases in the manner
                    // implemented below (grow the source before the map. This
                    // would result in source cobbling). Not an issue for
                    // Nearest-Neighbour.
                    //
                    if (!(interp instanceof InterpolationNearest)) {

                        if (newSrcRect.width <= interp.getWidth()) {

                            //
                            // Need to forward map this source rectangle.
                            // Since we need a minimum of 2 * (lpad + rpad + 1)
                            // in order to process, we contsruct a source
                            // rectangle of that size, forward map and then
                            // process the resulting destination rectangle.
                            //
                            Rectangle wSrcRect = new Rectangle();
                            Rectangle wDestRect;

                            wSrcRect.x = newSrcRect.x;
                            wSrcRect.y = newSrcRect.y - tpad - 1;
                            wSrcRect.width = 2 * (lpad + rpad + 1);
                            wSrcRect.height = newSrcRect.height + bpad + tpad + 2;

                            wSrcRect = wSrcRect.intersection(source0.getBounds());

                            wDestRect = mapSourceRect(wSrcRect, 0);

                            //
                            // Make sure this destination rectangle is
                            // within the bounds of our original writable
                            // destination rectangle
                            //
                            wDestRect = wDestRect.intersection(destRect);

                            if ((wDestRect.width > 0) && (wDestRect.height > 0)) {
                                // Do the operations with these new rectangles
                                if (extender == null) {
                                    sources[0] = source0.getData(wSrcRect);
                                } else {
                                    if(source0.getBounds().contains(srcRect)){
                                        sources[0] = source0.getData(srcRect);
                                    } else {
                                        sources[0] = extendedIMG.getData(srcRect);
                                    }
                                }

                                // Compute the destination tile.
                                computeRect(sources, dest, wDestRect);
                            }
                        }

                        if (newSrcRect.height <= interp.getHeight()) {

                            //
                            // Need to forward map this source rectangle.
                            // Since we need a minimum of 2 * (tpad + bpad + 1)
                            // in order to process, we create a source
                            // rectangle of that size, forward map and then
                            // process the resulting destinaltion rectangle
                            //
                            Rectangle hSrcRect = new Rectangle();
                            Rectangle hDestRect;

                            hSrcRect.x = newSrcRect.x - lpad - 1;
                            hSrcRect.y = newSrcRect.y;
                            hSrcRect.width = newSrcRect.width + lpad + rpad + 2;
                            hSrcRect.height = 2 * (tpad + bpad + 1);

                            hSrcRect = hSrcRect.intersection(source0.getBounds());

                            hDestRect = mapSourceRect(hSrcRect, 0);

                            //
                            // Make sure this destination rectangle is
                            // within the bounds of our original writable
                            // destination rectangle
                            //
                            hDestRect = hDestRect.intersection(destRect);

                            if ((hDestRect.width > 0) && (hDestRect.height > 0)) {
                                // Do the operations with these new rectangles
                                if (extender == null) {
                                    sources[0] = source0.getData(hSrcRect);
                                } else {
                                    if(source0.getBounds().contains(srcRect)){
                                        sources[0] = source0.getData(srcRect);
                                    }else{
                                        sources[0] = extendedIMG.getData(srcRect);
                                    }
                                }

                                // Compute the destination tile.
                                computeRect(sources, dest, hDestRect);
                            }
                        }
                    }

                    // Process source rectangle
                    if ((newSrcRect.width > 0) && (newSrcRect.height > 0)) {

                        // Forward map this source rectangle
                        // to get the destination rectangle
                        Rectangle newDestRect = mapSourceRect(newSrcRect, 0);

                        // Make sure this destination rectangle is
                        // within the bounds of our original writable
                        // destination rectangle
                        newDestRect = newDestRect.intersection(destRect);

                        if ((newDestRect.width > 0) && (newDestRect.height > 0)) {

                            // Do the operations with these new rectangles
                            if (extender == null) {
                                sources[0] = source0.getData(newSrcRect);
                            } else {
                                if(source0.getBounds().contains(srcRect)){
                                    sources[0] = source0.getData(srcRect);
                                }else{
                                    sources[0] = extendedIMG.getData(srcRect);
                                }
                            }

                            // Compute the destination tile.
                            computeRect(sources, dest, newDestRect);
                        }

                        //
                        // Since mapSourceRect (forward map) shrinks the
                        // source rectangle before the map, there are areas
                        // of this rectangle which never get mapped.
                        //
                        // These occur at the tile boundaries between
                        // rectangles. The following algorithm handles
                        // these edge conditions.
                        //
                        // The cases :
                        // Right edge
                        // Bottom edge
                        // Lower Right Corner
                        //

                        if (!(interp instanceof InterpolationNearest)) {
                            Rectangle RTSrcRect = new Rectangle();
                            Rectangle RTDestRect;

                            // Right Edge
                            RTSrcRect.x = newSrcRect.x + newSrcRect.width - 1 - rpad - lpad;
                            RTSrcRect.y = newSrcRect.y;

                            //
                            // The amount of src not used from the end of
                            // the first tile is rpad + 0.5. The amount
                            // not used from the beginning of the next tile
                            // is lpad + 0.5. Since we cannot start mapping
                            // at 0.5, we need to get the area of the half
                            // pixel on both sides. So we get another 0,5
                            // from both sides. In total (rpad + 0.5 +
                            // 0.5) + (lpad + 0.5 + 0.5)
                            // Since mapSourceRect subtracts rpad + 0.5 and
                            // lpad + 0.5 from the source before the
                            // forward map, we need to add that in.
                            //
                            RTSrcRect.width = 2 * (lpad + rpad + 1);
                            RTSrcRect.height = newSrcRect.height;

                            RTDestRect = mapSourceRect(RTSrcRect, 0);

                            // Clip this against the whole destrect
                            RTDestRect = RTDestRect.intersection(destRect);

                            // RTSrcRect may be out of image bounds;
                            // map one more time
                            RTSrcRect = mapDestRect(RTDestRect, 0);

                            if (RTDestRect.width > 0 && RTDestRect.height > 0) {
                                // Do the operations with these new rectangles
                                if (extender == null) {
                                    sources[0] = source0.getData(RTSrcRect);
                                } else {
                                    if(source0.getBounds().contains(srcRect)){
                                        sources[0] = source0.getData(srcRect);
                                    }else{
                                        sources[0] = extendedIMG.getData(srcRect);
                                    }

                                }

                                // Compute the destination tile.
                                computeRect(sources, dest, RTDestRect);
                            }

                            // Bottom Edge
                            Rectangle BTSrcRect = new Rectangle();
                            Rectangle BTDestRect;

                            BTSrcRect.x = newSrcRect.x;
                            BTSrcRect.y = newSrcRect.y + newSrcRect.height - 1 - bpad - tpad;

                            //
                            // The amount of src not used from the end of
                            // the first tile is tpad + 0.5. The amount
                            // not used from the beginning of the next tile
                            // is bpad + 0.5. Since we cannot start mapping
                            // at 0.5, we need to get the area of the half
                            // pixel on both sides. So we get another 0,5
                            // from both sides. In total (tpad + 0.5 +
                            // 0.5) + (bpad + 0.5 + 0.5)
                            // Since mapSourceRect subtracts tpad + 0.5 and
                            // bpad + 0.5 from the source before the
                            // forward map, we need to add that in.
                            //
                            BTSrcRect.width = newSrcRect.width;
                            BTSrcRect.height = 2 * (tpad + bpad + 1);

                            BTDestRect = mapSourceRect(BTSrcRect, 0);

                            // Clip this against the whole destrect
                            BTDestRect = BTDestRect.intersection(destRect);

                            // BTSrcRect maybe out of bounds
                            // map one more time
                            BTSrcRect = mapDestRect(BTDestRect, 0);
                            // end

                            if (BTDestRect.width > 0 && BTDestRect.height > 0) {

                                // Do the operations with these new rectangles
                                if (extender == null) {
                                    sources[0] = source0.getData(BTSrcRect);
                                } else {
                                    if(source0.getBounds().contains(srcRect)){
                                        sources[0] = source0.getData(srcRect);
                                    }else{
                                        sources[0] = extendedIMG.getData(srcRect);
                                    }
                                }

                                // Compute the destination tile.
                                computeRect(sources, dest, BTDestRect);
                            }

                            // Lower Right Area
                            Rectangle LRTSrcRect = new Rectangle();
                            Rectangle LRTDestRect;

                            LRTSrcRect.x = newSrcRect.x + newSrcRect.width - 1 - rpad - lpad;
                            LRTSrcRect.y = newSrcRect.y + newSrcRect.height - 1 - bpad - tpad;

                            // Comment forthcoming
                            LRTSrcRect.width = 2 * (rpad + lpad + 1);
                            LRTSrcRect.height = 2 * (tpad + bpad + 1);

                            LRTDestRect = mapSourceRect(LRTSrcRect, 0);

                            // Clip this against the whole destrect
                            LRTDestRect = LRTDestRect.intersection(destRect);

                            // LRTSrcRect may still be out of bounds
                            LRTSrcRect = mapDestRect(LRTDestRect, 0);

                            if (LRTDestRect.width > 0 && LRTDestRect.height > 0) {
                                // Do the operations with these new rectangles
                                if (extender == null) {
                                    sources[0] = source0.getData(LRTSrcRect);
                                } else {
                                    if(source0.getBounds().contains(srcRect)){
                                        sources[0] = source0.getData(srcRect);
                                    }else{
                                        sources[0] = extendedIMG.getData(srcRect);
                                    }
                                }

                                // Compute the destination tile.
                                computeRect(sources, dest, LRTDestRect);
                            }
                        }
                    }
                }
            }
        }

        // Return the written destination raster
        return dest;
    }

    @Override
    public synchronized void dispose() {
        if (srcROIImage != null) {
            srcROIImage.dispose();
        }
        if(srcROIImgExt != null) {
            srcROIImgExt.dispose();
        }
        super.dispose();
    }

}
