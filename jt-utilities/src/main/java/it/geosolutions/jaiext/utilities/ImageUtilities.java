/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.geosolutions.jaiext.utilities;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ChoiceFormat;

import com.sun.media.imageioimpl.common.PackageUtil;

public class ImageUtilities {

	/**
	 * {@code true} if JAI media lib is available.
	 */
	private static final boolean mediaLibAvailable;
	static {

		// do we wrappers at hand?
		boolean mediaLib = false;
		Class mediaLibImage = null;
		try {
			mediaLibImage = Class.forName("com.sun.medialib.mlib.Image");
		} catch (ClassNotFoundException e) {
		}
		mediaLib = (mediaLibImage != null);

		// npw check if we either wanted to disable explicitly and if we
		// installed the native libs
		if (mediaLib) {

			try {
				// explicit disable
				mediaLib = !Boolean.getBoolean("com.sun.media.jai.disableMediaLib");

				// native libs installed
				if (mediaLib) {
					final Class mImage = mediaLibImage;
					mediaLib = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
								public Boolean run() {
									try {
										// get the method
										final Class params[] = {};
										Method method = mImage.getDeclaredMethod("isAvailable", params);

										// invoke
										final Object paramsObj[] = {};

										final Object o = mImage.newInstance();
										return (Boolean) method.invoke(o, paramsObj);
									} catch (Throwable e) {
										return false;
									}
								}
							});
				}
			} catch (Throwable e) {
				// Because the property com.sun.media.jai.disableMediaLib isn't
				// defined as public, the users shouldn't know it. In most of
				// the cases, it isn't defined, and thus no access permission
				// is granted to it in the policy file. When JAI is utilized in
				// a security environment, AccessControlException will be
				// thrown.
				// In this case, we suppose that the users would like to use
				// medialib accelaration. So, the medialib won't be disabled.

				// The fix of 4531501

				mediaLib = false;
			}

		}

		mediaLibAvailable = mediaLib;
	}

	/**
	 * Tells me whether or not the native libraries for JAI are active or not.
	 * 
	 * @return <code>false</code> in case the JAI native libs are not in the
	 *         path, <code>true</code> otherwise.
	 */
	public static boolean isMediaLibAvailable() {
		return mediaLibAvailable;
	}

	/**
	 * Tells me whether or not the native libraries for JAI/ImageIO are active
	 * or not.
	 * 
	 * @return <code>false</code> in case the JAI/ImageIO native libs are not in
	 *         the path, <code>true</code> otherwise.
	 */
	public static boolean isCLibAvailable() {
		return PackageUtil.isCodecLibAvailable();
	}

	       /**
         * Returns the next or previous representable number. If {@code amount} is
         * equals to {@code 0}, then this method returns the {@code value}
         * unchanged. Otherwise, The operation performed depends on the specified
         * {@code type}:
         * <ul>
         * <li>
         * <p>
         * If the {@code type} is {@link Double}, then this method is equivalent to
         * invoking {@link #previous(double)} if {@code amount} is equals to
         * {@code -1}, or invoking {@link #next(double)} if {@code amount} is equals
         * to {@code +1}. If {@code amount} is smaller than {@code -1} or greater
         * than {@code +1}, then this method invokes {@link #previous(double)} or
         * {@link #next(double)} in a loop for {@code abs(amount)} times.
         * </p>
         * </li>
         * 
         * <li>
         * <p>
         * If the {@code type} is {@link Float}, then this method is equivalent to
         * invoking {@link #previous(float)} if {@code amount} is equals to
         * {@code -1}, or invoking {@link #next(float)} if {@code amount} is equals
         * to {@code +1}. If {@code amount} is smaller than {@code -1} or greater
         * than {@code +1}, then this method invokes {@link #previous(float)} or
         * {@link #next(float)} in a loop for {@code abs(amount)} times.
         * </p>
         * </li>
         * 
         * <li>
         * <p>
         * If the {@code type} is an {@linkplain #isInteger integer}, then invoking
         * this method is equivalent to computing {@code value + amount}.
         * </p>
         * </li>
         * </ul>
         * 
         * @param type
         *            The type. Should be the class of {@link Double}, {@link Float}
         *            , {@link Long}, {@link Integer}, {@link Short} or {@link Byte}
         *            .
         * @param value
         *            The number to rool.
         * @param amount
         *            -1 to return the previous representable number, +1 to return
         *            the next representable number, or 0 to return the number with
         *            no change.
         * @return One of previous or next representable number as a {@code double}.
         * @throws IllegalArgumentException
         *             if {@code type} is not one of supported types.
         */
        public static double rool(final Class type, double value, int amount)
                        throws IllegalArgumentException {
                if (Double.class.equals(type)) {
                        if (amount < 0) {
                                do {
                                        value = previous(value);
                                } while (++amount != 0);
                        } else if (amount != 0) {
                                do {
                                        value = next(value);
                                } while (--amount != 0);
                        }
                        return value;
                }
                if (Float.class.equals(type)) {
                        float vf = (float) value;
                        if (amount < 0) {
                                do {
                                        vf = next(vf, false);
                                } while (++amount != 0);
                        } else if (amount != 0) {
                                do {
                                        vf = next(vf, true);
                                } while (--amount != 0);
                        }
                        return vf;
                }
                if (isInteger(type)) {
                        return value + amount;
                }
                throw new IllegalArgumentException("Unsupported DataType: " + type);
        }

        /**
         * Returns {@code true} if the specified {@code type} is one of integer
         * types. Integer types includes {@link Long}, {@link Integer},
         * {@link Short} and {@link Byte}.
         * 
         * @param type
         *            The type to test (may be {@code null}).
         * @return {@code true} if {@code type} is the class {@link Long},
         *         {@link Integer}, {@link Short} or {@link Byte}.
         * 
         * @deprecated Moved to {@link Classes}.
         */
        @Deprecated
        public static boolean isInteger(final Class<?> type) {
                return type != null && Long.class.equals(type)
                                || Integer.class.equals(type) || Short.class.equals(type)
                                || Byte.class.equals(type);
        }

        /**
         * Finds the least double greater than <var>f</var>. If {@code NaN}, returns
         * same value.
         * 
         * @see java.text.ChoiceFormat#nextDouble
         * 
         * @todo Remove this method when we will be allowed to use Java 6.
         */
        public static double next(final double f) {
                return ChoiceFormat.nextDouble(f);
        }

        /**
         * Finds the greatest double less than <var>f</var>. If {@code NaN}, returns
         * same value.
         * 
         * @see java.text.ChoiceFormat#previousDouble
         * 
         * @todo Remove this method when we will be allowed to use Java 6.
         */
        public static double previous(final double f) {
                return ChoiceFormat.previousDouble(f);
        }

        private static float next(final float f, final boolean positive) {
                final int SIGN = 0x80000000;
                final int POSITIVEINFINITY = 0x7F800000;

                // Filter out NaN's
                if (Float.isNaN(f)) {
                        return f;
                }

                // Zero's are also a special case
                if (f == 0f) {
                        final float smallestPositiveFloat = Float.intBitsToFloat(1);
                        return (positive) ? smallestPositiveFloat : -smallestPositiveFloat;
                }

                // If entering here, d is a nonzero value.
                // Hold all bits in a int for later use.
                final int bits = Float.floatToIntBits(f);

                // Strip off the sign bit.
                int magnitude = bits & ~SIGN;

                // If next float away from zero, increase magnitude.
                // Else decrease magnitude
                if ((bits > 0) == positive) {
                        if (magnitude != POSITIVEINFINITY) {
                                magnitude++;
                        }
                } else {
                        magnitude--;
                }

                // Restore sign bit and return.
                final int signbit = bits & SIGN;
                return Float.intBitsToFloat(magnitude | signbit);
        }
}
