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
package it.geosolutions.jaiext.rlookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for RangeLookupTable. We don't do comprehensive testing of source and lookup data types here because that is tested as part of the image
 * lookup tests in {@link RangeLookupTest}.
 * 
 * @author Michael Bedward
 */
public class RangeLookupTableTest extends TestBase {

    private RangeLookupTable.Builder<Integer, Integer> builder;

    @Before
    public void initialsetup() {
        builder = new RangeLookupTable.Builder<Integer, Integer>();
    }

    @Test
    public void simpleLookup() throws Exception {

        Integer[] breaks = { -10, -5, 0, 5, 10 };
        Integer[] values = { -99, -1, 0, 1, 2, 99 };
        RangeLookupTable<Integer, Integer> table = RangeLookupTest.createTable(breaks, values);

        final int N = breaks.length;
        final int startVal = breaks[0] - 1;
        final int endVal = breaks[N - 1] + 1;

        int k = 0;
        int expected = values[0];
        LookupItem<Integer, Integer> match;

        for (int val = startVal; val <= endVal; val++) {
            if (val >= breaks[k]) {
                expected = values[k + 1];
                if (k < N - 1)
                    k++;
            }

            match = table.getLookupItem(val);
            assertNotNull(match);
            assertEquals(expected, match.getValue().intValue());
        }
    }

    @Test
    public void addOverlappedRange() throws Exception {

        builder.add(RangeFactory.create(5, true, 10, true), 1);

        // this range is overlapped by the first range
        builder.add(RangeFactory.create(0, true, 20, true), 2);

        RangeLookupTable<Integer, Integer> table = builder.build();

        /*
         * The table should now be: [0, 5) => 2 [5, 10] => 1 (10, 20] => 2
         */

        LookupItem<Integer, Integer> match;
        for (int val = 0; val <= 20; val++) {
            int expected = val < 5 || val > 10 ? 2 : 1;

            match = table.getLookupItem(val);
            assertNotNull(match);
            assertEquals(expected, match.getValue().intValue());
        }
    }

    @Test
    public void addCompletelyOverlappedRange() throws Exception {

        builder.add(RangeFactory.create(0, true, 20, true), 1);

        // this range is overlapped by the first range
        builder.add(RangeFactory.create(5, true, 10, true), 2);

        RangeLookupTable<Integer, Integer> table = builder.build();

        for (int val = 0; val <= 20; val++) {
            LookupItem<Integer, Integer> match = table.getLookupItem(val);
            assertNotNull(match);
            assertEquals(1, match.getValue().intValue());
        }
    }

    @Test
    public void lookupWithIntervalGap() throws Exception {

        builder.add(RangeFactory.create(Double.NEGATIVE_INFINITY, false, -1, false), 1);
        builder.add(RangeFactory.create(1, false, Double.POSITIVE_INFINITY, false), 1);

        RangeLookupTable<Integer, Integer> table = builder.build();

        assertEquals(1, table.getLookupItem(-2).getValue().intValue());
        assertNull(table.getLookupItem(-1));
        assertNull(table.getLookupItem(0));
        assertNull(table.getLookupItem(1));
        assertEquals(1, table.getLookupItem(2).getValue().intValue());
    }

    @Test
    public void lookupWithPointGap() throws Exception {

        // all numbers excluding 0
        builder.add(RangeFactory.create(Double.NEGATIVE_INFINITY, false, 0, false), 1);
        builder.add(RangeFactory.create(0, false, Double.POSITIVE_INFINITY, false), 1);

        RangeLookupTable<Integer, Integer> table = builder.build();

        assertEquals(1, table.getLookupItem(-1).getValue().intValue());
        assertNull(table.getLookupItem(0));
        assertEquals(1, table.getLookupItem(1).getValue().intValue());
    }
}
