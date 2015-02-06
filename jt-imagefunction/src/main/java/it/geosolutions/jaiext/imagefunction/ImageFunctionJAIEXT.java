package it.geosolutions.jaiext.imagefunction;

import java.awt.Rectangle;

import it.geosolutions.jaiext.range.Range;

import javax.media.jai.ImageFunction;
import javax.media.jai.ROI;

public interface ImageFunctionJAIEXT extends ImageFunction {

	/**
	 * Returns all values of a given element for a specified set of coordinates.
	 * An ArrayIndexOutOfBoundsException may be thrown if the length of the
	 * supplied array(s) is insufficient.
	 * 
	 * @param startX
	 *            The X coordinate of the upper left location to evaluate.
	 * @param startY
	 *            The Y coordinate of the upper left location to evaluate.
	 * @param deltaX
	 *            The horizontal increment.
	 * @param deltaY
	 *            The vertical increment.
	 * @param countX
	 *            The number of points in the horizontal direction.
	 * @param countY
	 *            The number of points in the vertical direction.
	 * @param real
	 *            A pre-allocated float array of length at least countX*countY
	 *            in which the real parts of all elements will be returned.
	 * @param imag
	 *            A pre-allocated float array of length at least countX*countY
	 *            in which the imaginary parts of all elements will be returned;
	 *            may be null for real data, i.e., when <code>isComplex()</code>
	 *            returns false.
         * @param destRect 
         *            Destination Rectangle where the results must be calculated            
         * @param roi 
         *            Optional ROI used for reducing calculations to a defined region
         * @param nodata
         *            Optional NoData range to use for masking particular values
         * @param destNoData
         *            Value to set for pixels which are not accepted or are outside ROI              
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the length of the supplied array(s) is insufficient.
	 */
	void getElements(float startX, float startY, float deltaX, float deltaY,
			int countX, int countY, int element, float[] real, float[] imag,
			Rectangle destRect, ROI roi, Range nodata, float destNoData);

	/**
	 * Returns all values of a given element for a specified set of coordinates.
	 * An ArrayIndexOutOfBoundsException may be thrown if the length of the
	 * supplied array(s) is insufficient.
	 * 
	 * @param startX
	 *            The X coordinate of the upper left location to evaluate.
	 * @param startY
	 *            The Y coordinate of the upper left location to evaluate.
	 * @param deltaX
	 *            The horizontal increment.
	 * @param deltaY
	 *            The vertical increment.
	 * @param countX
	 *            The number of points in the horizontal direction.
	 * @param countY
	 *            The number of points in the vertical direction.
	 * @param real
	 *            A pre-allocated double array of length at least countX*countY
	 *            in which the real parts of all elements will be returned.
	 * @param imag
	 *            A pre-allocated double array of length at least countX*countY
	 *            in which the imaginary parts of all elements will be returned;
	 *            may be null for real data, i.e., when <code>isComplex()</code>
	 *            returns false.
	 * @param destRect 
	 *            Destination Rectangle where the results must be calculated
	 * @param roi 
	 *            Optional ROI used for reducing calculations to a defined region
	 * @param nodata
	 *            Optional NoData range to use for masking particular values
	 * @param destNoData
	 *            Value to set for pixels which are not accepted or are outside ROI           
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the length of the supplied array(s) is insufficient.
	 */
	void getElements(double startX, double startY, double deltaX,
			double deltaY, int countX, int countY, int element, double[] real,
			double[] imag, Rectangle destRect, ROI roi, Range nodata, float destNoData);

}
