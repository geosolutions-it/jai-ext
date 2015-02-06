package it.geosolutions.jaiext.rlookup;


import it.geosolutions.jaiext.testclasses.TestBase;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests for RangeLookupTable. We don't do comprehensive testing of source
 * and lookup data types here because that is tested as part of the image
 * lookup tests in {@link RangeLookupTest}.
 */
public class RangeLookupTableTest extends TestBase {
    
    private RangeLookupTable.Builder<Integer, Integer> builder;

    
    @Before
    public void setup() {
        builder = new RangeLookupTable.Builder<Integer, Integer>();
    }
    
    @Test
    public void simpleLookup() throws Exception {
        System.out.println("   simple integer lookup");
        
        Integer[] breaks = { -10, -5, 0, 5, 10 };
        Integer[] values = { -99, -1, 0, 1, 2, 99 };
        RangeLookupTable<Integer, Integer> table = createTableFromBreaks(breaks, values);

        final int N = breaks.length;
        final int startVal = breaks[0] - 1;
        final int endVal = breaks[N-1] + 1;
        
        int k = 0;
        int expected = values[0];
        LookupItem<Integer, Integer> match;
        
        for (int val = startVal; val <= endVal; val++) {
            if (val >= breaks[k]) {
                expected = values[k+1];
                if (k < N-1) k++ ;
            }
            
            match = table.getLookupItem(val);
            assertNotNull(match);
            assertEquals(expected, match.getValue().intValue());
        }
    }
    
    @Test
    public void addOverlappedRange() throws Exception {
        System.out.println("   add overlapping range");
        
        builder.add(Range.create(5, true, 10, true), 1);
        
        // this range is overlapped by the first range
        builder.add(Range.create(0, true, 20, true), 2);
        
        RangeLookupTable<Integer, Integer> table = builder.build();
        
        /*
         * The table should now be:
         *   [0, 5) => 2
         *   [5, 10] => 1
         *   (10, 20] => 2
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
        System.out.println("   add completely overlapped range");

        builder.add(Range.create(0, true, 20, true), 1);

        // this range is overlapped by the first range
        builder.add(Range.create(5, true, 10, true), 2);

        RangeLookupTable<Integer, Integer> table = builder.build();

        for (int val = 0; val <= 20; val++) {
            LookupItem<Integer, Integer> match = table.getLookupItem(val);
            assertNotNull(match);
            assertEquals(1, match.getValue().intValue());
        }
    }
    
    @Test
    public void lookupWithIntervalGap() throws Exception {
        System.out.println("   lookup with interval gaps");
        
        builder.add(Range.create(null, false, -1, false), 1);
        builder.add(Range.create(1, false, null, false), 1);
        
        RangeLookupTable<Integer, Integer> table = builder.build();
        
        assertEquals(1, table.getLookupItem(-2).getValue().intValue());
        assertNull( table.getLookupItem(-1) );
        assertNull( table.getLookupItem(0) );
        assertNull( table.getLookupItem(1) );
        assertEquals(1, table.getLookupItem(2).getValue().intValue());
    }
    
    @Test
    public void lookupWithPointGap() throws Exception {
        System.out.println("   lookup with interval gaps");
        
        // all numbers excluding 0
        builder.add(Range.create(null, false, 0, false), 1);
        builder.add(Range.create(0, false, null, false), 1);
        
        RangeLookupTable<Integer, Integer> table = builder.build();
        
        assertEquals(1, table.getLookupItem(-1).getValue().intValue());
        assertNull( table.getLookupItem(0) );
        assertEquals(1, table.getLookupItem(1).getValue().intValue());
    }
}
