/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.shadedrelief;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public enum ShadedReliefAlgorithm {
    ZEVENBERGEN_THORNE {

        @Override
        public double getX(double[] window) {
            return getZevenbergenThorneX(window);
        }

        @Override
        public double getY(double[] window) {
            return getZevenbergenThorneY(window);
        }

        @Override
        public double getFactor() {
            return getZevenbergenThorneFactor();
        }
    },
    ZEVENBERGEN_THORNE_COMBINED {
        @Override
        public double getX(double[] window) {
            return getZevenbergenThorneX(window);
        }

        @Override
        public double getY(double[] window) {
            return getZevenbergenThorneY(window);
        }

        @Override
        public double getFactor() {
            return getZevenbergenThorneFactor();
        }

        @Override
        public double refineValue(double value, double slope) {
            return combineValue(value, slope);
        }
    },

    DEFAULT,
    COMBINED {

        @Override
        public double refineValue(double value, double slope) {
            return combineValue(value, slope);
        }
    };

    public double getFactor() {
        return 8;
    }

    private static double getZevenbergenThorneFactor() {
        return 2;
    }

    private static double getZevenbergenThorneX(double[] window) {
        return (window[3] - window[5]);
    }

    private static double getZevenbergenThorneY(double[] window) {
        return (window[7] - window[1]);
    }

    public double getX(double[] window) {
        return (window[3] - window[5])
                + ((window[0] + window[3] + window[6]) - (window[2] + +window[5] + window[8]));
    }

    public double getY(double[] window) {
        return (window[7] - window[1])
                + ((window[6] + window[7] + window[8]) - (window[0] + window[1] + window[2]));
    }

    private static double combineValue(double value, double slope) {
        // combined shading
        value = Math.acos(value);

        value = 1 - (value * Math.atan(Math.sqrt(slope)) / SQUARED_PI_2);
        return value;
    }

    /**
     * Refine value.
     * Default implementation returns value as is
     */
    public double refineValue(double value, double slope) {
        return value;
    }

    /**
     * Compute the shaded Relief value
     */
    public float getValue(double[] window, ShadedReliefParameters params) {

        double x, y, aspect, square, value;
        double xNum = getX(window);
        double yNum = getY(window);

        // Computing slope
        x = xNum / params.resX;
        y = yNum / params.resY;
        square = (x * x) + (y * y);
        double slope = square * params.squaredZetaScaleFactor;

        // Computing aspect
        aspect = Math.atan2(y, x);

        // Computing shading
        value =
                (params.sineOfAltitude
                                - (params.cosinusOfaltitudeRadiansForZetaScaleFactor
                                        * Math.sqrt(square)
                                        * Math.sin(aspect - params.azimuthRadians)))
                        / Math.sqrt(1 + slope);

        value = refineValue(value, slope);
        if (value <= 0.0) {
            value = 1.0;
        } else {
            value = 1.0 + (254.0 * value);
        }

        return (float) value;
    }

    private static String DEBUG_PATH =
            System.getProperty("org.geotools.shadedrelief.debugProperties");

    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    static final double SQUARED_PI_2 = Math.PI * Math.PI / 4;

    /**
     * DataProcessor, delegated to compute shadedRelief value, depending on specified algorithm and
     * noData.
     */
    abstract static class DataProcessor {
        boolean hasNoData;
        //        Range noData;
        Double noData;
        double noDataDouble;
        ShadedReliefAlgorithm algorithm;
        ShadedReliefAlgorithm.ShadedReliefParameters params;

        public DataProcessor(
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            this.hasNoData = hasNoData;
            this.noData = noData;
            this.noDataDouble = noDataDouble;
            this.params = params;
            this.algorithm = params.algorithm;
        }

        /**
         * Get the value of the specified index from the source data array
         */
        abstract double getValue(int index);

        /**
         * Simple interpolation without any noData check
         */
        public final double interpolate(double a, double b) {
            return (2 * (a)) - (b);
        }

        /**
         * Interpolating version taking noData into account
         */
        public final double interpolateNoData(double a, double b) {
            return (hasNoData
                            && (areEquals(noData, a) || areEquals(noData, b)
                            /*noData.contains(a) || noData.contains(b)*/ ))
                    ? noDataDouble
                    : interpolate(a, b);
        }

        /**
         * Setup proper window values and return the computed value, taking noData into account
         */
        public double processWindowNoData(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                ProcessingCase processingCase) {
            processingCase.setWindowNoData(window, i, srcPixelOffset, centerScanlineOffset, this);
            if (isNoData(window[4])) {
                // Return NaN in case of noData. The caller will properly remap it to the proper
                // noDataType
                return Double.NaN;
            } else {
                for (int index = 0; index < 9; index++) {
                    if (isNoData(window[index])) {
                        window[index] = window[4];
                    }
                }
                return algorithm.getValue(window, params);
            }
        }

        /**
         * Setup proper window values and return the computed value.
         *
         * Optimized version without noData management
         */
        public double processWindow(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                ProcessingCase processingCase) {
            processingCase.setWindow(window, i, srcPixelOffset, centerScanlineOffset, this);
            return algorithm.getValue(window, params);
        }

        /**
         * Setup proper window values and return the computed value.
         *
         * Optimized version without noData management
         */
        public double processWindowRoi(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                ProcessingCase processingCase,
                boolean[] roiMask) {
            processingCase.setWindowRoi(
                    window, i, srcPixelOffset, centerScanlineOffset, this, roiMask);
            return algorithm.getValue(window, params);
        }

        /**
         * Setup proper window values and return the computed value.
         *
         * Optimized version without noData management
         */
        public double processWindowRoiNoData(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                ProcessingCase processingCase,
                boolean[] roiMask) {
            processingCase.setWindowRoiNoData(
                    window, i, srcPixelOffset, centerScanlineOffset, this, roiMask);
            if (isNoData(window[4])) {
                // Return NaN in case of noData. The caller will properly remap it to the proper
                // noDataType
                return Double.NaN;
            } else {
                for (int index = 0; index < 9; index++) {
                    if (isNoData(window[index])) {
                        window[index] = window[4];
                    }
                }
                return algorithm.getValue(window, params);
            }
        }

        private boolean isNoData(double value) {
            return (hasNoData && /*noData.contains(value)*/ areEquals(noData, value));
        }
    }

    /**
     * DataProcessor implementation using short arrays
     */
    static class DataProcessorShort extends DataProcessor {
        short[] srcDataShort;

        public DataProcessorShort(
                short[] data,
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            super(hasNoData, noData, noDataDouble, params);
            srcDataShort = data;
        }

        @Override
        double getValue(int index) {
            return srcDataShort[index];
        }
    }

    /**
     * DataProcessor implementation using int arrays
     */
    static class DataProcessorInt extends DataProcessor {
        int[] srcDataInt;

        public DataProcessorInt(
                int[] data,
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            super(hasNoData, noData, noDataDouble, params);
            srcDataInt = data;
        }

        @Override
        double getValue(int index) {
            return srcDataInt[index];
        }
    }

    /**
     * DataProcessor implementation using double arrays
     */
    static class DataProcessorDouble extends DataProcessor {
        double[] srcDataDouble;

        public DataProcessorDouble(
                double[] data,
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            super(hasNoData, noData, noDataDouble, params);
            srcDataDouble = data;
        }

        @Override
        double getValue(int index) {
            return srcDataDouble[index];
        }
    }

    /**
     * DataProcessor implementation using float arrays
     */
    static class DataProcessorFloat extends DataProcessor {
        float[] srcDataFloat;

        public DataProcessorFloat(
                float[] data,
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            super(hasNoData, noData, noDataDouble, params);
            srcDataFloat = data;
        }

        @Override
        double getValue(int index) {
            return srcDataFloat[index];
        }
    }

    /**
     * DataProcessor implementation using byte arrays
     */
    static class DataProcessorByte extends DataProcessor {
        byte[] srcDataByte;

        public DataProcessorByte(
                byte[] data,
                boolean hasNoData, /*Range*/
                Double noData,
                double noDataDouble,
                ShadedReliefAlgorithm.ShadedReliefParameters params) {
            super(hasNoData, noData, noDataDouble, params);
            srcDataByte = data;
        }

        @Override
        double getValue(int index) {
            return srcDataByte[index];
        }
    }

    /**
     * When moving across the edges of the input image we interpolate windows corners differently.
     */
    static enum ProcessingCase {
        TOP_LEFT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {

                window[3] = data.getValue(centerScanlineOffset + 1);
                window[4] = data.getValue(centerScanlineOffset + 1);
                window[5] = data.getValue(centerScanlineOffset + 2);
                window[6] = data.getValue(centerScanlineOffset * 2 + 1);
                window[7] = data.getValue(centerScanlineOffset * 2 + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + 2);
                window[0] = data.interpolate(window[3], window[6]);
                window[1] = data.interpolate(window[4], window[7]);
                window[2] = data.interpolate(window[5], window[8]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {

                window[3] = data.getValue(centerScanlineOffset + 1);
                window[4] = data.getValue(centerScanlineOffset + 1);
                window[5] = data.getValue(centerScanlineOffset + 2);
                window[6] = data.getValue(centerScanlineOffset * 2 + 1);
                window[7] = data.getValue(centerScanlineOffset * 2 + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + 2);
                window[0] = data.interpolateNoData(window[3], window[6]);
                window[1] = data.interpolateNoData(window[4], window[7]);
                window[2] = data.interpolateNoData(window[5], window[8]);
            }
        },

        TOP {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {

                window[3] = data.getValue(centerScanlineOffset + i);
                window[4] = data.getValue(centerScanlineOffset + i + 1);
                window[5] = data.getValue(centerScanlineOffset + i + 2);
                window[6] = data.getValue(centerScanlineOffset * 2 + i);
                window[7] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + i + 2);
                window[0] = data.interpolate(window[3], window[6]);
                window[1] = data.interpolate(window[4], window[7]);
                window[2] = data.interpolate(window[5], window[8]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {

                window[3] = data.getValue(centerScanlineOffset + i);
                window[4] = data.getValue(centerScanlineOffset + i + 1);
                window[5] = data.getValue(centerScanlineOffset + i + 2);
                window[6] = data.getValue(centerScanlineOffset * 2 + i);
                window[7] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + i + 2);
                window[0] = data.interpolateNoData(window[3], window[6]);
                window[1] = data.interpolateNoData(window[4], window[7]);
                window[2] = data.interpolateNoData(window[5], window[8]);
            }
        },

        TOP_RIGHT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[3] = data.getValue(centerScanlineOffset + i);
                window[4] = data.getValue(centerScanlineOffset + i + 1);
                window[5] = data.getValue(centerScanlineOffset + i + 1);
                window[6] = data.getValue(centerScanlineOffset * 2 + i);
                window[7] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[0] = data.interpolate(window[3], window[6]);
                window[1] = data.interpolate(window[4], window[7]);
                window[2] = data.interpolate(window[5], window[8]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[3] = data.getValue(centerScanlineOffset + i);
                window[4] = data.getValue(centerScanlineOffset + i + 1);
                window[5] = data.getValue(centerScanlineOffset + i + 1);
                window[6] = data.getValue(centerScanlineOffset * 2 + i);
                window[7] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(centerScanlineOffset * 2 + i + 1);
                window[0] = data.interpolateNoData(window[3], window[6]);
                window[1] = data.interpolateNoData(window[4], window[7]);
                window[2] = data.interpolateNoData(window[5], window[8]);
            }
        },

        LEFT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[1] = data.getValue(srcPixelOffset + i + 1);
                window[2] = data.getValue(srcPixelOffset + i + 2);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + i + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + i + 2);
                window[7] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + i + 2);
                window[0] = data.interpolate(window[1], window[2]);
                window[3] = data.interpolate(window[4], window[5]);
                window[6] = data.interpolate(window[7], window[8]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[1] = data.getValue(srcPixelOffset + i + 1);
                window[2] = data.getValue(srcPixelOffset + i + 2);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + i + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + i + 2);
                window[7] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + i + 1);
                window[8] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + i + 2);
                window[0] = data.interpolateNoData(window[1], window[2]);
                window[3] = data.interpolateNoData(window[4], window[5]);
                window[6] = data.interpolateNoData(window[7], window[8]);
            }
        },

        STANDARD {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 2);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 2);
                window[6] = data.getValue(srcPixelOffset + centerScanlineOffset * 2);
                window[7] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + 1);
                window[8] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + 2);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                setWindow(window, i, srcPixelOffset, centerScanlineOffset, data);
            }
        },

        RIGHT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[6] = data.getValue(srcPixelOffset + centerScanlineOffset * 2);
                window[7] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + 1);
                window[2] = data.interpolate(window[1], window[0]);
                window[5] = data.interpolate(window[4], window[3]);
                window[8] = data.interpolate(window[7], window[6]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[6] = data.getValue(srcPixelOffset + centerScanlineOffset * 2);
                window[7] = data.getValue(srcPixelOffset + centerScanlineOffset * 2 + 1);
                window[2] = data.interpolateNoData(window[1], window[0]);
                window[5] = data.interpolateNoData(window[4], window[3]);
                window[8] = data.interpolateNoData(window[7], window[6]);
            }
        },

        BOTTOM_LEFT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset + 1);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 2);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 2);
                window[6] = data.interpolate(window[3], window[0]);
                window[7] = data.interpolate(window[4], window[1]);
                window[8] = data.interpolate(window[5], window[2]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset + 1);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 2);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 2);
                window[6] = data.interpolateNoData(window[3], window[0]);
                window[7] = data.interpolateNoData(window[4], window[1]);
                window[8] = data.interpolateNoData(window[5], window[2]);
            }
        },

        BOTTOM {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 2);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 2);
                window[6] = data.interpolate(window[3], window[0]);
                window[7] = data.interpolate(window[4], window[1]);
                window[8] = data.interpolate(window[5], window[2]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 2);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 2);
                window[6] = data.interpolateNoData(window[3], window[0]);
                window[7] = data.interpolateNoData(window[4], window[1]);
                window[8] = data.interpolateNoData(window[5], window[2]);
            }
        },

        BOTTOM_RIGHT {
            @Override
            public void setWindow(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 1);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[6] = data.interpolate(window[3], window[0]);
                window[7] = data.interpolate(window[4], window[1]);
                window[8] = data.interpolate(window[5], window[2]);
            }

            @Override
            public void setWindowNoData(
                    double[] window,
                    int i,
                    int srcPixelOffset,
                    int centerScanlineOffset,
                    DataProcessor data) {
                window[0] = data.getValue(srcPixelOffset);
                window[1] = data.getValue(srcPixelOffset + 1);
                window[2] = data.getValue(srcPixelOffset + 1);
                window[3] = data.getValue(srcPixelOffset + centerScanlineOffset);
                window[4] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[5] = data.getValue(srcPixelOffset + centerScanlineOffset + 1);
                window[6] = data.interpolateNoData(window[3], window[0]);
                window[7] = data.interpolateNoData(window[4], window[1]);
                window[8] = data.interpolateNoData(window[5], window[2]);
            }
        };

        abstract void setWindow(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                DataProcessor data);

        abstract void setWindowNoData(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                DataProcessor data);

        void setWindowRoi(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                DataProcessor data,
                boolean[] roi) {
            setWindow(window, i, srcPixelOffset, centerScanlineOffset, data);
            for (int k = 0; k < 9; k++) {
                window[k] = (k == 4 || !roi[k]) ? window[4] : window[k];
            }
        };

        void setWindowRoiNoData(
                double[] window,
                int i,
                int srcPixelOffset,
                int centerScanlineOffset,
                DataProcessor data,
                boolean[] roi) {
            setWindowNoData(window, i, srcPixelOffset, centerScanlineOffset, data);
            for (int k = 0; k < 9; k++) {
                window[k] = (k == 4 || !roi[k]) ? window[4] : window[k];
            }
        };
    }

    static boolean areEquals(double a, double b) {
        return Math.abs(a - b) < DELTA;
    }

    private static double DELTA = 1E-10;

    static class ShadedReliefParameters {
        private ShadedReliefAlgorithm algorithm;

        public ShadedReliefParameters(
                double resX,
                double resY,
                double zetaFactor,
                double scale,
                double altitude,
                double azimuth,
                ShadedReliefAlgorithm algorithm) {

            if (DEBUG_PATH != null) {
                // This is not efficient but it allows to change the ShadedRelief
                // parameters at runtime by checking the specified properties file
                // (if any) at each invokation of this constructor

                File file = new File(DEBUG_PATH);
                if (file.exists() && file.canRead()) {
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        Properties prop = new Properties();
                        prop.load(fis);
                        String scaleS = prop.getProperty("scale");
                        if (scaleS != null) {
                            scale = Double.parseDouble(scaleS);
                        }
                        String altitudeS = prop.getProperty("altitude");
                        if (altitudeS != null) {
                            altitude = Double.parseDouble(altitudeS);
                        }
                        String azimuthS = prop.getProperty("azimuth");
                        if (azimuthS != null) {
                            azimuth = Double.parseDouble(azimuthS);
                        }
                        String zetaS = prop.getProperty("zetaFactor");
                        if (zetaS != null) {
                            zetaFactor = Double.parseDouble(zetaS);
                        }
                        String algorithmS = prop.getProperty("algorithm");
                        if (algorithmS != null) {
                            algorithm = ShadedReliefAlgorithm.valueOf(algorithmS);
                        }
                    } catch (Exception e) {
                        // Does Nothing: we are in debug
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                // Does nothing: We are in debug
                            }
                        }
                    }
                }
            }

            this.resY = resY;
            this.resX = resX;
            this.sineOfAltitude = Math.sin(altitude * DEGREES_TO_RADIANS);
            this.azimuthRadians = azimuth * DEGREES_TO_RADIANS;

            double zetaScaleFactor = zetaFactor / (algorithm.getFactor() * scale);
            this.zetaScaleFactor = zetaScaleFactor;
            this.cosinusOfaltitudeRadiansForZetaScaleFactor =
                    Math.cos(altitude * DEGREES_TO_RADIANS) * zetaScaleFactor;
            this.squaredZetaScaleFactor = zetaScaleFactor * zetaScaleFactor;
            this.algorithm = algorithm;
        }

        double resY;

        double resX;

        double sineOfAltitude;

        double azimuthRadians;

        double zetaScaleFactor;

        double cosinusOfaltitudeRadiansForZetaScaleFactor;

        double squaredZetaScaleFactor;
    }
}
