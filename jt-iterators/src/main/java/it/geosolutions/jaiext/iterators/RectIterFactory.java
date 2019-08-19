/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2019 GeoSolutions


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

/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package it.geosolutions.jaiext.iterators;
import com.sun.media.jai.iterator.RectIterCSMFloat;
import com.sun.media.jai.iterator.RectIterFallback;
import com.sun.media.jai.iterator.WrapperRI;
import com.sun.media.jai.iterator.WrapperWRI;
import com.sun.media.jai.iterator.WritableRectIterCSMByte;
import com.sun.media.jai.iterator.WritableRectIterCSMFloat;
import com.sun.media.jai.iterator.WritableRectIterFallback;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRenderedImage;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.WritableRectIter;


/**
 * A factory class to instantiate instances of the RectIter and
 * WritableRectIter interfaces on sources of type Raster,
 * RenderedImage, and WritableRenderedImage.
 *
 * @see RectIter
 * @see WritableRectIter
 */
public class RectIterFactory {

    /** Prevent this class from ever being instantiated. */
    private RectIterFactory() {}

    /**
     * Constructs and returns an instance of RectIter suitable
     * for iterating over the given bounding rectangle within the
     * given RenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a read-only RenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RectIter allowing read-only access to the source.
     */
    public static RectIter create(RenderedImage im,
            Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                    im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    ComponentSampleModel csm = (ComponentSampleModel) sm;
                    int[] offsets = csm.getBandOffsets();
                    if (offsets.length == 4 && offsets[0] == 3 && offsets[1] == 2 && offsets[2] == 1 && offsets[3] == 0) {
                        int[] bankIndices = csm.getBankIndices();
                        if (bankIndices.length == 4 && bankIndices[0] == 0 && bankIndices[1] == 0 && bankIndices[2] == 0 && bankIndices[3] == 0) {
                            return new RectIterCSMByteABGRSingleBank(im, bounds);
                        } else {
                            return new RectIterCSMByteABGR(im, bounds);
                        }
                    } else {
                        return new RectIterCSMByte(im, bounds);
                    }
                case DataBuffer.TYPE_SHORT:
                    // return new RectIterCSMShort(im, bounds);
                    break;
                case DataBuffer.TYPE_USHORT:
                    // return new RectIterCSMUShort(im, bounds);
                    break;
                case DataBuffer.TYPE_INT:
                    // return new RectIterCSMInt(im, bounds);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    return new RectIterCSMFloat(im, bounds);
                case DataBuffer.TYPE_DOUBLE:
                    // return new RectIterCSMDouble(im, bounds);
                    break;
            }
        }

        return new RectIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of RectIter suitable
     * for iterating over the given bounding rectangle within the
     * given Raster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a read-only Raster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RectIter allowing read-only access to the source.
     */
    public static RectIter create(Raster ras,
            Rectangle bounds) {
        RenderedImage im = new WrapperRI(ras);
        return create(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRectIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRenderedImage source.  If the bounds parameter is null,
     * the entire image will be used.
     *
     * @param im a WritableRenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRectIter allowing read/write access to the source.
     */
    public static WritableRectIter createWritable(WritableRenderedImage im,
            Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(),
                    im.getWidth(), im.getHeight());
        }

        SampleModel sm = im.getSampleModel();
        if (sm instanceof ComponentSampleModel) {
            switch (sm.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    int[] offsets = ((ComponentSampleModel) sm).getBandOffsets();
                    if (offsets.length == 1 && offsets[0] == 0) {
                        return new WritableRectIterCSMByteIndexed(im, bounds);
                    } else {
                        return new WritableRectIterCSMByte(im, bounds);
                    }
                    
                case DataBuffer.TYPE_SHORT:
                    // return new WritableRectIterCSMShort(im, bounds);
                    break;
                case DataBuffer.TYPE_USHORT:
                    // return new WritableRectIterCSMUShort(im, bounds);
                    break;
                case DataBuffer.TYPE_INT:
                    // return new WritableRectIterCSMInt(im, bounds);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    return new WritableRectIterCSMFloat(im, bounds);
                case DataBuffer.TYPE_DOUBLE:
                    // return new WritableRectIterCSMDouble(im, bounds);
                    break;
            }
        }

        return new WritableRectIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRectIter suitable for
     * iterating over the given bounding rectangle within the given
     * WritableRaster source.  If the bounds parameter is null,
     * the entire Raster will be used.
     *
     * @param ras a WritableRaster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRectIter allowing read/write access to the source.
     */
    public static WritableRectIter createWritable(WritableRaster ras,
            Rectangle bounds) {
        WritableRenderedImage im = new WrapperWRI(ras);
        return createWritable(im, bounds);
    }
}
