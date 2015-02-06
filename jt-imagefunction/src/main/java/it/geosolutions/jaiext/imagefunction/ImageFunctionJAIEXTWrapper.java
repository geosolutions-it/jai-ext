package it.geosolutions.jaiext.imagefunction;

import java.awt.Rectangle;

import it.geosolutions.jaiext.range.Range;

import javax.media.jai.ImageFunction;
import javax.media.jai.ROI;

/**
 * Wrapper function used for wrapping {@link ImageFunction} objects
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class ImageFunctionJAIEXTWrapper implements ImageFunctionJAIEXT {

    private ImageFunction f;

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
            int countY, int element, float[] real, float[] imag, Rectangle destRect, ROI roi, Range nodata,
            float destNoData) {
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(startX, startY, deltaX, deltaY, countX, countY,
                    element, real, imag, destRect, roi, nodata, destNoData);
        } else {
            f.getElements(startX, startY, deltaX, deltaY, countX, countY, element, real, imag);
        }
    }

    public void getElements(double startX, double startY, double deltaX, double deltaY, int countX,
            int countY, int element, double[] real, double[] imag, Rectangle destRect, ROI roi, Range nodata,
            float destNoData) {
        if (isJAIExt) {
            ((ImageFunctionJAIEXT) f).getElements(startX, startY, deltaX, deltaY, countX, countY,
                    element, real, imag, destRect, roi, nodata, destNoData);
        } else {
            f.getElements(startX, startY, deltaX, deltaY, countX, countY, element, real, imag);
        }
    }

}
