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
package it.geosolutions.jaiext.rlookup;

import it.geosolutions.jaiext.range.Range.DataType;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.List;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import javax.media.jai.RasterFactory;

import com.sun.media.jai.opimage.RIFUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * The image factory for the RangeLookup operation.
 * 
 * @see RangeLookupDescriptor
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RangeLookupRIF implements RenderedImageFactory {

    public RangeLookupRIF() {
    }

    /**
     * Create a new instance of RangeLookupOpImage in the rendered layer.
     * 
     * @param paramBlock an instance of ParameterBlock
     * @param renderHints useful to specify a {@link BorderExtender} and {@link ImageLayout}
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        final RenderedImage src = paramBlock.getRenderedSource(0);
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        final RangeLookupTable table = (RangeLookupTable) paramBlock
                .getObjectParameter(RangeLookupDescriptor.TABLE_ARG);

        /*
         * Default value may be null, indicating unmatched source values should be passed through.
         */
        final Number defaultValue = (Number) paramBlock
                .getObjectParameter(RangeLookupDescriptor.DEFAULT_ARG);

        /*
         * ROI value may be null, used for reducing the active computation area.
         */
        final ROI roi = (ROI) paramBlock.getObjectParameter(RangeLookupDescriptor.ROI_ARG);

        /*
         * Set the destination type based on the type and range of lookup table return values.
         */
        final Class<? extends Number> destClazz;
        List<LookupItem> items = table.getItems();
        if (items.size() > 0) {
            destClazz = items.get(0).getValue().getClass();
        } else if (defaultValue != null) {
            destClazz = defaultValue.getClass();
        } else {
            // fall back to source value class
            int dataType = paramBlock.getRenderedSource(0).getSampleModel().getDataType();
            destClazz = DataType.classFromType(dataType);
        }

        int dataType = -1;
        if (destClazz.equals(Short.class)) {

            // if the values are positive we should go with USHORT
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).getValue().shortValue() < 0) {
                    dataType = DataBuffer.TYPE_SHORT;
                    break;
                }
            }

            // No negative values so USHORT can be used
            if (dataType == -1) {
                dataType = DataBuffer.TYPE_USHORT;
            }

        } else { // All data classes other than Short
            try {
                dataType = DataType.dataTypeFromClass(destClazz);

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Illegal destination class for this rangelookuptable:"
                                + destClazz.toString());
            }
        }

        final boolean isDataTypeChanged;
        if (src.getSampleModel().getDataType() != dataType) {
            isDataTypeChanged = true;
        } else {
            isDataTypeChanged = false;
        }

        if (isDataTypeChanged) {
            // Create or clone the ImageLayout.
            if (layout == null) {
                layout = new ImageLayout(src);
            } else {
                layout = (ImageLayout) layout.clone();
            }

            // Get prospective destination SampleModel.
            SampleModel sampleModel = layout.getSampleModel(src);

            // Create a new SampleModel
            int tileWidth = layout.getTileWidth(src);
            int tileHeight = layout.getTileHeight(src);
            int numBands = src.getSampleModel().getNumBands();

            SampleModel csm = RasterFactory.createComponentSampleModel(sampleModel, dataType,
                    tileWidth, tileHeight, numBands);

            layout.setSampleModel(csm);

            // Check ColorModel.
            ColorModel colorModel = layout.getColorModel(null);
            if (colorModel != null
                    && !JDKWorkarounds.areCompatibleDataModels(layout.getSampleModel(null),
                            colorModel)) {
                // Clear the mask bit if incompatible.
                layout.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        return new RangeLookupOpImage(src, renderHints, layout, table, defaultValue, roi);
    }
}
