package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    SourceImage s_red;
    SourceImage s_nir;
    DestinationImage d_res;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initImageScopeVars() {
        s_red = (SourceImage) _images.get("red");
        s_nir = (SourceImage) _images.get("nir");
        d_res= (DestinationImage) _destImages.get("res");
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
        double sv_nir__x__y_0 = s_nir.read(_x, _y, 0);
        double sv_red__x__y_0 = s_red.read(_x, _y, 0);

        d_res.write(_x, _y, 0, (sv_nir__x__y_0 - sv_red__x__y_0) / (sv_nir__x__y_0 + sv_red__x__y_0));
    }
}
