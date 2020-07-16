package it.geosolutions.jaiext.jiffle.runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JiffleIndirectRuntimeImpl extends it.geosolutions.jaiext.jiffle.runtime.AbstractIndirectRuntime {
    SourceImage s_dtm;

    public JiffleIndirectRuntimeImpl() {
        super(new String[] {});
    }

    protected void initImageScopeVars() {
        s_dtm = (SourceImage) _images.get("dtm");
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

        double v_aData = 0.0;
        double v_bData = 0.0;
        double v_centralValue = s_dtm.read(_x, _y, 0);
        double v_nValue = s_dtm.read(_x + 0.0, _y + -1.0, 0);
        double v_sValue = s_dtm.read(_x + 0.0, _y + 1.0, 0);
        double v_wValue = s_dtm.read(_x + -1.0, _y + 0.0, 0);
        double v_eValue = s_dtm.read(_x + 1.0, _y + 0.0, 0);
        double v_nv = -9999.0;
        double v_aspect = v_nv;
        double v_PI = 3.141592653589793;
        result[0] = v_nValue;
        if (_FN.isTrue(_FN.NE(v_centralValue, v_nv))) {
            double v_sIsNovalue = _FN.EQ(v_sValue, v_nv);
            double v_nIsNovalue = _FN.EQ(v_nValue, v_nv);
            double v_wIsNovalue = _FN.EQ(v_wValue, v_nv);
            double v_eIsNovalue = _FN.EQ(v_eValue, v_nv);
            if (_FN.isTrue(_FN.AND(_FN.NOT(v_sIsNovalue), _FN.NOT(v_nIsNovalue)))) {
                v_aData = Math.atan((v_nValue - v_sValue) / (2.0 * getYRes()));
            } else {
                if (_FN.isTrue(_FN.AND(v_nIsNovalue, _FN.NOT(v_sIsNovalue)))) {
                    v_aData = Math.atan((v_centralValue - v_sValue) / (getYRes()));
                } else {
                    if (_FN.isTrue(_FN.AND(_FN.NOT(v_nIsNovalue), v_sIsNovalue))) {
                        v_aData = Math.atan((v_nValue - v_centralValue) / (getYRes()));
                    } else {
                        if (_FN.isTrue(_FN.AND(v_nIsNovalue, v_sIsNovalue))) {
                            v_aData = v_nv;
                        }
                    }
                }
            }
            if (_FN.isTrue(_FN.AND(_FN.NOT(v_wIsNovalue), _FN.NOT(v_eIsNovalue)))) {
                v_bData = Math.atan((v_wValue - v_eValue) / (2.0 * getXRes()));
            } else {
                if (_FN.isTrue(_FN.AND(v_wIsNovalue, _FN.NOT(v_eIsNovalue)))) {
                    v_bData = Math.atan((v_centralValue - v_eValue) / (getXRes()));
                } else {
                    if (_FN.isTrue(_FN.AND(_FN.NOT(v_wIsNovalue), v_eIsNovalue))) {
                        v_bData = Math.atan((v_wValue - v_centralValue) / (getXRes()));
                    } else {
                        if (_FN.isTrue(_FN.AND(v_wIsNovalue, v_eIsNovalue))) {
                            v_bData = v_nv;
                        }
                    }
                }
            }
            if (_FN.isTrue(_FN.AND(_FN.LT(v_aData, 0), _FN.GT(v_bData, 0)))) {
                double v_delta = Math.acos(Math.sin(Math.abs(v_aData)) * Math.cos(Math.abs(v_bData)) / (Math.sqrt(1.0 - Math.pow(Math.cos(v_aData), 2.0) * Math.pow(Math.cos(v_bData), 2.0))));
                v_aspect = _FN.radToDeg(v_delta);
            } else {
                if (_FN.isTrue(_FN.AND(_FN.GT(v_aData, 0), _FN.GT(v_bData, 0)))) {
                    double v_delta = Math.acos(Math.sin(Math.abs(v_aData)) * Math.cos(Math.abs(v_bData)) / (Math.sqrt(1.0 - Math.pow(Math.cos(v_aData), 2.0) * Math.pow(Math.cos(v_bData), 2.0))));
                    v_aspect = _FN.radToDeg(v_PI - v_delta);
                } else {
                    if (_FN.isTrue(_FN.AND(_FN.GT(v_aData, 0), _FN.LT(v_bData, 0)))) {
                        double v_delta = Math.acos(Math.sin(Math.abs(v_aData)) * Math.cos(Math.abs(v_bData)) / (Math.sqrt(1.0 - Math.pow(Math.cos(v_aData), 2.0) * Math.pow(Math.cos(v_bData), 2.0))));
                        v_aspect = _FN.radToDeg(v_PI + v_delta);
                    } else {
                        if (_FN.isTrue(_FN.AND(_FN.LT(v_aData, 0), _FN.LT(v_bData, 0)))) {
                            double v_delta = Math.acos(Math.sin(Math.abs(v_aData)) * Math.cos(Math.abs(v_bData)) / (Math.sqrt(1.0 - Math.pow(Math.cos(v_aData), 2.0) * Math.pow(Math.cos(v_bData), 2.0))));
                            v_aspect = _FN.radToDeg(2.0 * v_PI - v_delta);
                        } else {
                            if (_FN.isTrue(_FN.AND(_FN.EQ(v_aData, 0), _FN.GT(v_bData, 0)))) {
                                v_aspect = _FN.radToDeg(v_PI / 2.0);
                            } else {
                                if (_FN.isTrue(_FN.AND(_FN.EQ(v_aData, 0), _FN.LT(v_bData, 0)))) {
                                    v_aspect = _FN.radToDeg(v_PI * 3.0 / 2.0);
                                } else {
                                    if (_FN.isTrue(_FN.AND(_FN.GT(v_aData, 0), _FN.EQ(v_bData, 0)))) {
                                        v_aspect = _FN.radToDeg(v_PI);
                                    } else {
                                        if (_FN.isTrue(_FN.AND(_FN.LT(v_aData, 0), _FN.EQ(v_bData, 0)))) {
                                            v_aspect = _FN.radToDeg(2.0 * v_PI);
                                        } else {
                                            if (_FN.isTrue(_FN.AND(_FN.EQ(v_aData, 0), _FN.EQ(v_bData, 0)))) {
                                                v_aspect = 0.0;
                                            } else {
                                                if (_FN.isTrue(_FN.OR(_FN.EQ(v_aData, v_nv), _FN.EQ(v_bData, v_nv)))) {
                                                    v_aspect = v_nv;
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
            result[0] = Math.round(v_aspect);
        } else {
            result[0] = v_nv;
        }
    }
}
