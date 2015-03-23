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
package it.geosolutions.jaiext.imagefunction;

import java.awt.Rectangle;

import it.geosolutions.jaiext.range.Range;

import javax.media.jai.ImageFunction;
import javax.media.jai.ROI;

/**
 * Wrapper function used for wrapping {@link ImageFunction} objects in order to implement {@link ImageFunctionJAIEXT} interface.
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class ImageFunctionJAIEXTWrapper implements ImageFunctionJAIEXT {
    /** {@link ImageFunction} object being wrapped */
    private ImageFunction f;

    /** Boolean indicating if the input {@link ImageFunction} is an instance of {@link ImageFunctionJAIEXT} */
    private boolean isJAIExt;

    public ImageFunctionJAIEXTWrapper(ImageFunction f) {
        this.f = f;
        isJAIExt = f instanceof ImageFunctionJAIEXT;
    }

    public void getElements(float arg0, float arg1, float arg2, float arg3, int arg4, int arg5,
            int arg6, float[] arg7, float[] arg8) {
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    arg8, null, null, null, 0f);
        } else {
            f.getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }

    }

    public void getElements(double arg0, double arg1, double arg2, double arg3, int arg4, int arg5,
            int arg6, double[] arg7, double[] arg8) {
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    arg8, null, null, null, 0f);
        } else {
            f.getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }
    }

    public int getNumElements() {
        return f.getNumElements();
    }

    public boolean isComplex() {
        return f.isComplex();
    }

    public void getElements(float startX, float startY, float deltaX, float deltaY, int countX,
            int countY, int element, float[] real, float[] imag, Rectangle destRect, ROI roi,
            Range nodata, float destNoData) {
        // If ImageFunctionJAIExt, take into account ROI and NoData, otherwise simply act as a normal ImageFunction instance
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(startX, startY, deltaX, deltaY, countX, countY,
                    element, real, imag, destRect, roi, nodata, destNoData);
        } else {
            f.getElements(startX, startY, deltaX, deltaY, countX, countY, element, real, imag);
        }
    }

    public void getElements(double startX, double startY, double deltaX, double deltaY, int countX,
            int countY, int element, double[] real, double[] imag, Rectangle destRect, ROI roi,
            Range nodata, float destNoData) {
        // If ImageFunctionJAIExt, take into account ROI and NoData, otherwise simply act as a normal ImageFunction instance
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(startX, startY, deltaX, deltaY, countX, countY,
                    element, real, imag, destRect, roi, nodata, destNoData);
        } else {
            f.getElements(startX, startY, deltaX, deltaY, countX, countY, element, real, imag);
        }
    }

}
