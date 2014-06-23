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
package it.geosolutions.jaiext.affine;

import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.util.ImagingException;
import javax.media.jai.util.ImagingListener;

import com.sun.media.jai.util.ImageUtil;

/**
 * An OpImage class to perform (possibly filtered) affine mapping between a source and destination image.
 * 
 * The geometric relationship between source and destination pixels is defined as the following (<code>x</code> and <code>y</code> denote the source
 * pixel coordinates; <code>x'</code> and <code>y'</code> denote the destination pixel coordinates; <code>m</code> denotes the 3x2 transform matrix):
 * <ul>
 * <code>
 * x' = m[0][0] * x + m[0][1] * y + m[0][2]
 * <br>
 * y' = m[1][0] * x + m[1][1] * y + m[1][2]
 * </code>
 * </ul>
 * 
 */
abstract class AffineOpImage extends GeometricOpImage {

    /** ROI extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /**
     * Unsigned short Max Value
     */
    protected static final int USHORT_MAX_VALUE = Short.MAX_VALUE - Short.MIN_VALUE;

    /**
     * The forward AffineTransform describing the image transformation.
     */
    protected AffineTransform f_transform;

    /**
     * The inverse AffineTransform describing the image transformation.
     */
    protected AffineTransform i_transform;

    /** The Interpolation object. */
    protected Interpolation interp;

    /** Store source & padded rectangle info */
    protected Rectangle srcimg;
    
    protected Rectangle padimg;

    /** The BorderExtender */
    protected BorderExtender extender;

    /** The true writable area */
    protected Rectangle theDest;

    /** Cache the ImagingListener. */
    protected ImagingListener listener;

    /** Destination value for No Data byte */
    protected byte destinationNoDataByte = 0;

    /** Destination value for No Data ushort */
    protected short destinationNoDataUShort = 0;

    /** Destination value for No Data short */
    protected short destinationNoDataShort = 0;

    /** Destination value for No Data int */
    protected int destinationNoDataInt = 0;

    /** Destination value for No Data float */
    protected float destinationNoDataFloat = 0;

    /** Destination value for No Data double */
    protected double destinationNoDataDouble = 0;

    /** Boolean for checking if the interpolator is Nearest */
    protected boolean isNearestNew = false;

    /** Boolean for checking if the interpolator is Bilinear */
    protected boolean isBilinearNew = false;

    /** Boolean for checking if the interpolator is Bicubic */
    protected boolean isBicubicNew = false;

    /** Value indicating if roi RasterAccessor should be used on computations */
    protected boolean useROIAccessor;

    /**
     * Scanline walking : variables & constants
     */

    /** The fixed-point denominator of the fractional offsets. */
    protected static final int geom_frac_max = 0x100000;

    double m00, m10, flr_m00, flr_m10;

    double fracdx, fracdx1, fracdy, fracdy1;

    int incx, incx1, incy, incy1;

    int ifracdx, ifracdx1, ifracdy, ifracdy1;

    /**
     * Padding values for interpolation
     */
    protected int lpad, rpad, tpad, bpad;
    /** Source ROI */
    protected final ROI srcROI;
    /** ROI image */
    protected final PlanarImage srcROIImage;
    /** Random Iterator used iterating on the ROI data */
    protected final RandomIter roiIter;
    /** Rectangle containing ROI bounds */
    protected final Rectangle roiBounds;
    /** Boolean indicating if a ROI object is used */
    protected final boolean hasROI;

    /** Boolean for checking if no data range is present */
    protected boolean hasNoData = false;

    /** No Data Range */
    protected Range noData;
    
    /** Boolean indicating if No Data and ROI are not used */
    protected boolean caseA;

    /** Boolean indicating if only the ROI is used */
    protected boolean caseB;

    /** Boolean indicating if only the No Data are used */
    protected boolean caseC;

    /**
     * Computes floor(num/denom) using integer arithmetic. denom must not be equal to 0.
     */
    protected static int floorRatio(long num, long denom) {
        if (denom < 0) {
            denom = -denom;
            num = -num;
        }

        if (num >= 0) {
            return (int) (num / denom);
        } else {
            return (int) ((num - denom + 1) / denom);
        }
    }

    /**
     * Computes ceil(num/denom) using integer arithmetic. denom must not be equal to 0.
     */
    protected static int ceilRatio(long num, long denom) {
        if (denom < 0) {
            denom = -denom;
            num = -num;
        }

        if (num >= 0) {
            return (int) ((num + denom - 1) / denom);
        } else {
            return (int) (num / denom);
        }
    }

    private static ImageLayout layoutHelper(ImageLayout layout, RenderedImage source,
            AffineTransform forward_tr) {

        ImageLayout newLayout;
        if (layout != null) {
            newLayout = (ImageLayout) layout.clone();
        } else {
            newLayout = new ImageLayout();
        }

        //
        // Get sx0,sy0 coordinates and width & height of the source
        //
        float sx0 = (float) source.getMinX();
        float sy0 = (float) source.getMinY();
        float sw = (float) source.getWidth();
        float sh = (float) source.getHeight();

        //
        // The 4 points (clockwise order) are
        // (sx0, sy0), (sx0+sw, sy0)
        // (sx0, sy0+sh), (sx0+sw, sy0+sh)
        //
        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(sx0, sy0);
        pts[1] = new Point2D.Float((sx0 + sw), sy0);
        pts[2] = new Point2D.Float((sx0 + sw), (sy0 + sh));
        pts[3] = new Point2D.Float(sx0, (sy0 + sh));

        // Forward map
        forward_tr.transform(pts, 0, pts, 0, 4);

        float dx0 = Float.MAX_VALUE;
        float dy0 = Float.MAX_VALUE;
        float dx1 = -Float.MAX_VALUE;
        float dy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float) pts[i].getX();
            float py = (float) pts[i].getY();

            dx0 = Math.min(dx0, px);
            dy0 = Math.min(dy0, py);
            dx1 = Math.max(dx1, px);
            dy1 = Math.max(dy1, py);
        }

        //
        // Get the width & height of the resulting bounding box.
        // This is set on the layout
        //
        int lw = (int) (dx1 - dx0);
        int lh = (int) (dy1 - dy0);

        //
        // Set the starting integral coordinate
        // with the following criterion.
        // If it's greater than 0.5, set it to the next integral value (ceil)
        // else set it to the integral value (floor).
        //
        int lx0, ly0;

        int i_dx0 = (int) Math.floor(dx0);
        if (Math.abs(dx0 - i_dx0) <= 0.5) {
            lx0 = i_dx0;
        } else {
            lx0 = (int) Math.ceil(dx0);
        }

        int i_dy0 = (int) Math.floor(dy0);
        if (Math.abs(dy0 - i_dy0) <= 0.5) {
            ly0 = i_dy0;
        } else {
            ly0 = (int) Math.ceil(dy0);
        }

        //
        // Create the layout
        //
        newLayout.setMinX(lx0);
        newLayout.setMinY(ly0);
        newLayout.setWidth(lw);
        newLayout.setHeight(lh);

        return newLayout;
    }

    /**
     * Constructs an AffineOpImage from a RenderedImage source, AffineTransform, and Interpolation object. The image dimensions are determined by
     * forward-mapping the source bounds. The tile grid layout, SampleModel, and ColorModel are specified by the image source, possibly overridden by
     * values from the ImageLayout parameter.
     * 
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout, SampleModel, and ColorModel, or null.
     * @param transform the desired AffineTransform.
     * @param interp an Interpolation object.
     */
    public AffineOpImage(RenderedImage source, BorderExtender extender, Map config,
            ImageLayout layout, AffineTransform transform, Interpolation interp,
            double[] backgroundValues) {
        super(vectorize(source), layoutHelper(layout, source, transform), config, true, extender,
                interp, backgroundValues);

        listener = ImageUtil.getImagingListener((java.awt.RenderingHints) config);

        // store the interp and extender objects
        this.interp = interp;

        // the extender
        this.extender = extender;

        // Store the padding values
        lpad = interp.getLeftPadding();
        rpad = interp.getRightPadding();
        tpad = interp.getTopPadding();
        bpad = interp.getBottomPadding();

        //
        // Store source bounds rectangle
        // and the padded rectangle (for extension cases)
        //
        srcimg = new Rectangle(getSourceImage(0).getMinX(), getSourceImage(0).getMinY(),
                getSourceImage(0).getWidth(), getSourceImage(0).getHeight());
        padimg = new Rectangle(srcimg.x - lpad, srcimg.y - tpad, srcimg.width + lpad + rpad,
                srcimg.height + tpad + bpad);

        if (extender == null) {
            //
            // Source has to be shrunk as per interpolation
            // as a result the destination produced could
            // be different from the layout
            //

            //
            // Get sx0,sy0 coordinates and width & height of the source
            //
            float sx0 = (float) srcimg.x;
            float sy0 = (float) srcimg.y;
            float sw = (float) srcimg.width;
            float sh = (float) srcimg.height;

            //
            // get padding amounts as per interpolation
            //
            float f_lpad = (float) lpad;
            float f_rpad = (float) rpad;
            float f_tpad = (float) tpad;
            float f_bpad = (float) bpad;

            //
            // As per pixel defined to be at (0.5, 0.5)
            //
            if (!(interp instanceof InterpolationNearest)) {
                f_lpad += 0.5;
                f_tpad += 0.5;
                f_rpad += 0.5;
                f_bpad += 0.5;
            }

            //
            // Shrink the source by padding amount prior to forward map
            // This is the maxmimum available source than can be mapped
            //
            sx0 += f_lpad;
            sy0 += f_tpad;
            sw -= (f_lpad + f_rpad);
            sh -= (f_tpad + f_bpad);

            //
            // The 4 points are (x0, y0), (x0+w, y0)
            // (x0+w, y0+h), (x0, y0+h)
            //
            Point2D[] pts = new Point2D[4];
            pts[0] = new Point2D.Float(sx0, sy0);
            pts[1] = new Point2D.Float((sx0 + sw), sy0);
            pts[2] = new Point2D.Float((sx0 + sw), (sy0 + sh));
            pts[3] = new Point2D.Float(sx0, (sy0 + sh));

            // Forward map
            transform.transform(pts, 0, pts, 0, 4);

            float dx0 = Float.MAX_VALUE;
            float dy0 = Float.MAX_VALUE;
            float dx1 = -Float.MAX_VALUE;
            float dy1 = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float px = (float) pts[i].getX();
                float py = (float) pts[i].getY();

                dx0 = Math.min(dx0, px);
                dy0 = Math.min(dy0, py);
                dx1 = Math.max(dx1, px);
                dy1 = Math.max(dy1, py);
            }

            //
            // The layout is the wholly contained integer area of the
            // corresponding floating point bounding box.
            // We cannot round the corners of the floating rect because it
            // would increase the size of the rect, so we need to ceil the
            // upper corner and floor the lower corner.
            //
            int lx0 = (int) Math.ceil(dx0);
            int ly0 = (int) Math.ceil(dy0);
            int lx1 = (int) Math.floor(dx1);
            int ly1 = (int) Math.floor(dy1);

            theDest = new Rectangle(lx0, ly0, lx1 - lx0, ly1 - ly0);
        } else {
            theDest = getBounds();
        }

        // Store the inverse and forward transforms.
        try {
            this.i_transform = transform.createInverse();
        } catch (Exception e) {
            String message = JaiI18N.getString("AffineOpImage0");
            listener.errorOccurred(message, new ImagingException(message, e), this, false);
            // throw new RuntimeException(JaiI18N.getString("AffineOpImage0"));
        }
        this.f_transform = (AffineTransform) transform.clone();

        //
        // Store the incremental values used in scanline walking.
        //
        m00 = i_transform.getScaleX(); // get m00
        flr_m00 = Math.floor(m00);
        fracdx = m00 - flr_m00;
        fracdx1 = 1.0F - fracdx;
        incx = (int) flr_m00; // Movement
        incx1 = incx + 1; // along x
        ifracdx = (int) Math.round(fracdx * geom_frac_max);
        ifracdx1 = geom_frac_max - ifracdx;

        m10 = i_transform.getShearY(); // get m10
        flr_m10 = Math.floor(m10);
        fracdy = m10 - flr_m10;
        fracdy1 = 1.0F - fracdy;
        incy = (int) flr_m10; // Movement
        incy1 = incy + 1; // along y
        ifracdy = (int) Math.round(fracdy * geom_frac_max);
        ifracdy1 = geom_frac_max - ifracdy;
        
        // SG Retrieve the rendered source image and its ROI.
        Object property = source.getProperty("ROI");
        if (property instanceof ROI) {

            srcROI = (ROI) property;
            srcROIImage = srcROI.getAsImage();

            // FIXME can we use smaller bounds here?
            final Rectangle rect = new Rectangle(srcROIImage.getMinX() - lpad,
                    srcROIImage.getMinY() - tpad, srcROIImage.getWidth() + lpad + rpad,
                    srcROIImage.getHeight() + tpad + bpad);
            Raster data = srcROIImage.getExtendedData(rect,
                    BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
            roiIter = RandomIterFactory.create(data, data.getBounds(), false, true);
            roiBounds = srcROIImage.getBounds();
            hasROI = true;

        } else {
            srcROI = null;
            srcROIImage = null;
            roiBounds = null;
            roiIter = null;
            hasROI = false;
        }
    }

    /**
     * Computes the source point corresponding to the supplied point.
     * 
     * @param destPt the position in destination image coordinates to map to source image coordinates.
     * 
     * @return a <code>Point2D</code> of the same class as <code>destPt</code>.
     * 
     * @throws IllegalArgumentException if <code>destPt</code> is <code>null</code>.
     * 
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D dpt = (Point2D) destPt.clone();
        dpt.setLocation(dpt.getX() + 0.5, dpt.getY() + 0.5);

        Point2D spt = i_transform.transform(dpt, null);
        spt.setLocation(spt.getX() - 0.5, spt.getY() - 0.5);

        return spt;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     * 
     * @param sourcePt the position in source image coordinates to map to destination image coordinates.
     * 
     * @return a <code>Point2D</code> of the same class as <code>sourcePt</code>.
     * 
     * @throws IllegalArgumentException if <code>destPt</code> is <code>null</code>.
     * 
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D spt = (Point2D) sourcePt.clone();
        spt.setLocation(spt.getX() + 0.5, spt.getY() + 0.5);

        Point2D dpt = f_transform.transform(spt, null);
        dpt.setLocation(dpt.getX() - 0.5, dpt.getY() - 0.5);

        return dpt;
    }

    /**
     * Forward map the source Rectangle.
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        return f_transform.createTransformedShape(sourceRect).getBounds();
    }

    /**
     * Backward map the destination Rectangle.
     */
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        //
        // Backward map the destination to get the corresponding
        // source Rectangle
        //
        float dx0 = (float) destRect.x;
        float dy0 = (float) destRect.y;
        float dw = (float) (destRect.width);
        float dh = (float) (destRect.height);

        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(dx0, dy0);
        pts[1] = new Point2D.Float((dx0 + dw), dy0);
        pts[2] = new Point2D.Float((dx0 + dw), (dy0 + dh));
        pts[3] = new Point2D.Float(dx0, (dy0 + dh));

        i_transform.transform(pts, 0, pts, 0, 4);

        float f_sx0 = Float.MAX_VALUE;
        float f_sy0 = Float.MAX_VALUE;
        float f_sx1 = -Float.MAX_VALUE;
        float f_sy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float) pts[i].getX();
            float py = (float) pts[i].getY();

            f_sx0 = Math.min(f_sx0, px);
            f_sy0 = Math.min(f_sy0, py);
            f_sx1 = Math.max(f_sx1, px);
            f_sy1 = Math.max(f_sy1, py);
        }

        int s_x0 = 0, s_y0 = 0, s_x1 = 0, s_y1 = 0;

        // Find the bounding box of the source rectangle
        if (interp instanceof InterpolationNearest) {
            s_x0 = (int) Math.floor(f_sx0);
            s_y0 = (int) Math.floor(f_sy0);

            // Fix for bug 4485920 was to add " + 0.05" to the following
            // two lines. It should be noted that the fix was made based
            // on empirical evidence and tested thoroughly, but it is not
            // known whether this is the root cause.
            s_x1 = (int) Math.ceil(f_sx1 + 0.5);
            s_y1 = (int) Math.ceil(f_sy1 + 0.5);
        } else {
            s_x0 = (int) Math.floor(f_sx0 - 0.5);
            s_y0 = (int) Math.floor(f_sy0 - 0.5);
            s_x1 = (int) Math.ceil(f_sx1);
            s_y1 = (int) Math.ceil(f_sy1);
        }

        //
        // Return the new rectangle
        //
        return new Rectangle(s_x0, s_y0, s_x1 - s_x0, s_y1 - s_y0);
    }

    /**
     * Backward map a destination coordinate (using inverse_transform) to get the corresponding source coordinate. We need not worry about
     * interpolation here.
     * 
     * @param destPt the destination point to backward map
     * @return source point result of the backward map
     */
    public void mapDestPoint(Point2D destPoint, Point2D srcPoint) {
        i_transform.transform(destPoint, srcPoint);
    }

    public Raster computeTile(int tileX, int tileY) {
        //
        // Create a new WritableRaster to represent this tile.
        //
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        //
        // Clip output rectangle to image bounds.
        //
        Rectangle rect = new Rectangle(org.x, org.y, tileWidth, tileHeight);

        //
        // Clip destination tile against the writable destination
        // area. This is either the layout or a smaller area if
        // no extension is specified.
        //
        Rectangle destRect = rect.intersection(theDest);
        Rectangle destRect1 = rect.intersection(getBounds());
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            // No area to write
            if (setBackground)
                ImageUtil.fillBackground(dest, destRect1, backgroundValues);

            return dest;
        }

        //
        // determine the source rectangle needed to compute the destRect
        //
        Rectangle srcRect = mapDestRect(destRect, 0);
        if (extender == null) {
            srcRect = srcRect.intersection(srcimg);
        } else {
            srcRect = srcRect.intersection(padimg);
        }

        if (!(srcRect.width > 0 && srcRect.height > 0)) {
            if (setBackground)
                ImageUtil.fillBackground(dest, destRect1, backgroundValues);

            return dest;
        }

        if (!destRect1.equals(destRect)) {
            // beware that estRect1 contains destRect
            ImageUtil.fillBordersWithBackgroundValues(destRect1, destRect, dest, backgroundValues);
        }

        Raster[] sources = new Raster[1];
        Raster[] rois = new Raster[1];

        // SourceImage
        PlanarImage srcIMG = getSourceImage(0);

        // Get the source and ROI data
        if (extender == null) {
            sources[0] = srcIMG.getData(srcRect);
            if (hasROI && useROIAccessor) {
                // If roi accessor is used, the roi must be calculated only in the intersection between the source
                // image and the roi image.
                Rectangle roiComputableBounds = srcRect.intersection(srcROIImage.getBounds());
                rois[0] = srcROIImage.getData(roiComputableBounds);
                // rois[0] = srcROIImage.getExtendedData(srcRect, roiExtender);
            }
        } else {
            sources[0] = srcIMG.getExtendedData(srcRect, extender);
            if (hasROI && useROIAccessor) {
                rois[0] = srcROIImage.getExtendedData(srcRect, roiExtender);
            }
        }

        // Compute the destination tile.
        if (hasROI && useROIAccessor) {
            // Compute the destination tile.
            computeRect(sources, dest, destRect, rois);
        } else {
            computeRect(sources, dest, destRect);
        }

        // Recycle the source tile
        if (getSourceImage(0).overlapsMultipleTiles(srcRect)) {
            recycleTile(sources[0]);
        }

        return dest;
    }

    protected abstract void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect,
            Raster[] rois);

    @Override
    public synchronized void dispose() {
        if (srcROIImage != null) {
            srcROIImage.dispose();
            roiIter.done();
        }
        super.dispose();
    }

    // Scanline clipping stuff

    /**
     * Sets clipMinX, clipMaxX based on s_ix, s_iy, ifracx, ifracy, dst_min_x, and dst_min_y. Padding factors are added and subtracted from the source
     * bounds as given by src_rect_{x,y}{1,2}. For example, for nearest-neighbor interpo the padding factors should be set to (0, 0, 0, 0); for
     * bilinear, (0, 1, 0, 1); and for bicubic, (1, 2, 1, 2).
     * 
     * <p>
     * The returned Range object will be for the Integer class and will contain extrema equivalent to clipMinX and clipMaxX.
     */
    protected javax.media.jai.util.Range performScanlineClipping(float src_rect_x1,
            float src_rect_y1, float src_rect_x2, float src_rect_y2, int s_ix, int s_iy,
            int ifracx, int ifracy, int dst_min_x, int dst_max_x, int lpad, int rpad, int tpad,
            int bpad) {
        int clipMinX = dst_min_x;
        int clipMaxX = dst_max_x;

        long xdenom = incx * geom_frac_max + ifracdx;
        if (xdenom != 0) {
            long clipx1 = (long) src_rect_x1 + lpad;
            long clipx2 = (long) src_rect_x2 - rpad;

            long x1 = ((clipx1 - s_ix) * geom_frac_max - ifracx) + dst_min_x * xdenom;
            long x2 = ((clipx2 - s_ix) * geom_frac_max - ifracx) + dst_min_x * xdenom;

            // Moving backwards, switch roles of left and right edges
            if (xdenom < 0) {
                long tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            int dx1 = ceilRatio(x1, xdenom);
            clipMinX = Math.max(clipMinX, dx1);

            int dx2 = floorRatio(x2, xdenom) + 1;
            clipMaxX = Math.min(clipMaxX, dx2);
        } else {
            // xdenom == 0, all points have same x coordinate as the first
            if (s_ix < src_rect_x1 || s_ix >= src_rect_x2) {
                clipMinX = clipMaxX = dst_min_x;
                return new javax.media.jai.util.Range(Integer.class, new Integer(clipMinX),
                        new Integer(clipMaxX));
            }
        }

        long ydenom = incy * geom_frac_max + ifracdy;
        if (ydenom != 0) {
            long clipy1 = (long) src_rect_y1 + tpad;
            long clipy2 = (long) src_rect_y2 - bpad;

            long y1 = ((clipy1 - s_iy) * geom_frac_max - ifracy) + dst_min_x * ydenom;
            long y2 = ((clipy2 - s_iy) * geom_frac_max - ifracy) + dst_min_x * ydenom;

            // Moving backwards, switch roles of top and bottom edges
            if (ydenom < 0) {
                long tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            int dx1 = ceilRatio(y1, ydenom);
            clipMinX = Math.max(clipMinX, dx1);

            int dx2 = floorRatio(y2, ydenom) + 1;
            clipMaxX = Math.min(clipMaxX, dx2);
        } else {
            // ydenom == 0, all points have same y coordinate as the first
            if (s_iy < src_rect_y1 || s_iy >= src_rect_y2) {
                clipMinX = clipMaxX = dst_min_x;
            }
        }

        if (clipMinX > dst_max_x)
            clipMinX = dst_max_x;
        if (clipMaxX < dst_min_x)
            clipMaxX = dst_min_x;

        return new javax.media.jai.util.Range(Integer.class, new Integer(clipMinX), new Integer(
                clipMaxX));
    }

    /**
     * Sets s_ix, s_iy, ifracx, ifracy to their values at x == clipMinX from their initial values at x == dst_min_x.
     * 
     * <p>
     * The return Point array will contain the updated values of s_ix and s_iy in the first element and those of ifracx and ifracy in the second
     * element.
     */
    protected Point[] advanceToStartOfScanline(int dst_min_x, int clipMinX, int s_ix, int s_iy,
            int ifracx, int ifracy) {
        // Skip output up to clipMinX
        long skip = clipMinX - dst_min_x;
        long dx = ((long) ifracx + skip * ifracdx) / geom_frac_max;
        long dy = ((long) ifracy + skip * ifracdy) / geom_frac_max;
        s_ix += skip * incx + (int) dx;
        s_iy += skip * incy + (int) dy;

        long lfracx = ifracx + skip * ifracdx;
        if (lfracx >= 0) {
            ifracx = (int) (lfracx % geom_frac_max);
        } else {
            ifracx = (int) (-(-lfracx % geom_frac_max));
        }

        long lfracy = ifracy + skip * ifracdy;
        if (lfracy >= 0) {
            ifracy = (int) (lfracy % geom_frac_max);
        } else {
            ifracy = (int) (-(-lfracy % geom_frac_max));
        }

        return new Point[] { new Point(s_ix, s_iy), new Point(ifracx, ifracy) };
    }

}
