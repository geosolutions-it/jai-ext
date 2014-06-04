package it.geosolutions.concurrencytest;


import java.awt.image.RenderedImage;

import org.junit.Test;

public class CacheCoberturaTest {

/*
 * This test is similar to the ConcurrentCacheTest. This implementation is only
 * used because the maven cobertura plugin needs junit tests for checking the
 * code coverage
 */
@Test
public void testConcurrency() throws Exception {
    // setting the parameters
    int cacheUsed=1;
    int concurrencyLevel = 16;
    long memoryCacheCapacity = 128 * 1024 * 1024;
    RenderedImage imageSynth = ConcurrentCacheTest.getSynthetic(1);
    String path = null;
    boolean diagnosticEnabled = true;
    boolean multipleOp = false;
    // this cycle performs the same requests to every type of caches
        new ConcurrentCacheTest().testwriteImageAndWatchFlag(cacheUsed,
                concurrencyLevel, memoryCacheCapacity, imageSynth, path,
                diagnosticEnabled, multipleOp);
    

}

}
