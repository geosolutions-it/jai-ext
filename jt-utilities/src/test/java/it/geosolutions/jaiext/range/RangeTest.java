package it.geosolutions.jaiext.range;

import static org.junit.Assert.*;

import java.awt.image.DataBuffer;

import org.apache.commons.lang.math.DoubleRange;
import org.apache.commons.lang.math.FloatRange;
import org.apache.commons.lang.math.IntRange;
import org.geotools.util.NumberRange;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test-class is used for evaluating the functionalities of the {@link Range} class and its subclasses. Also this class is compared to other
 * Range classes for seeing if its <code>contain()</code> method could have a better performance than that of the other Range classes.
 * 
 * Guava Ranges are commented in order to wait to upgrade the Guava version to 14.0.1
 */
public class RangeTest {

    /** Number of benchmark iterations (Default 1) */
    private final static int BENCHMARK_ITERATION = Integer.getInteger("JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Number of not benchmark iterations (Default 0) */
    private final static boolean SINGLE_POINT = Boolean.getBoolean("JAI.Ext.SinglePoint");
    
    private final static Integer TEST_SELECTOR = Integer.getInteger("JAI.Ext.TestSelector",0);

    /** test values byte */
    private static byte[] arrayB;

    /** test values ushort */
    private static short[] arrayUS;

    /** test values short */
    private static short[] arrayS;

    /** test values integer */
    private static int[] arrayI;

    /** test values float */
    private static float[] arrayF;

    /** test values doble */
    private static double[] arrayD;
    
    /** test values long */
    private static long[] arrayL;

    /** test values byte speed comparison */
    private static Byte[] arrayBtest;

    /** test values short speed comparison */
    private static Short[] arrayStest;

    /** test values integer speed comparison */
    private static Integer[] arrayItest;

    /** test values float speed comparison */
    private static Float[] arrayFtest;

    /** test values doble speed comparison */
    private static Double[] arrayDtest;

    /** Range byte 2 bounds */
    private static Range rangeB2bounds;

    /** Range byte 1 point */
    private static Range rangeBpoint;

    /** Range ushort 2 bounds */
    private static Range rangeU2bounds;

    /** Range ushort 1 point */
    private static Range rangeUpoint;

    /** Range short 2 bounds */
    private static Range rangeS2bounds;

    /** Range short 1 point */
    private static Range rangeSpoint;

    /** Range int 2 bounds */
    private static Range rangeI2bounds;

    /** Range int 1 point */
    private static Range rangeIpoint;

    /** Range float 2 bounds */
    private static Range rangeF2bounds;

    /** Range float 1 point */
    private static Range rangeFpoint;

    /** Range double 2 bounds */
    private static Range rangeD2bounds;

    /** Range double 1 point */
    private static Range rangeDpoint;
    
    /** Range long 2 bounds */
    private static Range rangeL2bounds;

    /** Range long 1 point */
    private static Range rangeLpoint;

    private static org.jaitools.numeric.Range<Byte> rangeJTB;

    private static org.jaitools.numeric.Range<Short> rangeJTS;

    private static org.jaitools.numeric.Range<Integer> rangeJTI;

    private static org.jaitools.numeric.Range<Float> rangeJTF;

    private static org.jaitools.numeric.Range<Double> rangeJTD;

    private static org.jaitools.numeric.Range<Byte> rangeJTBpoint;

    private static org.jaitools.numeric.Range<Short> rangeJTSpoint;

    private static org.jaitools.numeric.Range<Integer> rangeJTIpoint;

    private static org.jaitools.numeric.Range<Float> rangeJTFpoint;

    private static org.jaitools.numeric.Range<Double> rangeJTDpoint;

    private static javax.media.jai.util.Range rangeJAIB;

    private static javax.media.jai.util.Range rangeJAIS;

    private static javax.media.jai.util.Range rangeJAII;

    private static javax.media.jai.util.Range rangeJAIF;

    private static javax.media.jai.util.Range rangeJAID;

    private static javax.media.jai.util.Range rangeJAIBpoint;

    private static javax.media.jai.util.Range rangeJAISpoint;

    private static javax.media.jai.util.Range rangeJAIIpoint;

    private static javax.media.jai.util.Range rangeJAIFpoint;

    private static javax.media.jai.util.Range rangeJAIDpoint;

    private static IntRange rangeCommonsB;

    private static IntRange rangeCommonsS;

    private static IntRange rangeCommonsI;

    private static FloatRange rangeCommonsF;

    private static DoubleRange rangeCommonsD;

    private static IntRange rangeCommonsBpoint;

    private static IntRange rangeCommonsSpoint;

    private static IntRange rangeCommonsIpoint;

    private static FloatRange rangeCommonsFpoint;

    private static DoubleRange rangeCommonsDpoint;

    private static NumberRange<Byte> rangeGeoToolsB;

    private static NumberRange<Short> rangeGeoToolsS;

    private static NumberRange<Integer> rangeGeoToolsI;

    private static NumberRange<Float> rangeGeoToolsF;

    private static NumberRange<Double> rangeGeoToolsD;

    private static NumberRange<Byte> rangeGeoToolsBpoint;

    private static NumberRange<Short> rangeGeoToolsSpoint;

    private static NumberRange<Integer> rangeGeoToolsIpoint;

    private static NumberRange<Float> rangeGeoToolsFpoint;

    private static NumberRange<Double> rangeGeoToolsDpoint;

//    private static com.google.common.collect.Range<Byte> rangeGuavaB;
//
//    private static com.google.common.collect.Range<Short> rangeGuavaS;
//
//    private static com.google.common.collect.Range<Integer> rangeGuavaI;
//
//    private static com.google.common.collect.Range<Float> rangeGuavaF;
//
//    private static com.google.common.collect.Range<Double> rangeGuavaD;
//
//    private static com.google.common.collect.Range<Byte> rangeGuavaBpoint;
//
//    private static com.google.common.collect.Range<Short> rangeGuavaSpoint;
//
//    private static com.google.common.collect.Range<Integer> rangeGuavaIpoint;
//
//    private static com.google.common.collect.Range<Float> rangeGuavaFpoint;
//
//    private static com.google.common.collect.Range<Double> rangeGuavaDpoint;

    @BeforeClass
    public static void initialSetup() { 
        arrayB = new byte[] { 0, 1, 5, 50, 100 };
        arrayUS = new short[] { 0, 1, 5, 50, 100 };
        arrayS = new short[] { -10, 0, 5, 50, 100 };
        arrayI = new int[] { -10, 0, 5, 50, 100 };
        arrayF = new float[] { -10, 0, 5, 50, 100 };
        arrayD = new double[] { -10, 0, 5, 50, 100 };
        arrayL = new long[] { -10, 0, 5, 50, 100 };

        rangeB2bounds = RangeFactory.create((byte) 2, true, (byte) 60, true);
        rangeBpoint = RangeFactory.create(arrayB[2],true,arrayB[2],true);
        rangeU2bounds = RangeFactory.createU((short) 2, true, (short) 60, true);
        rangeUpoint = RangeFactory.createU(arrayUS[2],true,arrayUS[2],true);
        rangeS2bounds = RangeFactory.create((short) 1, true, (short) 60, true);
        rangeSpoint = RangeFactory.create(arrayS[2],true,arrayS[2],true);
        rangeI2bounds = RangeFactory.create(1, true, 60, true);
        rangeIpoint = RangeFactory.create(arrayI[2],true,arrayI[2],true);
        rangeF2bounds = RangeFactory.create(0.5f, true, 60.5f, true,false);
        rangeFpoint = RangeFactory.create(arrayF[2],true,arrayF[2],true,false);
        rangeD2bounds = RangeFactory.create(1.5d, true, 60.5d, true,false);
        rangeDpoint = RangeFactory.create(arrayD[2],true,arrayD[2],true,false);
        rangeL2bounds = RangeFactory.create(1L, true, 60L, true);
        rangeLpoint = RangeFactory.create(arrayL[2],true,arrayL[2],true);

        arrayBtest = new Byte[100];
        arrayStest = new Short[100];
        arrayItest = new Integer[100];
        arrayFtest = new Float[100];
        arrayDtest = new Double[100];

        // Random value creation for the various Ranges
        for (int j = 0; j < 100; j++) {
            double randomValue = Math.random();

            arrayBtest[j] = (byte) (randomValue * (Byte.MAX_VALUE - Byte.MIN_VALUE) + Byte.MIN_VALUE);
            arrayStest[j] = (short) (randomValue * (Short.MAX_VALUE - Short.MIN_VALUE) + Short.MIN_VALUE);
            arrayItest[j] = (int) (randomValue * (Integer.MAX_VALUE - Integer.MIN_VALUE) + Integer.MIN_VALUE);
            arrayFtest[j] = (float) (randomValue * (Float.MAX_VALUE - Float.MIN_VALUE) + Float.MIN_VALUE);
            arrayDtest[j] = (randomValue * (Double.MAX_VALUE - Double.MIN_VALUE) + Double.MIN_VALUE);
        }

        // JAI tools Ranges
        rangeJTB = org.jaitools.numeric.Range.create((byte) 1, true, (byte) 60, true);
        rangeJTS = org.jaitools.numeric.Range.create((short) 1, true, (short) 60, true);
        rangeJTI = org.jaitools.numeric.Range.create(1, true, 60, true);
        rangeJTF = org.jaitools.numeric.Range.create(0.5f, true, 60.5f, true);
        rangeJTD = org.jaitools.numeric.Range.create(1.5d, true, 60.5d, true);
        // 1 point Ranges
        rangeJTBpoint = org.jaitools.numeric.Range.create((byte) 5, true, (byte) 5, true);
        rangeJTSpoint = org.jaitools.numeric.Range.create((short) 5, true, (short) 5, true);
        rangeJTIpoint = org.jaitools.numeric.Range.create(5, true, 5, true);
        rangeJTFpoint = org.jaitools.numeric.Range.create(5f, true, 5f, true);
        rangeJTDpoint = org.jaitools.numeric.Range.create(5d, true, 5d, true);

        // JAI Ranges
        rangeJAIB = new javax.media.jai.util.Range(Byte.class, (byte) 1, true, (byte) 60, true);
        rangeJAIS = new javax.media.jai.util.Range(Short.class, (short) 1, true, (short) 60, true);
        rangeJAII = new javax.media.jai.util.Range(Integer.class, 1, true, 60, true);
        rangeJAIF = new javax.media.jai.util.Range(Float.class, 0.5f, true, 60.5f, true);
        rangeJAID = new javax.media.jai.util.Range(Double.class, 1.5d, true, 60.5d, true);
        // 1 point Ranges
        rangeJAIBpoint = new javax.media.jai.util.Range(Byte.class, (byte) 5, true, (byte) 5, true);
        rangeJAISpoint = new javax.media.jai.util.Range(Short.class, (short) 5, true, (short) 5,
                true);
        rangeJAIIpoint = new javax.media.jai.util.Range(Integer.class, 5, true, 5, true);
        rangeJAIFpoint = new javax.media.jai.util.Range(Float.class, 5f, true, 5f, true);
        rangeJAIDpoint = new javax.media.jai.util.Range(Double.class, 5d, true, 5d, true);

        // Apache Common Ranges
        rangeCommonsB = new org.apache.commons.lang.math.IntRange((byte) 1, (byte) 60);
        rangeCommonsS = new org.apache.commons.lang.math.IntRange((short) 1, (short) 60);
        rangeCommonsI = new org.apache.commons.lang.math.IntRange(1, 60);
        rangeCommonsF = new org.apache.commons.lang.math.FloatRange(0.5f, 60.5f);
        rangeCommonsD = new org.apache.commons.lang.math.DoubleRange(1.5d, 60.5d);
        // 1 point Ranges
        rangeCommonsBpoint = new org.apache.commons.lang.math.IntRange(5);
        rangeCommonsSpoint = new org.apache.commons.lang.math.IntRange(5);
        rangeCommonsIpoint = new org.apache.commons.lang.math.IntRange(5);
        rangeCommonsFpoint = new org.apache.commons.lang.math.FloatRange(5f);
        rangeCommonsDpoint = new org.apache.commons.lang.math.DoubleRange(5d);

        // GeoTools Ranges
        rangeGeoToolsB = new org.geotools.util.NumberRange<Byte>(Byte.class, (byte) 1, (byte) 60);
        rangeGeoToolsS = new org.geotools.util.NumberRange<Short>(Short.class, (short) 1,
                (short) 60);
        rangeGeoToolsI = new org.geotools.util.NumberRange<Integer>(Integer.class, 1, 60);
        rangeGeoToolsF = new org.geotools.util.NumberRange<Float>(Float.class, 0.5f, 60.5f);
        rangeGeoToolsD = new org.geotools.util.NumberRange<Double>(Double.class, 1.5d, 60.5d);
        // 1 point Ranges
        rangeGeoToolsBpoint = new org.geotools.util.NumberRange<Byte>(Byte.class, (byte) 5,
                (byte) 5);
        rangeGeoToolsSpoint = new org.geotools.util.NumberRange<Short>(Short.class, (short) 5,
                (short) 5);
        rangeGeoToolsIpoint = new org.geotools.util.NumberRange<Integer>(Integer.class, 5, 5);
        rangeGeoToolsFpoint = new org.geotools.util.NumberRange<Float>(Float.class, 5f, 5f);
        rangeGeoToolsDpoint = new org.geotools.util.NumberRange<Double>(Double.class, 5d, 5d);

//        // Guava Ranges
//        rangeGuavaB = com.google.common.collect.Range.closed((byte) 1, (byte) 60);
//        rangeGuavaS = com.google.common.collect.Range.closed((short) 1, (short) 60);
//        rangeGuavaI = com.google.common.collect.Range.closed(1, 60);
//        rangeGuavaF = com.google.common.collect.Range.closed(0.5f, 60.5f);
//        rangeGuavaD = com.google.common.collect.Range.closed(1.5d, 60.5d);
//        // 1 point Ranges
//        rangeGuavaBpoint = com.google.common.collect.Range.singleton((byte) 5);
//        rangeGuavaSpoint = com.google.common.collect.Range.singleton((short) 5);
//        rangeGuavaIpoint = com.google.common.collect.Range.singleton(5);
//        rangeGuavaFpoint = com.google.common.collect.Range.singleton(5f);
//        rangeGuavaDpoint = com.google.common.collect.Range.singleton(5d);
    }

    @Test
    public void testRange() {
        for (int i = 0; i < arrayB.length; i++) {
            boolean check2pointByte = rangeB2bounds.contains(arrayB[i]);
            boolean check1pointByte = rangeBpoint.contains(arrayB[i]);
            boolean check2pointUshort = rangeU2bounds.contains(arrayUS[i]);
            boolean check1pointUshort = rangeUpoint.contains(arrayUS[i]);
            boolean check2pointShort = rangeS2bounds.contains(arrayS[i]);
            boolean check1pointShort = rangeSpoint.contains(arrayS[i]);
            boolean check2pointInt = rangeI2bounds.contains(arrayI[i]);
            boolean check1pointInt = rangeIpoint.contains(arrayI[i]);
            boolean check2pointFloat = rangeF2bounds.contains(arrayF[i]);
            boolean check1pointFloat = rangeFpoint.contains(arrayF[i]);
            boolean check2pointDouble = rangeD2bounds.contains(arrayD[i]);
            boolean check1pointDouble = rangeDpoint.contains(arrayD[i]);
            boolean check2pointLong = rangeL2bounds.contains(arrayL[i]);
            boolean check1pointLong = rangeLpoint.contains(arrayL[i]);

            if (i == 2) {
                assertTrue(check1pointByte);
                assertTrue(check2pointByte);
                assertTrue(check1pointUshort);
                assertTrue(check2pointUshort);
                assertTrue(check1pointShort);
                assertTrue(check2pointShort);
                assertTrue(check1pointInt);
                assertTrue(check2pointInt);
                assertTrue(check1pointFloat);
                assertTrue(check2pointFloat);
                assertTrue(check1pointDouble);
                assertTrue(check2pointDouble);
                assertTrue(check1pointLong);
                assertTrue(check2pointLong);
            } else if (i == 3) {
                assertFalse(check1pointByte);
                assertTrue(check2pointByte);
                assertFalse(check1pointUshort);
                assertTrue(check2pointUshort);
                assertFalse(check1pointShort);
                assertTrue(check2pointShort);
                assertFalse(check1pointInt);
                assertTrue(check2pointInt);
                assertFalse(check1pointFloat);
                assertTrue(check2pointFloat);
                assertFalse(check1pointDouble);
                assertTrue(check2pointDouble);
                assertFalse(check1pointLong);
                assertTrue(check2pointLong);
            } else {
                assertFalse(check1pointByte);
                assertFalse(check2pointByte);
                assertFalse(check1pointUshort);
                assertFalse(check2pointUshort);
                assertFalse(check1pointShort);
                assertFalse(check2pointShort);
                assertFalse(check1pointInt);
                assertFalse(check2pointInt);
                assertFalse(check1pointFloat);
                assertFalse(check2pointFloat);
                assertFalse(check1pointDouble);
                assertFalse(check2pointDouble);
                assertFalse(check1pointLong);
                assertFalse(check2pointLong);
            }
        }
    }

    @Test
    public void testRangeTimeByte1or2Points() {        
        if (!SINGLE_POINT) {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testRangeTimeByte(rangeB2bounds, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_SHORT:
                testRangeTimeShort(rangeS2bounds, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_INT:
                testRangeTimeInteger(rangeI2bounds, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_FLOAT:
                testRangeTimeFloat(rangeF2bounds, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testRangeTimeDouble(rangeD2bounds, SINGLE_POINT);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testRangeTimeByte(rangeBpoint, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_SHORT:
                testRangeTimeShort(rangeSpoint, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_INT:
                testRangeTimeInteger(rangeIpoint, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_FLOAT:
                testRangeTimeFloat(rangeFpoint, SINGLE_POINT);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testRangeTimeDouble(rangeDpoint, SINGLE_POINT);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
    }

    @Test
    public void testJaiToolsRangeTimeByte1or2Points() {
        if (!SINGLE_POINT) {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testJaiToolsRangeTime(rangeJTB, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testJaiToolsRangeTime(rangeJTS, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testJaiToolsRangeTime(rangeJTI, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testJaiToolsRangeTime(rangeJTF, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testJaiToolsRangeTime(rangeJTD, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testJaiToolsRangeTime(rangeJTBpoint, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testJaiToolsRangeTime(rangeJTSpoint, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testJaiToolsRangeTime(rangeJTIpoint, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testJaiToolsRangeTime(rangeJTFpoint, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testJaiToolsRangeTime(rangeJTDpoint, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
    }
    
    @Test
    public void testJAIRangeTimeByte1or2Points() {
        if (!SINGLE_POINT) {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testJAIRangeTime(rangeJAIB, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testJAIRangeTime(rangeJAIS, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testJAIRangeTime(rangeJAII, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testJAIRangeTime(rangeJAIF, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testJAIRangeTime(rangeJAID, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testJAIRangeTime(rangeJAIBpoint, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testJAIRangeTime(rangeJAISpoint, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testJAIRangeTime(rangeJAIIpoint, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testJAIRangeTime(rangeJAIFpoint, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testJAIRangeTime(rangeJAIDpoint, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
    }

    @Test
    public void testApacheCommonRangeTimeByte1or2Points() {

        if (!SINGLE_POINT) {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testApacheCommonsRangeTime(rangeCommonsB, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testApacheCommonsRangeTime(rangeCommonsS, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testApacheCommonsRangeTime(rangeCommonsI, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testApacheCommonsRangeTime(rangeCommonsF, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testApacheCommonsRangeTime(rangeCommonsD, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testApacheCommonsRangeTime(rangeCommonsBpoint, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testApacheCommonsRangeTime(rangeCommonsSpoint, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testApacheCommonsRangeTime(rangeCommonsIpoint, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testApacheCommonsRangeTime(rangeCommonsFpoint, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testApacheCommonsRangeTime(rangeCommonsDpoint, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
    }

    @Test
    public void testGeoToolsRangeTimeByte1or2Points() {
        if (!SINGLE_POINT) {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testGeoToolsRangeTime(rangeGeoToolsB, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testGeoToolsRangeTime(rangeGeoToolsS, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testGeoToolsRangeTime(rangeGeoToolsI, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testGeoToolsRangeTime(rangeGeoToolsF, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testGeoToolsRangeTime(rangeGeoToolsD, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                testGeoToolsRangeTime(rangeGeoToolsBpoint, SINGLE_POINT,arrayBtest);
                break;
            case DataBuffer.TYPE_SHORT:
                testGeoToolsRangeTime(rangeGeoToolsSpoint, SINGLE_POINT,arrayStest);
                break;
            case DataBuffer.TYPE_INT:
                testGeoToolsRangeTime(rangeGeoToolsIpoint, SINGLE_POINT,arrayItest);
                break;
            case DataBuffer.TYPE_FLOAT:
                testGeoToolsRangeTime(rangeGeoToolsFpoint, SINGLE_POINT,arrayFtest);
                break;
            case DataBuffer.TYPE_DOUBLE:
                testGeoToolsRangeTime(rangeGeoToolsDpoint, SINGLE_POINT,arrayDtest);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
    }

//    @Test
//    public void testGuavaRangeTimeByte1or2Points() {
//
//        if (!SINGLE_POINT) {
//            switch(TEST_SELECTOR){
//            case DataBuffer.TYPE_BYTE:
//                testGuavaRangeTime(rangeGuavaB, SINGLE_POINT,arrayBtest);
//                break;
//            case DataBuffer.TYPE_SHORT:
//                testGuavaRangeTime(rangeGuavaS, SINGLE_POINT,arrayStest);
//                break;
//            case DataBuffer.TYPE_INT:
//                testGuavaRangeTime(rangeGuavaI, SINGLE_POINT,arrayItest);
//                break;
//            case DataBuffer.TYPE_FLOAT:
//                testGuavaRangeTime(rangeGuavaF, SINGLE_POINT,arrayFtest);
//                break;
//            case DataBuffer.TYPE_DOUBLE:
//                testGuavaRangeTime(rangeGuavaD, SINGLE_POINT,arrayDtest);
//                break;
//                default:
//                    throw new IllegalArgumentException("Wrong data type");
//            }
//        } else {
//            switch(TEST_SELECTOR){
//            case DataBuffer.TYPE_BYTE:
//                testGuavaRangeTime(rangeGuavaBpoint, SINGLE_POINT,arrayBtest);
//                break;
//            case DataBuffer.TYPE_SHORT:
//                testGuavaRangeTime(rangeGuavaSpoint, SINGLE_POINT,arrayStest);
//                break;
//            case DataBuffer.TYPE_INT:
//                testGuavaRangeTime(rangeGuavaIpoint, SINGLE_POINT,arrayItest);
//                break;
//            case DataBuffer.TYPE_FLOAT:
//                testGuavaRangeTime(rangeGuavaFpoint, SINGLE_POINT,arrayFtest);
//                break;
//            case DataBuffer.TYPE_DOUBLE:
//                testGuavaRangeTime(rangeGuavaDpoint, SINGLE_POINT,arrayDtest);
//                break;
//                default:
//                    throw new IllegalArgumentException("Wrong data type");
//            }
//        }
//    }
    
    
    
    
    
    public void testRangeTimeByte(Range testRange, boolean isPoint) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < arrayBtest.length; j++) {
                testRange.contains(arrayBtest[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        System.out.println("Byte data");
        // Output print
        System.out.println("\nMean value for" + description + " Range : " + meanValue + " nsec.");
    }
    
    public void testRangeTimeShort(Range testRange, boolean isPoint) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < arrayStest.length; j++) {
                testRange.contains(arrayStest[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        System.out.println("Short data");
        // Output print
        System.out.println("\nMean value for" + description + " Range : " + meanValue + " nsec.");
    }
    
    public void testRangeTimeInteger(Range testRange, boolean isPoint) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < arrayItest.length; j++) {
                testRange.contains(arrayItest[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        System.out.println("Integer data");
        // Output print
        System.out.println("\nMean value for" + description + " Range : " + meanValue + " nsec.");
    }
    
    public void testRangeTimeFloat(Range testRange, boolean isPoint) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < arrayFtest.length; j++) {
                testRange.contains(arrayFtest[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        System.out.println("Float data");
        // Output print
        System.out.println("\nMean value for" + description + " Range : " + meanValue + " nsec.");
    }
    
    public void testRangeTimeDouble(Range testRange, boolean isPoint) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < arrayDtest.length; j++) {
                testRange.contains(arrayDtest[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        System.out.println("Double data");
        // Output print
        System.out.println("\nMean value for" + description + " Range : " + meanValue + " nsec.");
    }
    
    
    

    public <T extends Number & Comparable<T>>void testJaiToolsRangeTime(org.jaitools.numeric.Range<T> testRange,
            boolean isPoint,T[] array) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < array.length; j++) {
                testRange.contains(array[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        // Output print
        System.out.println("\nMean value for" + description + " JAITools Range : " + meanValue
                + " nsec.");
    }

    public <T extends Number & Comparable<T>>void testJAIRangeTime(javax.media.jai.util.Range testRange, boolean isPoint,T[] array) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < array.length; j++) {
                testRange.contains(array[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        // Output print
        System.out.println("\nMean value for" + description + " JAI Range : " + meanValue
                + " nsec.");
    }

    public <T extends Number>void testApacheCommonsRangeTime(org.apache.commons.lang.math.Range testRange,
            boolean isPoint, T[] array) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < array.length; j++) {
                testRange.containsNumber(array[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        // Output print
        System.out.println("\nMean value for" + description + " Apache Common Range : " + meanValue
                + " nsec.");
    }

    public <T extends Number & Comparable<T>>void testGeoToolsRangeTime(org.geotools.util.Range<T> testRange, boolean isPoint, T[] array) {
        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        for (int i = 0; i < totalCycles; i++) {
            // Total calculation time
            long start = System.nanoTime();

            for (int j = 0; j < array.length; j++) {
                testRange.contains(array[j]);
            }
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }
            }
        }
        String description = "";
        if (isPoint) {
            description += " a single point";
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;
        // Output print
        System.out.println("\nMean value for" + description + " GeoTools Range : " + meanValue
                + " nsec.");
    }

//    public <T extends Number & Comparable<T>>void testGuavaRangeTime(com.google.common.collect.Range<T> testRange,
//            boolean isPoint, T[] array) {
//        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
//        // Initialization of the statistics
//        long mean = 0;
//        for (int i = 0; i < totalCycles; i++) {
//            // Total calculation time
//            long start = System.nanoTime();
//
//            for (int j = 0; j < array.length; j++) {
//                testRange.contains(array[j]);
//            }
//            long end = System.nanoTime() - start;
//
//            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
//            if (i > NOT_BENCHMARK_ITERATION - 1) {
//                if (i == NOT_BENCHMARK_ITERATION) {
//                    mean = end;
//                } else {
//                    mean = mean + end;
//                }
//            }
//        }
//        String description = "";
//        if (isPoint) {
//            description += " a single point";
//        }
//        // Mean values
//        double meanValue = mean / BENCHMARK_ITERATION;
//        // Output print
//        System.out.println("\nMean value for" + description + " Guava Range : " + meanValue
//                + " nsec.");
//    }

}
