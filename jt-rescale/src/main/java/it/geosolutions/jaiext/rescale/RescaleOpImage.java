package it.geosolutions.jaiext.rescale;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ColormapOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

public class RescaleOpImage extends ColormapOpImage {

    private double[] constantArray;
    private double[] offsetArray;
    private boolean hasNoData;
    private boolean hasROI;
    private Range noData;
    private PlanarImage srcROIImage;
    private RandomIter roiIter;
    private Rectangle roiBounds;
    private boolean useROIAccessor;
    private boolean[] booleanLookupTable;
    private boolean caseA;
    private boolean caseB;
    private boolean caseC;

    public RescaleOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            double[] constantArray, double[] offsetArray, ROI roi, Range noData, boolean useROIAccessor) {
        super(source, layout, configuration, true);
        
        // Selection of the band number
        int numBands = getSampleModel().getNumBands();

        // Check if the constants number is equal to the band number
        // If they are not equal the first constant is used for all bands
        if (constantArray.length < numBands) {
            this.constantArray = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.constantArray[i] = constantArray[0];
            }
        } else {
            // Else the constants are copied
            this.constantArray = constantArray;
        }

        // Check if the offsets number is equal to the band number
        // If they are not equal the first offset is used for all bands
        if (offsetArray.length < numBands) {
            this.offsetArray   = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.offsetArray[i]   = offsetArray[0];
            }
        } else {
         // Else the offsets are copied
            this.offsetArray = offsetArray;
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
        } else {
            hasNoData = false;
        }

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            ROI srcROI = roi;
            // Creation of a PlanarImage containing the ROI data
            srcROIImage = srcROI.getAsImage();
            // ROI image bounds calculation
            final Rectangle rect = new Rectangle(srcROIImage.getBounds());
            // Roi image data store
            Raster data = srcROIImage.getData(rect);
            // Creation of a RandomIterator for selecting random pixel inside the ROI
            roiIter = RandomIterFactory.create(data, data.getBounds(), false, true);
            // ROI bounds are saved
            roiBounds = srcROIImage.getBounds();
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
        } else {
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
            roiIter = null;
            srcROIImage = null;
        }

        // Creation of a lookuptable containing the values to use for no data
        if (hasNoData && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = !noData.contains(value);
            }
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;
        
        
        // Initialize the colormap if necessary.
        if(caseA){
            initializeColormapOperation();
        }        
    }
    
    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {
        for (int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            float c = (float)(b < constantArray.length ?
                              constantArray[b] : constantArray[0]);
            float o = (float)(b < constantArray.length ?
                              offsetArray[b] : offsetArray[0]);

            for (int i = 0; i < mapSize; i++) {
                map[i] = ImageUtil.clampRoundByte((map[i] & 0xFF) * c + o);
            }
        }
    }


    
    /**
     * Rescales to the pixel values within a specified rectangle.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect){
        
        
        Raster tile = sources[0];
        
        
        
    }
    
    
}
