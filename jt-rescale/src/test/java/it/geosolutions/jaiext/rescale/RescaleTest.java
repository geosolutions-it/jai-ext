package it.geosolutions.jaiext.rescale;

import static org.junit.Assert.*;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.BeforeClass;
import org.junit.Ignore;
//import org.junit.Ignore;
import org.junit.Test;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

public class RescaleTest extends TestBase {

    private static final double TOLERANCE = 0.01d;

    private static RenderedImage[] sourceIMG;

    private static ROI roi;

    private static byte noDataB;

    private static short noDataU;

    private static short noDataS;

    private static int noDataI;

    private static float noDataF;

    private static double noDataD;

    private static Range noDataByte;

    private static Range noDataUShort;

    private static Range noDataShort;

    private static Range noDataInt;

    private static Range noDataFloat;

    private static Range noDataDouble;

    private static double[] scales;

    private static double[] offsets;

    private static double destNoData;

    private static Rectangle roiBounds;

    @BeforeClass
    public static void initialSetup() {

        // ROI creation
        roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);

        // No Data Range creation

        // No Data values
        noDataB = 50;
        noDataU = 50;
        noDataS = 50;
        noDataI = 50;
        noDataF = 50;
        noDataD = 50;

        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        // Image creations
        IMAGE_FILLER = true;

        sourceIMG = new RenderedImage[6];

        sourceIMG[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        sourceIMG[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataU, false);
        sourceIMG[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        sourceIMG[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        sourceIMG[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataF, false);
        sourceIMG[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataD, false);

        IMAGE_FILLER = false;

        // Scale values
        scales = new double[] { 10, 20, 30 };

        // Offset values
        offsets = new double[] { 0, 1, 2 };

        // Destination No Data
        destNoData = 0.0d;
    }

    
    
    // This test checks if the Rescale is correct in absence of No Data and ROI
    @Test
    public void testNoRangeNoRoi() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.NO_ROI_ONLY_DATA;

        // Byte data Type 
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of No Data but with ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiBounds() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.ROI_ONLY_DATA;
        
        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of No Data but with ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessor() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = true;
        TestSelection select = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of ROI but with No Data
    @Test
    public void testNoData() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.NO_ROI_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in presence of No Data and ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.ROI_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in presence of No Data and ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessorNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = true;
        TestSelection select = TestSelection.ROI_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }
    
    
    
    
    public void testRescale(RenderedImage source, boolean roiUsed, boolean noDataUsed,
            boolean useRoiAccessor, TestSelection select) {

        // The precalculated roi is used, if selected by the related boolean.
        ROI roiData;

        if (roiUsed) {
            roiData = roi;
        } else {
            roiData = null;
        }

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;
        // Image data type
        int dataType = source.getSampleModel().getDataType();
        
        if (noDataUsed) {
            
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noDataRange = null;
        }

        // Rescale operation
        PlanarImage rescaled = RescaleDescriptor.create(source, scales, offsets, roiData,
                noDataRange, useRoiAccessor, destNoData, null);


        // Display Image
        if (INTERACTIVE && TEST_SELECTOR == select.getType()) {
            RenderedImageBrowser.showChain(rescaled, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }else{
            // Calculation of all the image tiles
            rescaled.getTiles();
        }
        
        // Rescale control on the first band
        int tileMinX = rescaled.getMinTileX();
        int tileMinY = rescaled.getMinTileY();
        // Selection of the source and destination first tile
        Raster tileDest = rescaled.getTile(tileMinX, tileMinY);
        Raster tileSource = source.getTile(tileMinX, tileMinY);

         int tileMinXpix = tileDest.getMinX();
        int tileMinYpix = tileDest.getMinY();

        int tileMaxXpix = tileDest.getWidth() + tileMinXpix;
        int tileMaxYpix = tileDest.getHeight() + tileMinYpix;

        double scaleFactor = scales[0];
        double offset = offsets[0];
        // loop through the tile pixels
        for (int i = tileMinXpix; i < tileMaxXpix; i++) {
            for (int j = tileMinYpix; j < tileMaxYpix; j++) {

                switch(dataType){
                case DataBuffer.TYPE_BYTE:
                    // selection of the rescaled pixel
                    byte destValueB = (byte) tileDest.getSample(i, j, 0);
                    // rescale operation on the source pixel
                    int srcValueB =  tileSource.getSample(i, j, 0) & 0xFF;
                    byte calculationB = ImageUtil.clampRoundByte(srcValueB*scaleFactor + offset);     
                    // comparison
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains((byte)srcValueB)){ 
                            assertEquals(calculationB, destValueB);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationB, destValueB);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains((byte)srcValueB)){ 
                            assertEquals(calculationB, destValueB);
                        }
                    }else{
                        assertEquals(calculationB, destValueB);
                    }                   
                    break;
                case DataBuffer.TYPE_USHORT:
                    short destValueU = (short) tileDest.getSample(i, j, 0);
                    int srcValueU =  tileSource.getSample(i, j, 0) & 0xFFFF;
                    short calculationU = ImageUtil.clampRoundUShort(srcValueU*scaleFactor + offset);
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains((short)srcValueU)){ 
                            assertEquals(calculationU, destValueU);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationU, destValueU);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains((short)srcValueU)){ 
                            assertEquals(calculationU, destValueU);
                        }
                    }else{
                        assertEquals(calculationU, destValueU);
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    short destValueS = (short) tileDest.getSample(i, j, 0);
                    short srcValueS =  (short)tileSource.getSample(i, j, 0);
                    short calculationS = ImageUtil.clampRoundShort(srcValueS*scaleFactor + offset);
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains(srcValueS)){ 
                            assertEquals(calculationS, destValueS);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationS, destValueS);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains(srcValueS)){ 
                            assertEquals(calculationS, destValueS);
                        }
                    }else{
                        assertEquals(calculationS, destValueS);
                    }                    
                    break;
                case DataBuffer.TYPE_INT:
                    int destValueI = tileDest.getSample(i, j, 0);
                    int srcValueI =  tileSource.getSample(i, j, 0);
                    int calculationI = ImageUtil.clampRoundInt(srcValueI*scaleFactor + offset);
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains(srcValueI)){ 
                            assertEquals(calculationI, destValueI);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationI, destValueI);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains(srcValueI)){ 
                            assertEquals(calculationI, destValueI);
                        }
                    }else{
                        assertEquals(calculationI, destValueI);
                    }    
                    break;
                case DataBuffer.TYPE_FLOAT:
                    float destValueF = tileDest.getSampleFloat(i, j, 0);
                    float srcValueF =  tileSource.getSampleFloat(i, j, 0);
                    float calculationF = (float) ((srcValueF*scaleFactor) + offset);
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains(srcValueF)){ 
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains(srcValueF)){ 
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    }else{
                        assertEquals(calculationF, destValueF, TOLERANCE);
                    }                    
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    double destValueD = tileDest.getSampleDouble(i, j, 0);
                    double srcValueD =  tileSource.getSampleDouble(i, j, 0);
                    double calculationD = ((srcValueD*scaleFactor) + offset);
                    if(roiUsed && noDataUsed){
                        if(roiBounds.contains(i,j) && !noDataRange.contains(srcValueD)){ 
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    }else if(roiUsed){
                        if(roiBounds.contains(i,j)){ 
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    }else if(noDataUsed){
                        if(!noDataRange.contains(srcValueD)){ 
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    }else{
                        assertEquals(calculationD, destValueD, TOLERANCE);
                    }                   
                    break;
                    default:
                        throw new IllegalArgumentException("Wrong data type");
                }
            }
        }
    }

    // UNSUPPORTED OPERATIONS
    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

}
