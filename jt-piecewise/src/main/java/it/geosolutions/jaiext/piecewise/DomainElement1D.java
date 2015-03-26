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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;

import java.io.Serializable;

/**
 * A {@link DomainElement1D} can be seen as a monodimensional range of values with its own label.
 * 
 * <p>
 * All {@link DomainElement1D}D <strong>must</strong> have a human readable name.
 * <p>
 * All {@code DomainElement1D} objects are immutable and thread-safe.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * 
 * @source $URL$
 */
public interface DomainElement1D extends Serializable, Comparable<DomainElement1D> {

    /**
     * Returns the domain element name.
     */
    public String getName();

    /**
     * Compares the specified object with this domain element for equality.
     */
    public boolean equals(final Object object);

    /**
     * Provides access to the input {@link NumberRange} for this {@link DomainElement1D}.
     * 
     * @return the range where this {@link DomainElement1D} is defined.
     */
    public Range getRange();

    /**
     * This methods can be used to check whether or not a given value belongs to {@link DomainElement1D}.
     * 
     * @param value to check for the inclusion.
     * @return <code>true</code> if the value belongs to this {@link DomainElement1D}, <code>false</code> otherwise.
     */
    public boolean contains(final double value);

    /**
     * This methods can be used to check whether or not a given value belongs to {@link DomainElement1D}.
     * 
     * @param value to check for the inclusion.
     * @return <code>true</code> if the value belongs to this {@link DomainElement1D}, <code>false</code> otherwise.
     */
    public boolean contains(final Number value);

    /**
     * This methods can be used to check whether or not a given {@link NumberRange} belongs to {@link DomainElement1D}.
     * 
     * @param value to check for the inclusion.
     * @return <code>true</code> if the {@link NumberRange} belongs to this {@link DomainElement1D}, <code>false</code> otherwise.
     */
    public boolean contains(final Range range);

}
