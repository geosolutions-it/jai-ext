package it.geosolutions.jaiext.nullop;


import static org.junit.Assert.*;
import it.geosolutions.jaiext.testclasses.TestBase;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

public class NullOpTest extends TestBase {
    
    private final static double TOLERANCE = 0.1d;

    private static final int IMAGE_NUMBER = 6;

    private static RenderedImage[] testImage;

    @BeforeClass
    public static void initialSetup() {

        testImage = new RenderedImage[IMAGE_NUMBER];

        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        IMAGE_FILLER = true;
        
        testImage[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 1);
        testImage[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1);
        testImage[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1);
        testImage[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 1);
        testImage[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 1);
        testImage[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 1);
        
        IMAGE_FILLER = false;
    }

    
    @Test
    public void testNullOperation(){        
        singleNullOpTest(testImage[DataBuffer.TYPE_BYTE]);
        singleNullOpTest(testImage[DataBuffer.TYPE_USHORT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_SHORT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_INT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_FLOAT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_DOUBLE]);
    }
    
    
    private void singleNullOpTest(RenderedImage source){
        
        
        
        RenderedOp nullImage = NullDescriptor.create(source, null);
        
        
        int minTileX = nullImage.getMinTileX();
        int minTileY = nullImage.getMinTileY();

        Raster upperLeftTile = nullImage.getTile(minTileX, minTileY);
        
        Raster upperLeftTileOld = source.getTile(minTileX, minTileY);

        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;
        
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                
                double value = upperLeftTile.getSampleDouble(x, y, 0);
                
                double valueOld = upperLeftTileOld.getSampleDouble(x, y, 0);
                
                assertEquals(value, valueOld, TOLERANCE);
            }
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
