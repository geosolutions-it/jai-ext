package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    DestinationImage d_result;
    double v_w = Double.NaN;
    double v_h = Double.NaN;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {"w", "h"});
    }

    protected void initImageScopeVars() {
        d_result= (DestinationImage) _destImages.get("result");
        if (Double.isNaN(v_w)) {
            v_w = getWidth() - 1.0;
        }
        if (Double.isNaN(v_h)) {
            v_h = getHeight() - 1.0;
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

        double v_dx = 4.0 * 3.141592653589793 * (0.5 - _x / v_w);
        double v_dy = 4.0 * 3.141592653589793 * (0.5 - _y / v_h);
        d_result.write(_x, _y, 0, Math.sqrt(Math.abs(Math.cos(v_dx) + Math.cos(v_dy))));
    }
}
