package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import javax.media.jai.ColormapOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.iterator.RandomIter;
import org.jaitools.numeric.Range;
import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

public class LookupOpImage extends ColormapOpImage {

    private LookupTable lookupTable;
    private PlanarImage srcROIImage;
    private boolean useRoiAccessor;
    private boolean hasROI;
    private boolean hasNoData;

    public LookupOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            LookupTable lookupTable, double destinationNoData,ROI roi, boolean useRoiAccessor, Range noData) {
        super(source, layout, configuration, true);
        // Addition of the lookup table
        this.lookupTable = lookupTable;
        
        SampleModel sModel = source.getSampleModel(); // source sample model

        if (sampleModel.getTransferType() != lookupTable.getDataType()
                || sampleModel.getNumBands() != lookupTable.getDestNumBands(sModel.getNumBands())) {
            /*
             * The current SampleModel is not suitable for the supplied source and lookup table. Create a suitable SampleModel and ColorModel for the
             * destination image.
             */
            sampleModel = lookupTable.getDestSampleModel(sModel, tileWidth, tileHeight);
            if (colorModel != null
                    && !JDKWorkarounds.areCompatibleDataModels(sampleModel, colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel, configuration);
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();

        // If ROI is present, then ROI parameters are added
        if (roi !=null) {

            ROI srcROI = roi;
            srcROIImage = srcROI.getAsImage();

            // FIXME can we use smaller bounds here?
            final Rectangle rect = new Rectangle(srcROIImage.getBounds());
            Raster data = srcROIImage.getData(rect);
            RandomIter roiIter = RandomIterFactory.create(data, data.getBounds(),false,true);
            hasROI = true;
            Rectangle roiBounds = srcROIImage.getBounds();
            this.useRoiAccessor=useRoiAccessor;
            lookupTable.setROIparams(roiBounds,roiIter,srcROIImage, useRoiAccessor); 
        }else{
            useRoiAccessor = false;
        }
        
        // If no Data are present, then a No Data Range is added
        if(noData != null){
            hasNoData=true;
            lookupTable.setNoDataRange(noData);        
        }        
        
        // If no data or ROI are present, then the destination no data value is added. 
        if(hasNoData || hasROI){
            lookupTable.setDestinationNoData(destinationNoData);
        }
    }


    /**
     * Performs the table lookup operation within the specified bounds.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        
        //ROI bounds calculations
        Raster tile = sources[0];
        Rectangle rect = tile.getBounds();
        //ROI calculation if roiAccessor is used
        if(useRoiAccessor){
            Raster roi = srcROIImage.getData(rect);
            lookupTable.lookup(sources[0], dest, destRect, roi);
        }else{
            lookupTable.lookup(sources[0], dest, destRect, null);
        }
    }

    /**
     * Transform the colormap via the lookup table. NoData values are not considered.
     */
    protected void transformColormap(byte[][] colormap) {
        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            int band = lookupTable.getNumBands() < 3 ? 0 : b;

            for(int i = 0; i < mapSize; i++) {
                int result = lookupTable.lookup(band, map[i] & 0xFF);
                map[i] = ImageUtil.clampByte(result);
            }
        }
    }

}
