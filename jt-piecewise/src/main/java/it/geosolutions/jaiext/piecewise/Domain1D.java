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

import java.util.List;

/**
 * An immutable {@link Domain1D} as a list of {@link DomainElement1D}. {@link DomainElement1D} are sorted by their values. Overlapping ranges are not
 * allowed. The{@link #findDomainElement(double)} method is responsible for finding the right {@link DomainElement1D} for an arbitrary domain value.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * @source $URL$
 */
public interface Domain1D<T extends DomainElement1D> extends List<T> {

    /**
     * Returns the name of this object. The default implementation returns the name of what seems to be the "main" domain element (i.e. the domain
     * element with the widest range of values).
     */
    public abstract String getName();

    /**
     * Returns the range of values in this {@link Domain1D}. This is the union of the range of values of every {@link Domain1D}.
     * 
     * @return The range of values.
     * 
     */
    public abstract Range getApproximateDomainRange();

    /**
     * Returns the {@link DomainElement1D} of the specified sample value. If no {@link DomainElement1D} fits, then this method returns {@code null}.
     * 
     * @param sample The value.
     * @return The domain element of the supplied value, or {@code null}.
     */
    public T findDomainElement(final double sample);

    /**
     * Tell us if there is a gap in this {@link Domain1D} which means a range where no {@link DomainElement1D} is defined.
     * 
     * @return <code>true</code> in case a gap exists, <code>false</code> otherwise.
     * 
     * @return
     */
    public boolean hasGaps();

}
