package it.geosolutions.jaiext.interpolators;

import it.geosolutions.jaiext.range.Range;
/**
 * Simple interface for handling No Data for the interpolators
 * 
 * @author geosolutions
 *
 */
public interface InterpolationNoData {
    
    /**
     * Return NoData Range associated to the Interpolation object, if present.
     * 
     * @return NoData Range
     */
    public Range getNoDataRange();

    /**
     * Set NoData Range associated to the Interpolation object.
     * 
     */
    public void setNoDataRange(Range noDataRange);
    
    /**
     * Return the destinationNoData value associated to the Interpolation Object
     * 
     * @return destinationNoData
     */
    public double getDestinationNoData();
    
}
