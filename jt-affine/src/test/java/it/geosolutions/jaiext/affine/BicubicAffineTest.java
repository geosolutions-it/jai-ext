package it.geosolutions.jaiext.affine;

//import org.junit.Ignore;
import org.junit.Test;

public class BicubicAffineTest extends TestAffine{
    
    @Test
    public void testImageAffine() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
        boolean setDestinationNoData = true;      
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP, TestSelection.NO_ROI_ONLY_DATA);
    }

    @Test
    public void testImageAffineROIAccessor() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;
              
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA);
    }

    @Test
    public void testImageAffineROIBounds() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
        boolean setDestinationNoData = true;
              
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ONLY_DATA);
    }
    
    @Test
    public void testImageAffineTotal() {        
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;              
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA);
    }
    
    @Test
    public void testImageAffineBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;
                      
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.BICUBIC_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA);
    }
 
}
