package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new StatisticalDescriptor operation and the old JAI version. Roi or NoData range can be
 * used by setting to true JAI.Ext.ROIUsed or JAI.Ext.RangeUsed JVM boolean parameters are set to true. If the user wants to change the number of the
 * benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles
 * parameters.If the user want to use the old descriptor must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a
 * specific data type the user must set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2
 * Short, 3 Integer, 4 Float and 5 Double). The possible statistics to calculate can be chosen by setting the JVM Integer parameter JAI.Ext.Statistic
 * to the associated value:
 * <ul>
 * <li>Mean 0</li>
 * <li>Extrema 1</li>
 * <li>Histogram 2</li>
 * </ul>
 */

public class ComparisonTest extends TestBase {


}

