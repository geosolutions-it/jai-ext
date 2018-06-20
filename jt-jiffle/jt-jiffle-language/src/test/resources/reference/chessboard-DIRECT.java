package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    DestinationImage d_result;
    double v_len = Double.NaN;
    double v_square = Double.NaN;
    double v_edge_pos = Double.NaN;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {"len", "square", "edge_pos"});
    }

    protected void initImageScopeVars() {
        d_result= (DestinationImage) _destImages.get("result");
        if (Double.isNaN(v_len)) {
            v_len = (_stk.push(_FN.sign(_FN.GT(getWidth(), getHeight()))) == null ? Double.NaN : (_stk.peek() != 0 ? (getWidth()) : (getHeight())));
        }
        if (Double.isNaN(v_square)) {
            v_square = Math.floor(v_len / 8.0);
        }
        if (Double.isNaN(v_edge_pos)) {
            v_edge_pos = v_square * 8.0;
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

        double v_odd_row = _FN.EQ(Math.floor(_y / v_square) % 2.0, 1);
        double v_odd_col = _FN.EQ(Math.floor(_x / v_square) % 2.0, 1);
        double v_inside = _FN.AND(_FN.LT(_x, v_edge_pos), _FN.LT(_y, v_edge_pos));
        d_result.write(_x, _y, 0, (_stk.push(_FN.sign(v_inside)) == null ? Double.NaN : (_stk.peek() != 0 ? ((_FN.XOR(v_odd_row, v_odd_col))) : (Double.NaN))));
    }
}
