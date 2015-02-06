package it.geosolutions.jaiext.rlookup;

import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the RangeLookup operation.
 *
 */
public class RangeLookupTest extends TestBase {

    private static final int WIDTH = 10;
    
    @Test
    public void byteToByte() {
        System.out.println("   byte source to byte destination");
        Byte[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createByteTestImage((byte)0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void byteToShort() {
        System.out.println("   byte source to short destination");
        Byte[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createByteTestImage((byte)0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void byteToInt() {
        System.out.println("   byte source to int destination");
        Byte[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createByteTestImage((byte)0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void byteToFloat() {
        System.out.println("   byte source to float destination");
        Byte[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createByteTestImage((byte)0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void byteToDouble() {
        System.out.println("   byte source to double destination");
        Byte[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createByteTestImage((byte)0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    @Test
    public void shortToByte() {
        System.out.println("   short source to byte destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void shortToShort() {
        System.out.println("   short source to short destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void shortToInt() {
        System.out.println("   short source to int destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void shortToFloat() {
        System.out.println("   short source to float destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void shortToDouble() {
        System.out.println("   short source to double destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    @Test
    public void ushortToByte() {
        System.out.println("   ushort source to byte destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createUShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void ushortToShort() {
        System.out.println("   ushort source to short destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createUShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void ushortToInt() {
        System.out.println("   ushort source to int destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createUShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void ushortToFloat() {
        System.out.println("   ushort source to float destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createUShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void ushortToDouble() {
        System.out.println("   ushort source to double destination");
        Short[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createUShortTestImage((short)0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    @Test
    public void intToByte() {
        System.out.println("   int source to byte destination");
        Integer[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createIntTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void intToShort() {
        System.out.println("   int source to short destination");
        Integer[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createIntTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void intToInt() {
        System.out.println("   int source to int destination");
        Integer[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createIntTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void intToFloat() {
        System.out.println("   int source to float destination");
        Integer[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createIntTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void intToDouble() {
        System.out.println("   int source to double destination");
        Integer[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createIntTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    
    @Test
    public void floatToByte() {
        System.out.println("   float source to byte destination");
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createFloatTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void floatToShort() {
        System.out.println("   float source to short destination");
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createFloatTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void floatToInt() {
        System.out.println("   float source to int destination");
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createFloatTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void floatToFloat() {
        System.out.println("   float source to float destination");
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createFloatTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void floatToDouble() {
        System.out.println("   float source to double destination");
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createFloatTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    @Test
    public void doubleToByte() {
        System.out.println("   double source to byte destination");
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createDoubleTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.BYTE);
    }
    
    @Test
    public void doubleToShort() {
        System.out.println("   double source to short destination");
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createDoubleTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    @Test
    public void doubleToInt() {
        System.out.println("   double source to int destination");
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createDoubleTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.INT);
    }
    
    @Test
    public void doubleToFloat() {
        System.out.println("   double source to float destination");
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = createDoubleTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.FLOAT);
    }
    
    @Test
    public void doubleToDouble() {
        System.out.println("   double source to double destination");
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = createDoubleTestImage(0);
        assertLookup(breaks, values, srcImg, ImageDataType.DOUBLE);
    }
    
    @Test
    public void shortToShortWithNoNegativeValues() throws Exception {
        System.out.println("   short source to short dest");
        
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createShortTestImage((short) 0);
        
        // The destination image shoule be TYPE_USHORT
        assertLookup(breaks, values, srcImg, ImageDataType.USHORT);
    }
    
    @Test
    public void ushortToUShort() throws Exception {
        System.out.println("   ushort source to ushort dest");

        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = createUShortTestImage((short) 0);
        assertLookup(breaks, values, srcImg, ImageDataType.USHORT);
    }
    
    @Test
    public void ushortSourceWithNegativeDestValues() throws Exception {
        System.out.println("   ushort source and negative lookup values");

        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = createUShortTestImage((short) 0);
        
        // The destination image should be TYPE_SHORT
        assertLookup(breaks, values, srcImg, ImageDataType.SHORT);
    }
    
    
    /**
     * Runs the lookup operation and tests destination image values.
     *
     * @param breaks source image breakpoints
     * @param values lookup values
     * @param srcImg source image
     * @param destType expected destination image data type
     */
    private <T extends Number & Comparable<? super T>, 
             U extends Number & Comparable<? super U>>
            void assertLookup(
                    T[] breaks, U[] values, 
                    RenderedImage srcImg,
                    ImageDataType destType) {
        
        RangeLookupTable<T, U> table = createTableFromBreaks(breaks, values);
        RenderedOp destImg = doOp(srcImg, table);

        // check data type
        assertEquals(destType.getDataBufferType(), destImg.getSampleModel().getDataType());
        assertImageValues(srcImg, table, destImg);
    }

    /**
     * Runs the operation.
     * 
     * @param srcImg the source image
     * @param table the lookup table
     * 
     * @return the destination image
     */
    private RenderedOp doOp(RenderedImage srcImg, RangeLookupTable table) {
        ParameterBlockJAI pb = new ParameterBlockJAI("RangeLookup");
        pb.setSource("source0", srcImg);
        pb.setParameter("table", table);
        return JAI.create("RangeLookup", pb);
    }

    /**
     * Creates a TYPE_BYTE test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createByteTestImage(byte startVal) {
        return createTestImage(startVal, ImageDataType.BYTE, WIDTH, WIDTH);
    }

    /**
     * Creates a TYPE_SHORT test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createShortTestImage(short startVal) {
        return createTestImage(startVal, ImageDataType.SHORT, WIDTH, WIDTH);
    }
    
    /**
     * Creates a TYPE_USHORT test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createUShortTestImage(short startVal) {
        if (startVal < 0) {
            throw new IllegalArgumentException("startVal must be >= 0");
        }
        
        RenderedImage img = createTestImage(startVal, ImageDataType.SHORT, WIDTH, WIDTH);
        ParameterBlockJAI pb = new ParameterBlockJAI("format");
        pb.setSource("source0", img);
        pb.setParameter("dataType", ImageDataType.USHORT.getDataBufferType());
        return JAI.create("format", pb);
    }
    
    /**
     * Creates a TYPE_INT test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createIntTestImage(int startVal) {
        return createTestImage(startVal, ImageDataType.INT, WIDTH, WIDTH);
    }
    
    /**
     * Creates a TYPE_FLOAT test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createFloatTestImage(float startVal) {
        return createTestImage(startVal, ImageDataType.FLOAT, WIDTH, WIDTH);
    }
    
    /**
     * Creates a TYPE_DOUBLE test image with sequential values.
     * 
     * @param startVal min image value
     * 
     * @return the image
     */
    private RenderedImage createDoubleTestImage(double startVal) {
        return createTestImage(startVal, ImageDataType.DOUBLE, WIDTH, WIDTH);
    }
    
    
    /**
     * Tests that a destination image contains expected values given the
     * source image and lookup table.
     * 
     * @param srcImg source image
     * @param table lookup table
     * @param destImg destination image
     */
    private void assertImageValues(RenderedImage srcImg, RangeLookupTable table, 
            RenderedImage destImg) {
        
        final ImageDataType srcType = ImageDataType.getForDataBufferType(
                srcImg.getSampleModel().getDataType());
        
        final ImageDataType destType = ImageDataType.getForDataBufferType(
                destImg.getSampleModel().getDataType());
        
        RectIter srcIter = RectIterFactory.create(srcImg, null);
        RectIter destIter = RectIterFactory.create(destImg, null);

        do {
            do {
                Number srcVal = getSourceImageValue(srcIter, srcType);
                Number expectedVal = table.getLookupItem(srcVal).getValue();
                
                switch (destType) {
                    case BYTE:
                        assertEquals(0, NumberOperations.compare(expectedVal, (byte) destIter.getSample()));
                        break;
                        
                    case SHORT:
                        assertEquals(0, NumberOperations.compare(expectedVal, (short) destIter.getSample()));
                        break;
                        
                    case INT:
                        assertEquals(0, NumberOperations.compare(expectedVal, destIter.getSample()));
                        break;
                        
                    case FLOAT:
                        assertEquals(0, NumberOperations.compare(expectedVal, destIter.getSampleFloat()));
                        break;
                        
                    case DOUBLE:
                        assertEquals(0, NumberOperations.compare(expectedVal, destIter.getSampleDouble()));
                        break;
                }

                srcIter.nextPixelDone();
                
            } while (!destIter.nextPixelDone());
            
            srcIter.nextLineDone();
            srcIter.startPixels();
            destIter.startPixels();

        } while (!destIter.nextLineDone());
    }

    /**
     * Helper method for {@link #assertImageValues}.
     * 
     * @param srcIter source image iterator
     * @param srcType source image data type
     * 
     * @return source image value as a Number
     */
    private Number getSourceImageValue(RectIter srcIter, ImageDataType srcType) {
        Number val = null;
        switch (srcType) {
            case BYTE:
                val = (byte) (srcIter.getSample() & 0xff);
                break;
                
            case SHORT:
                val = (short) srcIter.getSample();
                break;
                
            case USHORT:
                val = (short) (srcIter.getSample() & 0xffff);
                break;
                
            case INT:
                val = srcIter.getSample();
                break;
                
            case FLOAT:
                val = srcIter.getSampleFloat();
                break;
                
            case DOUBLE:
                val = (short) srcIter.getSampleDouble();
                break;
                
            default:
                throw new IllegalArgumentException("Unknown image type");
        }
        return val;
    }

}
