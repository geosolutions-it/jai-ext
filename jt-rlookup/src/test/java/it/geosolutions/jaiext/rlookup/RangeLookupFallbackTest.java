package it.geosolutions.jaiext.rlookup;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests RangeLookup for unmatched source values.
 * 
 */
public class RangeLookupFallbackTest extends TestBase {
    
    private static final int WIDTH = 10;
    private static final int MATCH_VALUE = 1;
    private static final int DEFAULT_VALUE = 2;

    private RangeLookupTable.Builder<Integer, Integer> builder;

    @Before
    public void initialsetup() {
        builder = new RangeLookupTable.Builder<Integer, Integer>();
    }
    
    
    @Test
    public void defaultValue() throws Exception {
        assertLookup(true);
    }
    
    @Test
    public void passThroughSourceValue() throws Exception {
        assertLookup(false);
    }
    
    
    private void assertLookup(boolean useDefault) {
        int minValue = 0;
        int maxValue = WIDTH * WIDTH;
        int third = (maxValue - minValue) / 3;
        
        RenderedImage srcImage = createTestImage(
                Integer.valueOf(minValue), ImageDataType.INT, WIDTH, WIDTH);

        // lookup table with gap in middle third of source value range
        builder.add(Range.create(minValue, true, minValue + third, true), MATCH_VALUE);
        builder.add(Range.create(maxValue - third, true, maxValue, true), MATCH_VALUE);
        RangeLookupTable<Integer, Integer> table = builder.build();
        
        ParameterBlockJAI pb = new ParameterBlockJAI("RangeLookup");
        pb.setSource("source0", srcImage);
        pb.setParameter("table", table);
        
        if (useDefault) {
            pb.setParameter("default", DEFAULT_VALUE);
        }
        
        RenderedOp destImage = JAI.create("RangeLookup", pb);
        
        SimpleIterator srcIter = new SimpleIterator(srcImage, null, null);
        SimpleIterator destIter = new SimpleIterator(destImage, null, null);
        do {
            int srcValue = srcIter.getSample().intValue();
            int destValue = destIter.getSample().intValue();
            
            LookupItem<Integer, Integer> item = table.getLookupItem(srcValue);
            if (item != null) {
                assertEquals(MATCH_VALUE, destValue);
                
            } else if (useDefault) {
                assertEquals(DEFAULT_VALUE, destValue);
                
            } else { // pass-through source values
                assertEquals(srcValue, destValue);
            }
            
        } while (srcIter.next() && destIter.next());
    }
}
