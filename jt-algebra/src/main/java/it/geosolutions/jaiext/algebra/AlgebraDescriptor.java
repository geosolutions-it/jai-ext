package it.geosolutions.jaiext.algebra;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

public class AlgebraDescriptor extends OperationDescriptorImpl {

    public enum Operator {
        SUM(0, 0) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculate(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double getNullValue() {
                return nullValue;
            }

            @Override
            public byte calculate(byte value0, byte value1) {
                return (byte) (value0 + value1);
            }

            @Override
            public short calculate(short value0, short value1) {
                return (short) (value0 + value1);
            }

            @Override
            public int calculate(int value0, int value1) {
                return (value0 + value1);
            }

            @Override
            public float calculate(float value0, float value1) {
                return (value0 + value1);
            }

            @Override
            public double calculate(double value0, double value1) {
                return (value0 + value1);
            }

            @Override
            public long calculate(long value0, long value1) {
                return (value0 + value1);
            }
        },
        SUBTRACT(1, 0) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculate(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double getNullValue() {
                return nullValue;
            }

            @Override
            public byte calculate(byte value0, byte value1) {
                return (byte) (value0 - value1);
            }

            @Override
            public short calculate(short value0, short value1) {
                return (short) (value0 - value1);
            }

            @Override
            public int calculate(int value0, int value1) {
                return (value0 - value1);
            }

            @Override
            public float calculate(float value0, float value1) {
                return (value0 - value1);
            }

            @Override
            public double calculate(double value0, double value1) {
                return (value0 - value1);
            }

            @Override
            public long calculate(long value0, long value1) {
                return (value0 - value1);
            }

        },
        MULTIPLY(2, 1) {
            @Override
            public byte calculate(byte... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Byte.MAX_VALUE) {
                        values[0] = Byte.MAX_VALUE;
                    } else if (temp < Byte.MIN_VALUE) {
                        values[0] = Byte.MIN_VALUE;
                    } else {
                        values[0] = (byte) temp;
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Short.MAX_VALUE) {
                        values[0] = Short.MAX_VALUE;
                    } else if (temp < Short.MIN_VALUE) {
                        values[0] = Short.MIN_VALUE;
                    } else {
                        values[0] = (short) temp;
                    }
                }
                return values[0];
            }

            @Override
            public int calculate(int... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Integer.MAX_VALUE) {
                        values[0] = Integer.MAX_VALUE;
                    } else if (temp < Integer.MIN_VALUE) {
                        values[0] = Integer.MIN_VALUE;
                    } else {
                        values[0] = (int) temp;
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                double temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Float.MAX_VALUE) {
                        values[0] = Float.MAX_VALUE;
                    } else if (temp < -Float.MAX_VALUE) {
                        values[0] = -Float.MAX_VALUE;
                    } else {
                        values[0] = (float) temp;
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] *= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculate(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] *= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double getNullValue() {
                return nullValue;
            }

            @Override
            public byte calculate(byte value0, byte value1) {
                long temp = 1;
                temp = 1L * value0 * value1;
                if (temp > Byte.MAX_VALUE) {
                    temp = Byte.MAX_VALUE;
                } else if (temp < Byte.MIN_VALUE) {
                    temp = Byte.MIN_VALUE;
                }
                return (byte) (temp);
            }

            @Override
            public short calculate(short value0, short value1) {
                long temp = 1;
                temp = 1L * value0 * value1;
                if (temp > Short.MAX_VALUE) {
                    temp = Short.MAX_VALUE;
                } else if (temp < Short.MIN_VALUE) {
                    temp = Short.MIN_VALUE;
                }
                return (short) (temp);
            }

            @Override
            public int calculate(int value0, int value1) {
                long temp = 1;
                temp = 1L * value0 * value1;
                if (temp > Integer.MAX_VALUE) {
                    temp = Integer.MAX_VALUE;
                } else if (temp < Integer.MIN_VALUE) {
                    temp = Integer.MIN_VALUE;
                }
                return (int) (temp);
            }

            @Override
            public float calculate(float value0, float value1) {
                double temp = 1;
                temp = 1.0d * value0 * value1;
                if (temp > Float.MAX_VALUE) {
                    temp = Float.MAX_VALUE;
                } else if (temp < -Float.MAX_VALUE) {
                    temp = -Float.MAX_VALUE;
                }
                return (float) (temp);
            }

            @Override
            public double calculate(double value0, double value1) {
                return (value0 * value1);
            }

            @Override
            public long calculate(long value0, long value1) {
                return (value0 * value1);
            }

        },
        DIVIDE(3, 1) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Byte.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Byte.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Short.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Short.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Integer.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Integer.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] /= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] /= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculate(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Long.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Long.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public double getNullValue() {
                return nullValue;
            }

            @Override
            public byte calculate(byte value0, byte value1) {
                if (value1 == 0 && value0 >= 0) {
                    return Byte.MAX_VALUE;
                } else if (value1 == 0 && value0 < 0) {
                    return Byte.MIN_VALUE;
                } else {
                    return (byte) (value0 / value1);
                }
            }

            @Override
            public short calculate(short value0, short value1) {
                if (value1 == 0 && value0 >= 0) {
                    return Short.MAX_VALUE;
                } else if (value1 == 0 && value0 < 0) {
                    return Short.MIN_VALUE;
                } else {
                    return (short) (value0 / value1);
                }
            }

            @Override
            public int calculate(int value0, int value1) {
                if (value1 == 0 && value0 >= 0) {
                    return Integer.MAX_VALUE;
                } else if (value1 == 0 && value0 < 0) {
                    return Integer.MIN_VALUE;
                } else {
                    return (value0 / value1);
                }
            }

            @Override
            public float calculate(float value0, float value1) {
                return (value0 / value1);
            }

            @Override
            public double calculate(double value0, double value1) {
                return (value0 / value1);
            }

            @Override
            public long calculate(long value0, long value1) {
                if (value1 == 0 && value0 >= 0) {
                    return Long.MAX_VALUE;
                } else if (value1 == 0 && value0 < 0) {
                    return Long.MIN_VALUE;
                } else {
                    return (value0 / value1);
                }
            }

        };

        protected final double nullValue;

        private final int type;

        private Operator(int type, double nullValue) {
            this.nullValue = nullValue;
            this.type = type;
        }

        public abstract byte calculate(byte value0, byte value1);

        public abstract short calculate(short value0, short value1);

        public abstract int calculate(int value0, int value1);

        public abstract float calculate(float value0, float value1);

        public abstract double calculate(double value0, double value1);

        public abstract long calculate(long value0, long value1);

        public abstract byte calculate(byte... values);

        public abstract short calculate(short... values);

        public abstract int calculate(int... values);

        public abstract float calculate(float... values);

        public abstract double calculate(double... values);

        public abstract long calculate(long... values);

        public abstract double getNullValue();

        public int getType() {
            return type;
        }

    }

    public final static int OPERATION_INDEX = 0;

    public final static int ROI_INDEX = 1;

    public final static int RANGE_INDEX = 2;

    public final static int DEST_NODATA_INDEX = 3;

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "algebric" },
            { "LocalName", "algebric" },
            { "Vendor", "it.geosolutions.jaiext.roiaware" },
            { "Description",
                    "This class executes the operation selected by the user on each pixel of the source images " },
            { "DocURL", "Not Defined" }, { "Version", "1.0" },
            { "arg0Desc", "Operation to execute" }, { "arg1Desc", "ROI object used" },
            { "arg2Desc", "No Data Range used" }, { "arg3Desc", "Output value for No Data" }, };

    /**
     * Input Parameter name
     */
    private static final String[] paramNames = { "operation", "roi", "noData", "destinationNoData" };

    /**
     * Input Parameter class
     */
    private static final Class[] paramClasses = { Operator.class, javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, Double.class };

    /**
     * Input Parameter default values
     */
    private static final Object[] paramDefaults = { null, null, null, 0d };

    /** Constructor. */
    public AlgebraDescriptor() {
        super(resources, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Adds two images.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(Operator op, ROI roi, Range noData, double destinationNoData,
            RenderingHints hints, RenderedImage... sources) {

        ParameterBlockJAI pb = new ParameterBlockJAI("algebric", RenderedRegistryMode.MODE_NAME);

        int numSources = sources.length;

        for (int i = 0; i < numSources; i++) {
            RenderedImage img = sources[i];
            if (img != null) {
                pb.setSource(img, i);
            }
        }

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.create("algebric", pb, hints);
    }

    /**
     * Adds two images.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     * 
     * @param source0 <code>RenderableImage</code> source 0.
     * @param source1 <code>RenderableImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(Operator op, ROI roi, Range noData,
            double destinationNoData, RenderingHints hints, RenderableImage... sources) {

        ParameterBlockJAI pb = new ParameterBlockJAI("algebric", RenderableRegistryMode.MODE_NAME);

        int numSources = sources.length;

        for (int i = 0; i < numSources; i++) {
            RenderableImage img = sources[i];
            if (img != null) {
                pb.setSource(img, i);
            }
        }

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.createRenderable("algebric", pb, hints);
    }
}
