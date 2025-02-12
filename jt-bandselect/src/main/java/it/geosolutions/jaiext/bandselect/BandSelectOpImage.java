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
package it.geosolutions.jaiext.bandselect;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Map;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterFactory;

import com.sun.media.imageioimpl.common.BogusColorSpace;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandSelect" operation.
 *
 * <p>This <code>OpImage</code> copies the specified bands of the source
 * image to the destination image in the order that is specified.
 *
 * @see javax.media.jai.operator.BandSelectDescriptor
 * @see BandSelectCRIF
 *
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class BandSelectOpImage extends PointOpImage {

    private final static boolean DATABUFFER_WORKAROUND =
            Boolean.valueOf(System.getProperty("it.geosolutions.jaiext.databuffer.workaround", "true"));

    // Set if the source has a SinglePixelPackedSampleModel and
    // bands.length < 3.
    private boolean triggerCopyOperation;

    private int[] bands;

    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            int[] bandIndices) {
        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Create a sub-banded SampleModel.
        SampleModel sourceSM = source.getSampleModel();
        int numBands = bandIndices.length;

        // The only ColorModel compatible with a SinglePixelPackedSampleModel
        // in the J2SE is a DirectColorModel which is by definition of
        // ColorSpace.TYPE_RGB. Therefore if there are fewer than 3 bands
        // a data copy is obligatory if a ColorModel will be possible.
        SampleModel sm = null;
        if (sourceSM instanceof SinglePixelPackedSampleModel && numBands < 3) {
            sm = new PixelInterleavedSampleModel(
                     DataBuffer.TYPE_BYTE,
                     sourceSM.getWidth(), sourceSM.getHeight(),
                     numBands, sourceSM.getWidth()*numBands,
                     numBands == 1 ? new int[] {0} : new int[] {0, 1});
        } else if (DATABUFFER_WORKAROUND && sourceSM instanceof ComponentSampleModelJAI) {
            // Do not use standard method. Let's create a subSampleModel
            // using an internal method to keep into account
            // pixelStride/lineStride adjustments
            sm = createSubSampleComponentSampleModel(sourceSM, bandIndices);
        } else {
            sm = sourceSM.createSubsetSampleModel(bandIndices);
        }
        il.setSampleModel(sm);

        // Clear the ColorModel mask if needed.
        ColorModel cm = il.getColorModel(null);
        if (cm == null) {
            // for floating point we cannot rely on planarimage to set a sane color model
            forceSaneFloatingPointColorModel(sm, numBands, il);
        } else if(!JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            // Clear the mask bit if incompatible.
            il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            // force a sane color model for floating point
            forceSaneFloatingPointColorModel(sm, numBands, il);
        }

        // Force the tile grid to be identical to that of the source.
        il.setTileGridXOffset(source.getTileGridXOffset());
        il.setTileGridYOffset(source.getTileGridYOffset());
        il.setTileWidth(source.getTileWidth());
        il.setTileHeight(source.getTileHeight());

        return il;
    }

    /**
     * PlanarImage will set a ColorModel with alpha if the source has 2 or 4 bands, even if
     * the data type is float or double... don't go there
     */
    private static void forceSaneFloatingPointColorModel(SampleModel sm, int numBands, ImageLayout il) {
        int dataType = sm.getDataType();
        if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
            ColorSpace cs = new BogusColorSpace(numBands);
            ColorModel cm = RasterFactory.createComponentColorModel(dataType, cs, false, false, ComponentColorModel.OPAQUE);
            il.setColorModel(cm);
        }
    }

    private static SampleModel createSubSampleComponentSampleModel(SampleModel sourceSM, int[] bandIndices) {
        ComponentSampleModelJAI csm = (ComponentSampleModelJAI) sourceSM;
        // These same checks are made in PlanarImage's getData code
        int[] bankIndices = csm.getBankIndices();
        int[] bandOffsets = csm.getBandOffsets();
        int numBands = bandIndices.length;
        int pixelStride = csm.getPixelStride();
        int scanlineStride = csm.getScanlineStride();
        boolean isBandInt = pixelStride == 1 && numBands > 1;
        boolean isBandChild = pixelStride > 1 && numBands != pixelStride;
        boolean fastCobbleIsPossible = false;
        if (!isBandChild && !isBandInt) {
            int i;
            for(i = 0; i < numBands && bandOffsets[i] < numBands; ++i) { }
            if (i == numBands) {
                fastCobbleIsPossible = true;
            }
        }

        if (pixelStride > bandIndices.length && !fastCobbleIsPossible) {
            // Future PlanarImage getData and getTile calls
            // will internally create a destination raster
            // with a number of bands equal to the number of selected bands
            // which may result into ArrayIndexOutOfBounds exception if
            // not updating pixelStride and scanlineStride accordingly
            scanlineStride/= pixelStride;
            pixelStride = bandIndices.length;
            scanlineStride*= pixelStride;
        }
        int newBankIndices[] = new int[bandIndices.length];
        int newBandOffsets[] = new int[bandIndices.length];
        for (int  i = 0; i<bandIndices.length; i++) {
            int b = bandIndices[i];
            newBankIndices[i] = bankIndices[b];
            newBandOffsets[i] = bandOffsets[b];
        }
        return new ComponentSampleModelJAI(csm.getDataType(), csm.getWidth(), csm.getHeight(),
            pixelStride, scanlineStride, newBankIndices, newBandOffsets);
    }

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param bandIndices  The selected band indices of the source.
     *                     The number of bands of the destination is
     *                     determined by <code>bands.length</code>.
     */
    public BandSelectOpImage(RenderedImage source,
                             Map<?,?> config,
                             ImageLayout layout,
                             int[] bandIndices) {
        super(vectorize(source),
              layoutHelper(layout, source, bandIndices),
              config, true);

        this.triggerCopyOperation =
            source.getSampleModel() instanceof SinglePixelPackedSampleModel &&
            bandIndices.length < 3;
        this.bands = (int[])bandIndices.clone();
    }

    public boolean computesUniqueTiles() {
        return triggerCopyOperation;
    }

    public Raster computeTile(int tileX, int tileY) {
        Raster tile = getSourceImage(0).getTile(tileX, tileY);

        if (triggerCopyOperation) {
            // Copy the data as there is no concrete ColorModel for
            // a SinglePixelPackedSampleModel with numBands < 3.
            tile = tile.createChild(tile.getMinX(), tile.getMinY(),
                                    tile.getWidth(), tile.getHeight(),
                                    tile.getMinX(), tile.getMinY(),
                                    bands);
            WritableRaster raster = createTile(tileX, tileY);
            raster.setRect(tile);

            return raster;
        } else {

            Raster raster =  tile.createChild(tile.getMinX(), tile.getMinY(),
                            tile.getWidth(), tile.getHeight(),
                            tile.getMinX(), tile.getMinY(),
                            bands);

            SampleModel sm = getSampleModel();
            SampleModel dataSampleModel = raster.getSampleModel();
            if (DATABUFFER_WORKAROUND && dataSampleModel instanceof ComponentSampleModelJAI && sm instanceof ComponentSampleModelJAI) {
                int opPixelStride = ((ComponentSampleModel) dataSampleModel).getPixelStride();
                int tilePixelStride = ((ComponentSampleModel) sm).getPixelStride();
                if (opPixelStride != tilePixelStride) {
                    // Adopt same code internally used by PlanarImage's getData to make sure to
                    // respect actual pixelStrides to avoid ArrayIndexOutOfBounds exception
                    // or some weirdness in accessing bad bands
                    sm = dataSampleModel.createCompatibleSampleModel(raster.getWidth(), raster.getHeight());
                    WritableRaster destinationRaster = this.createWritableRaster(sm, new Point(raster.getMinX(), raster.getMinY()));
                    JDKWorkarounds.setRect(destinationRaster, raster);
                    raster = destinationRaster;
                }
            }
            return raster;
        }
    }

    public Raster getTile(int tileX, int tileY) {

        // if we have to perform a copy, caching is needed!
        if (triggerCopyOperation) {
            return super.getTile(tileX, tileY);
        }
        // Just to return computeTile() result so as to avoid caching.
        return computeTile(tileX, tileY);
    }
}
