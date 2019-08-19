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
import java.awt.Rectangle;
import java.awt.image.RenderedImage;

import javax.media.jai.iterator.WritableRectIter;

/**
 */
public class WritableRectIterCSMByte extends RectIterCSMByte
        implements WritableRectIter {

    public WritableRectIterCSMByte(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    public void setSample(int s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, int s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setSample(float s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, float s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setSample(double s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, double s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setPixel(int[] iArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)iArray[b];
        }
    }

    public void setPixel(float[] fArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)fArray[b];
        }
    }

    public void setPixel(double[] dArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)dArray[b];
        }
    }
}