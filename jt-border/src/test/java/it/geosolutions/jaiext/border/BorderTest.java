package it.geosolutions.jaiext.border;

import static org.junit.Assert.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

public class BorderTest extends TestBase {

    private static final double TOLERANCE = 0.1d;

    private static RenderedImage[] sourceIMG;

    private static Range noDataByte;

    private static Range noDataUShort;

    private static Range noDataShort;

    private static Range noDataInt;

    private static Range noDataFloat;

    private static Range noDataDouble;

    private static BorderExtender[] extender;

    private static double destNoData;

    private static int leftPad;

    private static int rightPad;

    private static int topPad;

    private static int bottomPad;

    @BeforeClass
    @Ignore
    public static void initialSetup() {

        // No Data values
        byte noDataB = 50;
        short noDataU = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50f;
        double noDataD = 50d;
        // Range parameters
        boolean minIncluded = true;
        boolean maxIncluded = true;
        boolean nanIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, nanIncluded);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, nanIncluded);

        sourceIMG = new RenderedImage[DataBuffer.TYPE_DOUBLE + 1];
        
        IMAGE_FILLER = true;

        sourceIMG[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 1);
        sourceIMG[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataU, false, 1);
        sourceIMG[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1);
        sourceIMG[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 1);
        sourceIMG[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 1);
        sourceIMG[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 1);

        IMAGE_FILLER = false;

        extender = new BorderExtender[BorderExtender.BORDER_WRAP +1];
        
        extender[BorderExtender.BORDER_ZERO] = BorderExtender
                .createInstance(BorderExtender.BORDER_ZERO);
        extender[BorderExtender.BORDER_COPY] = BorderExtender
                .createInstance(BorderExtender.BORDER_COPY);
        extender[BorderExtender.BORDER_REFLECT] = BorderExtender
                .createInstance(BorderExtender.BORDER_REFLECT);
        extender[BorderExtender.BORDER_WRAP] = BorderExtender
                .createInstance(BorderExtender.BORDER_WRAP);

        destNoData = 100d;

        leftPad = 2;
        rightPad = 2;
        topPad = 2;
        bottomPad = 2;

    }

    @Test
    @Ignore
    public void testBorderZero(){
        
        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_ZERO;
        
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);  
        
        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType); 
    }
    
    @Test
    @Ignore
    public void testBorderCopy(){
        
        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_COPY;
        
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);  
        
        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType); 
    }
    
    @Test
    @Ignore
    public void testBorderReflect(){
        
        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_REFLECT;
        
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);  
        
        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType); 
    }
    
    @Test
    @Ignore
    public void testBorderWrap(){
        
        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_WRAP;
        
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);  
        
        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType); 
    }
    
    
    
    private void testBorder(int dataType, boolean noDataRangeUsed, int borderType) {

        RenderedImage source = sourceIMG[dataType];

        BorderExtender extend = extender[borderType];

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;

        if (noDataRangeUsed) {
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

        RenderedOp borderIMG = BorderDescriptor.create(source, leftPad, rightPad, topPad,
                bottomPad, extend, noDataRange, destNoData, null);

//        RenderedImageBrowser.showChain(borderIMG, false, false);
//        
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            
//        }
        
        borderIMG.getTiles();
        
        int minTileX = borderIMG.getMinTileX();
        int minTileY = borderIMG.getMinTileY();

        Raster upperLeft = borderIMG.getTile(minTileX, minTileY);
        
        int maxTileX = borderIMG.getMaxTileX();
        int maxTileY = borderIMG.getMaxTileY();
        
        int width = upperLeft.getWidth();
        int height = upperLeft.getHeight();

        int widthSrc = source.getTileWidth();
        int heightSrc = source.getTileHeight();
        
        int minTileXsrc = source.getMinTileX();
        int minTileYsrc = source.getMinTileY();
        
        int numTileXsrc = source.getNumXTiles();
        int numTileYsrc = source.getNumYTiles();
        
        int maxTileXsrc = source.getMinTileX() + numTileXsrc;
        int maxTileYsrc = source.getMinTileY() + numTileYsrc;
        
        Raster lowerLeft = source.getTile(minTileXsrc, maxTileYsrc - 1 );
        
        Raster upperRight = source.getTile(maxTileXsrc - 1, minTileYsrc);
        
        int maxXSource = upperRight.getMinX() + widthSrc - 1;
        
        int maxYSource = lowerLeft.getMinY() + heightSrc - 1;

        int minX = upperLeft.getMinX();
        int minY = upperLeft.getMinY();

        int maxXPadded = minX + leftPad;
        int maxYPadded = minY + topPad;

        int maxX = minX + width;
        int maxY = minY + height;

        int x = 0;
        int y = 0;
        
        // Check on the top padding
        for (int xIndex = leftPad; xIndex < width; xIndex++) {
            for (int yIndex = 0; yIndex < topPad; yIndex++) {

                x = xIndex + minX;
                y = yIndex + minY;
                
                double value = upperLeft.getSampleDouble(x, y, 0);

                switch (borderType) {
                case BorderExtender.BORDER_ZERO:
                    assertEquals(value, 0, TOLERANCE);
                    break;
                case BorderExtender.BORDER_COPY:                    
                    double valueCopy = upperLeft.getSampleDouble(x, maxYPadded, 0);
                    if(noDataRangeUsed){
                        if(noDataDouble.contains(valueCopy)){
                            assertEquals(value, destNoData, TOLERANCE);
                        }else{
                            assertEquals(value, valueCopy, TOLERANCE);
                        }
                    }else{
                        assertEquals(value, valueCopy, TOLERANCE);
                    }                   
                    break;
                case BorderExtender.BORDER_REFLECT:                    
                    
                    double valueReflect = upperLeft.getSampleDouble(x, (topPad - yIndex - 1), 0);
                    if(noDataRangeUsed){
                        if(noDataDouble.contains(valueReflect)){
                            assertEquals(value, destNoData, TOLERANCE);
                        }else{
                            assertEquals(value, valueReflect, TOLERANCE);
                        }
                    }else{
                        assertEquals(value, valueReflect, TOLERANCE);
                    }
                    break;
                case BorderExtender.BORDER_WRAP:
                    x = x - leftPad;
                    y = maxYSource - yIndex - 1;
                    double valueWrap = lowerLeft.getSampleDouble(x, y, 0);
                    if(noDataRangeUsed){
                        if(noDataDouble.contains(valueWrap)){
                            assertEquals(value, destNoData, TOLERANCE);
                        }else{
                            assertEquals(value, valueWrap, TOLERANCE);
                        }
                    }else{
                        assertEquals(value, valueWrap, TOLERANCE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Wrong BorderExtender type");
                }
            }
        }

        
        // Check on the left padding
//        for (int xIndex = 0; xIndex < leftPad; xIndex++) {
//            for (int yIndex = 0; yIndex < height; yIndex++) {
//
//                x = xIndex + minX;
//                y = yIndex + minY;
//                
//                double value = upperLeft.getSampleDouble(x, y, 0);
//
//                switch (borderType) {
//                case BorderExtender.BORDER_ZERO:
//                    assertEquals(value, 0, TOLERANCE);
//                    break;
//                case BorderExtender.BORDER_COPY:                    
//                    double valueCopy = upperLeft.getSampleDouble(maxXPadded + 1, y, 0);
//                    if(noDataRangeUsed){
//                        if(noDataDouble.contains(valueCopy)){
//                            assertEquals(value, destNoData, TOLERANCE);
//                        }else{
//                            assertEquals(value, valueCopy, TOLERANCE);
//                        }
//                    }else{
//                        assertEquals(value, valueCopy, TOLERANCE);
//                    }                   
//                    break;
//                case BorderExtender.BORDER_REFLECT:
//                    double valueReflect = upperLeft.getSampleDouble(maxXPadded + xIndex + 1, y, 0);
//                    if(noDataRangeUsed){
//                        if(noDataDouble.contains(valueReflect)){
//                            assertEquals(value, destNoData, TOLERANCE);
//                        }else{
//                            assertEquals(value, valueReflect, TOLERANCE);
//                        }
//                    }else{
//                        assertEquals(value, valueReflect, TOLERANCE);
//                    }
//                    break;
//                case BorderExtender.BORDER_WRAP:
//                    x = maxXSource - xIndex;
//                    double valueWrap = lowerLeft.getSampleDouble(x, y, 0);
//                    if(noDataRangeUsed){
//                        if(noDataDouble.contains(valueWrap)){
//                            assertEquals(value, destNoData, TOLERANCE);
//                        }else{
//                            assertEquals(value, valueWrap, TOLERANCE);
//                        }
//                    }else{
//                        assertEquals(value, valueWrap, TOLERANCE);
//                    }
//                    break;
//                default:
//                    throw new IllegalArgumentException("Wrong BorderExtender type");
//                }
//            }
//        }

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
