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
package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.ROI;

/**
 * This class is used for wrapping {@link ColorSpaceJAI} instances into
 * {@link ColorSpaceJAIExt} ones.
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class ColorSpaceJAIExtWrapper extends ColorSpaceJAIExt {

	/** Input ColorSpace Provided */
	private ColorSpaceJAI cs;

	/** Input Colorspace JAIEXt used if the input colorspace is JaiEXT */
	private ColorSpaceJAIExt csJE;

	/**
	 * Boolean used for checking if the input ColorSpace is a
	 * {@link ColorSpaceJAIExt} instance
	 */
	boolean isJAIExt = false;

	protected ColorSpaceJAIExtWrapper(ColorSpaceJAI cs) {
		super(cs.getType(), cs.getNumComponents(), cs
				.isRGBPreferredIntermediary());
		this.cs = cs;
		if (cs instanceof ColorSpaceJAIExt) {
			isJAIExt = true;
			csJE = (ColorSpaceJAIExt) cs;
		}
	}

	@Override
	public WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize,
			WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata,
			float[] destNodata) {
		if (isJAIExt) {
			return csJE.fromCIEXYZ(src, srcComponentSize, dest,
					dstComponentSize, roi, nodata, destNodata);
		}
		return cs.fromCIEXYZ(src, srcComponentSize, dest, dstComponentSize);
	}

	@Override
	public WritableRaster fromRGB(Raster src, int[] srcComponentSize,
			WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata,
			float[] destNodata) {
		if (isJAIExt) {
			return csJE.fromRGB(src, srcComponentSize, dest, dstComponentSize,
					roi, nodata, destNodata);
		}
		return cs.fromRGB(src, srcComponentSize, dest, dstComponentSize);
	}

	@Override
	public WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize,
			WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata,
			float[] destNodata) {
		if (isJAIExt) {
			return csJE.toCIEXYZ(src, srcComponentSize, dest, dstComponentSize,
					roi, nodata, destNodata);
		}
		return cs.toCIEXYZ(src, srcComponentSize, dest, dstComponentSize);
	}

	@Override
	public WritableRaster toRGB(Raster src, int[] srcComponentSize,
			WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata,
			float[] destNodata) {
		if (isJAIExt) {
			return csJE.toRGB(src, srcComponentSize, dest, dstComponentSize,
					roi, nodata, destNodata);
		}
		return cs.toRGB(src, srcComponentSize, dest, dstComponentSize);
	}

	@Override
	public float[] fromCIEXYZ(float[] src) {
		return cs.fromCIEXYZ(src);
	}

	@Override
	public float[] fromRGB(float[] src) {
		return cs.fromRGB(src);
	}

	@Override
	public float[] toCIEXYZ(float[] src) {
		return cs.toCIEXYZ(src);
	}

	@Override
	public float[] toRGB(float[] src) {
		return cs.toRGB(src);
	}
}
