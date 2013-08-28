package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.jaitools.numeric.Range;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class LookupTest extends TestBase {

    private static RenderedImage testImageByte;

    private static RenderedImage testImageUShort;

    private static RenderedImage testImageShort;

    private static RenderedImage testImageInt;

    private static LookupTableByte byteToByteTable;

    private static LookupTableByte byteToUshortTable;

    private static LookupTableByte byteToShortTable;

    private static LookupTableByte byteToIntTable;

    private static LookupTableByte byteToFloatTable;

    private static LookupTableByte byteToDoubleTable;

    private static LookupTableUShort ushortToByteTable;

    private static LookupTableUShort ushortToUshortTable;

    private static LookupTableUShort ushortToShortTable;

    private static LookupTableUShort ushortToIntTable;

    private static LookupTableUShort ushortToFloatTable;

    private static LookupTableUShort ushortToDoubleTable;

    private static LookupTableShort shortToByteTable;

    private static LookupTableShort shortToUshortTable;

    private static LookupTableShort shortToShortTable;

    private static LookupTableShort shortToIntTable;

    private static LookupTableShort shortToFloatTable;

    private static LookupTableShort shortToDoubleTable;

    private static LookupTableInt intToByteTable;

    private static LookupTableInt intToUshortTable;

    private static LookupTableInt intToShortTable;

    private static LookupTableInt intToIntTable;

    private static LookupTableInt intToFloatTable;

    private static LookupTableInt intToDoubleTable;

    private static ROIShape roi;

    private static double destinationNoDataValue;

    private static Range<Byte> rangeB;

    private static Range<Short> rangeUS;

    private static Range<Short> rangeS;

    private static Range<Integer> rangeI;

    @BeforeClass
    public static void initialSetup() {
        //Setting of an input parameter to be always false, avoiding the image to be totally filled by values
        IMAGE_FILLER = false;
        // Images initialization
        byte noDataB = (byte) 156;
        short noDataUS = 100;
        short noDataS = -100;
        int noDataI = -100;
        testImageByte = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        testImageUShort = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataUS, false);
        testImageShort = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        testImageInt = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        //Offset creation
        int byteOffset = -100;
        int ushortOffset = 0;
        int shortOffset = noDataS;
        int intOffset = noDataI;
        
        // Array Lookup creation
        int arrayLength = 201;
        int startValue=-100;
        
        byte[] dataByteB = new byte[arrayLength];
        short[] dataByteUS = new short[arrayLength];
        short[] dataByteS = new short[arrayLength];
        int[] dataByteI = new int[arrayLength];
        float[] dataByteF = new float[arrayLength];
        double[] dataByteD = new double[arrayLength];
        
        byte[] dataUShortB = new byte[arrayLength];
        short[] dataUShortUS = new short[arrayLength];
        short[] dataUShortS = new short[arrayLength];
        int[] dataUShortI = new int[arrayLength];
        float[] dataUShortF = new float[arrayLength];
        double[] dataUShortD = new double[arrayLength];
        
        byte[] dataShortB = new byte[arrayLength];
        short[] dataShortUS = new short[arrayLength];
        short[] dataShortS = new short[arrayLength];
        int[] dataShortI = new int[arrayLength];
        float[] dataShortF = new float[arrayLength];
        double[] dataShortD = new double[arrayLength];
        
        byte[] dataIntB = new byte[arrayLength];
        short[] dataIntUS = new short[arrayLength];
        short[] dataIntS = new short[arrayLength];
        int[] dataIntI = new int[arrayLength];
        float[] dataIntF = new float[arrayLength];
        double[] dataIntD = new double[arrayLength];
        
        for(int i = 0; i<arrayLength;i++){
            //byte-to-all arrays
            dataByteB[i]=0;
            dataByteUS[i]=0;
            dataByteS[i]=0;
            dataByteI[i]=0;
            dataByteF[i]=i/arrayLength;
            dataByteD[i]=i/arrayLength*2;
            
            //ushort-to-all arrays
            dataUShortB[i]=0;
            dataUShortUS[i]=0;
            dataUShortS[i]=0;
            dataUShortI[i]=0;
            dataUShortF[i]=i/arrayLength;
            dataUShortD[i]=i/arrayLength*2;
            
            //short-to-all arrays
            dataShortB[i]=0;
            dataShortUS[i]=0;
            dataShortS[i]=0;
            dataShortI[i]=0;
            dataShortF[i]=i/arrayLength;
            dataShortD[i]=i/arrayLength*2;
            
            //int-to-all arrays
            dataIntB[i]=0;
            dataIntUS[i]=0;
            dataIntS[i]=0;
            dataIntI[i]=0;
            dataIntF[i]=i/arrayLength;
            dataIntD[i]=i/arrayLength*2;
            
            int value = i + startValue;
            
            if(value == noDataI){
                //byte-to-all arrays
                dataByteB[i]=50;
                dataByteUS[i]=50;
                dataByteS[i]=50;
                dataByteI[i]=50;
                
                //short-to-all arrays
                dataShortB[i]=50;
                dataShortUS[i]=50;
                dataShortS[i]=50;
                dataShortI[i]=50;
                
              //int-to-all arrays
                dataIntB[i]=50;
                dataIntUS[i]=50;
                dataIntS[i]=50;
                dataIntI[i]=50;
            }
            
            if(i==noDataUS){
                //ushort-to-all arrays
                dataUShortB[i]=50;
                dataUShortUS[i]=50;
                dataUShortS[i]=50;
                dataUShortI[i]=50;
            }
            
        }
        
        // LookupTables creation
        byteToByteTable = new LookupTableByte(dataByteB,byteOffset);
        byteToUshortTable = new LookupTableByte(dataByteUS,byteOffset,true);
        byteToShortTable = new LookupTableByte(dataByteS,byteOffset,false);
        byteToIntTable = new LookupTableByte(dataByteI,byteOffset);
        byteToFloatTable = new LookupTableByte(dataByteF,byteOffset);
        byteToDoubleTable = new LookupTableByte(dataByteD,byteOffset);

        ushortToByteTable = new LookupTableUShort(dataUShortB,ushortOffset);
        ushortToUshortTable = new LookupTableUShort(dataUShortUS,ushortOffset,true);
        ushortToShortTable = new LookupTableUShort(dataUShortS,ushortOffset,false);
        ushortToIntTable = new LookupTableUShort(dataUShortI,ushortOffset);
        ushortToFloatTable = new LookupTableUShort(dataUShortF,ushortOffset);
        ushortToDoubleTable = new LookupTableUShort(dataUShortD,ushortOffset);

        shortToByteTable = new LookupTableShort(dataShortB,shortOffset);
        shortToUshortTable = new LookupTableShort(dataShortUS,shortOffset,true);
        shortToShortTable = new LookupTableShort(dataShortS,shortOffset,false);
        shortToIntTable = new LookupTableShort(dataShortI,shortOffset);
        shortToFloatTable = new LookupTableShort(dataShortF,shortOffset);
        shortToDoubleTable = new LookupTableShort(dataShortD,shortOffset);

        intToByteTable = new LookupTableInt(dataIntB,intOffset);
        intToUshortTable = new LookupTableInt(dataIntUS,intOffset,true);
        intToShortTable = new LookupTableInt(dataIntS,intOffset,false);
        intToIntTable = new LookupTableInt(dataIntI,intOffset);
        intToFloatTable = new LookupTableInt(dataIntF,intOffset);
        intToDoubleTable = new LookupTableInt(dataIntD,intOffset);
        // ROI creation
        Rectangle roiBounds = new Rectangle(0, 0, DEFAULT_WIDTH/4, DEFAULT_HEIGHT/4);
        roi = new ROIShape(roiBounds);
        // NoData creation
        rangeB=Range.create(noDataB, true, noDataB, true);
        rangeUS=Range.create(noDataUS, true, noDataUS, true);
        rangeS=Range.create(noDataS, true, noDataS, true);
        rangeI=Range.create(noDataI, true, noDataI, true);
        // Destination No Data
        destinationNoDataValue = 50;
    }

    
    // No ROI tested; NoData not present
    @Test
    @Ignore
    public void testByteToAllTypes(){
        boolean roiUsed = false;
        boolean noDataPresent=false;
        boolean useRoiAccessor = false;
        
        
        testOperation(testImageByte,byteToByteTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);

        testOperation(testImageByte,byteToUshortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);

        testOperation(testImageByte,byteToShortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);

        testOperation(testImageByte,byteToIntTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);

        testOperation(testImageByte,byteToFloatTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);

        testOperation(testImageByte,byteToDoubleTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_BYTE);
    }
    
    @Test
    public void testUshortToAllTypes(){
        boolean roiUsed = false;
        boolean noDataPresent=false;
        boolean useRoiAccessor = false;
        
        testOperation(testImageUShort,ushortToByteTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);

        testOperation(testImageUShort,ushortToUshortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);

        testOperation(testImageUShort,ushortToShortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);

        testOperation(testImageUShort,ushortToIntTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);

        testOperation(testImageUShort,ushortToFloatTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);

        testOperation(testImageUShort,ushortToDoubleTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_USHORT);
    }
    
    @Test
    public void testShortToAllTypes(){
        boolean roiUsed = false;
        boolean noDataPresent=false;
        boolean useRoiAccessor = false;
        
        testOperation(testImageShort,shortToByteTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);

        testOperation(testImageShort,shortToUshortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);

        testOperation(testImageShort,shortToShortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);

        testOperation(testImageShort,shortToIntTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);

        testOperation(testImageShort,shortToFloatTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);

        testOperation(testImageShort,shortToDoubleTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_SHORT);
    }
    
    @Test
    public void testIntToAllTypes(){
        boolean roiUsed = false;
        boolean noDataPresent=false;
        boolean useRoiAccessor = false;
        
        testOperation(testImageInt,intToByteTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);

        testOperation(testImageInt,intToUshortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);

        testOperation(testImageInt,intToShortTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);

        testOperation(testImageInt,intToIntTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);

        testOperation(testImageInt,intToFloatTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);

        testOperation(testImageInt,intToDoubleTable,roiUsed,noDataPresent,useRoiAccessor,DataBuffer.TYPE_INT);
    }

    public void testOperation(RenderedImage img, LookupTable table,boolean roiUsed,boolean noDataUsed,boolean useRoiAccessor, int dataTypeInput){
        
        ROI roiData = null;
        
        if(roiUsed){
            roiData = roi;
        }
        
        Range noDataRange = null;
        
        if(noDataUsed){
            switch(dataTypeInput){
            case DataBuffer.TYPE_BYTE:
                noDataRange = rangeB;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = rangeUS;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = rangeS;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = rangeI; 
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
        RenderedImage destinationIMG = LookupDescriptor.create(img, table, destinationNoDataValue, roiData, noDataRange, useRoiAccessor, null);
        
        if(INTERACTIVE && table.getDataType() == DataBuffer.TYPE_BYTE && TEST_SELECTOR == dataTypeInput &&(dataTypeInput == 0 ||dataTypeInput == 1 ||dataTypeInput == 3 )){
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            // Calculates all the image tiles
            ((PlanarImage)destinationIMG).getTiles();
        }        
    }
    
    
    
    
    // UNSUPPORTED OPERATIONS
    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }
}
