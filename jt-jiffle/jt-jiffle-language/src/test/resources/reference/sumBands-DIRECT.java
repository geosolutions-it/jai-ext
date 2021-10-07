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

        double v_sum = 0.0;
        double v_goodBands = 0.0;
        double v_maxBand = getBands("src") - 1.0;
        final int _lob = (int) (0);
        final int _hi_lob = (int) (v_maxBand);
        for(int v_b = _lob; v_b <= _hi_lob; v_b++) {
            double v_value = s_src.read(_x, _y, (int)(v_b));
            if (_FN.isTrue(_FN.NE(v_value, -9999.0))) {
                v_sum += v_value;
                v_goodBands += 1.0;
            }
            d_dst.write(_x, _y, (int)(v_b), v_value);
        }
        if (_FN.isTrue(_FN.EQ(v_goodBands, 0))) {
            d_dst.write(_x, _y, (int)(v_maxBand + 1.0), -9999.0);
            d_dst.write(_x, _y, (int)(v_maxBand + 2.0), -9999.0);
        } else {
            d_dst.write(_x, _y, (int)(v_maxBand + 1.0), v_sum);
            d_dst.write(_x, _y, (int)(v_maxBand + 2.0), v_sum / v_goodBands);
        }
    }
}
