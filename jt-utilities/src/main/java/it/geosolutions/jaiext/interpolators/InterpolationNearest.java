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
package it.geosolutions.jaiext.interpolators;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;

import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.iterator.RandomIter;

public class InterpolationNearest extends Interpolation implements InterpolationNoData {

    /** serialVersionUID */
    private static final long serialVersionUID = -6994369085300227735L;

    /** Boolean for checking if the ROI Accessor must be used by the interpolator */
    private boolean useROIAccessor;

    /** Range of NO DATA values to be checked */
    private Range noDataRange;

    /**
     * Destination NO DATA value used when the image pixel is outside of the ROI or is contained in the NO DATA range
     * */
    private double destinationNoData;

    /** ROI bounds used for checking the position of the pixel */
    private Rectangle roiBounds;

    /** Image data Type */
    private int dataType;

    /** This value is the destination NO DATA values for binary images */
    private int black;
    
    /** Boolean used for indicating that the No Data Range is not degenarated(useful only for NaN check inside Float or Double Range) */
    private boolean isNotPointRange;

    // Method overriding. Performs the default nearest-neighbor interpolation without NO DATA or ROI control.
    @Override
    public int interpolateH(int[] samples, int arg1) {
        return samples[0];
    }

    @Override
    public float interpolateH(float[] samples, float arg1) {
        return samples[0];
    }

    @Override
    public double interpolateH(double[] samples, float arg1) {
        return samples[0];
    }

    /**
     * Simple interpolator object used for Nearest-Neighbor interpolation. On construction it is possible to set a range for no data values that will
     * be considered in the interpolation method.
     */
    public InterpolationNearest(Range noDataRange, boolean useROIAccessor,
            double destinationNoData, int dataType) {
        super(1, 1, 0, 0, 0, 0, 0, 0);
        if (noDataRange != null) {
            this.noDataRange = noDataRange;
            this.isNotPointRange = !noDataRange.isPoint();
        }
        this.useROIAccessor = useROIAccessor;
        this.destinationNoData = destinationNoData;
        black = ((int) destinationNoData) & 1;
        this.dataType = dataType;
    }

    public void setROIBounds(Rectangle roiBounds) {
        this.roiBounds = roiBounds;
    }

    public double getDestinationNoData() {
        return destinationNoData;
    }

	public void setDestinationNoData(double destinationNoData) {
		this.destinationNoData = destinationNoData;
	}
    
    public boolean getUseROIAccessor() {
    	return useROIAccessor;
    }
    
    public void setUseROIAccessor(boolean useROIAccessor) {
    	this.useROIAccessor = useROIAccessor;
    }
    
    public Range getNoDataRange() {
        return noDataRange;
    }

    public void setNoDataRange(Range noDataRange) {
        if (noDataRange != null) {
            this.noDataRange = noDataRange;
            this.isNotPointRange = !noDataRange.isPoint();
        }
    }
    
    public int getDataType() {
        return dataType;
    }

    // method for calculating the nearest-neighbor interpolation (no Binary data).
    public Number interpolate(RasterAccessor src, int bandIndex, int dnumband, int posx, int posy,
            Integer yROIValue, RasterAccessor roiAccessor, RandomIter roiIter, boolean setNoData) {
        // src data and destination data
        Number destData = null;
        // the destination data is set equal to the source data but could change if the ROI is present. If
        // it is no data, destination no data value is returned.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byte srcDataByte = src.getByteDataArray(bandIndex)[posx + posy];
            if ((noDataRange != null && (noDataRange).contains(srcDataByte)) || setNoData) {
                return destinationNoData;
            }
            destData = srcDataByte;
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            short srcDataShort = src.getShortDataArray(bandIndex)[posx + posy];
            if ((noDataRange != null && (noDataRange).contains(srcDataShort)) || setNoData) {
                return destinationNoData;
            }
            destData = srcDataShort;
            break;
        case DataBuffer.TYPE_INT:
            int srcDataInt = src.getIntDataArray(bandIndex)[posx + posy];
            if ((noDataRange != null && (noDataRange).contains(srcDataInt)) || setNoData) {
                return destinationNoData;
            }
            destData = srcDataInt;
            break;
        case DataBuffer.TYPE_FLOAT:
            float srcDataFloat = src.getFloatDataArray(bandIndex)[posx + posy];
            if ((noDataRange != null && (noDataRange).contains(srcDataFloat)) || (isNotPointRange && Float.isNaN(srcDataFloat)) || setNoData) {
                return destinationNoData;
            }
            destData = srcDataFloat;
            break;
        case DataBuffer.TYPE_DOUBLE:
            double srcDataDouble = src.getDoubleDataArray(bandIndex)[posx + posy];
            if ((noDataRange != null && (noDataRange).contains(srcDataDouble)) || (isNotPointRange && Double.isNaN(srcDataDouble)) || setNoData) {
                return destinationNoData;
            }
            destData = srcDataDouble;
            break;
        default:
            break;
        }
        // If ROI accessor is used,source pixel is tested if is contained inside the ROI.
        if (useROIAccessor) {
            if (roiAccessor == null || yROIValue == null) {
                throw new IllegalArgumentException("ROI Accessor or ROI y value not found");
            }
            // Operations for taking the correct index pixel in roi array.
            int roiIndex = posx / dnumband + yROIValue;

            byte[] roiDataArray = roiAccessor.getByteDataArray(0);

            if (roiIndex < roiDataArray.length) {
                // if the ROI pixel value is 0 the value returned is NO DATA
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:

                    byte valueROIByte = (byte) (roiDataArray[roiIndex] & 0xff);
                    if (valueROIByte != 0) {
                        return destData;
                    } else {
                        return destinationNoData;
                    }
                case DataBuffer.TYPE_USHORT:
                    short valueROIUShort = (short) (roiDataArray[roiIndex] & 0xffff);
                    if (valueROIUShort != 0) {
                        return destData;
                    } else {
                        return destinationNoData;
                    }
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_INT:
                case DataBuffer.TYPE_FLOAT:
                case DataBuffer.TYPE_DOUBLE:
                    double valueROI = roiDataArray[roiIndex];
                    if (valueROI != 0) {
                        return destData;
                    } else {
                        return destinationNoData;
                    }
                default:
                    break;
                }

            } else {
                return destinationNoData;
            }
            // If there is no ROI accessor but a ROI object is present, a test similar to that above is performed.
        } else if (roiBounds != null) {
            // Pixel position
            int x0 = src.getX() + posx / src.getPixelStride();
            int y0 = src.getY() + (posy - src.getBandOffset(bandIndex)) / src.getScanlineStride();
            // check if the roi pixel is inside the roi bounds
            if (!roiBounds.contains(x0, y0)) {
                return destinationNoData;
            } else {
                // if it is inside ROI bounds and the associated roi pixel is 1, the src pixel is returned.
                // Otherwise, destination NO DATA is returned.
                int wx = 0;
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    wx = roiIter.getSample(x0, y0, 0) & 0xFF;
                    break;
                case DataBuffer.TYPE_USHORT:
                    wx = roiIter.getSample(x0, y0, 0) & 0xFFFF;
                    break;
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_INT:
                case DataBuffer.TYPE_FLOAT:
                case DataBuffer.TYPE_DOUBLE:
                    wx = roiIter.getSample(x0, y0, 0);
                    break;
                default:
                    break;
                }

                final boolean insideROI = wx == 1;
                if (insideROI) {
                    return destData;
                } else {
                    return destinationNoData;
                }
            }
        }
        return destData;
    }

    // Interpolation operation for Binary images (coordinates are useful only if ROI is present)
    public int interpolateBinary(int xNextBitNo, Number[] sourceData, int sourceYOffset,
            int sourceScanlineStride, int[] coordinates, int[] roiDataArray, int roiYOffset,
            int roiScanlineStride, RandomIter roiIter) {
        // pixel initialization
        int s = 0;
        // Shift to the selected pixel
        int sshift = 0;
        // Calculates the bit number of the selected pixel's position
        int sbitnum = xNextBitNo - 1;

        int w00index = 0;

        int w00 = 1;

        // If an image ROI has been saved, this ROI is used for checking if
        // all the surrounding pixel belongs to the ROI.
        if (coordinates != null && roiBounds != null && !useROIAccessor) {
            // Central pixel positions
            int x0 = coordinates[0];
            int y0 = coordinates[1];
            // ROI control
            if (roiBounds.contains(x0, y0)) {
                w00 = roiIter.getSample(x0, y0, 0) & 0x1;
            } else {
                return black;
            }
        }

        // Calculates the interpolation for every type of data that allows binary images.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // This value is used for searching the selected pixel inside the element.
            sshift = 7 - (sbitnum & 7);
            // Conversion from bit to Byte for searching the element in which the selected pixel is found.
            int sbytenum = sbitnum >> 3;
            // Searching of the 4 pixels surrounding the selected one.
            s = (sourceData[sourceYOffset + sbytenum].byteValue() >> sshift) & 0x01;

            if (useROIAccessor) {
                int roiDataLength = roiDataArray.length;
                w00index = roiYOffset + sbytenum;

                w00 *= w00index < roiDataLength ? roiDataArray[w00index] >> sshift & 0x01 : 0;
            }
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            // Same operations like the ones above but the step is done with short and not byte
            int sshortnum = sbitnum >> 4;
            sshift = 15 - (sbitnum & 15);

            s = (sourceData[sourceYOffset + sshortnum].shortValue() >> sshift) & 0x01;

            if (useROIAccessor) {
                int roiDataLength = roiDataArray.length;
                w00index = roiYOffset + sshortnum;

                w00 *= w00index < roiDataLength ? roiDataArray[w00index] >> sshift & 0x01 : 0;
            }

            break;
        case DataBuffer.TYPE_INT:
            // Same operations like the ones above but the step is done with integers and not short
            int sintnum = sbitnum >> 5;
            sshift = 31 - (sbitnum & 31);

            s = (sourceData[sourceYOffset + sintnum].intValue() >> sshift) & 0x01;

            if (useROIAccessor) {
                int roiDataLength = roiDataArray.length;
                w00index = roiYOffset + sintnum;

                w00 *= w00index < roiDataLength ? roiDataArray[w00index] >> sshift & 0x01 : 0;
            }
            break;
        default:
            break;
        }

        if (w00 == 0) {
            return black;
        }

        return s;
    }
}
