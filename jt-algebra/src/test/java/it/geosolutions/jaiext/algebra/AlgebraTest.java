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
package it.geosolutions.jaiext.algebra;

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

public class AlgebraTest extends TestBase {

    private static final int NUM_IMAGES = 3;

    private static final int NUM_TYPES = 6;

    private static final int DEFAULT_WIDTH_REDUCED = DEFAULT_WIDTH / 2;

    private static final int DEFAULT_HEIGHT_REDUCED = DEFAULT_HEIGHT / 2;

    private static final double TOLERANCE = 0.1d;

    private static RenderedImage[][] testImages;

    private static Range noDataByte;

    private static Range noDataUShort;

    private static Range noDataShort;

    private static Range noDataInt;

    private static Range noDataFloat;

    private static Range noDataDouble;

    private static int destNoData;

    private static ROI roiObject;

    @BeforeClass
    public static void initialSetup() {

        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        testImages = new RenderedImage[NUM_TYPES][NUM_IMAGES];

        IMAGE_FILLER = true;
        for (int j = 0; j < NUM_IMAGES; j++) {
            testImages[DataBuffer.TYPE_BYTE][j] = createTestImage(DataBuffer.TYPE_BYTE,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataB, false, j == 0 ? 1 : 3, 64 + j);
            testImages[DataBuffer.TYPE_USHORT][j] = createTestImage(DataBuffer.TYPE_USHORT,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataS, false, j == 0 ? 1 : 3, Short.MAX_VALUE / 4 + j);
            testImages[DataBuffer.TYPE_SHORT][j] = createTestImage(DataBuffer.TYPE_SHORT,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataS, false, j == 0 ? 1 : 3, -50 + j);
            testImages[DataBuffer.TYPE_INT][j] = createTestImage(DataBuffer.TYPE_INT,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataI, false, j == 0 ? 1 : 3, 100 + j );
            testImages[DataBuffer.TYPE_FLOAT][j] = createTestImage(DataBuffer.TYPE_FLOAT,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataF, false, j == 0 ? 1 : 3, (255 / 2) * 5 + j);
            testImages[DataBuffer.TYPE_DOUBLE][j] = createTestImage(DataBuffer.TYPE_DOUBLE,
                    DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataD, false, j == 0 ? 1 : 3, (255 / 1) * 4 + j);
        }
        IMAGE_FILLER = false;

        // No Data Ranges
        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataS, minIncluded, noDataS, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        // Destination No Data
        destNoData = 100;

        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH_REDUCED / 4,
                DEFAULT_HEIGHT_REDUCED / 4);
        roiObject = new ROIShape(roiBounds);
    }

    @Test
    public void testNoROINoNoData() {

        boolean roiUsed = false;
        boolean noDataUsed = false;
        
        for(int i = 0; i < 6; i++){
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testOnlyNoData() {

        boolean roiUsed = false;
        boolean noDataUsed = true;
        
        for(int i = 0; i < 6; i++){
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testOnlyROI() {

        boolean roiUsed = true;
        boolean noDataUsed = false;
        
        for(int i = 0; i < 6; i++){
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testROIAndNoData() {

        boolean roiUsed = true;
        boolean noDataUsed = true;
        
        for(int i = 0; i < 6; i++){
            runTests(i, noDataUsed, roiUsed);
        }
    }
    
    private void runTests(int dataType, boolean noDataUsed, boolean roiUsed) {
        testOperation(testImages[dataType], Operator.SUM, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.SUBTRACT, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.MULTIPLY, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.DIVIDE, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.LOG, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.EXP, noDataUsed, roiUsed);
        if(dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE){
            testOperation(testImages[dataType], Operator.ABSOLUTE, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.AND, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.OR, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.XOR, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.INVERT, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.NOT, noDataUsed, roiUsed);
        }
    }

    private void testOperation(RenderedImage[] sources, Operator op, boolean noDataUsed,
            boolean roiUsed) {
        // Optional No Data Range used
        Range noData;
        // Source image data type
        int dataType = sources[0].getSampleModel().getDataType();
        // If no Data are present, the No Data Range associated is used
        if (noDataUsed) {

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noData = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noData = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noData = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noData = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noData = null;
        }

        ROI roi;

        if (roiUsed) {
            roi = roiObject;
        } else {
            roi = null;
        }

        int minBandNumber = Integer.MAX_VALUE;

        for (int i = 0; i < NUM_IMAGES; i++) {

            int numBandImage = sources[i].getSampleModel().getNumBands();

            if (numBandImage < minBandNumber) {
                minBandNumber = numBandImage;
            }
        }

        // operation
        RenderedOp calculated = AlgebraDescriptor
                .create(op, roi, noData, destNoData, null, sources);
        // Check if the bands number is the same
        assertEquals(minBandNumber, calculated.getNumBands());

        switch (op) {
        case SUM:
            testSum(calculated, sources, roi, noData, minBandNumber);
            break;
        case SUBTRACT:
            testSubtract(calculated, sources, roi, noData, minBandNumber);
            break;
        case MULTIPLY:
            testMultiply(calculated, sources, roi, noData, minBandNumber);
            break;
        case DIVIDE:
            testDivide(calculated, sources, roi, noData, minBandNumber);
            break;
        case AND:
        case OR:
        case XOR:
            testLogicalOp(calculated, sources, roi, noData, minBandNumber, op);
            break;
        case EXP:
        case NOT:
        case INVERT:
        case ABSOLUTE:
        case LOG:
            testSingleImageOp(calculated, sources, roi, noData, minBandNumber, op);
            break;
        }
        

        // Disposal of the output image
        calculated.dispose();
    }

    private void testLogicalOp(RenderedOp calculated, RenderedImage[] sources, ROI roi, Range noData,
            int minBandNumber, Operator op) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        int numSrc = sources.length;

        // Source Raster Array
        Raster[] sourceRasters = new Raster[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sourceRasters[i] = sources[i].getTile(minTileX, minTileY);
        }

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = false;

                    boolean isValidROI = !roiUsed || roiUsed && roi.contains(x, y);
                    if(isValidROI){
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            byte valueB = 0;
                            for(int i = 0; i < numSrc; i++){
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if(!noDataUsed || noDataUsed && !noDataDouble.contains(sample)){
                                    isValidData = true;
                                    if(i == 0){
                                        valueB = (byte) sample;
                                    } else {
                                        valueB = op.calculate(valueB, (byte) sample);
                                    }
                                }
                            }
                            valueOld = valueB;
                            //valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            short valueU = 0;
                            for(int i = 0; i < numSrc; i++){
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if(!noDataUsed || noDataUsed && !noDataDouble.contains(sample)){
                                    isValidData = true;
                                    if(i == 0){
                                        valueU = (short) sample;
                                    } else {
                                        valueU = op.calculate(true, valueU, (short) sample);
                                    }
                                }
                            }
                            valueOld = valueU;
                            //valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            break;
                        case DataBuffer.TYPE_SHORT:
                            short valueS = 0;
                            for(int i = 0; i < numSrc; i++){
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if(!noDataUsed || noDataUsed && !noDataDouble.contains(sample)){
                                    isValidData = true;
                                    if(i == 0){
                                        valueS = (short) sample;
                                    } else {
                                        valueS = op.calculate(false, valueS, (short) sample);
                                    }
                                }
                            }
                            valueOld = valueS;
                            //valueOld = ImageUtil.clampRoundShort(valueOld);
                            break;
                        case DataBuffer.TYPE_INT:
                            int valueI = 0;
                            for(int i = 0; i < numSrc; i++){
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if(!noDataUsed || noDataUsed && !noDataDouble.contains(sample)){
                                    isValidData = true;
                                    if(i == 0){
                                        valueI = (int) sample;
                                    } else {
                                        valueI = op.calculate(valueI, (int) sample);
                                    }
                                }
                            }
                            valueOld = valueI;
                            //valueOld = ImageUtil.clampRoundInt(valueOld);
                            break;
                        default:
                            break;
                        }
                        
                        if(!isValidData){
                            assertEquals(value, destNoData, TOLERANCE);
                        }else {
                            assertEquals(value, valueOld, TOLERANCE);
                        }
                    }else{
                        assertEquals(value, destNoData, TOLERANCE);
                    }
                }
            }
        }
    }

    private void testSingleImageOp(RenderedOp calculated, RenderedImage[] sources, ROI roi, Range noData,
            int minBandNumber, Operator op) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        // Source Raster Array
        Raster sourceRasters = sources[0].getTile(minTileX, minTileY);

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;
                    
                    sample = sourceRasters.getSampleDouble(x, y, b);

                    isValidData = (!roiUsed || roiUsed && roi.contains(x, y))
                            && (!noDataUsed || noDataUsed && !noDataDouble.contains(sample));    
                    
                    if(isValidData){
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            valueOld = op.calculate((byte)sample);
                            //valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = op.calculate(true, (short)sample)&0xFFFF;
                            //valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = op.calculate(false, (short)sample);
                            valueOld = ImageUtil.clampRoundShort(valueOld);
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = op.calculate((int)sample);
                            valueOld = ImageUtil.clampRoundInt(valueOld);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = op.calculate((float)sample);
                            //valueOld = (float) sample;
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            valueOld = op.calculate(sample);
                            break;
                        default:
                            break;
                        }
                        assertEquals(value, valueOld, TOLERANCE);
                    } else {
                        assertEquals(value, destNoData, TOLERANCE);
                    }
                }
            }
        }
    }
    
    private void testSum(RenderedOp calculated, RenderedImage[] sources, ROI roi, Range noData,
            int minBandNumber) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        int numSrc = sources.length;

        // Source Raster Array
        Raster[] sourceRasters = new Raster[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sourceRasters[i] = sources[i].getTile(minTileX, minTileY);
        }

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = false;

                    // If no Data are present, no data check is performed
                    if (noDataUsed && roiUsed) {
                        if (roi.contains(x, y)) {
                            for (int i = 0; i < numSrc; i++) {
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if (!noDataDouble.contains(sample)) {
                                    valueOld += sample;
                                    isValidData = true;
                                }
                            }
                            if (isValidData) {
                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                    value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    valueOld = ImageUtil.clampRoundShort(valueOld);
                                    break;
                                case DataBuffer.TYPE_INT:
                                    valueOld = ImageUtil.clampRoundInt(valueOld);
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    valueOld = (float) valueOld;
                                    break;
                                default:
                                    break;
                                }
                                assertEquals(value, valueOld, TOLERANCE);
                            } else {
                                assertEquals(value, destNoData, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        for (int i = 0; i < numSrc; i++) {
                            sample = sourceRasters[i].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld += sample;
                                isValidData = true;
                            }
                        }
                        if (isValidData) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roi.contains(x, y)) {
                            for (int i = 0; i < numSrc; i++) {
                                valueOld += sourceRasters[i].getSampleDouble(x, y, b);
                            }
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else {
                        for (int i = 0; i < numSrc; i++) {
                            valueOld += sourceRasters[i].getSampleDouble(x, y, b);
                        }
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = ImageUtil.clampRoundShort(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = ImageUtil.clampRoundInt(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = (float) valueOld;
                            break;
                        default:
                            break;
                        }
                        // Else a simple value comparison is done
                        assertEquals(value, valueOld, TOLERANCE);
                    }
                }
            }
        }
    }

    private void testSubtract(RenderedOp calculated, RenderedImage[] sources, ROI roi,
            Range noData, int minBandNumber) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        int numSrc = sources.length;

        // Source Raster Array
        Raster[] sourceRasters = new Raster[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sourceRasters[i] = sources[i].getTile(minTileX, minTileY);
        }

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = false;

                    // If no Data are present, no data check is performed
                    if (noDataUsed && roiUsed) {
                        if (roi.contains(x, y)) {
                            sample = sourceRasters[0].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld = sample;
                                isValidData = true;
                            }
                            for (int i = 1; i < numSrc; i++) {
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if (!noDataDouble.contains(sample)) {
                                    valueOld -= sample;
                                    isValidData = true;
                                }
                            }

                            if (isValidData) {
                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                    value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                    
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    valueOld = ImageUtil.clampRoundShort(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_INT:
                                    valueOld = ImageUtil.clampRoundInt(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    valueOld = (float) valueOld;
                                    break;
                                default:
                                    break;
                                }
                                assertEquals(value, valueOld, TOLERANCE);
                            } else {
                                assertEquals(value, destNoData, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        sample = sourceRasters[0].getSampleDouble(x, y, b);
                        if (!noDataDouble.contains(sample)) {
                            valueOld = sample;
                            isValidData = true;
                        }
                        for (int i = 1; i < numSrc; i++) {
                            sample = sourceRasters[i].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld -= sample;
                                isValidData = true;
                            }
                        }
                        if (isValidData) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roi.contains(x, y)) {
                            valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                            for (int i = 1; i < numSrc; i++) {
                                valueOld -= sourceRasters[i].getSampleDouble(x, y, b);
                            }
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else {
                        valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                        for (int i = 1; i < numSrc; i++) {
                            valueOld -= sourceRasters[i].getSampleDouble(x, y, b);
                        }
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = ImageUtil.clampRoundShort(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = ImageUtil.clampRoundInt(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = (float) valueOld;
                            break;
                        default:
                            break;
                        }
                        // Else a simple value comparison is done
                        assertEquals(value, valueOld, TOLERANCE);
                    }
                }
            }
        }
    }

    private void testMultiply(RenderedOp calculated, RenderedImage[] sources, ROI roi,
            Range noData, int minBandNumber) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        int numSrc = sources.length;

        // Source Raster Array
        Raster[] sourceRasters = new Raster[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sourceRasters[i] = sources[i].getTile(minTileX, minTileY);
        }

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = false;

                    // If no Data are present, no data check is performed
                    if (noDataUsed && roiUsed) {
                        if (roi.contains(x, y)) {
                            sample = sourceRasters[0].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld = sample;
                                isValidData = true;
                            } else {
                                valueOld = 1;
                            }
                            for (int i = 1; i < numSrc; i++) {
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if (!noDataDouble.contains(sample)) {
                                    valueOld *= sample;
                                    isValidData = true;
                                }
                            }

                            if (isValidData) {
                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                    value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                    
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    valueOld = ImageUtil.clampRoundShort(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_INT:
                                    valueOld = ImageUtil.clampRoundInt(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    valueOld = (float) valueOld;
                                    break;
                                default:
                                    break;
                                }
                                assertEquals(value, valueOld, TOLERANCE);
                            } else {
                                assertEquals(value, destNoData, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        sample = sourceRasters[0].getSampleDouble(x, y, b);
                        if (!noDataDouble.contains(sample)) {
                            valueOld = sample;
                            isValidData = true;
                        } else {
                            valueOld = 1;
                        }
                        for (int i = 1; i < numSrc; i++) {
                            sample = sourceRasters[i].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld *= sample;
                                isValidData = true;
                            }
                        }
                        if (isValidData) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roi.contains(x, y)) {
                            valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                            for (int i = 1; i < numSrc; i++) {
                                valueOld *= sourceRasters[i].getSampleDouble(x, y, b);
                            }
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else {
                        valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                        for (int i = 1; i < numSrc; i++) {
                            valueOld *= sourceRasters[i].getSampleDouble(x, y, b);
                        }
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = ImageUtil.clampRoundShort(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = ImageUtil.clampRoundInt(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = (float) valueOld;
                            break;
                        default:
                            break;
                        }
                        // Else a simple value comparison is done
                        assertEquals(value, valueOld, TOLERANCE);
                    }
                }
            }
        }
    }

    private void testDivide(RenderedOp calculated, RenderedImage[] sources, ROI roi, Range noData,
            int minBandNumber) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        int numSrc = sources.length;

        // Source Raster Array
        Raster[] sourceRasters = new Raster[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sourceRasters[i] = sources[i].getTile(minTileX, minTileY);
        }

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;

        int dataType = calculated.getSampleModel().getDataType();

        // Cycle on all the tile Bands
        for (int b = 0; b < minBandNumber; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = false;

                    // If no Data are present, no data check is performed
                    if (noDataUsed && roiUsed) {
                        if (roi.contains(x, y)) {
                            sample = sourceRasters[0].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld = sample;
                                isValidData = true;
                            } else {
                                valueOld = 1;
                            }
                            for (int i = 1; i < numSrc; i++) {
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                if (!noDataDouble.contains(sample)) {
                                    valueOld /= sample == 0 ? 1 : sample;
                                    isValidData = true;
                                }
                            }

                            if (isValidData) {
                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                    value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                    
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    valueOld = ImageUtil.clampRoundShort(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_INT:
                                    valueOld = ImageUtil.clampRoundInt(valueOld);
                                    
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    valueOld = (float) valueOld;
                                    break;
                                default:
                                    break;
                                }
                                assertEquals(value, valueOld, TOLERANCE);
                            } else {
                                assertEquals(value, destNoData, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        sample = sourceRasters[0].getSampleDouble(x, y, b);
                        if (!noDataDouble.contains(sample)) {
                            valueOld = sample;
                            isValidData = true;
                        } else {
                            valueOld = 1;
                        }
                        for (int i = 1; i < numSrc; i++) {
                            sample = sourceRasters[i].getSampleDouble(x, y, b);
                            if (!noDataDouble.contains(sample)) {
                                valueOld /= sample == 0 ? 1 : sample;
                                isValidData = true;
                            }
                        }
                        if (isValidData) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roi.contains(x, y)) {
                            valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                            for (int i = 1; i < numSrc; i++) {
                                sample = sourceRasters[i].getSampleDouble(x, y, b);
                                valueOld /= sample == 0 ? 1 : sample;
                            }
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                                value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                                
                                break;
                            case DataBuffer.TYPE_SHORT:
                                valueOld = ImageUtil.clampRoundShort(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_INT:
                                valueOld = ImageUtil.clampRoundInt(valueOld);
                                
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                valueOld = (float) valueOld;
                                break;
                            default:
                                break;
                            }
                            assertEquals(value, valueOld, TOLERANCE);
                        } else {
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    } else {
                        valueOld = sourceRasters[0].getSampleDouble(x, y, b);
                        for (int i = 1; i < numSrc; i++) {
                            sample = sourceRasters[i].getSampleDouble(x, y, b);
                            valueOld /= sample == 0 ? 1 : sample;
                        }
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            valueOld = (byte) (((((int) valueOld << 23) >> 31) | (int) valueOld) & 0xFF);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = ImageUtil.clampRoundUShort(valueOld)&0xFFFF;
                            
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = ImageUtil.clampRoundShort(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = ImageUtil.clampRoundInt(valueOld);
                            
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = (float) valueOld;
                            break;
                        default:
                            break;
                        }
                        // Else a simple value comparison is done
                        assertEquals(value, valueOld, TOLERANCE);
                    }
                }
            }
        }
    }
}
