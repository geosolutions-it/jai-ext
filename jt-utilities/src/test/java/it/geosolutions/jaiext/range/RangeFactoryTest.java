/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2015 GeoSolutions


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
package it.geosolutions.jaiext.range;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.geosolutions.jaiext.range.Range.DataType;

public class RangeFactoryTest {

    @Test
    public void testCast() {
        RangeByte range = new RangeByte((byte) 0, true, (byte) 10, false);
        Range cast = RangeFactory.cast(range, DataType.INTEGER);
        assertThat(cast, instanceOf(RangeInt.class));
        assertEquals(0, cast.getMin().intValue());
        assertTrue(cast.isMinIncluded);
        assertEquals(10, cast.getMax().intValue());
        assertFalse(cast.isMaxIncluded);
    }
}
