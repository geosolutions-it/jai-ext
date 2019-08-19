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

import com.sun.media.jai.iterator.RectIterFallback;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;

/**
 */
public abstract class RectIterCSM extends RectIterFallback {

    protected final  int[] bankIndices;
    protected final int scanlineStride;
    protected final int pixelStride;
    protected final int[] bandOffsets;
    protected int[] DBOffsets;
    protected int oldTileX;
    protected int oldTileY;

    protected int offset;
    protected int bandOffset;

    public RectIterCSM(RenderedImage im, Rectangle bounds) {
        super(im, bounds);

        ComponentSampleModel csm = (ComponentSampleModel)sampleModel;

        this.scanlineStride = csm.getScanlineStride();
        this.pixelStride = csm.getPixelStride();
        this.bankIndices = csm.getBankIndices();
        int[] bo = csm.getBandOffsets();

        this.bandOffsets = new int[numBands + 1];
        for (int i = 0; i < numBands; i++) {
            bandOffsets[i] = bo[i];
        }
        bandOffsets[numBands] = 0;

        this.DBOffsets = new int[numBands];

        this.offset = (y - sampleModelTranslateY)*scanlineStride +
                (x - sampleModelTranslateX)*pixelStride;
        this.bandOffset = bandOffsets[0];
    }

    protected void dataBufferChanged() {}

    protected void adjustBandOffsets() {
        int[] newDBOffsets = dataBuffer.getOffsets();
        for (int i = 0; i < numBands; i++) {
            int bankNum = bankIndices[i];
            bandOffsets[i] += newDBOffsets[bankNum] - DBOffsets[bankNum];
        }
        this.DBOffsets = newDBOffsets;
    }

    protected void setDataBuffer() {
        // split to allow JIT to inline this method
        if (dataBuffer == null || oldTileX != tileX || oldTileY != tileY) {
            reallySetDataBuffer();
        }
    }

    protected void reallySetDataBuffer() {
        Raster tile = im.getTile(tileX, tileY);
        this.dataBuffer = tile.getDataBuffer();
        dataBufferChanged();

        int newSampleModelTranslateX = tile.getSampleModelTranslateX();
        int newSampleModelTranslateY = tile.getSampleModelTranslateY();

        int deltaX = sampleModelTranslateX - newSampleModelTranslateX;
        int deltaY = sampleModelTranslateY - newSampleModelTranslateY;

        offset += deltaY*scanlineStride + deltaX*pixelStride;

        this.sampleModelTranslateX = newSampleModelTranslateX;
        this.sampleModelTranslateY = newSampleModelTranslateY;
        this.oldTileX = tileX;
        this.oldTileY = tileY;
    }

    public void startLines() {
        offset += (bounds.y - y)*scanlineStride;
        y = bounds.y;

        tileY = startTileY;
        setTileYBounds();
        setDataBuffer();
    }

    public void nextLine() {
        ++y;
        offset += scanlineStride;
    }

    public void jumpLines(int num) {
        int jumpY = y + num;
        if (jumpY < bounds.y || jumpY > lastY) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException();
        }

        y = jumpY;
        offset += num*scanlineStride;

        if (y < prevYBoundary || y > nextYBoundary) {
            this.tileY = PlanarImage.YToTileY(y,
                    tileGridYOffset,
                    tileHeight);
            setTileYBounds();
            setDataBuffer();
        }
    }

    public void startPixels() {
        offset += (bounds.x - x)*pixelStride;
        x = bounds.x;

        tileX = startTileX;
        setTileXBounds();
        setDataBuffer();
    }

    public void nextPixel() {
        ++x;
        offset += pixelStride;
    }

    public void jumpPixels(int num) {
        int jumpX = x + num;
        if (jumpX < bounds.x || jumpX > lastX) {
            // Jumped outside the image.
            throw new IndexOutOfBoundsException();
        }

        x = jumpX;
        offset += num*pixelStride;

        if (x < prevXBoundary || x > nextXBoundary) {
            this.tileX = PlanarImage.XToTileX(x,
                    tileGridXOffset,
                    tileWidth);

            setTileXBounds();
            setDataBuffer();
        }
    }

    public void startBands() {
        b = 0;
        bandOffset = bandOffsets[0];
    }

    public void nextBand() {
        ++b;
        bandOffset = bandOffsets[b];
    }
}
