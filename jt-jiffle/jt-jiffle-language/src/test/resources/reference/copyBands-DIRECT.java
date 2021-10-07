package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    SourceImage s_src;
    DestinationImage d_dst;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initImageScopeVars() {
        s_src = (SourceImage) _images.get("src");
        d_dst= (DestinationImage) _destImages.get("dst");
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

        final int _lob = (int) (0);
        final int _hi_lob = (int) (getBands("src"));
        for(int v_b = _lob; v_b <= _hi_lob; v_b++) {
            d_dst.write(_x, _y, (int)(v_b), s_src.read(_x, _y, (int)(v_b)));
        }
    }
}
