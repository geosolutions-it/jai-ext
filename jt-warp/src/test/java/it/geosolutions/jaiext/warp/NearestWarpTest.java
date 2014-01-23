package it.geosolutions.jaiext.warp;

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.TiledImage;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class NearestWarpTest extends TestWarp{

    private final static double ANGLE_ROTATION = 45d;
    private final static Warp WARP_OBJ = new WarpAffine(AffineTransform.getRotateInstance(Math.toRadians(ANGLE_ROTATION)));
    private static RenderedImage[] images;
    private static byte noDataValueB;
    private static InterpolationType interpType;
    private static short noDataValueU;
    private static short noDataValueS;
    private static int noDataValueI;
    private static float noDataValueF;
    private static double noDataValueD; 
    


    public static void setup(){
        // Definition of the input data types
        
        noDataValueB = 55;
        noDataValueU = 55;
        noDataValueS = 55;
        noDataValueI = 55;
        noDataValueF = 55;
        noDataValueD = 55;
        
        // Creation of the images
        images[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueB, false);
        images[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueU, false);
        images[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueS, false);
        images[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueI, false);
        images[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueF, false);
        images[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValueD, false);
    
        // Interpolation type
        interpType = InterpolationType.NEAREST_INTERP;
        
    }
    
    
    @Test
    @Ignore
    public void testImage(){
        boolean roiUsed = false;
        boolean noDataUsed = false;
        TestSelection testSelect = TestSelection.NO_ROI_ONLY_DATA;
        
        testWarp(images[0], noDataUsed, roiUsed, WARP_OBJ, noDataValueB, interpType, testSelect, null);
        testWarp(images[1], noDataUsed, roiUsed, WARP_OBJ, noDataValueU, interpType, testSelect, null);
        testWarp(images[2], noDataUsed, roiUsed, WARP_OBJ, noDataValueS, interpType, testSelect, null);
        testWarp(images[3], noDataUsed, roiUsed, WARP_OBJ, noDataValueI, interpType, testSelect, null);
        testWarp(images[4], noDataUsed, roiUsed, WARP_OBJ, noDataValueF, interpType, testSelect, null);
        testWarp(images[5], noDataUsed, roiUsed, WARP_OBJ, noDataValueD, interpType, testSelect, null);
    }
    
    @Test
    @Ignore
    public void testImageROI(){
        boolean roiUsed = true;
        boolean noDataUsed = false;
        TestSelection testSelect = TestSelection.ROI_ONLY_DATA;
        
        testWarp(images[0], noDataUsed, roiUsed, WARP_OBJ, noDataValueB, interpType, testSelect, null);
        testWarp(images[1], noDataUsed, roiUsed, WARP_OBJ, noDataValueU, interpType, testSelect, null);
        testWarp(images[2], noDataUsed, roiUsed, WARP_OBJ, noDataValueS, interpType, testSelect, null);
        testWarp(images[3], noDataUsed, roiUsed, WARP_OBJ, noDataValueI, interpType, testSelect, null);
        testWarp(images[4], noDataUsed, roiUsed, WARP_OBJ, noDataValueF, interpType, testSelect, null);
        testWarp(images[5], noDataUsed, roiUsed, WARP_OBJ, noDataValueD, interpType, testSelect, null);
    }
    
    @Test
    @Ignore
    public void testImageNoData(){
        boolean roiUsed = false;
        boolean noDataUsed = true;
        TestSelection testSelect = TestSelection.NO_ROI_NO_DATA;
        
        testWarp(images[0], noDataUsed, roiUsed, WARP_OBJ, noDataValueB, interpType, testSelect, null);
        testWarp(images[1], noDataUsed, roiUsed, WARP_OBJ, noDataValueU, interpType, testSelect, null);
        testWarp(images[2], noDataUsed, roiUsed, WARP_OBJ, noDataValueS, interpType, testSelect, null);
        testWarp(images[3], noDataUsed, roiUsed, WARP_OBJ, noDataValueI, interpType, testSelect, null);
        testWarp(images[4], noDataUsed, roiUsed, WARP_OBJ, noDataValueF, interpType, testSelect, null);
        testWarp(images[5], noDataUsed, roiUsed, WARP_OBJ, noDataValueD, interpType, testSelect, null);
    }
    
    @Test
    @Ignore
    public void testImageNoDataROI(){
        boolean roiUsed = true;
        boolean noDataUsed = true;
        TestSelection testSelect = TestSelection.ROI_NO_DATA;
        
        testWarp(images[0], noDataUsed, roiUsed, WARP_OBJ, noDataValueB, interpType, testSelect, null);
        testWarp(images[1], noDataUsed, roiUsed, WARP_OBJ, noDataValueU, interpType, testSelect, null);
        testWarp(images[2], noDataUsed, roiUsed, WARP_OBJ, noDataValueS, interpType, testSelect, null);
        testWarp(images[3], noDataUsed, roiUsed, WARP_OBJ, noDataValueI, interpType, testSelect, null);
        testWarp(images[4], noDataUsed, roiUsed, WARP_OBJ, noDataValueF, interpType, testSelect, null);
        testWarp(images[5], noDataUsed, roiUsed, WARP_OBJ, noDataValueD, interpType, testSelect, null);
    }
    
    
    @AfterClass
    @Ignore
    public static void finalStuff(){
     // Creation of the images
        if(images!=null){
            ((TiledImage)images[0]).dispose();
            ((TiledImage)images[1]).dispose();
            ((TiledImage)images[2]).dispose();
            ((TiledImage)images[3]).dispose();
            ((TiledImage)images[4]).dispose();
            ((TiledImage)images[5]).dispose();
        }
    }
}
