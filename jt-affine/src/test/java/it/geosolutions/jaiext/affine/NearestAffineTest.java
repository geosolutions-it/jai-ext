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
package it.geosolutions.jaiext.affine;


import org.junit.Ignore;
import org.junit.Test;


/**
 * This test-class extends the TestAffine class and is used for testing the nearest interpolation inside the Affine operation.
 * The first method tests the affine operation without the presence of a ROI or a No Data Range. The 2nd method introduces a ROI 
 * object calculated using a ROI RasterAccessor while the 3rd method uses an Iterator on the ROI Object. The last method performs 
 * the affine operation with all the components. The last method is similar to the 4th method but executes its operations on binary 
 * images. In this class the last method is ignored because it has a bug that must be solved. 
 */
public class NearestAffineTest extends TestAffine {
    
    @Test
    public void testImageAffine() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
        boolean setDestinationNoData = true;
              
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.REDUCTION);
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
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.REDUCTION);
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
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ONLY_DATA,ScaleType.REDUCTION);
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
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }

    
    @Test
    @Ignore
    public void testImageAffinegBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
        boolean setDestinationNoData = true;
                      
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        
        testGlobalAffine(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,setDestinationNoData, InterpolationType.NEAREST_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }
}
