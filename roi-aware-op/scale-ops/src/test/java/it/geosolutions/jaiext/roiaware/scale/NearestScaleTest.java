package it.geosolutions.jaiext.roiaware.scale;

import static org.junit.Assert.assertFalse;
import org.junit.Ignore;
import org.junit.Test;

public class NearestScaleTest<T extends Number & Comparable<? super T>> extends TestBase<T> {
    
    
    
    @Test
    @Ignore
    public void testScaleRegistration() {
        // The registration test must be done
        assertFalse("This operation must be already registered", true);
    }

    @Test
    public void testImageScaling() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.NEAREST_INTERP, TestSelection.NO_ROI_ONLY_DATA);
    }

    @Test
    public void testImageScalingROIAccessor() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA);
    }

    @Test
    public void testImageScalingROIBounds() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.NEAREST_INTERP,TestSelection.ROI_ONLY_DATA);
    }
    
    @Test
    public void testImageScalingTotal() {        
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA);
    }

    
    @Test
    public void testImageScalingBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
                      
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.NEAREST_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA);
    }
}
