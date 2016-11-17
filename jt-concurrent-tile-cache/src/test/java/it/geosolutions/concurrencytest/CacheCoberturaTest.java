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
package it.geosolutions.concurrencytest;


import java.awt.image.RenderedImage;

import org.junit.Test;

import it.geosolutions.concurrencytest.ConcurrentCacheTest.CacheType;

public class CacheCoberturaTest {

    /*
     * This test is similar to the ConcurrentCacheTest. This implementation is only
     * used because the maven cobertura plugin needs junit tests for checking the
     * code coverage
     */
    @Test
    public void testConcurrency() throws Exception {
        // setting the parameters
        CacheType cacheUsed = CacheType.CONCURRENT_TILE_CACHE;
        int concurrencyLevel = 16;
        long memoryCacheCapacity = 128 * 1024 * 1024;
        RenderedImage imageSynth = ConcurrentCacheTest.getSynthetic(1);
        String path = null;
        boolean diagnosticEnabled = true;
        boolean multipleOp = false;
        // this cycle performs the same requests to every type of caches
        new ConcurrentCacheTest().testwriteImageAndWatchFlag(cacheUsed, concurrencyLevel,
                memoryCacheCapacity, imageSynth, path, diagnosticEnabled, multipleOp);

    }
    
    @Test
    public void testConcurrencyMultiMap() throws Exception {
        // setting the parameters
        CacheType cacheUsed = CacheType.CONCURRENT_MULTIMAP_TILE_CACHE;
        int concurrencyLevel = 16;
        long memoryCacheCapacity = 128 * 1024 * 1024;
        RenderedImage imageSynth = ConcurrentCacheTest.getSynthetic(1);
        String path = null;
        boolean diagnosticEnabled = true;
        boolean multipleOp = false;
        // this cycle performs the same requests to every type of caches
        new ConcurrentCacheTest().testwriteImageAndWatchFlag(cacheUsed, concurrencyLevel,
                memoryCacheCapacity, imageSynth, path, diagnosticEnabled, multipleOp);

    }


}
