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
package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ColormapOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * The LookupOpImage class performs the lookup operation on an image with integral data type. This operation consist of passing the input pixels
 * through a lookupTable(an array) of all the JAI data types. The output pixels are calculated from the table values by simply taking the array value
 * associated to the selected index indicated by the input pixel. The table and source data type can be different, and the destination image will have
 * the table data type. Even the band number can be different, in this case the destination image number will depend from the source and table band
 * numbers. If the destination sample model is not the same as that of the table, another one is created from the table. A ROI object passed to the
 * constructor is taken into account by passing to the table the informations extracted from it; an eventual No Data Range is passed to table if
 * present. If No Data or ROI are used, then the destination No Data value is passed to the table. The image calculation is performed by calling the
 * computeRect() method that selects an image tile, a raster containing Roi data if Roi RasterAccessor is used, and then these parameters are passed
 * to the table that executes the lookup operation.
 */
public class LookupOpImage extends ColormapOpImage {

    /** Lookup table currently used*/
    private LookupTable lookupTable;

    /** ROI image*/
    private PlanarImage srcROIImage;

    /** Boolean indicating if Roi RasterAccessor must be used*/
    private boolean useRoiAccessor;

    /** Extended ROI image*/
    private RenderedOp srcROIImgExt;

    /** ROI Border Extender */
    private final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    public LookupOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            LookupTable lookupTable, double destinationNoData, ROI roi, Range noData,
            boolean useRoiAccessor) {
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

        //Boolean initialization
        boolean hasNoData = false;
        boolean hasROI = false;
        
        // If ROI is present, then ROI parameters are added
        if (roi != null) {
            // Roi object 
            ROI srcROI = roi;
            // Creation of a PlanarImage containing the ROI data 
            srcROIImage = srcROI.getAsImage();
            // Source Bounds
            Rectangle srcRect = new Rectangle(source.getMinX(), source.getMinY(),
                    source.getWidth(), source.getHeight());
            // Padding of the input ROI image in order to avoid the call of the getExtendedData() method
            // ROI bounds are saved 
            Rectangle roiBounds = srcROIImage.getBounds();
            int deltaX0 = (roiBounds.x - srcRect.x);
            int leftP = deltaX0 > 0 ? deltaX0 : 0;
            int deltaY0 = (roiBounds.y - srcRect.y);
            int topP = deltaY0 > 0 ? deltaY0 : 0;
            int deltaX1 = (srcRect.x + srcRect.width - roiBounds.x + roiBounds.width);
            int rightP = deltaX1 > 0 ? deltaX1 : 0;
            int deltaY1 = (srcRect.y + srcRect.height - roiBounds.y + roiBounds.height);
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
            // Boolean indicating if roi is present
            hasROI = true;

            // The useRoiAccessor parameter is set
            this.useRoiAccessor = useRoiAccessor;
            // Then all the ROI informations are passed to the table
            lookupTable.setROIparams(roiBounds, srcROIImage, useRoiAccessor);
        } else {
            //If no ROI is present then all the ROI information are set to null.
            this.useRoiAccessor = false;
            lookupTable.unsetROI();
        }

        // If no Data are present, then a No Data Range is added to the table
        if (noData != null) {
            hasNoData = true;
            // Control if the range data type is the same of the source image
            if (noData.getDataType().getDataType() != source.getSampleModel().getDataType()) {
                // Convert NoData
                noData = RangeFactory.convert(noData, source.getSampleModel().getDataType());
            }
            lookupTable.setNoDataRange(noData);
        } else {
            // if no data range is not present the table no data range is set to null.
            lookupTable.unsetNoData();
        }

        // If no data Range or ROI are present, then the destination no data value is added.
        if (hasNoData || hasROI) {
            lookupTable.setDestinationNoData(destinationNoData);
        }
    }

    /**
     * Performs the table lookup operation within the specified bounds.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        // ROI bounds calculations
        Raster tile = sources[0];
        Rectangle rect = tile.getBounds();
        // ROI calculation if roiAccessor is used
        if (useRoiAccessor) {
            // Note that the getExtendedData() method is not called because the input images are padded.
            // For each image there is a check if the rectangle is contained inside the source image;
            // if this not happen, the data is taken from the padded image.
            Raster roi = null;
            if (srcROIImage.getBounds().contains(rect)) {
                roi = srcROIImage.getData(rect);
            } else {
                roi = srcROIImgExt.getData(rect);
            }
            lookupTable.lookup(sources[0], dest, destRect, roi);
        } else {
            lookupTable.lookup(sources[0], dest, destRect, null);
        }
    }

    /**
     * Transform the colormap via the lookup table. NoData values are not considered.
     */
    protected void transformColormap(byte[][] colormap) {
        for (int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            int band = lookupTable.getNumBands() < 3 ? 0 : b;

            for (int i = 0; i < mapSize; i++) {
                // Lookup operation for every table element
                int result = lookupTable.lookup(band, map[i] & 0xFF);
                map[i] = ImageUtil.clampByte(result);
            }
        }
    }
    
    @Override
    public synchronized void dispose() {
        if(srcROIImgExt != null) {
            srcROIImgExt.dispose();
        }
        super.dispose();
    }
}
