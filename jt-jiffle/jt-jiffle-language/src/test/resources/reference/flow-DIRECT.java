package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleDirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime {
    SourceImage s_dtm;
    DestinationImage d_result;

    public JiffleDirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initImageScopeVars() {
        s_dtm = (SourceImage) _images.get("dtm");
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

        double v_minValue = 9999999.0;
        double v_minCol = 0.0;
        double v_minRow = 0.0;
        List v_delta = new ArrayList(Arrays.asList(-1.0, 0.0, 1.0));
        double v_stop = 0.0;
        final int _lody = (int) (-1);
        final int _hi_lody = (int) (1);
        for(int v_dy = _lody; v_dy <= _hi_lody; v_dy++) {
            final int _lodx = (int) (-1);
            final int _hi_lodx = (int) (1);
            for(int v_dx = _lodx; v_dx <= _hi_lodx; v_dx++) {
                double v_neighValue = s_dtm.read(_x + v_dx, _y + v_dy, 0);
                if (_FN.isTrue(_FN.isnull(v_neighValue))) {
                    v_stop = 1.0;
                } else {
                    if (_FN.isTrue(_FN.AND(_FN.NOT(v_stop), _FN.LT(v_neighValue, v_minValue)))) {
                        v_minValue = v_neighValue;
                        v_minCol = v_dx;
                        v_minRow = v_dy;
                    }
                }
            }
        }
        if (_FN.isTrue(v_stop)) {
            d_result.write(_x, _y, 0, -9999.0);
        } else {
            if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 0), _FN.EQ(v_minRow, 0)))) {
                d_result.write(_x, _y, 0, 0);
            } else {
                if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 1), _FN.EQ(v_minRow, 0)))) {
                    d_result.write(_x, _y, 0, 1);
                } else {
                    if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 1), _FN.EQ(v_minRow, -1)))) {
                        d_result.write(_x, _y, 0, 2);
                    } else {
                        if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 0), _FN.EQ(v_minRow, -1)))) {
                            d_result.write(_x, _y, 0, 3);
                        } else {
                            if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, -1), _FN.EQ(v_minRow, -1)))) {
                                d_result.write(_x, _y, 0, 4);
                            } else {
                                if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, -1), _FN.EQ(v_minRow, 0)))) {
                                    d_result.write(_x, _y, 0, 5);
                                } else {
                                    if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, -1), _FN.EQ(v_minRow, 1)))) {
                                        d_result.write(_x, _y, 0, 6);
                                    } else {
                                        if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 0), _FN.EQ(v_minRow, 1)))) {
                                            d_result.write(_x, _y, 0, 7);
                                        } else {
                                            if (_FN.isTrue(_FN.AND(_FN.EQ(v_minCol, 1), _FN.EQ(v_minRow, 1)))) {
                                                d_result.write(_x, _y, 0, 8);
                                            } else {
                                                d_result.write(_x, _y, 0, 10);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
