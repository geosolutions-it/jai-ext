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
package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.testclasses.TestBase;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.ROIShape;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for expanding the code coverage of the jt-lookup project. This purpose is reached by testing all the LookupTable construction method, both
 * by giving exact data arrays and by giving null data arrays. If the input array is null an IllegalArgumentException is thrown.
 */
public class CoverageTest extends TestBase{
    private static final int DEFAULT_WIDTH = 256;
    private static final int DEFAULT_HEIGHT = 256;
    
    
    /** LookupTable from byte to byte */
    private static LookupTable bTobTableNoOffset;
    private static LookupTable bTobTable;
    private static LookupTable bTobTableOffset;
    private static LookupTable bTobTableOffsetArray;
    
    /** LookupTable from byte to ushort */
    private static LookupTable bTousTableNoOffset;
    private static LookupTable bTousTable;
    private static LookupTable bTousTableOffset;
    private static LookupTable bTousTableOffsetArray;
    
    
    /** LookupTable from byte to short */
    private static LookupTable bTosTableNoOffset;
    private static LookupTable bTosTable;
    private static LookupTable bTosTableOffset;
    private static LookupTable bTosTableOffsetArray;
    
    
    /** LookupTable from byte to int */
    private static LookupTable bToiTableNoOffset;
    private static LookupTable bToiTable;
    private static LookupTable bToiTableOffset;
    private static LookupTable bToiTableOffsetArray;
    
    
    /** LookupTable from byte to float */
    private static LookupTable bTofTableNoOffset;
    private static LookupTable bTofTable;
    private static LookupTable bTofTableOffset;
    private static LookupTable bTofTableOffsetArray;
    
    /** LookupTable from byte to double */
    private static LookupTable bTodTableNoOffset;
    private static LookupTable bTodTable;
    private static LookupTable bTodTableOffset;
    private static LookupTable bTodTableOffsetArray;
        
    private static ROIShape roi;
    private static RenderedImage testImageByte;
    private static double destinationNoDataValue;
    private static byte noDataB;
    private static short noDataUS;
    private static short noDataS;
    private static int noDataI;
    
 // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of an input parameter to be always false, avoiding the image to be totally filled by values
        IMAGE_FILLER = false;
        // Byte Range goes from 0 to 255
        noDataB = (byte) 156;
        noDataUS = 100;
        noDataS = -100;
        noDataI = -100;
        
        // Images initialization
        // Test images creation
        testImageByte = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);

        // ROI creation
        Rectangle roiBounds = new Rectangle(0, 0, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);
        // Destination No Data
        destinationNoDataValue = 255;
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullByteArray1D(){
        testNullData(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullByteArray2D(){
        byte[] testData = new byte[1];
        testNullData(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullByteArray2DOffset(){
        byte[] testData = new byte[1];
        testNullData(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullByteArray2DOffsetArray(){
        byte[] testData = new byte[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullData(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullData(byte[] data1D, byte[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D);
        }
   }
    
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullUShortArray1D(){
        testNullDataUShort(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullUShortArray2D(){
        short[] testData = new short[1];
        testNullDataUShort(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullUShortArray2DOffset(){
        short[] testData = new short[1];
        testNullDataUShort(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullUShortArray2DOffsetArray(){
        short[] testData = new short[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullDataUShort(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullDataUShort(short[] data1D, short[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D,true);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset,true);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray,true);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D,true);
        }
   }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullShortArray1D(){
        testNullDataUShort(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullShortArray2D(){
        short[] testData = new short[1];
        testNullDataUShort(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullShortArray2DOffset(){
        short[] testData = new short[1];
        testNullDataUShort(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullShortArray2DOffsetArray(){
        short[] testData = new short[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullDataUShort(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullDataShort(short[] data1D, short[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D,false);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset,false);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray,false);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D,false);
        }
   }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullIntArray1D(){
        testNullDataInt(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullIntArray2D(){
        int[] testData = new int[1];
        testNullDataInt(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullIntArray2DOffset(){
        int[] testData = new int[1];
        testNullDataInt(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullIntArray2DOffsetArray(){
        int[] testData = new int[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullDataInt(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullDataInt(int[] data1D, int[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D);
        }
   }
    
    
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullFloatArray1D(){
        testNullDataFloat(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullFloatArray2D(){
        float[] testData = new float[1];
        testNullDataFloat(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullFloatArray2DOffset(){
        float[] testData = new float[1];
        testNullDataFloat(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullFloatArray2DOffsetArray(){
        float[] testData = new float[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullDataFloat(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullDataFloat(float[] data1D, float[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D);
        }
   }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullDoubleArray1D(){
        testNullDataDouble(null,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullDoubleArray2D(){
        double[] testData = new double[1];
        testNullDataDouble(testData,null,0,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullDoubleArray2DOffset(){
        double[] testData = new double[1];
        testNullDataDouble(testData,null,1,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullDoubleArray2DOffsetArray(){
        double[] testData = new double[1];
        int[] offsetArray = new int[]{0,0,0};
        testNullDataDouble(testData,null,0,offsetArray);
    }
    
    
    // This test checks if an exception is thrown when null data values are passed to the table.
    public void testNullDataDouble(double[] data1D, double[][] data2D, int offset, int[] offsetArray) {
        
        if(data1D==null && data2D == null){
            LookupTable table = new LookupTable(data1D);
        }else if(data1D!=null && data2D == null && offset!=0){
            LookupTable table = new LookupTable(data2D,offset);
        }else if(data1D!=null && data2D == null && offsetArray !=null){
            LookupTable table = new LookupTable(data2D,offsetArray);
        }else if(data1D!=null && data2D == null){
            LookupTable table = new LookupTable(data2D);
        }
   }
    
    
    // This test is used for calling all the various LookupTable constructors.
    @Test
    public void testConstructors() {
        // Offset creation
        int byteOffset = 0;
        
        int[] offsetArray = new int[]{byteOffset,byteOffset,byteOffset};
        // Array Lookup creation
        int arrayLength = 201;

        byte[] dataByteB = new byte[arrayLength];
        short[] dataByteUS = new short[arrayLength];
        short[] dataByteS = new short[arrayLength];
        int[] dataByteI = new int[arrayLength];
        float[] dataByteF = new float[arrayLength];
        double[] dataByteD = new double[arrayLength];

        byte[][] dataByteB3Bands = new byte[3][arrayLength];
        short[][] dataByteUS3Bands = new short[3][arrayLength];
        short[][] dataByteS3Bands = new short[3][arrayLength];
        int[][] dataByteI3Bands = new int[3][arrayLength];
        float[][] dataByteF3Bands = new float[3][arrayLength];
        double[][] dataByteD3Bands = new double[3][arrayLength];
        
        // Array construction
        for(int b = 0; b<3;b++){
            for (int i = 0; i < arrayLength; i++) {
                // byte-to-all arrays
                dataByteB[i] = 0;
                dataByteUS[i] = 0;
                dataByteS[i] = 0;
                dataByteI[i] = 0;
                dataByteF[i] = (i * 1.0f) / arrayLength;
                dataByteD[i] = (i * 1.0d) / arrayLength * 2;

                if (i == (noDataB & 0xFF)) {
                    // byte-to-all arrays
                    dataByteB[i] = 50;
                    dataByteUS[i] = 50;
                    dataByteS[i] = 50;
                    dataByteI[i] = 50;
                }
                //3Band arrays
                // byte-to-all arrays
                dataByteB3Bands[b][i] = 0;
                dataByteUS3Bands[b][i] = 0;
                dataByteS3Bands[b][i] = 0;
                dataByteI3Bands[b][i] = 0;
                dataByteF3Bands[b][i] = (i * 1.0f) / arrayLength;
                dataByteD3Bands[b][i] = (i * 1.0d) / arrayLength * 2;

                if (i == (noDataB & 0xFF)) {
                    // byte-to-all arrays
                    dataByteB3Bands[b][i] = 50;
                    dataByteUS3Bands[b][i] = 50;
                    dataByteS3Bands[b][i] = 50;
                    dataByteI3Bands[b][i] = 50;
                }  
            } 
        }

        // LookupTables creation
        // No Offset
        bTobTableNoOffset = new LookupTable(dataByteB);
        bTousTableNoOffset = new LookupTable(dataByteUS,true);
        bTosTableNoOffset = new LookupTable(dataByteS,false);
        bToiTableNoOffset = new LookupTable(dataByteI);
        bTofTableNoOffset = new LookupTable(dataByteF);
        bTodTableNoOffset = new LookupTable(dataByteD);
        
        // No Offset with a 2-dimension array 
        bTobTable = new LookupTable(dataByteB3Bands);
        bTousTable = new LookupTable(dataByteUS3Bands,true);
        bTosTable = new LookupTable(dataByteS3Bands,false);
        bToiTable = new LookupTable(dataByteI3Bands);
        bTofTable = new LookupTable(dataByteF3Bands);
        bTodTable = new LookupTable(dataByteD3Bands);
         
        // With scalar Offset with a 2-dimension array 
        bTobTableOffset = new LookupTable(dataByteB3Bands,byteOffset);
        bTousTableOffset = new LookupTable(dataByteUS3Bands,byteOffset,true);
        bTosTableOffset = new LookupTable(dataByteS3Bands,byteOffset,false);
        bToiTableOffset = new LookupTable(dataByteI3Bands,byteOffset);
        bTofTableOffset = new LookupTable(dataByteF3Bands,byteOffset);
        bTodTableOffset = new LookupTable(dataByteD3Bands,byteOffset);
        
        // With scalar Offset with a 2-dimension array 
        bTobTableOffsetArray = new LookupTable(dataByteB3Bands,offsetArray);
        bTousTableOffsetArray = new LookupTable(dataByteUS3Bands,offsetArray,true);
        bTosTableOffsetArray = new LookupTable(dataByteS3Bands,offsetArray,false);
        bToiTableOffsetArray = new LookupTable(dataByteI3Bands,offsetArray);
        bTofTableOffsetArray = new LookupTable(dataByteF3Bands,offsetArray);
        bTodTableOffsetArray = new LookupTable(dataByteD3Bands,offsetArray);       
    }
    
}
