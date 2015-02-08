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
package it.geosolutions.jaiext.convolve;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

public class SeparableConvolveOpImage extends ConvolveOpImage {
    
    private float hValues[];
    private float vValues[];
    private float hTables[][]; 

    public SeparableConvolveOpImage(RenderedImage source, BorderExtender extender,
            RenderingHints hints, ImageLayout l, KernelJAI kernel, ROI roi, Range noData,
            double destinationNoData, boolean skipNoData) {
        
        super(source, extender, hints, l, kernel, roi, noData, destinationNoData, skipNoData); // TODO Auto-generated constructor stub
        
        this.kernel = kernel;
        kw = kernel.getWidth();
        kh = kernel.getHeight();
        kx = kernel.getXOrigin();
        ky = kernel.getYOrigin();
        hValues = kernel.getHorizontalKernelData();
        vValues = kernel.getVerticalKernelData();

        if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
            hTables = new float[hValues.length][256];
            for (int i = 0; i < hValues.length; i++) {
                float k = hValues[i];
                for (int j = 0; j < 256; j++) {
                    byte b = (byte)j;
                    float f = (float)j;
                    hTables[i][b+128] = hasNoData && noData.contains(b) ? 0 : k*f;
                }
            }
        }
    }

    @Override
    protected void byteLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        float tmpBuffer[] = new float[kh*dwidth];
        int   tmpBufferSize = kh*dwidth;
        
        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();
        
        
        if(caseA || (hasROI && hasNoData && roiContainsTile)){
            for (int k = 0; k < dnumBands; k++)  {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                int revolver = 0;
                int kvRevolver = 0;                 // to match kernel vValues
                for (int j = 0; j < kh-1; j++) {
                    int srcPixelOffset = srcScanlineOffset;

                    for (int i = 0; i < dwidth; i++) {
                         int imageOffset = srcPixelOffset;
                         float f = 0.0f;
                         for (int v = 0; v < kw; v++)  {
                              f += hTables[v][srcData[imageOffset]+128];
                              imageOffset += srcPixelStride;
                         }
                         tmpBuffer[revolver+i] = f;
                         srcPixelOffset += srcPixelStride;
                    }
                    revolver += dwidth;
                    srcScanlineOffset += srcScanlineStride;
                }

                // srcScanlineStride already bumped by 
                // kh-1*scanlineStride
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
     
                    for (int i = 0; i < dwidth; i++)  {
                        int imageOffset = srcPixelOffset;
                        float f = 0.0f;
                        for (int v = 0; v < kw; v++)  {
                             f += hTables[v][srcData[imageOffset]+128];
                             imageOffset += srcPixelStride;
                        }
                        tmpBuffer[revolver + i] = f;

                        f = 0.5f;
                        // int a = 0;  
                        // The vertical kernel must revolve as well
                        int b = kvRevolver + i;
                        for (int a=0; a < kh; a++){
                            f += tmpBuffer[b] * vValues[a];
                            b += dwidth;
                            if (b >= tmpBufferSize) b -= tmpBufferSize;
                        }

                        dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }

                    revolver += dwidth;
                    if (revolver == tmpBufferSize) {
                        revolver = 0;
                    } 
                    kvRevolver += dwidth;
                    if (kvRevolver == tmpBufferSize) {
                        kvRevolver = 0;
                    } 
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }else if(caseB){
            for (int k = 0; k < dnumBands; k++)  {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                int revolver = 0;
                int kvRevolver = 0;                 // to match kernel vValues
                for (int j = 0; j < kh-1; j++) {
                    int srcPixelOffset = srcScanlineOffset;

                    for (int i = 0; i < dwidth; i++) {
                         int imageOffset = srcPixelOffset;
                         float f = 0.0f;
                         for (int v = 0; v < kw; v++)  {
                              f += hTables[v][srcData[imageOffset]+128];
                              imageOffset += srcPixelStride;
                         }
                         tmpBuffer[revolver+i] = f;
                         srcPixelOffset += srcPixelStride;
                    }
                    revolver += dwidth;
                    srcScanlineOffset += srcScanlineStride;
                }

                // srcScanlineStride already bumped by 
                // kh-1*scanlineStride
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
     
                    y0 = srcY + j;
                    
                    for (int i = 0; i < dwidth; i++)  {
                        
                        x0 = srcX + i;
                        
                        int imageOffset = srcPixelOffset;
                        float f = 0.0f;
                        for (int v = 0; v < kw; v++)  {
                             f += hTables[v][srcData[imageOffset]+128];
                             imageOffset += srcPixelStride;
                        }
                        tmpBuffer[revolver + i] = f;
                        
                        if(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0){            
                            f = 0.5f;
                            // int a = 0;  
                            // The vertical kernel must revolve as well
                            int b = kvRevolver + i;
                            for (int a=0; a < kh; a++){
                                f += tmpBuffer[b] * vValues[a];
                                b += dwidth;
                                if (b >= tmpBufferSize) b -= tmpBufferSize;
                            }
                            
                            dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataByte;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }

                    revolver += dwidth;
                    if (revolver == tmpBufferSize) {
                        revolver = 0;
                    } 
                    kvRevolver += dwidth;
                    if (kvRevolver == tmpBufferSize) {
                        kvRevolver = 0;
                    } 
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }else if(caseC || (hasROI && hasNoData && roiContainsTile)){
            for (int k = 0; k < dnumBands; k++)  {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                int revolver = 0;
                int kvRevolver = 0;                 // to match kernel vValues
                for (int j = 0; j < kh-1; j++) {
                    int srcPixelOffset = srcScanlineOffset;

                    for (int i = 0; i < dwidth; i++) {
                         int imageOffset = srcPixelOffset;
                         float f = 0.0f;
                         for (int v = 0; v < kw; v++)  {
                              f += hTables[v][srcData[imageOffset]+128];
                              imageOffset += srcPixelStride;
                         }
                         tmpBuffer[revolver+i] = f;
                         srcPixelOffset += srcPixelStride;
                    }
                    revolver += dwidth;
                    srcScanlineOffset += srcScanlineStride;
                }

                // srcScanlineStride already bumped by 
                // kh-1*scanlineStride
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
     
                    for (int i = 0; i < dwidth; i++)  {
                        int imageOffset = srcPixelOffset;
                        float f = 0.0f;
                        for (int v = 0; v < kw; v++)  {
                             f += hTables[v][srcData[imageOffset]+128];
                             imageOffset += srcPixelStride;
                        }
                        tmpBuffer[revolver + i] = f;

                        boolean isValid = true;
                        if(skipNoData){
                            int bandOff = srcBandOffsets[k];
                            for(int kj = 0; kj < kh && isValid; kj++){
                                int lineOff = (j + kj)*srcScanlineStride + bandOff;
                                for(int ki = 0; ki < kw  && isValid; ki++){
                                    int pixelOff = (i + ki) * srcPixelStride + lineOff;
                                    byte value = srcData[pixelOff];
                                    if(!lut[value + 128]){
                                        isValid = false;
                                    }
                                }
                            }
                        }

                        if(isValid){
                            f = 0.5f;
                            // int a = 0;  
                            // The vertical kernel must revolve as well
                            int b = kvRevolver + i;
                            for (int a=0; a < kh; a++){
                                f += tmpBuffer[b] * vValues[a];
                                b += dwidth;
                                if (b >= tmpBufferSize) b -= tmpBufferSize;
                            }
                            dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataByte;
                        }
                       
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }

                    revolver += dwidth;
                    if (revolver == tmpBufferSize) {
                        revolver = 0;
                    } 
                    kvRevolver += dwidth;
                    if (kvRevolver == tmpBufferSize) {
                        kvRevolver = 0;
                    } 
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }else{
            for (int k = 0; k < dnumBands; k++)  {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                int revolver = 0;
                int kvRevolver = 0;                 // to match kernel vValues
                for (int j = 0; j < kh-1; j++) {
                    int srcPixelOffset = srcScanlineOffset;

                    y0 = srcY + j;
                    
                    for (int i = 0; i < dwidth; i++)  {
                        
                        x0 = srcX + i;
                        
                         int imageOffset = srcPixelOffset;
                         float f = 0.0f;
                         for (int v = 0; v < kw; v++)  {
                              f += hTables[v][srcData[imageOffset]+128];
                              imageOffset += srcPixelStride;
                         }
                         tmpBuffer[revolver+i] = f;
                         srcPixelOffset += srcPixelStride;
                    }
                    revolver += dwidth;
                    srcScanlineOffset += srcScanlineStride;
                }

                // srcScanlineStride already bumped by 
                // kh-1*scanlineStride
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
     
                    for (int i = 0; i < dwidth; i++)  {
                        int imageOffset = srcPixelOffset;
                        float f = 0.0f;
                        for (int v = 0; v < kw; v++)  {
                             f += hTables[v][srcData[imageOffset]+128];
                             imageOffset += srcPixelStride;
                        }
                        tmpBuffer[revolver + i] = f;

                        if(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0){
                            
                        }
                        
                        
                        boolean isValid = true;
                        if(skipNoData){
                            int bandOff = srcBandOffsets[k];
                            for(int kj = 0; kj < kh && isValid; kj++){
                                int lineOff = (j + kj)*srcScanlineStride + bandOff;
                                for(int ki = 0; ki < kw  && isValid; ki++){
                                    int pixelOff = (i + ki) * srcPixelStride + lineOff;
                                    byte value = srcData[pixelOff];
                                    if(!lut[value + 128]){
                                        isValid = false;
                                    }
                                }
                            }
                        }

                        if(isValid){
                            f = 0.5f;
                            // int a = 0;  
                            // The vertical kernel must revolve as well
                            int b = kvRevolver + i;
                            for (int a=0; a < kh; a++){
                                f += tmpBuffer[b] * vValues[a];
                                b += dwidth;
                                if (b >= tmpBufferSize) b -= tmpBufferSize;
                            }
                            dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataByte;
                        }
                       
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }

                    revolver += dwidth;
                    if (revolver == tmpBufferSize) {
                        revolver = 0;
                    } 
                    kvRevolver += dwidth;
                    if (kvRevolver == tmpBufferSize) {
                        kvRevolver = 0;
                    } 
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    @Override
    protected void ushortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {

        // TODO Auto-generated method stub

    }

    @Override
    protected void shortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {

        // TODO Auto-generated method stub

    }

    @Override
    protected void intLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {

        // TODO Auto-generated method stub

    }

    @Override
    protected void floatLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {

        // TODO Auto-generated method stub

    }

    @Override
    protected void doubleLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {

        // TODO Auto-generated method stub

    }

}
