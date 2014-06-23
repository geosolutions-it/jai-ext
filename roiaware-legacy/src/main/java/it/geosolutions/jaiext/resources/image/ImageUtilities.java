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
package it.geosolutions.jaiext.resources.image;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

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

}
