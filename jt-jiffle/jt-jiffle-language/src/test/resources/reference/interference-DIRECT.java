package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    DestinationImage d_result;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initImageScopeVars() {
        d_result= (DestinationImage) _destImages.get("result");
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

        double v_dx = _x / getWidth();
        double v_dy = _y / getHeight();
        double v_dxy = Math.sqrt(Math.pow((v_dx - 0.5), 2.0) + Math.pow((v_dy - 0.5), 2.0));
        d_result.write(_x, _y, 0, Math.sin(v_dx * 100.0) + Math.sin(v_dxy * 100.0));
    }
}
