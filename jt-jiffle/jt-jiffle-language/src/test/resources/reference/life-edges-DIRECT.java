package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    SourceImage s_world;
    DestinationImage d_nextworld;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initOptionVars() {
        _outsideValueSet = true;
_outsideValue = 0;
    }
    protected void initImageScopeVars() {
        s_world = (SourceImage) _images.get("world");
        d_nextworld= (DestinationImage) _destImages.get("nextworld");
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
        double sv_world__x__y_0 = s_world.read(_x, _y, 0);

        double v_n = 0.0;
        final int _loiy = (int) (-1);
        final int _hi_loiy = (int) (1);
        for(int v_iy = _loiy; v_iy <= _hi_loiy; v_iy++) {
            checkLoopIterations();
            final int _loix = (int) (-1);
            final int _hi_loix = (int) (1);
            for(int v_ix = _loix; v_ix <= _hi_loix; v_ix++) {
                checkLoopIterations();
                v_n += s_world.read(_x + v_ix, _y + v_iy, 0);
            }
        }
        v_n -= sv_world__x__y_0;
        d_nextworld.write(_x, _y, 0, _FN.OR((_FN.EQ(v_n, 3)), (_FN.AND(sv_world__x__y_0, _FN.EQ(v_n, 2)))));
    }
}
