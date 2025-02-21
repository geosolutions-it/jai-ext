package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleIndirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractIndirectRuntime {
    double v_MaxIter = Double.NaN;
    double v_MinRe = Double.NaN;
    double v_MaxRe = Double.NaN;
    double v_MinIm = Double.NaN;
    double v_MaxIm = Double.NaN;
    double v_Re_scale = Double.NaN;
    double v_Im_scale = Double.NaN;

    public JiffleIndirectRuntimeImpl() {
        super(new String[] {"MaxIter", "MinRe", "MaxRe", "MinIm", "MaxIm", "Re_scale", "Im_scale"});
    }

    protected void initImageScopeVars() {
        if (Double.isNaN(v_MaxIter)) {
            v_MaxIter = 30.0;
        }
        if (Double.isNaN(v_MinRe)) {
            v_MinRe = -2.0;
        }
        if (Double.isNaN(v_MaxRe)) {
            v_MaxRe = 1.0;
        }
        if (Double.isNaN(v_MinIm)) {
            v_MinIm = -1.2;
        }
        if (Double.isNaN(v_MaxIm)) {
            v_MaxIm = v_MinIm + (v_MaxRe - v_MinRe) * getHeight() / getWidth();
        }
        if (Double.isNaN(v_Re_scale)) {
            v_Re_scale = (v_MaxRe - v_MinRe) / (getWidth() - 1.0);
        }
        if (Double.isNaN(v_Im_scale)) {
            v_Im_scale = (v_MaxIm - v_MinIm) / (getHeight() - 1.0);
        }
        _imageScopeVarsInitialized = true;
    }

    public void evaluate(double _x, double _y, double[] result) {
        if (!isWorldSet()) {
            setDefaultBounds();
        }
        if (!_imageScopeVarsInitialized) {
            initImageScopeVars();
        }
        _stk.clear();
        _iterations = 0;

        double v_c_im = v_MaxIm - _y * v_Im_scale;
        double v_c_re = v_MinRe + _x * v_Re_scale;
        double v_Z_re = v_c_re;
        double v_Z_im = v_c_im;
        double v_outside = 0.0;
        double v_n = 0.0;
        while (!_FN.isTrue(_FN.GE(v_n, v_MaxIter))) {
            checkLoopIterations();
            double v_Z_re2 = v_Z_re * v_Z_re;
            double v_Z_im2 = v_Z_im * v_Z_im;
            v_outside = _FN.GT(v_Z_re2 + v_Z_im2, 4);
            if (_FN.isTrue(v_outside)) break;
            v_Z_im = 2.0 * v_Z_re * v_Z_im + v_c_im;
            v_Z_re = v_Z_re2 - v_Z_im2 + v_c_re;
            v_n++;
        }
        result[0] = v_outside;
    }
}
