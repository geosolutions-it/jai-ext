package it.geosolutions.jaiext.rlookup;

import java.util.Comparator;

/**
 * Compares LookupItems on the basis of their source value ranges.
 * 
 */
public class LookupItemComparator<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>>
        implements Comparator<LookupItem<T, U>> {

    public int compare(LookupItem<T, U> item1, LookupItem<T, U> item2) {
        return item1.getRange().compare(item2.getRange());
    }
}
