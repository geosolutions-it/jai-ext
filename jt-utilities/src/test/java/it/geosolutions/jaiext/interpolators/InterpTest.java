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

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;

import org.junit.Test;

import com.sun.media.jai.util.Rational;

import junit.framework.TestCase;




/**
 * This class extends the TestCase class and is used for testing the 3 interpolation types used in various JAI-EXT modules:
 * Nearest-Neighbor, Bilinear, Bicubic. The 9 tests are divided in 3 groups, one for every interpolation type. Inside every method 
 * all the data types are tested. Inside every group there are 3 kind of tests:
 * 
 * <ul>
 * <li>test with No Data Range</li>
 * <li>test with No Data Range and ROI using a ROI iterator</li>
 * <li>test with No Data Range and ROI using a ROI RasterAccessor</li>
 * </ul>
 * 
 * Before starting all the tests the initialSetup() method is used for creating an array of images of all data types with the associated 
 * ROI. These synthetic images are not constant because at the creation time they are filled with random values in the upper left tile.
 * Binary images are not tested in this class. 
 */
public class InterpTest extends TestCase {

    /**
     * Default value for subsample bits
     * */
    public static final int DEFAULT_SUBSAMPLE_BITS = 8;

    /**
     * Default value for precision bits
     * */
    public static final int DEFAULT_PRECISION_BITS = 8;

    /** Default value for image width */
    public static float DEFAULT_WIDTH = 512;

    /** Default value for image height */
    public static float DEFAULT_HEIGHT = 512;

    /** default tolerance for comparison */
    public final static double DEFAULT_DELTA = 1.5d;

    /** Initial setup starting value */
    public static boolean DEFAULT_INITIAL_SETUP = false;

    /** Boolean for checking if the initial setup has been done */
    private boolean initialSetup = DEFAULT_INITIAL_SETUP;

    /** Test image where interpolation is performed */
    private RenderedImage[] testImages;

    /** Source No Data value */
    private double noData;

    /** Index position of the horizontal coordinate */
    private int posy;

    /** Index position of the vertical coordinate */
    private int posx;

    /** Index position of the roi vertical coordinate */
    private int posYroi;

    /** Source rasterAccessor */
    private RasterAccessor[] src;

    /** ROI rasterAccessor */
    private RasterAccessor roiAccessor;

    /** Rectangle containing roi bounds */
    private Rectangle roiBounds;

    /** Iterator for searching pixel inside ROI */
    private RandomIter roiIter;

    /** ROI data values */
    private Number[][] roiDataArray;

    /** Fractional position for destination pixel placed between two source pixels ([x axis , y axis]) */
    private Number[] fracvalues = new Number[2];

    /** Fractional position for destination pixel placed between two source pixels ([x axis , y axis]) */
    private Number[] fracvaluesFloat = new Number[2];;

    /** Value used for calculating the fractional position */
    private float one;

    /** Value used for computing bilinear interpolation */
    private int shift2;

    /** Round value used for computing bilinear interpolation */
    private int round2;

    /** Subsample bits used for subsampling the interval between two pixels */
    private int subsampleBits = DEFAULT_SUBSAMPLE_BITS;

    /** Precision bits used for calculating the bicubic interpolation */
    private int precisionBits = DEFAULT_PRECISION_BITS;

    /** Values used for filling pixels that are NO DATA or are outside of the ROI */
    private int destinationNoData;

    // SIMPLE TEST CLASSES FOR CHECKING THE FUNCTIONALITY OF THE 3 INTERPOLATORS.
    // NO BINARY IMAGES. ONLY TYPE BYTE.

    @Test
    public void testInterpolatorNearestNew() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        nearestMethod(dataType, false, false);

        dataType = DataBuffer.TYPE_USHORT;
        nearestMethod(dataType, false, false);

        dataType = DataBuffer.TYPE_SHORT;
        nearestMethod(dataType, false, false);

        dataType = DataBuffer.TYPE_INT;
        nearestMethod(dataType, false, false);

        dataType = DataBuffer.TYPE_FLOAT;
        nearestMethod(dataType, false, false);

        dataType = DataBuffer.TYPE_DOUBLE;
        nearestMethod(dataType, false, false);
    }

    @Test
    public void testInterpolatorNearestNewROIBounds() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        nearestMethod(dataType, true, false);

        dataType = DataBuffer.TYPE_USHORT;
        nearestMethod(dataType, true, false);

        dataType = DataBuffer.TYPE_SHORT;
        nearestMethod(dataType, true, false);

        dataType = DataBuffer.TYPE_INT;
        nearestMethod(dataType, true, false);

        dataType = DataBuffer.TYPE_FLOAT;
        nearestMethod(dataType, true, false);

        dataType = DataBuffer.TYPE_DOUBLE;
        nearestMethod(dataType, true, false);
    }

    @Test
    public void testInterpolatorNearestNewROIAccessor() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        nearestMethod(dataType, true, true);

        dataType = DataBuffer.TYPE_USHORT;
        nearestMethod(dataType, true, true);

        dataType = DataBuffer.TYPE_SHORT;
        nearestMethod(dataType, true, true);

        dataType = DataBuffer.TYPE_INT;
        nearestMethod(dataType, true, true);

        dataType = DataBuffer.TYPE_FLOAT;
        nearestMethod(dataType, true, true);

        dataType = DataBuffer.TYPE_DOUBLE;
        nearestMethod(dataType, true, true);
    }

    @Test
    public void testInterpolatorBilinearNew() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, false, false, true);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, false, false, true);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, false, false, true);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, false, false, true);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, false, false, true);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, false, false, true);

    }

    @Test
    public void testInterpolatorBilinearNewROIBounds() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, true, false, true);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, true, false, true);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, true, false, true);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, true, false, true);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, true, false, true);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, true, false, true);
    }

    @Test
    public void testInterpolatorBilinearNewROIAccessor() {
        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, true, true, true);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, true, true, true);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, true, true, true);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, true, true, true);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, true, true, true);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, true, true, true);
    }

    @Test
    public void testInterpolatorBicubicNew() {

        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, false, false, false);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, false, false, false);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, false, false, false);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, false, false, false);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, false, false, false);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, false, false, false);
    }

    @Test
    public void testInterpolatorBicubicNewROIBounds() {

        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }

        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, true, false, false);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, true, false, false);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, true, false, false);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, true, false, false);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, true, false, false);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, true, false, false);
    }

    @Test
    public void testInterpolatorBicubicNewROIAccessor() {

        // Data Initialization and image creation
        if (!initialSetup) {
            initialSetup();
            initialSetup = true;
        }
        int dataType = DataBuffer.TYPE_BYTE;
        bilinearBicubicMethod(dataType, true, true, false);

        dataType = DataBuffer.TYPE_USHORT;
        bilinearBicubicMethod(dataType, true, true, false);

        dataType = DataBuffer.TYPE_SHORT;
        bilinearBicubicMethod(dataType, true, true, false);

        dataType = DataBuffer.TYPE_INT;
        bilinearBicubicMethod(dataType, true, true, false);

        dataType = DataBuffer.TYPE_FLOAT;
        bilinearBicubicMethod(dataType, true, true, false);

        dataType = DataBuffer.TYPE_DOUBLE;
        bilinearBicubicMethod(dataType, true, true, false);
    }

    // --------------------------------Nearest-Neighbor-----------------
    private void nearestMethod(int dataType, boolean roiUsed, boolean useROIAccessor) {
        InterpolationNearest interpN = null;
        Number expected = null;
        Number testInterpolationValue = null;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            expected = src[dataType].getByteDataArray(0)[posx + posy];
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            expected = src[dataType].getShortDataArray(0)[posx + posy];
            break;
        case DataBuffer.TYPE_INT:
            expected = src[dataType].getIntDataArray(0)[posx + posy];
            break;
        case DataBuffer.TYPE_FLOAT:
            expected = src[dataType].getFloatDataArray(0)[posx + posy];
            break;
        case DataBuffer.TYPE_DOUBLE:
            expected = src[dataType].getDoubleDataArray(0)[posx + posy];
            break;
        }
        if (roiUsed) {
            if (useROIAccessor) {
                int windex = posx + posYroi;

                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    byte wB = (byte) (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .byteValue() & 0xff : 0);
                    if (wB == 0) {
                        expected = (byte) destinationNoData;
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    short wUS = (short) (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .shortValue() & 0xffff : 0);
                    if (wUS == 0) {
                        expected = (short) destinationNoData;
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    short wS = (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .shortValue() : 0);
                    if (wS == 0) {
                        expected = (short) destinationNoData;
                    }
                    break;
                case DataBuffer.TYPE_INT:
                    int wI = (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .intValue() : 0);
                    if (wI == 0) {
                        expected = (int) destinationNoData;
                    }
                    break;
                case DataBuffer.TYPE_FLOAT:
                    float wF = (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .floatValue() : 0);
                    if (wF == 0) {
                        expected = (float) destinationNoData;
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    double wD = (windex < roiDataArray[dataType].length ? roiDataArray[dataType][windex]
                            .doubleValue() : 0);
                    if (wD == 0) {
                        expected = destinationNoData;
                    }
                    break;
                }
                interpN = new InterpolationNearest(null, useROIAccessor, destinationNoData,
                        dataType);
                testInterpolationValue = interpN.interpolate(src[dataType], 0, 1, posx, posy,
                        posYroi, roiAccessor, null, false);
            } else {
                int x0 = src[dataType].getX() + posx / src[dataType].getPixelStride();
                int y0 = src[dataType].getY() + (posy - src[dataType].getBandOffset(0))
                        / src[dataType].getScanlineStride();

                if (!roiBounds.contains(x0, y0)) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(null, useROIAccessor, destinationNoData,
                        dataType);
                interpN.setROIBounds(roiBounds);
                testInterpolationValue = interpN.interpolate(src[dataType], 0, 1, posx, posy, null,
                        null, roiIter, false);
            }
        } else {
            // Expected value
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                Range noDataRangeB = RangeFactory.create((byte) noData, true, (byte) noData, true);
                if (noDataRangeB.contains(expected.byteValue())) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(noDataRangeB, useROIAccessor,
                        destinationNoData, dataType);
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                Range noDataRangeS = RangeFactory.create((short) noData, true, (short) noData,
                        true);
                if (noDataRangeS.contains(expected.shortValue())) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(noDataRangeS, useROIAccessor,
                        destinationNoData, dataType);
                break;
            case DataBuffer.TYPE_INT:
                Range noDataRangeI = RangeFactory.create((int) noData, true, (int) noData,
                        true);
                if (noDataRangeI.contains(expected.intValue())) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(noDataRangeI,  useROIAccessor,
                        destinationNoData, dataType);
                break;
            case DataBuffer.TYPE_FLOAT:
                Range noDataRangeF = RangeFactory.create((float) noData, true, (float) noData,
                        true,true);
                if (noDataRangeF.contains(expected.floatValue())) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(noDataRangeF, useROIAccessor,
                        destinationNoData, dataType);
                break;
            case DataBuffer.TYPE_DOUBLE:
                Range noDataRangeD = RangeFactory.create(noData, true, noData, true,true);
                if (noDataRangeD.contains(expected.doubleValue())) {
                    expected = destinationNoData;
                }
                interpN = new InterpolationNearest(noDataRangeD,  useROIAccessor,
                        destinationNoData, dataType);
                break;
            }

            // Interpolation
            testInterpolationValue = interpN.interpolate(src[dataType], 0, 1, posx, posy, null,
                    null, roiIter, false);
        }
        // Test control
        assertEquality(dataType, expected, testInterpolationValue);
    }

    private Number bicubicCalculation(int dataType, Number[][] pixelArray, int[][] weightArray,
            InterpolationBicubic interpBN) {
        // Retrieval of the interpolation tables
        int[] dataHi = interpBN.getHorizontalTableData();
        int[] dataVi = interpBN.getVerticalTableData();

        float[] dataHf = interpBN.getHorizontalTableDataFloat();
        float[] dataVf = interpBN.getVerticalTableDataFloat();

        double[] dataHd = interpBN.getHorizontalTableDataDouble();
        double[] dataVd = interpBN.getVerticalTableDataDouble();

        // Rounding value
        int round = 1 << (precisionBits - 1);
        // fractional values
        int xfrac = fracvalues[0].intValue();
        int yfrac = fracvalues[1].intValue();
        // Table index
        int offsetX = 4 * xfrac;
        int offsetY = 4 * yfrac;

        // Array initialization for storing the value of the sum for every table row
        long[] rowSum = new long[4];
        float[] rowSumF = new float[4];
        double[] rowSumD = new double[4];
        // Temporary sum variable
        int temp = 0;
        float tempf = 0;
        double tempd = 0;
        //Impainting for no Data Values 
        //boolean array for evaluating if every weight line is composed by 0
        boolean[] weight0 = new boolean[4];
        // Array containg only 1 value, an array of all the 
        for(int ii =0;ii<4;ii++){
        	int[][] lineArray={weightArray[ii]};
        	if(sumWeight(lineArray)==0){
        		weight0[ii]=true;
        	}else{
        		weight0[ii]=false;
        	}
        }
        long[] valueArray=null;
        float[] valueArrayF=null;
        double[] valueArrayD=null;
        
        int value_=0;
        int value0=0;
        int value1=0;
        int value2=0;
        
        float value_f=0;
        float value0f=0;
        float value1f=0;
        float value2f=0;
        
        double value_d=0;
        double value0d=0;
        double value1d=0;
        double value2d=0;
        // Bicubic interpolation with interpolation table

        for (int k = 0; k < rowSum.length; k++) {
            rowSum[k] = 0;
            rowSumF[k] = 0;
            rowSumD[k] = 0;
            
            //Inpainting of the no data values by substituting them with the neighbor values  
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                value_ = pixelArray[k][0].byteValue() & 0xff;
                value0 = pixelArray[k][1].byteValue() & 0xff;
                value1 = pixelArray[k][2].byteValue() & 0xff;
                value2 = pixelArray[k][3].byteValue() & 0xff;
                valueArray=bicubicInpainting(value_, value0, value1, value2, weightArray[k], null);
                break;
            case DataBuffer.TYPE_USHORT:
            	value_ = pixelArray[k][0].shortValue() & 0xffff;
                value0 = pixelArray[k][1].shortValue() & 0xffff;
                value1 = pixelArray[k][2].shortValue() & 0xffff;
                value2 = pixelArray[k][3].shortValue() & 0xffff;
                valueArray=bicubicInpainting(value_, value0, value1, value2, weightArray[k], null);
                break;
            case DataBuffer.TYPE_SHORT:
            	value_ = pixelArray[k][0].shortValue();
                value0 = pixelArray[k][1].shortValue();
                value1 = pixelArray[k][2].shortValue();
                value2 = pixelArray[k][3].shortValue();
                valueArray=bicubicInpainting(value_, value0, value1, value2, weightArray[k], null);
                break;
            case DataBuffer.TYPE_INT:
            	value_ = pixelArray[k][0].intValue();
                value0 = pixelArray[k][1].intValue();
                value1 = pixelArray[k][2].intValue();
                value2 = pixelArray[k][3].intValue();
                valueArray=bicubicInpainting(value_, value0, value1, value2, weightArray[k], null);
                break;
            case DataBuffer.TYPE_FLOAT:
            	value_f = pixelArray[k][0].floatValue();
                value0f = pixelArray[k][1].floatValue();
                value1f = pixelArray[k][2].floatValue();
                value2f = pixelArray[k][3].floatValue();
                valueArrayF=bicubicInpaintingFloat(value_f, value0f, value1f, value2f, weightArray[k], null);
                break;
            case DataBuffer.TYPE_DOUBLE:
            	value_d = pixelArray[k][0].doubleValue();
                value0d = pixelArray[k][1].doubleValue();
                value1d = pixelArray[k][2].doubleValue();
                value2d = pixelArray[k][3].doubleValue();
                valueArrayD=bicubicInpaintingDouble(value_d, value0d, value1d, value2d, weightArray[k], null);
                break;
            }
        

            for (int h = 0; h < rowSum.length; h++) {
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_INT:
                    rowSum[k] += dataHi[offsetX + h] * valueArray[h];
                    break;
                case DataBuffer.TYPE_FLOAT:
                    rowSumF[k] += dataHf[offsetX + h] * valueArrayF[h];
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    rowSumD[k] += dataHd[offsetX + h] * valueArrayD[h];
                    break;
                }
            }
            // Rounding
            rowSum[k] = (rowSum[k] + round) >> precisionBits;

        }

        //Inpainting of the no data values by substituting them with the neighbor values        
        long[] valueArrayV=null;  
        float[] valueArrayVF=null;  
        double[] valueArrayVD=null;  
        
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
        	valueArrayV=bicubicInpainting(rowSum[0], rowSum[1], rowSum[2], rowSum[3], null,weight0);
            break;
        case DataBuffer.TYPE_FLOAT:
        	valueArrayVF=bicubicInpaintingFloat(rowSumF[0], rowSumF[1], rowSumF[2], rowSumF[3], null,weight0);
            break;
        case DataBuffer.TYPE_DOUBLE:
        	valueArrayVD=bicubicInpaintingDouble(rowSumD[0], rowSumD[1], rowSumD[2], rowSumD[3], null,weight0);
            break;
        }
        
        
        for(int k=0;k<rowSum.length; k++){
            // Updating value
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                temp += (long) dataVi[offsetY + k] * valueArrayV[k];
                break;
            case DataBuffer.TYPE_FLOAT:
                tempf += dataVf[offsetY + k] * valueArrayVF[k];
                break;
            case DataBuffer.TYPE_DOUBLE:
                tempd += dataVd[offsetY + k] * valueArrayVD[k];
                break;
            }
        }

        temp = (int) ((temp + round) >> precisionBits);

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // clamp the value to byte range
            if (temp > 255) {
                temp = 255;
            } else if (temp < 0) {
                temp = 0;
            }
            return temp;
        case DataBuffer.TYPE_USHORT:
            // clamp the value to byte range
            if (temp > 65536) {
                temp = 65536;
            } else if (temp < 0) {
                temp = 0;
            }
            return temp;
        case DataBuffer.TYPE_SHORT:
            if (temp > Short.MAX_VALUE) {
                temp = Short.MAX_VALUE;
            } else if (temp < Short.MIN_VALUE) {
                temp = Short.MIN_VALUE;
            }
            return temp;
        case DataBuffer.TYPE_INT:
            return temp;
        case DataBuffer.TYPE_FLOAT:
            if (tempf > Float.MAX_VALUE) {
                tempf = Float.MAX_VALUE;
            } else if (tempf < -Float.MAX_VALUE) {
                tempf = -Float.MAX_VALUE;
            }
            return tempf;
        case DataBuffer.TYPE_DOUBLE:
            return tempd;
        }

        return 0;
    }

    
    
    //This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private long[] bicubicInpainting(long s_, long s0, long s1, long s2, int[] weightArray, boolean[] weight0){
    	if(weightArray == null){
    		weightArray=new int[4];
    		if(s_==0 && weight0[0]){
    			weightArray[0]=0;
    		}else{
    			weightArray[0]=1;
    		}
    		if(s0==0 && weight0[1]){
    			weightArray[1]=0;
    		}else{
    			weightArray[1]=1;
    		}
    		if(s1==0 && weight0[2]){
    			weightArray[2]=0;
    		}else{
    			weightArray[2]=1;
    		}
    		if(s2==0 && weight0[3]){
    			weightArray[3]=0;
    		}else{
    			weightArray[3]=1;
    		}
    	}
    	
    	int[][] array = {weightArray};
    	//empty array containing the final values of the selected 4 pixels
    	long[] emptyArray=new long[4];
    	
    	//Calculation of the number of data
    	int sum = (int) sumWeight(array);
    	// mean value used in calculations
    	long meanValue=0;
    	switch(sum){
    	// All the 4 pixels are no data, an array of 0 data is returned
    	case 0:
    		return emptyArray;
		// Only one pixel is a valid data, all the pixel of the line have the same value.
    	case 1:
    		long validData=0;
    		if(weightArray[0]==1){
    			validData=s_;
    		}else if(weightArray[1]==1){
    			validData=s0;
    		}else if(weightArray[2]==1){
    			validData=s1;
    		}else{
    			validData=s2;
    		}    		
    		emptyArray[0]=validData;
    		emptyArray[1]=validData;
    		emptyArray[2]=validData;
    		emptyArray[3]=validData;    		
    		return emptyArray;
		// Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
    	// else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better 
		// comprehension. 0 is no Data and x is valid data.
    	case 2:
    		
    		// 0 0 x x
    		if(weightArray[0]==0 && weightArray[1]==0){
    			emptyArray[0]=s1;
        		emptyArray[1]=s1;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// 0 x 0 x
    		}else if(weightArray[0]==0 && weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// 0 x x 0
    		}else if(weightArray[0]==0 && weightArray[3]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x 0 0 x
    		}else if(weightArray[1]==0 && weightArray[2]==0){
    			meanValue= (s_ + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x 0 x 0
    		}else if(weightArray[1]==0 && weightArray[3]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x x 0 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s0;
        		emptyArray[3]=s0;
    		}
    		return emptyArray;
    	// Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
    	// of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better 
    	// comprehension. 0 is no Data and x is valid data.
    	case 3:    		
    		// 0 x x x
    		if(weightArray[0]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x 0 x x
    		}else if(weightArray[1]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x x 0 x
    		}else if(weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x x x 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
    		} 
    		return emptyArray;
		// Absence of No Data, the pixels are returned.
    	case 4:
    		emptyArray[0]=s_;
    		emptyArray[1]=s0;
    		emptyArray[2]=s1;
    		emptyArray[3]=s2;    		
    		return emptyArray;
		default:
			throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
    	}
    }
     
    
    //This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private float[] bicubicInpaintingFloat(float s_, float s0, float s1, float s2, int[] weightArray, boolean[] weight0){
    	if(weightArray == null){
    		weightArray=new int[4];
    		if(s_==0 && weight0[0]){
    			weightArray[0]=0;
    		}else{
    			weightArray[0]=1;
    		}
    		if(s0==0 && weight0[1]){
    			weightArray[1]=0;
    		}else{
    			weightArray[1]=1;
    		}
    		if(s1==0 && weight0[2]){
    			weightArray[2]=0;
    		}else{
    			weightArray[2]=1;
    		}
    		if(s2==0 && weight0[3]){
    			weightArray[3]=0;
    		}else{
    			weightArray[3]=1;
    		}
    	}
    	
    	int[][] array = {weightArray};
    	//empty array containing the final values of the selected 4 pixels
    	float[] emptyArray=new float[4];
    	
    	//Calculation of the number of data
    	int sum = (int) sumWeight(array);
    	// mean value used in calculations
    	float meanValue=0;
    	switch(sum){
    	// All the 4 pixels are no data, an array of 0 data is returned
    	case 0:
    		return emptyArray;
		// Only one pixel is a valid data, all the pixel of the line have the same value.
    	case 1:
    		float validData=0;
    		if(weightArray[0]==1){
    			validData=s_;
    		}else if(weightArray[1]==1){
    			validData=s0;
    		}else if(weightArray[2]==1){
    			validData=s1;
    		}else{
    			validData=s2;
    		}    		
    		emptyArray[0]=validData;
    		emptyArray[1]=validData;
    		emptyArray[2]=validData;
    		emptyArray[3]=validData;    		
    		return emptyArray;
		// Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
    	// else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better 
		// comprehension. 0 is no Data and x is valid data.
    	case 2:
    		
    		// 0 0 x x
    		if(weightArray[0]==0 && weightArray[1]==0){
    			emptyArray[0]=s1;
        		emptyArray[1]=s1;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// 0 x 0 x
    		}else if(weightArray[0]==0 && weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// 0 x x 0
    		}else if(weightArray[0]==0 && weightArray[3]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x 0 0 x
    		}else if(weightArray[1]==0 && weightArray[2]==0){
    			meanValue= (s_ + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x 0 x 0
    		}else if(weightArray[1]==0 && weightArray[3]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x x 0 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s0;
        		emptyArray[3]=s0;
    		}
    		return emptyArray;
    	// Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
    	// of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better 
    	// comprehension. 0 is no Data and x is valid data.
    	case 3:    		
    		// 0 x x x
    		if(weightArray[0]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x 0 x x
    		}else if(weightArray[1]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x x 0 x
    		}else if(weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x x x 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
    		} 
    		return emptyArray;
		// Absence of No Data, the pixels are returned.
    	case 4:
    		emptyArray[0]=s_;
    		emptyArray[1]=s0;
    		emptyArray[2]=s1;
    		emptyArray[3]=s2;    		
    		return emptyArray;
		default:
			throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
    	}
    }
    
    //This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private double[] bicubicInpaintingDouble(double s_, double s0, double s1, double s2, int[] weightArray, boolean[] weight0){
    	if(weightArray == null){
    		weightArray=new int[4];
    		if(s_==0 && weight0[0]){
    			weightArray[0]=0;
    		}else{
    			weightArray[0]=1;
    		}
    		if(s0==0 && weight0[1]){
    			weightArray[1]=0;
    		}else{
    			weightArray[1]=1;
    		}
    		if(s1==0 && weight0[2]){
    			weightArray[2]=0;
    		}else{
    			weightArray[2]=1;
    		}
    		if(s2==0 && weight0[3]){
    			weightArray[3]=0;
    		}else{
    			weightArray[3]=1;
    		}
    	}
    	
    	int[][] array = {weightArray};
    	//empty array containing the final values of the selected 4 pixels
    	double[] emptyArray=new double[4];
    	
    	//Calculation of the number of data
    	int sum = (int) sumWeight(array);
    	// mean value used in calculations
    	double meanValue=0;
    	switch(sum){
    	// All the 4 pixels are no data, an array of 0 data is returned
    	case 0:
    		return emptyArray;
		// Only one pixel is a valid data, all the pixel of the line have the same value.
    	case 1:
    		double validData=0;
    		if(weightArray[0]==1){
    			validData=s_;
    		}else if(weightArray[1]==1){
    			validData=s0;
    		}else if(weightArray[2]==1){
    			validData=s1;
    		}else{
    			validData=s2;
    		}    		
    		emptyArray[0]=validData;
    		emptyArray[1]=validData;
    		emptyArray[2]=validData;
    		emptyArray[3]=validData;    		
    		return emptyArray;
		// Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
    	// else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better 
		// comprehension. 0 is no Data and x is valid data.
    	case 2:
    		
    		// 0 0 x x
    		if(weightArray[0]==0 && weightArray[1]==0){
    			emptyArray[0]=s1;
        		emptyArray[1]=s1;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// 0 x 0 x
    		}else if(weightArray[0]==0 && weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// 0 x x 0
    		}else if(weightArray[0]==0 && weightArray[3]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x 0 0 x
    		}else if(weightArray[1]==0 && weightArray[2]==0){
    			meanValue= (s_ + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x 0 x 0
    		}else if(weightArray[1]==0 && weightArray[3]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
        	// x x 0 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s0;
        		emptyArray[3]=s0;
    		}
    		return emptyArray;
    	// Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
    	// of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better 
    	// comprehension. 0 is no Data and x is valid data.
    	case 3:    		
    		// 0 x x x
    		if(weightArray[0]==0){
    			emptyArray[0]=s0;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x 0 x x
    		}else if(weightArray[1]==0){
    			meanValue= (s_ + s1)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=meanValue;
        		emptyArray[2]=s1;
        		emptyArray[3]=s2;
        	// x x 0 x
    		}else if(weightArray[2]==0){
    			meanValue= (s0 + s2)/2;
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=meanValue;
        		emptyArray[3]=s2;
        	// x x x 0
    		}else{
    			emptyArray[0]=s_;
        		emptyArray[1]=s0;
        		emptyArray[2]=s1;
        		emptyArray[3]=s1;
    		} 
    		return emptyArray;
		// Absence of No Data, the pixels are returned.
    	case 4:
    		emptyArray[0]=s_;
    		emptyArray[1]=s0;
    		emptyArray[2]=s1;
    		emptyArray[3]=s2;    		
    		return emptyArray;
		default:
			throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
    	}
    }
    
    
    
    
    
    // --------------------------------Bilinear-Bicubic----------------
    private void bilinearBicubicMethod(int dataType, boolean roiUsed, boolean useROIAccessor,
            boolean bilinearUsed) {
        InterpolationBilinear interpB = null;
        InterpolationBicubic interpBN = null;
        Number expected = 0;
        Number testInterpolationValue = null;
        Number[][] pixelArray = pixelCalculation(bilinearUsed, dataType);

        int sum = 0;
        // Array initialization
        int[][] weightArray = null;

        if (bilinearUsed) {
            weightArray = new int[2][2];
        } else {
            weightArray = new int[4][4];
        }

        if (roiUsed) {
            if (useROIAccessor) {
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(subsampleBits, null,  true,
                            destinationNoData, dataType);
                    // Interpolation
                    testInterpolationValue = interpB.interpolate(src[dataType], 0, 1, posx, posy,
                            fracvalues, posYroi, roiAccessor, roiIter, false);
                } else {
                    interpBN = new InterpolationBicubic(subsampleBits, null,  true,
                            destinationNoData, dataType, false, precisionBits);                    
                    // Interpolation
                    testInterpolationValue = interpBN.interpolate(src[dataType], 0, 1, posx, posy,
                            fracvalues, posYroi, roiAccessor, roiIter, false);
                }
                weightArray = roiAccessorCheck(dataType, pixelArray, weightArray, bilinearUsed);
            } else {
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(subsampleBits, null,  false,
                            destinationNoData, dataType);
                    interpB.setROIBounds(roiBounds);
                    // Interpolation
                    testInterpolationValue = interpB.interpolate(src[dataType], 0, 1, posx, posy,
                            fracvalues, null, null, roiIter, false);
                } else {
                    interpBN = new InterpolationBicubic(subsampleBits, null,  false,
                            destinationNoData, dataType, false, precisionBits);
                    interpBN.setROIBounds(roiBounds);
                    // Interpolation
                    testInterpolationValue = interpBN.interpolate(src[dataType], 0, 1, posx, posy,
                            fracvalues, null, null, roiIter, false);
                }
                weightArray = roiBoundCheck(dataType, pixelArray, weightArray, bilinearUsed);
            }
            
            sum = sumWeight(weightArray);
            if(sum>0){
            	for (int i = 0; i < weightArray.length; i++) {
                    for (int j = 0; j < weightArray.length; j++) {
                    	weightArray[i][j]=1;
                    }
                }
            }
            
        } else {
            // Expected value
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                Range noDataRangeB = RangeFactory.create((byte) noData, true, (byte) noData, true);
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeB,
                             useROIAccessor, destinationNoData, dataType);
                } else {
                    interpBN = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRangeB,
                            useROIAccessor, destinationNoData, dataType, false,
                            precisionBits);
                }
                weightArray = noDataCheck(dataType, pixelArray, weightArray, noDataRangeB);
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                Range noDataRangeS = RangeFactory.create((short) noData, true, (short) noData,
                        true);
                interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeS, 
                        useROIAccessor, destinationNoData, dataType);
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeS,
                             useROIAccessor, destinationNoData, dataType);
                } else {
                    interpBN = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRangeS,
                             useROIAccessor, destinationNoData, dataType, false,
                            precisionBits);
                }
                weightArray = noDataCheck(dataType, pixelArray, weightArray, noDataRangeS);
                break;
            case DataBuffer.TYPE_INT:
                Range noDataRangeI = RangeFactory.create((int) noData, true, (int) noData,
                        true);
                interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeI, 
                        useROIAccessor, destinationNoData, dataType);
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeI,
                             useROIAccessor, destinationNoData, dataType);
                } else {
                    interpBN = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRangeI,
                             useROIAccessor, destinationNoData, dataType, false,
                            precisionBits);
                }
                weightArray = noDataCheck(dataType, pixelArray, weightArray, noDataRangeI);
                break;
            case DataBuffer.TYPE_FLOAT:
                Range noDataRangeF = RangeFactory.create((float) noData, true, (float) noData,
                        true,true);
                interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeF, 
                        useROIAccessor, destinationNoData, dataType);
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeF,
                             useROIAccessor, destinationNoData, dataType);
                } else {
                    interpBN = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRangeF,
                             useROIAccessor, destinationNoData, dataType,  false,
                            precisionBits);
                }
                weightArray = noDataCheck(dataType, pixelArray, weightArray, noDataRangeF);
                break;
            case DataBuffer.TYPE_DOUBLE:
                Range noDataRangeD = RangeFactory.create(noData, true, noData, true,true);
                interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeD, 
                        useROIAccessor, destinationNoData, dataType);
                if (bilinearUsed) {
                    interpB = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRangeD,
                             useROIAccessor, destinationNoData, dataType);
                } else {
                    interpBN = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRangeD,
                             useROIAccessor, destinationNoData, dataType, false,
                            precisionBits);
                }
                weightArray = noDataCheck(dataType, pixelArray, weightArray, noDataRangeD);
                break;
            }
            // Interpolation
            if (bilinearUsed) {
                testInterpolationValue = interpB.interpolate(src[dataType], 0, 1, posx, posy,
                        fracvalues, null, null, roiIter, false);
            } else {
                testInterpolationValue = interpBN.interpolate(src[dataType], 0, 1, posx, posy,
                        fracvalues, null, null, roiIter, false);
            }
        }
        sum = sumWeight(weightArray);
        if (sum == 0) {
            expected = (byte) destinationNoData;
        } else {
            if (bilinearUsed) {
                // Perform the bilinear interpolation
                if (dataType < DataBuffer.TYPE_FLOAT) {
                    expected = computeValue(dataType, pixelArray[0][0].intValue(),
                            pixelArray[0][1].intValue(), pixelArray[1][0].intValue(),
                            pixelArray[1][1].intValue(), weightArray[0][0], weightArray[0][1],
                            weightArray[1][0], weightArray[1][1], fracvalues[0].intValue(),
                            fracvalues[1].intValue());
                } else {
                    expected = computeValueDouble(dataType, pixelArray[0][0].doubleValue(),
                            pixelArray[0][1].doubleValue(), pixelArray[1][0].doubleValue(),
                            pixelArray[1][1].doubleValue(), weightArray[0][0], weightArray[0][1],
                            weightArray[1][0], weightArray[1][1], fracvalues[0].intValue(),
                            fracvalues[1].intValue());
                }

            } else {
                // Performs the bicubic interpolation
                expected = bicubicCalculation(dataType, pixelArray, weightArray, interpBN);
            }
        }
        // Test control
        assertEquality(dataType, expected, testInterpolationValue);
    }

    public void assertEquality(int dataType, Number expected, Number testInterpolationValue) {
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            assertEquals("These values must be equal", expected.byteValue(),
                    testInterpolationValue.byteValue());
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            assertEquals("These values must be equal", expected.shortValue(),
                    testInterpolationValue.shortValue());
            break;
        case DataBuffer.TYPE_INT:
            assertEquals("These values must be equal", expected.intValue(),
                    testInterpolationValue.intValue());
            break;
        case DataBuffer.TYPE_FLOAT:
            assertEquals("These values must be equal", expected.floatValue(),
                    testInterpolationValue.floatValue(), DEFAULT_DELTA);
            break;
        case DataBuffer.TYPE_DOUBLE:
            assertEquals("These values must be equal", expected.doubleValue(),
                    testInterpolationValue.doubleValue(), DEFAULT_DELTA);
            break;
        }
    }

    public  int[][] noDataCheck(int dataType,
            Number[][] pixelArray, int[][] weightArray, Range noDataRange) {
        for (int i = 0; i < pixelArray.length; i++) {
            for (int j = 0; j < pixelArray.length; j++) {
                
                switch(dataType){
                case DataBuffer.TYPE_BYTE:
                    if (noDataRange.contains(pixelArray[i][j].byteValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    if (noDataRange.contains( pixelArray[i][j].shortValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    if (noDataRange.contains( pixelArray[i][j].shortValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                case DataBuffer.TYPE_INT:
                    if (noDataRange.contains( pixelArray[i][j].intValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                case DataBuffer.TYPE_FLOAT:
                    if (noDataRange.contains( pixelArray[i][j].floatValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    if (noDataRange.contains(pixelArray[i][j].doubleValue())) {
                        weightArray[i][j] = 0;
                    } else {
                        weightArray[i][j] = 1;
                    }
                    break;
                    default:
                        throw new IllegalArgumentException("Wrong data type");
                }
            }
        }
        return weightArray;
    }

    public int[][] roiBoundCheck(int dataType, Number[][] pixelArray, int[][] weightArray,
            boolean bilinearUsed) {
        int x0 = src[dataType].getX() + posx / src[dataType].getPixelStride();
        int y0 = src[dataType].getY() + (posy - src[dataType].getBandOffset(0))
                / src[dataType].getScanlineStride();
        // get the 4/16 weight
        if (roiBounds.contains(x0, y0)) {
            for (int i = 0; i < pixelArray.length; i++) {
                for (int j = 0; j < pixelArray.length; j++) {
                    if (bilinearUsed) {
                        weightArray[i][j] = roiIter.getSample(x0 + j, y0 + i, 0);
                    } else {
                        weightArray[i][j] = roiIter.getSample(x0 + (j - 1), y0 + (i - 1), 0);
                    }

                }
            }
        }
        return weightArray;
    }

    public int[][] roiAccessorCheck(int dataType, Number[][] pixelArray, int[][] weightArray,
            boolean bilinearUsed) {
        int[][] weightArrayIndex = weightArray;
        int roiDataLength = roiDataArray[dataType].length;
        // get the 4/16 weight
        for (int i = 0; i < pixelArray.length; i++) {
            for (int j = 0; j < pixelArray.length; j++) {
                
                if (bilinearUsed) {
                    weightArrayIndex[i][j] = posx + j + posYroi
                            + (i * roiAccessor.getScanlineStride());                                        
                } else {
                    weightArrayIndex[i][j] = posx + (j - 1) + posYroi
                            + ((i - 1) * roiAccessor.getScanlineStride());
                }
                weightArray[i][j] = (weightArrayIndex[i][j] < roiDataLength ? (roiDataArray[dataType][weightArrayIndex[i][j]]
                        .intValue()) : 0);
            }
        }
        
        if(bilinearUsed&&(weightArrayIndex[0][0]>roiDataLength || roiDataArray[dataType][weightArrayIndex[0][0]].intValue()==0)){
        	for (int i = 0; i < pixelArray.length; i++) {
                for (int j = 0; j < pixelArray.length; j++) {
                	weightArray[i][j] =0;
                }
            }
        }else if(!bilinearUsed&&(weightArrayIndex[1][1]>roiDataLength || roiDataArray[dataType][weightArrayIndex[1][1]].intValue()==0)){
        	for (int i = 0; i < pixelArray.length; i++) {
                for (int j = 0; j < pixelArray.length; j++) {
                	weightArray[i][j] =0;
                }
            }
        }
        
        return weightArray;
    }

    public int sumWeight(int[][] weightArray) {
        int sum = 0;
        for (int i = 0; i < weightArray.length; i++) {
            for (int j = 0; j < weightArray[i].length; j++) {
                sum += weightArray[i][j];
            }
        }
        return sum;
    }

    private Number[][] pixelCalculation(boolean bilinearUsed, int datatype) {

        Number[][] pixelArray = null;
        Number[] srcData = null;
        // Stride field
        int srcPixelStride = src[datatype].getPixelStride();
        int srcLineStride = src[datatype].getScanlineStride();
        // retrieve of the source data
        switch (datatype) {
        case DataBuffer.TYPE_BYTE:
            byte[] srcDataB = src[datatype].getByteDataArray(0);
            srcData = new Number[srcDataB.length];
            for (int i = 0; i < srcDataB.length; i++) {
                srcData[i] = srcDataB[i];
            }
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            short[] srcDataS = src[datatype].getShortDataArray(0);
            srcData = new Number[srcDataS.length];
            for (int i = 0; i < srcDataS.length; i++) {
                srcData[i] = srcDataS[i];
            }
            break;
        case DataBuffer.TYPE_INT:
            int[] srcDataI = src[datatype].getIntDataArray(0);
            srcData = new Number[srcDataI.length];
            for (int i = 0; i < srcDataI.length; i++) {
                srcData[i] = srcDataI[i];
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            float[] srcDataF = src[datatype].getFloatDataArray(0);
            srcData = new Number[srcDataF.length];
            for (int i = 0; i < srcDataF.length; i++) {
                srcData[i] = srcDataF[i];
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            double[] srcDataD = src[datatype].getDoubleDataArray(0);
            srcData = new Number[srcDataD.length];
            for (int i = 0; i < srcDataD.length; i++) {
                srcData[i] = srcDataD[i];
            }
            break;
        default:
            break;
        }

        if (bilinearUsed) {
            // initialize the pixel array
            pixelArray = new Number[2][2];
            // Get the 16 surrounding pixel values
            for (int i = 0; i < pixelArray.length; i++) {
                for (int j = 0; j < pixelArray.length; j++) {
                    switch (datatype) {
                    case DataBuffer.TYPE_BYTE:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].byteValue() & 0xff;
                        break;
                    case DataBuffer.TYPE_USHORT:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].shortValue() & 0xffff;
                        break;
                    case DataBuffer.TYPE_SHORT:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].shortValue();
                        break;
                    case DataBuffer.TYPE_INT:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].intValue();
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].floatValue();
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        pixelArray[i][j] = srcData[posx + i * srcLineStride + posy + j
                                * srcPixelStride].doubleValue();
                        break;
                    default:
                        break;
                    }
                }
            }
        } else {
            // initialize the pixel array
            pixelArray = new Number[4][4];
            // Get the 16 surrounding pixel values
            for (int i = 0; i < pixelArray.length; i++) {
                for (int j = 0; j < pixelArray.length; j++) {
                    switch (datatype) {
                    case DataBuffer.TYPE_BYTE:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].byteValue() & 0xff;
                        break;
                    case DataBuffer.TYPE_USHORT:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].shortValue() & 0xffff;
                        break;
                    case DataBuffer.TYPE_SHORT:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].shortValue();
                        break;
                    case DataBuffer.TYPE_INT:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].intValue();
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].floatValue();
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        pixelArray[i][j] = srcData[posx + (i - 1) * srcLineStride + posy + (j - 1)
                                * srcPixelStride].doubleValue();
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        return pixelArray;

    }

    private void initialSetup() {
        // Set the setup to true
        this.initialSetup = true;
        // initialize data
        noData = 1;
        // dataType = DataBuffer.TYPE_BYTE;
        one = 1 << subsampleBits;
        shift2 = 2 * subsampleBits;
        round2 = 1 << (shift2 - 1);
        destinationNoData = 80;

        testImages = new RenderedImage[DataBuffer.TYPE_DOUBLE + 1];
        Raster[] testRasters = new Raster[testImages.length];
        // Cycle for creating all of the various image, each of them with a different dataType
        for (int s = 0; s < testImages.length; s++) {
            testImages[s] = getSyntheticNotUniformImage(1, s, noData);
            // Sets the source and destination images
            RenderedImage temporaryImage = testImages[s];
            // Index of the first tile
            int minTileX = temporaryImage.getMinTileX();
            int minTileY = temporaryImage.getMinTileY();
            // Raster selection
            testRasters[s] = temporaryImage.getTile(minTileX, minTileY);
        }
        Raster testRaster = testRasters[0];
        // Rectangle calculated from the selected raster
        Rectangle srcRect = testRaster.getBounds();

        // ROI creation
        roiBounds = new Rectangle(0, 0, testRaster.getWidth() / 2, testRaster.getHeight() / 2);

        ROIShape roiRect = new ROIShape(roiBounds);

        PlanarImage roiImage = roiRect.getAsImage();

        Raster roiData = roiImage.getExtendedData(testRaster.getBounds(),
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        roiIter = RandomIterFactory.create(roiData, testRaster.getBounds(),true,true);

        // roi raster accessor creation

        Raster roiDataAcc = roiImage.getExtendedData(testRaster.getBounds(),
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        roiAccessor = new RasterAccessor(roiDataAcc, testRaster.getBounds(),
                RasterAccessor.findCompatibleTags(new RenderedImage[] { roiImage }, roiImage)[0],
                roiImage.getColorModel());

        // SOURCE RASTER AND ROI DATA ARRAY CREATION
        src = new RasterAccessor[DataBuffer.TYPE_DOUBLE + 1];
        roiDataArray = new Number[src.length][];
        for (int z = 0; z < src.length; z++) {
            // Rendered image is inserted into an array for finding the compatible tags
            RenderedImage[] arrayIMG = { testImages[z] };
            RasterFormatTag[] formatTags = RasterAccessor.findCompatibleTags(arrayIMG,
                    testImages[z]);
            // src raster accessor creation
            src[z] = new RasterAccessor(testRasters[z], srcRect, formatTags[0],
                    testImages[z].getColorModel());

            byte[] roiDataArrayByte = roiAccessor.getByteDataArray(0);
            roiDataArray[z] = new Number[roiDataArrayByte.length];
            switch (z) {
            case DataBuffer.TYPE_BYTE:
                // Storing the roi data in an array
                for (int i = 0; i < roiDataArrayByte.length; i++) {
                    roiDataArray[z][i] = roiDataArrayByte[i];
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                for (int i = 0; i < roiDataArrayByte.length; i++) {
                    roiDataArray[z][i] = (short) roiDataArrayByte[i];
                }
                break;
            case DataBuffer.TYPE_INT:
                for (int i = 0; i < roiDataArrayByte.length; i++) {
                    roiDataArray[z][i] = (int) roiDataArrayByte[i];
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                for (int i = 0; i < roiDataArrayByte.length; i++) {
                    roiDataArray[z][i] = (float) roiDataArrayByte[i];
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                for (int i = 0; i < roiDataArrayByte.length; i++) {
                    roiDataArray[z][i] = (double) roiDataArrayByte[i];
                }
                break;
            }
        }

        // X axis scaling without the scale operation
        long sxNum = 1, sxDenom = 1;

        long transXRationalDenom = 1;
        long transXRationalNum = 0;
        // Subtract the X translation factor sx -= transX
        sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
        sxDenom *= transXRationalDenom;

        // Add 0.5
        sxNum = 2 * sxNum + sxDenom;
        sxDenom *= 2;

        long invScaleXRationalNum = 1;
        long invScaleXRationalDenom = 4;
        // Multply by invScaleX
        sxNum *= invScaleXRationalNum;
        sxDenom *= invScaleXRationalDenom;

        int srcXInt = Rational.floor(sxNum, sxDenom);
        long srcXFrac = sxNum % sxDenom;
        if (srcXInt < 0) {
            srcXFrac = sxDenom + srcXFrac;
        }

        // Random X position
        double value=0;
        if(Math.random()>0.5){
            value = 0.6d;
        }else{
            value = 0.4d;
        }
        srcXInt += (int) ((value ) * (srcRect.width - 1)+ srcRect.x);

        // common denominator between the source x denominator and the scale fractional denominator
        long commonXDenom = sxDenom * invScaleXRationalDenom;

        // Calculate the position
        posx = (srcXInt - srcRect.x) * src[0].getPixelStride();

        fracvalues[0] = (int) (((float) srcXFrac / (float) commonXDenom) * one);
        fracvaluesFloat[0] = (float) srcXFrac / (float) commonXDenom;

        // Y axis scaling without the scale operation

        long syNum = 1, syDenom = 1;

        long transYRationalDenom = 1;
        long transYRationalNum = 0;
        // Subtract the y translation factor sy -= transy
        syNum = syNum * transYRationalDenom - transYRationalNum * sxDenom;
        syDenom *= transYRationalDenom;

        // Add 0.5
        syNum = 2 * syNum + syDenom;
        syDenom *= 2;

        long invScaleYRationalNum = 1;
        long invScaleYRationalDenom = 4;
        // Multply by invScaleY
        syNum *= invScaleYRationalNum;
        syDenom *= invScaleYRationalDenom;

        int srcYInt = Rational.floor(syNum, syDenom);
        long srcYFrac = syNum % syDenom;
        if (srcYInt < 0) {
            srcYFrac = syDenom + srcYFrac;
        }

        // Random Y position
        srcYInt += (int) ((value ) * (srcRect.height - 1) + srcRect.y);

        // common denominator between the source y denominator and the scale fractional denominator
        long commonYDenom = syDenom * invScaleYRationalDenom;

        // Calculate the position
        posy = (srcYInt - srcRect.y) * src[0].getScanlineStride() + src[0].getBandOffset(0);
        // Calculate the roi position
        posYroi = (srcYInt - srcRect.y) * roiAccessor.getScanlineStride();

        // Calculate the yfrac value
        fracvalues[1] = (int) (((float) srcYFrac / (float) commonYDenom) * one);
        fracvaluesFloat[1] = (float) srcYFrac / (float) commonYDenom;

        // Clamp posx and posy values if the kernel goes outside the tile bounds.
        if (posx < 1) {
            posx = 1;
        }
        if (posy < src[0].getScanlineStride() + src[0].getBandOffset(0)) {
            posy = src[0].getScanlineStride() + src[0].getBandOffset(0);
            posYroi = roiAccessor.getScanlineStride();
        }
        if (posx > src[0].getScanlineStride() - 3) {
            posx = 1;
        }
        if (posy > (src[0].getScanlineStride() * (src[0].getHeight() - 3))) {
            posy = src[0].getScanlineStride() + src[0].getBandOffset(0);
            posYroi = roiAccessor.getScanlineStride();
        }

    }

    private static RenderedImage getSyntheticNotUniformImage(int numBands, int dataType,
            double noData) {
        // parameter block initialization
        ParameterBlock pb = new ParameterBlock();
        pb.add(DEFAULT_WIDTH);
        pb.add(DEFAULT_HEIGHT);
        // Constant image value
        Number value = noData;
        // tiled image to write for creating a variable image
        TiledImage writeImage = null;
        // dataType interval for the random image creation
        double interval = 0;
        // cycle for 3-band images

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // value for filling the constant image
            byte valueByte = value.byteValue();
            Byte[] arraybyte = null;
            if (numBands == 3) {
                arraybyte = new Byte[] { valueByte, valueByte, valueByte };
            } else {
                arraybyte = new Byte[] { valueByte };
            }
            // addition to the parameterBlock
            pb.add(arraybyte);
            break;
        case DataBuffer.TYPE_USHORT:
            short valueUShort = value.shortValue();
            Short[] arrayUShort = null;
            if (numBands == 3) {
                arrayUShort = new Short[] { (short) (valueUShort & 0xffff),
                        (short) (valueUShort & 0xffff), (short) (valueUShort & 0xffff) };
            } else {
                arrayUShort = new Short[] { (short) (valueUShort & 0xffff) };
            }
            pb.add(arrayUShort);
            break;
        case DataBuffer.TYPE_SHORT:
            short valueShort = value.shortValue();
            Short[] arrayShort = null;
            if (numBands == 3) {
                arrayShort = new Short[] { valueShort, valueShort, valueShort };
            } else {
                arrayShort = new Short[] { valueShort };
            }
            pb.add(arrayShort);
            break;
        case DataBuffer.TYPE_INT:
            int valueInt = value.intValue();
            Integer[] arrayInteger = null;
            if (numBands == 3) {
                arrayInteger = new Integer[] { valueInt, valueInt, valueInt };
            } else {
                arrayInteger = new Integer[] { valueInt };
            }
            pb.add(arrayInteger);
            break;
        case DataBuffer.TYPE_FLOAT:
            float valueFloat = value.floatValue();
            Float[] arrayFloat = null;
            if (numBands == 3) {
                arrayFloat = new Float[] { valueFloat, valueFloat, valueFloat };
            } else {
                arrayFloat = new Float[] { valueFloat };
            }
            pb.add(arrayFloat);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double valueDouble = value.doubleValue();
            Double[] arrayDouble = null;
            if (numBands == 3) {
                arrayDouble = new Double[] { valueDouble, valueDouble, valueDouble };
            } else {
                arrayDouble = new Double[] { valueDouble };
            }
            pb.add(arrayDouble);
            break;
        }

        // Create the constant operation.
        RenderedImage constant = JAI.create("constant", pb);
        // From the old constant image another tiled image is created for writing on its pixels
        writeImage = new TiledImage(constant, (int) (DEFAULT_WIDTH / 16),
                (int) (DEFAULT_HEIGHT / 16));
        // index for iterating on the image pixel.
        // int minX = writeImage.getMinX();
        // int minY = writeImage.getMinY();
        Raster firstRaster = writeImage.getTile(writeImage.getMinTileX(), writeImage.getMinTileY());

        // int minX = writeImage.getMinX();
        // int minY = writeImage.getMinY();

        int minXpix = firstRaster.getMinX();
        int minYpix = firstRaster.getMinY();
        // cycle through the bands
        for (int k = 0; k < numBands; k++) {
            // X axis
            for (int i = minXpix; i < firstRaster.getWidth() + minXpix; i = i + 2) {
                // Y axis
                for (int j = minYpix; j < firstRaster.getHeight() + minYpix; j++) {
                    // initialization of the sample value
                    int sI = 0;

                    switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_USHORT:
                        // random value creation
                        sI = (int) (Math.random() * interval);
                        // pixel writing
                        writeImage.setSample(i, j, 0, sI);
                        break;
                    case DataBuffer.TYPE_SHORT:
                    case DataBuffer.TYPE_INT:
                        sI = (int) ((Math.random() - 0.5) * interval);
                        writeImage.setSample(i, j, 0, sI);
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        float sF = (float) ((Math.random() - 0.5) * interval);
                        writeImage.setSample(i, j, 0, sF);
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        double s = (Math.random() - 0.5) * interval;
                        writeImage.setSample(i, j, 0, s);
                        break;
                    }

                }
            }
        }
        return writeImage;
    }

    /* Private method for calculate bilinear interpolation for byte, short/ushort, integer dataType */
    private Number computeValue(int dataType, int s00, int s01, int s10, int s11, int w00, int w01,
            int w10, int w11, int xfrac, int yfrac) {

        int s0 = 0;
        int s1 = 0;
        int s = 0;

        long s0L = 0;
        long s1L = 0;

      //Complementary values of the fractional part
        int xfracCompl= (int) Math.pow(2, subsampleBits) - xfrac;
        int yfracCompl= (int) Math.pow(2, subsampleBits) - yfrac;
        
        int shift = 29 - subsampleBits;
        // For Integer value is possible that a bitshift of "subsampleBits" could shift over the integer bit number
        // so the samples, in this case, are expanded to Long.
        boolean s0Long = ((s00 | s10) >>> shift == 0);
        boolean s1Long = ((s01 | s11) >>> shift == 0);
        // If all the weight are 0 the destination NO DATA is returned
        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
            return destinationNoData;
        }
        // Otherwise all the possible weight combination are checked
        if (w00 == 0 || w01 == 0 || w10 == 0 || w11 == 0) {
            // For integers is even considered the case when the integers are expanded to longs
            if (dataType == DataBuffer.TYPE_INT) {

                if (w00 == 0 && w01 == 0) {

                    s0L = 0;
                } else if (w00 == 0) { // w01 = 1
                    if (s1Long) {
                        s0L = -s01*xfracCompl + (s01 << subsampleBits);
                    } else {
                        s0L = -s01*xfracCompl + ((long) s01 << subsampleBits);
                    }
                } else if (w01 == 0) {// w00 = 1
                    if (s0Long) {
                        s0L = -s00*xfrac + (s00 << subsampleBits);
                    } else {
                        s0L = -s00*xfrac + ((long) s00 << subsampleBits);
                    }
                } else {// w00 = 1 & W01 = 1
                    if (s0Long) {
                        if (s1Long) {
                            s0L = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        } else {
                            s0L = ((long) s01 - s00) * xfrac + (s00 << subsampleBits);
                        }
                    } else {
                        s0L = ((long) s01 - s00) * xfrac + ((long) s00 << subsampleBits);
                    }
                }

                // lower value

                if (w10 == 0 && w11 == 0) {
                    s1L = 0;
                } else if (w10 == 0) { // w11 = 1
                    if (s1Long) {
                        s1L = -s11*xfracCompl + (s11 << subsampleBits);
                    } else {
                        s1L = -s11*xfracCompl + ((long) s11 << subsampleBits);
                    }
                } else if (w11 == 0) { // w10 = 1
                    if (s0Long) {// - (s10 * xfrac); //s10;
                        s1L = -s10*xfrac + (s10 << subsampleBits);
                    } else {
                        s1L = -s10*xfrac + ((long) s10 << subsampleBits);
                    }
                } else {
                    if (s0Long) {
                        if (s1Long) {
                            s1L = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        } else {
                            s1L = ((long) s11 - s10) * xfrac + (s10 << subsampleBits);
                        }
                    } else {
                        s1L = ((long) s11 - s10) * xfrac + ((long) s10 << subsampleBits);
                    }
                }
                if (w00 == 0 && w01 == 0) {
                    s = (int) ((-s1L*yfracCompl + (s1L << subsampleBits) + round2) >> shift2);
                } else {
                    if (w10 == 0 && w11 == 0) {
                        s = (int) ((-s0L*yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    } else {
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                }

            } else {
                // Interpolation for type byte, ushort, short
                if (w00 == 0 && w01 == 0) {
                    s0 = 0;
                } else if (w00 == 0) { // w01 = 1
                    s0 = -s01*xfracCompl + (s01 << subsampleBits);
                } else if (w01 == 0) {// w00 = 1
                    s0 = -s00*xfrac + (s00 << subsampleBits);// s00;
                } else {// w00 = 1 & W01 = 1
                    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                }

                // lower value

                if (w10 == 0 && w11 == 0) {
                    s1 = 0;
                } else if (w10 == 0) { // w11 = 1
                    s1 = -s11*xfracCompl + (s11 << subsampleBits);
                } else if (w11 == 0) { // w10 = 1
                    s1 = -s10*xfrac + (s10 << subsampleBits);// - (s10 * xfrac); //s10;
                } else {
                    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                }

                if (w00 == 0 && w01 == 0) {
                    s = (-s1*yfracCompl + (s1 << subsampleBits) + round2) >> shift2;
                } else {
                    if (w10 == 0 && w11 == 0) {
                        s = (-s0*yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    } else {
                        s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    }
                }

            }
        } else {
            // Perform the bilinear interpolation
            if (dataType == DataBuffer.TYPE_INT) {
                if (s0Long) {
                    if (s1Long) {
                        s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    } else {
                        s0L = ((long) s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1L = ((long) s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                } else {
                    s0L = ((long) s01 - s00) * xfrac + ((long) s00 << subsampleBits);
                    s1L = ((long) s11 - s10) * xfrac + ((long) s10 << subsampleBits);
                    s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                }
            } else {
                s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
            }
        }

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            s = (byte) s & 0xff;
            break;
        case DataBuffer.TYPE_USHORT:
            s = (short) s & 0xffff;
            break;
        case DataBuffer.TYPE_SHORT:
            s = (short) s;
            break;
        default:
            break;
        }
        return s;
    }

    private Number computeValueDouble(int dataType, double s00, double s01, double s10, double s11,
            double w00, double w01, double w10, double w11, double xfrac, double yfrac) {

        double s0 = 0;
        double s1 = 0;
        double s = 0;

        //Complementary values of the fractional part
        double xfracCompl= 1 - xfrac;
        double yfracCompl= 1 - yfrac;
        
        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
            return destinationNoData;
        }

        if (w00 == 0 || w01 == 0 || w10 == 0 || w11 == 0) {

            if (w00 == 0 && w01 == 0) {
                s0 = 0;
            } else if (w00 == 0) { // w01 = 1
                s0 = s01*xfrac;
            } else if (w01 == 0) {// w00 = 1
                s0 = s00*xfracCompl;// s00;
            } else {// w00 = 1 & W01 = 1
                s0 = (s01 - s00) * xfrac + s00;
            }

            // lower value

            if (w10 == 0 && w11 == 0) {
                s1 = 0;
            } else if (w10 == 0) { // w11 = 1
                s1 = s11*xfrac;
            } else if (w11 == 0) { // w10 = 1
                s1 = s10*xfracCompl;// - (s10 * xfrac); //s10;
            } else {
                s1 = (s11 - s10) * xfrac + s10;
            }

            if (w00 == 0 && w01 == 0) {
                s = s1*yfrac;
            } else {
                if (w10 == 0 && w11 == 0) {
                    s = s0*yfracCompl;
                } else {
                    s = (s1 - s0) * yfrac + s0;
                }
            }
        } else {

            // Perform the bilinear interpolation because all the weight are not 0.
            s0 = (s01 - s00) * xfrac + s00;
            s1 = (s11 - s10) * xfrac + s10;
            s = (s1 - s0) * yfrac + s0;
        }

        // Simple conversion for float dataType.
        if (dataType == DataBuffer.TYPE_FLOAT) {
            return (float) s;
        } else {
            return s;
        }

    }

}
