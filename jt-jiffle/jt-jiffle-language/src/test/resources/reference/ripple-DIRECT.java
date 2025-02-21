package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    DestinationImage d_result;
    double v_xc = Double.NaN;
    double v_yc = Double.NaN;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {"xc", "yc"});
    }

    protected void initImageScopeVars() {
        d_result= (DestinationImage) _destImages.get("result");
        if (Double.isNaN(v_xc)) {
            v_xc = getWidth() / 2.0;
        }
        if (Double.isNaN(v_yc)) {
            v_yc = getHeight() / 2.0;
        }
        _imageScopeVarsInitialized = true;
    }

    public void evaluate(double _x, double _y) {
        if (!isWorldSet()) {
            setDefaultBounds();
        }
        if (!_imageScopeVarsInitialized) {
            initImageScopeVars();
        }
        _stk.clear();
        _iterations = 0;

        double v_dx = (_x - v_xc) / v_xc;
        double v_dy = (_y - v_yc) / v_yc;
        double v_d = Math.sqrt(Math.pow(v_dx, 2.0) + Math.pow(v_dy, 2.0));
        d_result.write(_x, _y, 0, Math.sin(8.0 * 3.141592653589793 * v_d));
    }
}
