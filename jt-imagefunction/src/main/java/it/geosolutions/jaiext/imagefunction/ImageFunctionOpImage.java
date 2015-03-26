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
package it.geosolutions.jaiext.imagefunction;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageFunction;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.SourcelessOpImage;

import com.sun.media.jai.util.ImageUtil;

/**
 * An OpImage class to generate an image from a functional description.
 */
public class ImageFunctionOpImage extends SourcelessOpImage {

    /**
     * Constant indicating that the inner random iterators must pre-calculate an array of the image positions
     */
    public static final boolean ARRAY_CALC = true;

    /**
     * Constant indicating that the inner random iterators must cache the current tile position
     */
    public static final boolean TILE_CACHED = true;

    /** The functional description of the image. */
    protected ImageFunctionJAIEXT function;

    /** The X scale factor. */
    protected float xScale;

    /** The Y scale factor. */
    protected float yScale;

    /** The X translation. */
    protected float xTrans;

    /** The Y translation. */
    protected float yTrans;

    /** ROI used for reducing calculation area */
    private ROI roi;

    /** NoData used for checking if input pixels are NoData */
    private Range nodata;

    /** Boolean indicating if ROI is present */
    private boolean hasROI;

    /** Value to set as output NoData */
    private float destNoData;

    /** {@link PlanarImage} containing ROI data */
    private PlanarImage roiImage;

    /** Rectangle defining ROI bounds */
    private Rectangle roiBounds;

    /** Helper function for creating a suitable sample model for the final image */
    private static SampleModel sampleModelHelper(int numBands, ImageLayout layout) {
        SampleModel sampleModel;
        if (layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            sampleModel = layout.getSampleModel(null);

            if (sampleModel.getNumBands() != numBands) {
                throw new RuntimeException(JaiI18N.getString("ImageFunctionRIF0"));
            }
        } else { // Create a SampleModel.
            // Use a dummy width and height, OpImage will fix them
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, 1, 1,
                    numBands);
        }

        return sampleModel;
    }

    /**
     * Constructs an ImageFunctionOpImage.
     * 
     * @param width The output image width.
     * @param height The output image height.
     */
    public ImageFunctionOpImage(ImageFunction function, int minX, int minY, int width, int height,
            float xScale, float yScale, float xTrans, float yTrans, ROI roi, Range nodata,
            float destNoData, Map config, ImageLayout layout) {
        super(layout, config, sampleModelHelper(function.getNumElements()
                * (function.isComplex() ? 2 : 1), layout), minX, minY, width, height);

        // Cache the parameters.
        this.function = function instanceof ImageFunctionJAIEXT ? (ImageFunctionJAIEXT) function
                : new ImageFunctionJAIEXTWrapper(function);
        this.xScale = xScale;
        this.yScale = yScale;
        this.xTrans = xTrans;
        this.yTrans = yTrans;

        // Check if ROI is present
        hasROI = roi != null;
        if (hasROI) {
            this.roiBounds = roi.getBounds();
            this.roi = roi;
        }
        // Check on NoData
        if (nodata != null) {
            this.nodata = RangeFactory.convertToFloatRange(nodata);
        }

        this.destNoData = destNoData;
    }

    /**
     * Compute a Rectangle of output data based on the ImageFunction. Note that the sources parameter is not used.
     */
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Cache some info.
        int dataType = sampleModel.getTransferType();
        int numBands = sampleModel.getNumBands();

        // ROI check
        boolean roiDisjointTile = false;
        ROI roiTile = null;

        // If a ROI is present, then only the part contained inside the current
        // tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = destRect.getBounds();
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            }
        }

        if (roiDisjointTile) {
            double[] bkg = new double[function.isComplex() ? 2 : 1];
            bkg[0] = destNoData;
            if (function.isComplex()) {
                bkg[1] = destNoData;
            }
            ImageUtil.fillBackground(dest, destRect, bkg);
            return;
        }

        // Allocate the actual data memory.
        int length = width * height;
        Object data;
        if (dataType == DataBuffer.TYPE_DOUBLE) {
            data = function.isComplex() ? (Object) new double[2][length]
                    : (Object) new double[length];
        } else {
            data = function.isComplex() ? (Object) new float[2][length]
                    : (Object) new float[length];
        }

        if (dataType == DataBuffer.TYPE_DOUBLE) {
            double[] real = function.isComplex() ? ((double[][]) data)[0] : ((double[]) data);
            double[] imag = function.isComplex() ? ((double[][]) data)[1] : null;

            int element = 0;
            for (int band = 0; band < numBands; band++) {
                function.getElements(xScale * (destRect.x - xTrans),
                        yScale * (destRect.y - yTrans), xScale, yScale, destRect.width,
                        destRect.height, element++, real, imag, destRect, roiTile, nodata,
                        destNoData);
                dest.setSamples(destRect.x, destRect.y, destRect.width, destRect.height, band,
                        (double[]) real);
                if (function.isComplex()) {
                    dest.setSamples(destRect.x, destRect.y, destRect.width, destRect.height,
                            ++band, imag);
                }
            } // for (band ...
        } else { // not double precision
            float[] real = function.isComplex() ? ((float[][]) data)[0] : ((float[]) data);
            float[] imag = function.isComplex() ? ((float[][]) data)[1] : null;

            int element = 0;
            for (int band = 0; band < numBands; band++) {
                function.getElements(xScale * (destRect.x - xTrans),
                        yScale * (destRect.y - yTrans), xScale, yScale, destRect.width,
                        destRect.height, element++, real, imag, destRect, roiTile, nodata,
                        destNoData);
                dest.setSamples(destRect.x, destRect.y, destRect.width, destRect.height, band, real);
                if (function.isComplex()) {
                    dest.setSamples(destRect.x, destRect.y, destRect.width, destRect.height,
                            ++band, imag);
                }
            } // for (band ...
        }
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return a PlanarImage representing ROI image
     */
    private PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }
}
