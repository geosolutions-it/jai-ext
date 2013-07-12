package it.geosolutions.jaiext.roiaware.interpolators;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationTable;
import javax.media.jai.RasterAccessor;
import javax.media.jai.iterator.RandomIter;

import org.jaitools.numeric.Range;

public class InterpolationBicubicNew extends InterpolationTable {

    /** serialVersionUID */
    private static final long serialVersionUID = -3699824574811467489L;

    /**
     * Default value for precision bits
     * */
    public static final int PRECISION_BITS = 8;

    /** The scaled (by 2<sup>precisionBits</sup>) value of 0.5 for rounding */
    private int round;

    /** The number of horizontal subpixel positions within a pixel. */
    private int numSubsamplesH;

    /** The number of vertical subpixel positions within a pixel. */
    private int numSubsamplesV;

    /**
     * InterpolationCubic instance used only with the ScaleOpImage constructor for setting the interpolation type as BiCubic
     * */
    private InterpolationBicubic interpBiCubic;

    /** Range of NO DATA values to be checked */
    private Range noDataRange;

    /** ROI bounds used for checking the position of the pixel */
    private Rectangle roiBounds;

    /** Random interator used for navigate inside the ROI and searching the pixel */
    private RandomIter roiIter;

    /** Boolean for checking if the ROI Accessor must be used by the interpolator */
    private boolean useROIAccessor;

    /**
     * Destination NO DATA value used when the image pixel is outside of the ROI or is contained in the NO DATA range
     * */
    private double destinationNoData;

    /** This value is the destination NO DATA values for binary images */
    private int black;

    /** Image data Type */
    private int dataType;

    /**
     * Simple interpolator object used for Bicubic/Bicubic2 interpolation. On construction it is possible to set a range for no data values that will
     * be considered in the interpolation method.
     */
    public InterpolationBicubicNew(int subsampleBits, Range noDataRange, 
            boolean useROIAccessor, double destinationNoData, int dataType, boolean bicubic2Disabled, int precisionBits) {

        super(1, 1, 4, 4, subsampleBits, subsampleBits, precisionBits, dataHelper(subsampleBits,
                bicubic2Disabled), null);

        if (noDataRange != null) {
            this.noDataRange = noDataRange;
        }
        this.useROIAccessor = useROIAccessor;
        this.destinationNoData = destinationNoData;
        black = ((int) destinationNoData) & 1;
        this.dataType = dataType;
        this.numSubsamplesH = (1 << subsampleBits);
        this.numSubsamplesV = numSubsamplesH;

        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        }
    }

    
    public void setROIdata(Rectangle roiBounds, RandomIter roiIter){
        if (roiBounds != null && roiIter != null) {
            this.roiBounds = roiBounds;
            this.roiIter = roiIter;

        } else if ((roiBounds == null && roiIter != null) || (roiBounds != null && roiIter == null)) {
            throw new IllegalArgumentException(
                    "If roiBounds or roiIter are not null, so even the other must be not null");
        }
    }
    
    public Range getNoDataRange() {
        return noDataRange;
    }

    public void setNoDataRange(Range noDataRange) {
        this.noDataRange = noDataRange;
    }
    
    public int getDataType() {
        return dataType;
    }
    
    /* Interpolator nearest getter, must be used on creation only for ScaleOpImage Constructor */
    public synchronized InterpolationBicubic getInterpBiCubic() {
        if (interpBiCubic == null) {
            interpBiCubic = new InterpolationBicubic(subsampleBitsH);
        }
        return interpBiCubic;
    }

    public static float[] dataHelper(int subsampleBits, boolean bicubic2Disabled) {

        int one = 1 << subsampleBits;
        int arrayLength = one * 4;
        float tableValues[] = new float[arrayLength];
        float f;

        float onef = (float) one;
        // float t;
        int count = 0;
        for (int i = 0; i < one; i++) {
            // t = (float) i;
            f = (i / onef);

            tableValues[count++] = bicubic(f + 1.0F, bicubic2Disabled);
            tableValues[count++] = bicubic(f, bicubic2Disabled);
            tableValues[count++] = bicubic(f - 1.0F, bicubic2Disabled);
            tableValues[count++] = bicubic(f - 2.0F, bicubic2Disabled);
        }
        return tableValues;
    }

    /** Returns the bicubic polynomial value at a certain value of x. */
    public static float bicubic(float x, boolean bicubic2Disabled) {
        if (x < 0) {
            x = -x;
        }
        float A = 0;
        // If bicubic2Disabled is true, the parameters used are those of the bicubic interpolation.
        // If bicubic2Disabled is false, the parameters used are those of the bicubic2 interpolation.
        if (bicubic2Disabled) {
            // The parameter "a" for the bicubic polynomial
            A = -0.5F;
        } else {
            // The parameter "a" for the bicubic2 polynomial
            A = -1.0F;
        }
        // Define all of the polynomial coefficients in terms of "a"
        float A3 = A + 2.0F;
        float A2 = -(A + 3.0F);
        float A0 = 1.0F;

        float B3 = A;
        float B2 = -(5.0F * A);
        float B1 = 8.0F * A;
        float B0 = -(4.0F * A);

        // Evaluate with Horner's rule
        if (x >= 1) {
            return (((B3 * x) + B2) * x + B1) * x + B0;
        } else {
            return ((A3 * x) + A2) * x * x + A0;
        }
    }

    public Number interpolate(RasterAccessor src, int bandIndex, int dnumbands, int posx, int posy,
            Number[] fracValues, Integer yValueROI, RasterAccessor roi, boolean setNoData) {
        // If this pixel doesn't need any computation, destination NO DATA is returned.
        if (setNoData) {
            return destinationNoData;
        }

        // ------------------------------DATA-INITIALIZATION------------------------------

        // Value used for setting the position of the pixel inside the interpolation kernel
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Position of the interpolation pixel kernel
        // x axis
        int posxlow = posx - srcPixelStride;
        int posxhigh = posx + srcPixelStride;
        int posxhigh2 = posxhigh + srcPixelStride;
        // y axis
        int posylow = posy - srcScanlineStride;
        int posyhigh = posy + srcScanlineStride;
        int posyhigh2 = posyhigh + srcScanlineStride;
        // Pixel initialization. For float and double dataType, different pixel are used
        int s__ = 0, s_0 = 0, s_1 = 0, s_2 = 0;
        int s0_ = 0, s00 = 0, s01 = 0, s02 = 0;
        int s1_ = 0, s10 = 0, s11 = 0, s12 = 0;
        int s2_ = 0, s20 = 0, s21 = 0, s22 = 0;

        float s__f = 0, s_0f = 0, s_1f = 0, s_2f = 0;
        float s0_f = 0, s00f = 0, s01f = 0, s02f = 0;
        float s1_f = 0, s10f = 0, s11f = 0, s12f = 0;
        float s2_f = 0, s20f = 0, s21f = 0, s22f = 0;

        double s__d = 0, s_0d = 0, s_1d = 0, s_2d = 0;
        double s0_d = 0, s00d = 0, s01d = 0, s02d = 0;
        double s1_d = 0, s10d = 0, s11d = 0, s12d = 0;
        double s2_d = 0, s20d = 0, s21d = 0, s22d = 0;

        // src data array initialization

        byte[] srcDataByte;
        short[] srcDataShort;
        int[] srcDataInt;
        float[] srcDataFloat;
        double[] srcDataDouble;

        // Fractional Value
        int xfrac = 0;
        int yfrac = 0;

        // Offset initialization for interpolation on X axis.
        int offsetX = 0;
        // Offset initialization for interpolation on Y axis.
        int offsetY = 0;

        // All the data are inserted into an array for simplify the code.
        int[][] kernelArray = new int[4][4];
        float[][] kernelArrayF = new float[4][4];
        double[][] kernelArrayD = new double[4][4];

        // Get the sixteen surrounding pixel values (same code, only the dataType is changed)
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // Src data
            srcDataByte = src.getByteDataArray(bandIndex);
            // Selection of the 16 values from the src data
            kernelArray[0][0] = s__ = srcDataByte[posxlow + posylow] & 0xff;
            kernelArray[0][1] = s_0 = srcDataByte[posx + posylow] & 0xff;
            kernelArray[0][2] = s_1 = srcDataByte[posxhigh + posylow] & 0xff;
            kernelArray[0][3] = s_2 = srcDataByte[posxhigh2 + posylow] & 0xff;

            kernelArray[1][0] = s0_ = srcDataByte[posxlow + posy] & 0xff;
            kernelArray[1][1] = s00 = srcDataByte[posx + posy] & 0xff;
            kernelArray[1][2] = s01 = srcDataByte[posxhigh + posy] & 0xff;
            kernelArray[1][3] = s02 = srcDataByte[posxhigh2 + posy] & 0xff;

            kernelArray[2][0] = s1_ = srcDataByte[posxlow + posyhigh] & 0xff;
            kernelArray[2][1] = s10 = srcDataByte[posx + posyhigh] & 0xff;
            kernelArray[2][2] = s11 = srcDataByte[posxhigh + posyhigh] & 0xff;
            kernelArray[2][3] = s12 = srcDataByte[posxhigh2 + posyhigh] & 0xff;

            kernelArray[3][0] = s2_ = srcDataByte[posxlow + posyhigh2] & 0xff;
            kernelArray[3][1] = s20 = srcDataByte[posx + posyhigh2] & 0xff;
            kernelArray[3][2] = s21 = srcDataByte[posxhigh + posyhigh2] & 0xff;
            kernelArray[3][3] = s22 = srcDataByte[posxhigh2 + posyhigh2] & 0xff;

            // fractional x and y value
            xfrac = fracValues[0].intValue();
            yfrac = fracValues[1].intValue();
            // offset calculated from the fractional values
            offsetX = 4 * xfrac;
            offsetY = 4 * yfrac;
            break;
        case DataBuffer.TYPE_USHORT:
            srcDataShort = src.getShortDataArray(bandIndex);

            kernelArray[0][0] = s__ = srcDataShort[posxlow + posylow] & 0xffff;
            kernelArray[0][1] = s_0 = srcDataShort[posx + posylow] & 0xffff;
            kernelArray[0][2] = s_1 = srcDataShort[posxhigh + posylow] & 0xffff;
            kernelArray[0][3] = s_2 = srcDataShort[posxhigh2 + posylow] & 0xffff;

            kernelArray[1][0] = s0_ = srcDataShort[posxlow + posy] & 0xffff;
            kernelArray[1][1] = s00 = srcDataShort[posx + posy] & 0xffff;
            kernelArray[1][2] = s01 = srcDataShort[posxhigh + posy] & 0xffff;
            kernelArray[1][3] = s02 = srcDataShort[posxhigh2 + posy] & 0xffff;

            kernelArray[2][0] = s1_ = srcDataShort[posxlow + posyhigh] & 0xffff;
            kernelArray[2][1] = s10 = srcDataShort[posx + posyhigh] & 0xffff;
            kernelArray[2][2] = s11 = srcDataShort[posxhigh + posyhigh] & 0xffff;
            kernelArray[2][3] = s12 = srcDataShort[posxhigh2 + posyhigh] & 0xffff;

            kernelArray[3][0] = s2_ = srcDataShort[posxlow + posyhigh2] & 0xffff;
            kernelArray[3][1] = s20 = srcDataShort[posx + posyhigh2] & 0xffff;
            kernelArray[3][2] = s21 = srcDataShort[posxhigh + posyhigh2] & 0xffff;
            kernelArray[3][3] = s22 = srcDataShort[posxhigh2 + posyhigh2] & 0xffff;

            xfrac = fracValues[0].intValue();
            yfrac = fracValues[1].intValue();

            offsetX = 4 * xfrac;
            offsetY = 4 * yfrac;
            break;
        case DataBuffer.TYPE_SHORT:
            srcDataShort = src.getShortDataArray(bandIndex);

            kernelArray[0][0] = s__ = srcDataShort[posxlow + posylow];
            kernelArray[0][1] = s_0 = srcDataShort[posx + posylow];
            kernelArray[0][2] = s_1 = srcDataShort[posxhigh + posylow];
            kernelArray[0][3] = s_2 = srcDataShort[posxhigh2 + posylow];

            kernelArray[1][0] = s0_ = srcDataShort[posxlow + posy];
            kernelArray[1][1] = s00 = srcDataShort[posx + posy];
            kernelArray[1][2] = s01 = srcDataShort[posxhigh + posy];
            kernelArray[1][3] = s02 = srcDataShort[posxhigh2 + posy];

            kernelArray[2][0] = s1_ = srcDataShort[posxlow + posyhigh];
            kernelArray[2][1] = s10 = srcDataShort[posx + posyhigh];
            kernelArray[2][2] = s11 = srcDataShort[posxhigh + posyhigh];
            kernelArray[2][3] = s12 = srcDataShort[posxhigh2 + posyhigh];

            kernelArray[3][0] = s2_ = srcDataShort[posxlow + posyhigh2];
            kernelArray[3][1] = s20 = srcDataShort[posx + posyhigh2];
            kernelArray[3][2] = s21 = srcDataShort[posxhigh + posyhigh2];
            kernelArray[3][3] = s22 = srcDataShort[posxhigh2 + posyhigh2];

            xfrac = fracValues[0].intValue();
            yfrac = fracValues[1].intValue();

            offsetX = 4 * xfrac;
            offsetY = 4 * yfrac;
            break;
        case DataBuffer.TYPE_INT:
            srcDataInt = src.getIntDataArray(bandIndex);

            kernelArray[0][0] = s__ = srcDataInt[posxlow + posylow];
            kernelArray[0][1] = s_0 = srcDataInt[posx + posylow];
            kernelArray[0][2] = s_1 = srcDataInt[posxhigh + posylow];
            kernelArray[0][3] = s_2 = srcDataInt[posxhigh2 + posylow];

            kernelArray[1][0] = s0_ = srcDataInt[posxlow + posy];
            kernelArray[1][1] = s00 = srcDataInt[posx + posy];
            kernelArray[1][2] = s01 = srcDataInt[posxhigh + posy];
            kernelArray[1][3] = s02 = srcDataInt[posxhigh2 + posy];

            kernelArray[2][0] = s1_ = srcDataInt[posxlow + posyhigh];
            kernelArray[2][1] = s10 = srcDataInt[posx + posyhigh];
            kernelArray[2][2] = s11 = srcDataInt[posxhigh + posyhigh];
            kernelArray[2][3] = s12 = srcDataInt[posxhigh2 + posyhigh];

            kernelArray[3][0] = s2_ = srcDataInt[posxlow + posyhigh2];
            kernelArray[3][1] = s20 = srcDataInt[posx + posyhigh2];
            kernelArray[3][2] = s21 = srcDataInt[posxhigh + posyhigh2];
            kernelArray[3][3] = s22 = srcDataInt[posxhigh2 + posyhigh2];

            long xfracL = fracValues[0].longValue();
            long yfracL = fracValues[1].longValue();

            offsetX = (int) (4 * xfracL);
            offsetY = (int) (4 * yfracL);
            break;
        case DataBuffer.TYPE_FLOAT:
            srcDataFloat = src.getFloatDataArray(bandIndex);

            kernelArrayF[0][0] = s__f = srcDataFloat[posxlow + posylow];
            kernelArrayF[0][1] = s_0f = srcDataFloat[posx + posylow];
            kernelArrayF[0][2] = s_1f = srcDataFloat[posxhigh + posylow];
            kernelArrayF[0][3] = s_2f = srcDataFloat[posxhigh2 + posylow];

            kernelArrayF[1][0] = s0_f = srcDataFloat[posxlow + posy];
            kernelArrayF[1][1] = s00f = srcDataFloat[posx + posy];
            kernelArrayF[1][2] = s01f = srcDataFloat[posxhigh + posy];
            kernelArrayF[1][3] = s02f = srcDataFloat[posxhigh2 + posy];

            kernelArrayF[2][0] = s1_f = srcDataFloat[posxlow + posyhigh];
            kernelArrayF[2][1] = s10f = srcDataFloat[posx + posyhigh];
            kernelArrayF[2][2] = s11f = srcDataFloat[posxhigh + posyhigh];
            kernelArrayF[2][3] = s12f = srcDataFloat[posxhigh2 + posyhigh];

            kernelArrayF[3][0] = s2_f = srcDataFloat[posxlow + posyhigh2];
            kernelArrayF[3][1] = s20f = srcDataFloat[posx + posyhigh2];
            kernelArrayF[3][2] = s21f = srcDataFloat[posxhigh + posyhigh2];
            kernelArrayF[3][3] = s22f = srcDataFloat[posxhigh2 + posyhigh2];

            xfrac = fracValues[0].intValue();
            yfrac = fracValues[1].intValue();

            offsetX = 4 * xfrac;
            offsetY = 4 * yfrac;
            break;
        case DataBuffer.TYPE_DOUBLE:
            srcDataDouble = src.getDoubleDataArray(bandIndex);

            kernelArrayD[0][0] = s__d = srcDataDouble[posxlow + posylow];
            kernelArrayD[0][1] = s_0d = srcDataDouble[posx + posylow];
            kernelArrayD[0][2] = s_1d = srcDataDouble[posxhigh + posylow];
            kernelArrayD[0][3] = s_2d = srcDataDouble[posxhigh2 + posylow];

            kernelArrayD[1][0] = s0_d = srcDataDouble[posxlow + posy];
            kernelArrayD[1][1] = s00d = srcDataDouble[posx + posy];
            kernelArrayD[1][2] = s01d = srcDataDouble[posxhigh + posy];
            kernelArrayD[1][3] = s02d = srcDataDouble[posxhigh2 + posy];

            kernelArrayD[2][0] = s1_d = srcDataDouble[posxlow + posyhigh];
            kernelArrayD[2][1] = s10d = srcDataDouble[posx + posyhigh];
            kernelArrayD[2][2] = s11d = srcDataDouble[posxhigh + posyhigh];
            kernelArrayD[2][3] = s12d = srcDataDouble[posxhigh2 + posyhigh];

            kernelArrayD[3][0] = s2_d = srcDataDouble[posxlow + posyhigh2];
            kernelArrayD[3][1] = s20d = srcDataDouble[posx + posyhigh2];
            kernelArrayD[3][2] = s21d = srcDataDouble[posxhigh + posyhigh2];
            kernelArrayD[3][3] = s22d = srcDataDouble[posxhigh2 + posyhigh2];

            xfrac = fracValues[0].intValue();
            yfrac = fracValues[1].intValue();

            offsetX = 4 * xfrac;
            offsetY = 4 * yfrac;
            break;
        default:
            break;
        }

        // Weight initialization

        double[][] weightArray = new double[4][4];
        int weightArrayLength = weightArray.length;

        // -------------------------NO-DATA-CONTROL--------------------------------
        if (noDataRange != null) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                // Range cast to the selcted data Type
                Range<Byte> rangeB = (Range<Byte>) noDataRange;
                // Every pixel is tested if it is a NO DATA.
                // If so, the associated weight is set to 0, else to 1.
                for (int i = 0; i < weightArrayLength; i++) {
                    for (int j = 0; j < weightArrayLength; j++) {
                        if (rangeB.contains((byte) kernelArray[i][j])) {
                            weightArray[i][j] = 0;
                        } else {
                            weightArray[i][j] = 1;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                Range<Short> rangeS = (Range<Short>) noDataRange;
                for (int i = 0; i < weightArrayLength; i++) {
                    for (int j = 0; j < weightArrayLength; j++) {
                        if (rangeS.contains((short) kernelArray[i][j])) {
                            weightArray[i][j] = 0;
                        } else {
                            weightArray[i][j] = 1;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_INT:
                Range<Integer> rangeI = (Range<Integer>) noDataRange;
                for (int i = 0; i < weightArrayLength; i++) {
                    for (int j = 0; j < weightArrayLength; j++) {
                        if (rangeI.contains((int) kernelArray[i][j])) {
                            weightArray[i][j] = 0;
                        } else {
                            weightArray[i][j] = 1;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                Range<Float> rangeF = (Range<Float>) noDataRange;
                for (int i = 0; i < weightArrayLength; i++) {
                    for (int j = 0; j < weightArrayLength; j++) {
                        if (rangeF.contains(kernelArrayF[i][j])) {
                            weightArray[i][j] = 0;
                        } else {
                            weightArray[i][j] = 1;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                Range<Double> rangeD = (Range<Double>) noDataRange;
                for (int i = 0; i < weightArrayLength; i++) {
                    for (int j = 0; j < weightArrayLength; j++) {
                        if (rangeD.contains(kernelArrayD[i][j])) {
                            weightArray[i][j] = 0;
                        } else {
                            weightArray[i][j] = 1;
                        }
                    }
                }
                break;
            default:
                break;
            }
        } else {
            // If NO DATA Range is not defined, all the weight are set to 1
            for (int i = 0; i < weightArrayLength; i++) {
                for (int j = 0; j < weightArrayLength; j++) {
                    weightArray[i][j] = 1;

                }
            }
        }

        // If all the pixel values are NO DATA, the destination NO DATA value is returned.
        if (sumZero(weightArray)) {
            return destinationNoData;
        }

        // ------------------------------ROI-CONTROL---------------------------------------------------

        // Temporary ROI values initialization
        byte tempValueB = 0;
        short tempValueS = 0;
//        int tempValueI = 0;
//        float tempValueF = 0;
        double tempValueD = 0;

        if (useROIAccessor) {

            if (yValueROI == null || roi == null) {
                throw new IllegalArgumentException(
                        "If rasterAccessor is set, ROI value must be provided");
            }
            // ROI scan line stride used for selecting the 4 surrounding pixels
            int roiScanLineStride = roi.getScanlineStride();

            int[][] weightArrayIndex = new int[4][4];

            int baseIndex = (posx / dnumbands) + (yValueROI);
            // cycle for filling all the ROI index by shifting of 1 on the x axis
            // and by roiscanlinestride on the y axis.
            for (int i = 0; i < weightArrayIndex.length; i++) {
                for (int j = 0; j < weightArrayIndex.length; j++) {
                    weightArrayIndex[i][j] = baseIndex - 1 + j + (i - 1) * (roiScanLineStride);
                }
            }

            // Array length initialization
            int roiDataLength = 0;
            // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
            // Otherwise it takes the related value.
            
            byte[] roiDataArrayByte = roi.getByteDataArray(0);
            roiDataLength = roiDataArrayByte.length;
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:


                for (int i = 0; i < weightArrayIndex.length; i++) {
                    for (int j = 0; j < weightArrayIndex.length; j++) {
                        if (weightArrayIndex[i][j] < roiDataLength) {
                            // Save the roi value in a temporary image.
                            tempValueB = (byte) (roiDataArrayByte[weightArrayIndex[i][j]] & 0xff);
                            // Multiply the pixel weight to the new ROI weight.
                            weightArray[i][j] *= (tempValueB != 0 ? 1 : 0);
                        } else {
                            weightArray[i][j] = 0;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
//                short[] roiDataArrayUShort = roi.getShortDataArray(bandIndex);
//                roiDataLength = roiDataArrayUShort.length;

                for (int i = 0; i < weightArrayIndex.length; i++) {
                    for (int j = 0; j < weightArrayIndex.length; j++) {
                        if (weightArrayIndex[i][j] < roiDataLength) {
                            tempValueS = (short) (roiDataArrayByte[weightArrayIndex[i][j]] & 0xffff);
                            weightArray[i][j] *= (tempValueS != 0 ? 1 : 0);
                        } else {
                            weightArray[i][j] = 0;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:

                for (int i = 0; i < weightArrayIndex.length; i++) {
                    for (int j = 0; j < weightArrayIndex.length; j++) {
                        if (weightArrayIndex[i][j] < roiDataLength) {
                            tempValueD = roiDataArrayByte[weightArrayIndex[i][j]];
                            weightArray[i][j] *= (tempValueD != 0 ? 1 : 0);
                        } else {
                            weightArray[i][j] = 0;
                        }
                    }
                }

                break;
            default:
                break;
            }

            // If all the pixel values are outside the ROI, the destination NO DATA value is returned.
            if (sumZero(weightArray)) {
                return destinationNoData;
            }
        } else if (roiBounds != null) {
            int srcBandOffset = src.getBandOffset(bandIndex);

            // Central pixel positions
            int x0 = src.getX() + posx / srcPixelStride;
            int y0 = src.getY() + (posy - srcBandOffset) / srcScanlineStride;
            // ROI control
            if (roiBounds.contains(x0, y0)) {
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    for (int i = 0; i < weightArray.length; i++) {
                        for (int j = 0; j < weightArray.length; j++) {
                            tempValueB = (byte) (roiIter.getSample(x0 + j - 1, y0 + i - 1, 0) & 0xFF);
                            weightArray[i][j] *= (tempValueB != 0 ? 1 : 0);
                        }
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    for (int i = 0; i < weightArray.length; i++) {
                        for (int j = 0; j < weightArray.length; j++) {
                            tempValueS = (short) (roiIter.getSample(x0 + j - 1, y0 + i - 1, 0) & 0xFFFF);
                            weightArray[i][j] *= (tempValueS != 0 ? 1 : 0);
                        }
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_INT:
                case DataBuffer.TYPE_FLOAT:
                case DataBuffer.TYPE_DOUBLE:
                    for (int i = 0; i < weightArray.length; i++) {
                        for (int j = 0; j < weightArray.length; j++) {
                            tempValueD = roiIter.getSample(x0 + j - 1, y0 + i - 1, 0);
                            weightArray[i][j] *= (tempValueD != 0 ? 1 : 0);
                        }
                    }
                    break;
                default:
                    break;
                }
                // If all the pixel values are outside the ROI, the destination NO DATA value is returned.
                if (sumZero(weightArray)) {
                    return destinationNoData;
                }
            } else {
                return destinationNoData;
            }

        }

        // -----------------BICUBIC-INTERPOLATION---------------------------------------

        // Initial sum value.
        long sum = 0;
        double sumd = 0;

        // Interpolate in X
        int offsetX1 = offsetX + 1;
        int offsetX2 = offsetX + 2;
        int offsetX3 = offsetX + 3;

        // Interpolate in y
        int offsetY1 = offsetY + 1;
        int offsetY2 = offsetY + 2;
        int offsetY3 = offsetY + 3;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:

            // Interpolation on the X axis
            long sum_ = (long) dataHi[offsetX] * s__ * ((long) weightArray[0][0]);
            sum_ += (long) dataHi[offsetX1] * s_0 * ((long) weightArray[0][1]);
            sum_ += (long) dataHi[offsetX2] * s_1 * ((long) weightArray[0][2]);
            sum_ += (long) dataHi[offsetX3] * s_2 * ((long) weightArray[0][3]);

            long sum0 = (long) dataHi[offsetX] * s0_ * ((long) weightArray[1][0]);
            sum0 += (long) dataHi[offsetX1] * s00 * ((long) weightArray[1][1]);
            sum0 += (long) dataHi[offsetX2] * s01 * ((long) weightArray[1][2]);
            sum0 += (long) dataHi[offsetX3] * s02 * ((long) weightArray[1][3]);

            long sum1 = (long) dataHi[offsetX] * s1_ * ((long) weightArray[2][0]);
            sum1 += (long) dataHi[offsetX1] * s10 * ((long) weightArray[2][1]);
            sum1 += (long) dataHi[offsetX2] * s11 * ((long) weightArray[2][2]);
            sum1 += (long) dataHi[offsetX3] * s12 * ((long) weightArray[2][3]);

            long sum2 = (long) dataHi[offsetX] * s2_ * ((long) weightArray[3][0]);
            sum2 += (long) dataHi[offsetX1] * s20 * ((long) weightArray[3][1]);
            sum2 += (long) dataHi[offsetX2] * s21 * ((long) weightArray[3][2]);
            sum2 += (long) dataHi[offsetX3] * s22 * ((long) weightArray[3][3]);

            // Intermediate rounding
            sum_ = (sum_ + round) >> precisionBits;
            sum0 = (sum0 + round) >> precisionBits;
            sum1 = (sum1 + round) >> precisionBits;
            sum2 = (sum2 + round) >> precisionBits;

            // Interpolation on the Y axis
            sum = (long) dataVi[offsetY] * sum_;
            sum += (long) dataVi[offsetY1] * sum0;
            sum += (long) dataVi[offsetY2] * sum1;
            sum += (long) dataVi[offsetY3] * sum2;
            break;

        case DataBuffer.TYPE_FLOAT:

            // Interpolation on the X axis
            double sum_f = dataHf[offsetX] * s__f * (weightArray[0][0]);
            sum_f += dataHf[offsetX1] * s_0f * (weightArray[0][1]);
            sum_f += dataHf[offsetX2] * s_1f * (weightArray[0][2]);
            sum_f += dataHf[offsetX3] * s_2f * (weightArray[0][3]);

            double sum0f = dataHf[offsetX] * s0_f * (weightArray[1][0]);
            sum0f += dataHf[offsetX1] * s00f * (weightArray[1][1]);
            sum0f += dataHf[offsetX2] * s01f * (weightArray[1][2]);
            sum0f += dataHf[offsetX3] * s02f * (weightArray[1][3]);

            double sum1f = dataHf[offsetX] * s1_f * (weightArray[2][0]);
            sum1f += dataHf[offsetX1] * s10f * (weightArray[2][1]);
            sum1f += dataHf[offsetX2] * s11f * (weightArray[2][2]);
            sum1f += dataHf[offsetX3] * s12f * (weightArray[2][3]);

            double sum2f = dataHf[offsetX] * s2_f * (weightArray[3][0]);
            sum2f += dataHf[offsetX1] * s20f * (weightArray[3][1]);
            sum2f += dataHf[offsetX2] * s21f * (weightArray[3][2]);
            sum2f += dataHf[offsetX3] * s22f * (weightArray[3][3]);

            // Interpolation on the Y axis
            sumd = dataVf[offsetY] * sum_f;
            sumd += dataVf[offsetY + 1] * sum0f;
            sumd += dataVf[offsetY + 2] * sum1f;
            sumd += dataVf[offsetY + 3] * sum2f;

            // Data Clamping
            if (sumd > Float.MAX_VALUE) {
                sumd = Float.MAX_VALUE;
            } else if (sumd < -Float.MAX_VALUE) {
                sumd = -Float.MAX_VALUE;
            }

            return sumd;

        case DataBuffer.TYPE_DOUBLE:
            // Interpolation on the X axis
            double sum_d = dataHd[offsetX] * s__d * (weightArray[0][0]);
            sum_d += dataHd[offsetX1] * s_0d * (weightArray[0][1]);
            sum_d += dataHd[offsetX2] * s_1d * (weightArray[0][2]);
            sum_d += dataHd[offsetX3] * s_2d * (weightArray[0][3]);

            double sum0d = dataHd[offsetX] * s0_d * (weightArray[1][0]);
            sum0d += dataHd[offsetX1] * s00d * (weightArray[1][1]);
            sum0d += dataHd[offsetX2] * s01d * (weightArray[1][2]);
            sum0d += dataHd[offsetX3] * s02d * (weightArray[1][3]);

            double sum1d = dataHd[offsetX] * s1_d * (weightArray[2][0]);
            sum1d += dataHd[offsetX1] * s10d * (weightArray[2][1]);
            sum1d += dataHd[offsetX2] * s11d * (weightArray[2][2]);
            sum1d += dataHd[offsetX3] * s12d * (weightArray[2][3]);

            double sum2d = dataHd[offsetX] * s2_d * (weightArray[3][0]);
            sum2d += dataHd[offsetX1] * s20d * (weightArray[3][1]);
            sum2d += dataHd[offsetX2] * s21d * (weightArray[3][2]);
            sum2d += dataHd[offsetX3] * s22d * (weightArray[3][3]);

            // Interpolation on the Y axis
            sumd = dataVd[offsetY] * sum_d;
            sumd += dataVd[offsetY + 1] * sum0d;
            sumd += dataVd[offsetY + 2] * sum1d;
            sumd += dataVd[offsetY + 3] * sum2d;

            return sumd;
        default:
            break;
        }

        // Result calculation (only for integer/short/ushort/byte)
        int s = (int) ((sum + round) >> precisionBits);

        // Data Clamping
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            if (s > 255) {
                s = 255;
            } else if (s < 0) {
                s = 0;
            }
            break;
        case DataBuffer.TYPE_USHORT:
            if (s > 65536) {
                s = 65536;
            } else if (s < 0) {
                s = 0;
            }

            break;
        case DataBuffer.TYPE_SHORT:
            if (s > Short.MAX_VALUE) {
                s = Short.MAX_VALUE;
            } else if (s < Short.MIN_VALUE) {
                s = Short.MIN_VALUE;
            }

            break;
        case DataBuffer.TYPE_INT:
            break;
        }
        return s;

    }

    // This method compute the sum of all the elements inside the array
    private boolean sumZero(double[][] values) {
        // sum initialization
        double sum = 0;
        // cycle through all the values and update the sum value
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                sum += values[i][j];
            }
        }
        // control if the total sum is 0
        if (sum == 0) {
            return true;
        }
        return false;
    }

    public int interpolateBinary(int xNextBitNo, Number[] sourceData, int xfrac, int yfrac,
            int sourceYOffset, int sourceScanlineStride, int[] coordinates,int[] roiDataArray, int roiYOffset, int roiScanlineStride ) {

        // -----------------DATA-INITIALIZATION------------------------------------------------

        // 16 surrounding pixel initialization
        int[][] bitArray = new int[4][4];

        int[] byteshift=null;
        int[] shortshift=null;
        int[] intshift=null;
        
        // xNextBitNo is the shift inside the pixel element, to the bit on the right of the selected one

        // Calculates the bit number of the selected pixel's position
        int sbitnum = xNextBitNo - 1;
        // Shift inside the pixel element, to the bit on the left of the selected one.
        int xBeforeBitNo = sbitnum - 1;
        // Shift inside the pixel element, to the second bit on the right of the selected one.
        int xNextBitNo2 = sbitnum + 2;

        // initialization of the shift bit array
        int[] bitshift = new int[4];

        // Offset initialization for interpolation on X axis.
        int offsetX = 0;
        // Offset initialization for interpolation on Y axis.
        int offsetY = 0;

        // Calculates the interpolation for every type of data that allows binary images.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // initialization of the shift byte array
            byteshift = new int[4];
            // This value is used for searching the selected pixel inside the element.
            bitshift[1] = 7 - (sbitnum & 7);
            // Conversion from bit to Byte for searching the element in which the selected pixel is found.
            byteshift[1] = sbitnum >> 3;
            // int sbytenum = sbitnum >> 3;

            // This value is used for searching the pixel on the right of the selected one inside the element.
            bitshift[2] = 7 - (xNextBitNo & 7);
            // Conversion from bit to Byte for searching the element of the pixel on the right of the selected one.
            byteshift[2] = xNextBitNo >> 3;
            // int xNextByteNo = xNextBitNo >> 3;

            // This value is used for searching the pixel on the left of the selected one inside the element.
            bitshift[0] = 7 - (xBeforeBitNo & 7);
            // Conversion from bit to Byte for searching the element of the pixel on the left of the selected one.
            byteshift[0] = xBeforeBitNo >> 3;
            // int xBeforeByteNo = xBeforeBitNo >> 3;

            // This value is used for searching the second pixel on the right of the selected one inside the element.
            bitshift[3] = 7 - (xNextBitNo2 & 7);
            // Conversion from bit to Byte for searching the element of the second pixel on the right of the selected one.
            byteshift[3] = xNextBitNo2 >> 3;
            // int xNextByteNo2 = xNextBitNo2 >> 3;

            // Searching of the 16 pixels surrounding the selected one.

            for (int i = 0; i < bitArray.length; i++) {
                for (int j = 0; j < bitArray.length; j++) {

                    bitArray[i][j] = (sourceData[sourceYOffset + ((i - 1) * sourceScanlineStride)
                            + byteshift[j]].byteValue() >> bitshift[j]) & 0x01;
                }
            }

            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            // initialization of the shift byte array
            shortshift = new int[4];
            // This value is used for searching the selected pixel inside the element.
            bitshift[1] = 15 - (sbitnum & 15);
            // Conversion from bit to Short.
            shortshift[1] = sbitnum >> 4;

            // This value is used for searching the pixel on the right of the selected one inside the element.
            bitshift[2] = 15 - (xNextBitNo & 15);
            // Conversion from bit to Short.
            shortshift[2] = xNextBitNo >> 4;

            // This value is used for searching the pixel on the left of the selected one inside the element.
            bitshift[0] = 15 - (xBeforeBitNo & 15);
            // Conversion from bit to Short.
            shortshift[0] = xBeforeBitNo >> 4;

            // This value is used for searching the second pixel on the right of the selected one inside the element.
            bitshift[3] = 15 - (xNextBitNo2 & 15);
            // Conversion from bit to Short.
            shortshift[3] = xNextBitNo2 >> 4;

            // Searching of the 16 pixels surrounding the selected one.

            for (int i = 0; i < bitArray.length; i++) {
                for (int j = 0; j < bitArray.length; j++) {
                    bitArray[i][j] = (sourceData[sourceYOffset + ((i - 1) * sourceScanlineStride)
                            + shortshift[j]].shortValue() >> bitshift[j]) & 0x01;
                }
            }
            break;
        case DataBuffer.TYPE_INT:
            // initialization of the shift byte array
            intshift = new int[4];
            // This value is used for searching the selected pixel inside the element.
            bitshift[1] = 31 - (sbitnum & 31);
            // Conversion from bit to Integer.
            intshift[1] = sbitnum >> 5;

            // This value is used for searching the pixel on the right of the selected one inside the element.
            bitshift[2] = 31 - (xNextBitNo & 31);
            // Conversion from bit to Integer.
            intshift[2] = xNextBitNo >> 5;

            // This value is used for searching the pixel on the left of the selected one inside the element.
            bitshift[0] = 31 - (xBeforeBitNo & 31);
            // Conversion from bit to Integer.
            intshift[0] = xBeforeBitNo >> 5;

            // This value is used for searching the second pixel on the right of the selected one inside the element.
            bitshift[3] = 31 - (xNextBitNo2 & 31);
            // Conversion from bit to Integer.
            intshift[3] = xNextBitNo2 >> 5;

            // Searching of the 16 pixels surrounding the selected one.

            for (int i = 0; i < bitArray.length; i++) {
                for (int j = 0; j < bitArray.length; j++) {
                    bitArray[i][j] = (sourceData[sourceYOffset + ((i - 1) * sourceScanlineStride)
                            + intshift[j]].intValue() >> bitshift[j]) & 0x01;
                }
            }
            break;
        default:
            break;
        }

        // --------------------------ROI-CONTROL------------------------------------------------------

        // If an image ROI has been saved, this ROI is used for checking if
        // all the surrounding pixel belongs to the ROI.

        // Initial weight array as an array of ones.
        int[][] weightArray = new int[4][4];
        for (int i = 0; i < weightArray.length; i++) {
            for (int j = 0; j < weightArray.length; j++) {
                weightArray[i][j] = 1;
            }
        }

        if(useROIAccessor){
            
            int roiDataLength=roiDataArray.length;
            
            int[] sbyteShortIntShift = null;
            
            switch(dataType){
            case DataBuffer.TYPE_BYTE:
                sbyteShortIntShift=byteshift;
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                sbyteShortIntShift=shortshift;
                break;
            case DataBuffer.TYPE_INT:
                sbyteShortIntShift=intshift;
                break;
            }
            
            for (int i = 0; i < weightArray.length; i++) {
                for (int j = 0; j < weightArray.length; j++) {
                    int windex = roiYOffset + sbyteShortIntShift[j] + (i-1)*roiScanlineStride;                   
                    weightArray[i][j]=windex<roiDataLength ? roiDataArray[windex]>> bitshift[j] & 0x01 : 0;
                }
            }
        }else if (coordinates != null && roiBounds != null) {
            // Central pixel positions
            int x0 = coordinates[0];
            int y0 = coordinates[1];
            // ROI control
            if (roiBounds.contains(x0, y0)) {
                // Total weight sum
                int sum = 0;
                // Temporary variable for storing the iterators result
                int valueIter = 0;
                for (int i = 0; i < weightArray.length; i++) {
                    for (int j = 0; j < weightArray.length; j++) {

                        valueIter = roiIter.getSample(x0 + j - 1, y0 + i - 1, 0) & 0x01;
                        sum += valueIter;
                        weightArray[i][j] = valueIter;
                    }
                }
                // If the total weight sum is 0 no more computations are performed
                if (sum == 0) {
                    return black;
                }
            } else {// If the pixel is outside the ROI no more computations are performed
                return black;
            }
        }

        
        
        
        
        
        // -----------------BICUBIC-INTERPOLATION-----------------------------------------------------

        long[] sumH = new long[4];
        long sum = 0;

        for (int i = 0; i < sumH.length; i++) {
            sumH[i] = 0;
            for (int j = 0; j < sumH.length; j++) {
                // Interpolation on the X axis
                sumH[i] += (long) dataHi[offsetX + j] * bitArray[i][j] * ((long) weightArray[i][j]);
            }
            // Intermediate rounding
            sumH[i] = (sumH[i] + round) >> precisionBits;
            // Interpolation on the Y axis
            sum += (long) dataVi[offsetY + i] * sumH[i];
        }
        return (int)((sum + round) >> precisionBits);

        // // SAME CODE BUT WITHOUT THE FOR CYCLE
        //
        // // Interpolate in X
        // int offsetX1 = offsetX + 1;
        // int offsetX2 = offsetX + 2;
        // int offsetX3 = offsetX + 3;
        //
        // // Interpolate in y
        // int offsetY1 = offsetY + 1;
        // int offsetY2 = offsetY + 2;
        // int offsetY3 = offsetY + 3;
        //
        // // Interpolation on the X axis
        // long sum_ = (long) dataHi[offsetX] * bitArray[0][0] * ((long) weightArray[0][0]);
        // sum_ += (long) dataHi[offsetX1] * bitArray[0][1] * ((long) weightArray[0][1]);
        // sum_ += (long) dataHi[offsetX2] * bitArray[0][2] * ((long) weightArray[0][2]);
        // sum_ += (long) dataHi[offsetX3] * bitArray[0][3] * ((long) weightArray[0][3]);
        //
        // long sum0 = (long) dataHi[offsetX] * bitArray[1][0] * ((long) weightArray[1][0]);
        // sum0 += (long) dataHi[offsetX1] * bitArray[1][1] * ((long) weightArray[1][1]);
        // sum0 += (long) dataHi[offsetX2] * bitArray[1][2] * ((long) weightArray[1][2]);
        // sum0 += (long) dataHi[offsetX3] * bitArray[1][3] * ((long) weightArray[1][3]);
        //
        // long sum1 = (long) dataHi[offsetX] * bitArray[2][0] * ((long) weightArray[2][0]);
        // sum1 += (long) dataHi[offsetX1] * bitArray[2][1] * ((long) weightArray[2][1]);
        // sum1 += (long) dataHi[offsetX2] * bitArray[2][2] * ((long) weightArray[2][2]);
        // sum1 += (long) dataHi[offsetX3] * bitArray[2][3] * ((long) weightArray[2][3]);
        //
        // long sum2 = (long) dataHi[offsetX] * bitArray[3][0] * ((long) weightArray[3][0]);
        // sum2 += (long) dataHi[offsetX1] * bitArray[3][1] * ((long) weightArray[3][1]);
        // sum2 += (long) dataHi[offsetX2] * bitArray[3][2] * ((long) weightArray[3][2]);
        // sum2 += (long) dataHi[offsetX3] * bitArray[3][3] * ((long) weightArray[3][3]);
        //
        // // Intermediate rounding
        // sum_ = (sum_ + round) >> precisionBits;
        // sum0 = (sum0 + round) >> precisionBits;
        // sum1 = (sum1 + round) >> precisionBits;
        // sum2 = (sum2 + round) >> precisionBits;
        //
        // // Interpolation on the Y axis
        // long sum = (long) dataVi[offsetY] * sum_;
        // sum += (long) dataVi[offsetY1] * sum0;
        // sum += (long) dataVi[offsetY2] * sum1;
        // sum += (long) dataVi[offsetY3] * sum2;

    }

}
