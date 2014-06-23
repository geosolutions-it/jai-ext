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

import it.geosolutions.concurrent.ConcurrentTileCacheMultiMap;

import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.RenderedOp;

import junit.framework.Assert;
import it.geosolutions.jaiext.testclasses.TestData;
import org.junit.Test;

import com.sun.media.jai.operator.ImageReadDescriptor;

/**
 * This test class is used for checking if the {@link ConcurrentTileCacheMultiMap} behaves correctly.
 * 
 * @author Nicola Lagomarsini GeoSolutions S.A.S.
 * 
 */
public class ConcurrentMultiMapTest {

    /** Total number of requests to execute */
    private final static int TOTAL = 100;

    @Test
    public void testAddAndGetTile() throws InterruptedException, FileNotFoundException, IOException {
        // Input stream to use
        ImageInputStream stream_in = null;
        try {
            stream_in = new FileImageInputStream(TestData.file(this, "world.tiff"));
            // Input RenderedImage to use
            final RenderedOp input = ImageReadDescriptor.create(stream_in, 0, false, false, false,
                    null, null, null, null, null);

            // Boolean used for checking if the conditions are passed
            final AtomicBoolean passed = new AtomicBoolean(true);
            // Cache creation
            final ConcurrentTileCacheMultiMap cache = new ConcurrentTileCacheMultiMap(1000 * 1000,
                    false, 1f, 4);
            // Selection of one tile from the image
            Raster data = input.getTile(input.getMinTileX(), input.getMinTileY());
            // Setting the tile inside the cache
            cache.add(input, input.getMinTileX(), input.getMinTileY(), data);
            // Thread pool to use for doing concurrent access on the cache
            ThreadPoolExecutor executor = new ThreadPoolExecutor(TOTAL, TOTAL, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1000000));
            // Latch used for waiting all the threads to end their work
            final CountDownLatch latch = new CountDownLatch(TOTAL);
            // Cycle for launching various requests
            int counter = TOTAL;
            while (counter > 0) {

                executor.execute(new Runnable() {

                    public void run() {
                        // Get the tile to use
                        Raster data = cache.getTile(input, input.getMinTileX(), input.getMinTileY());
                        if (data == null) {
                            passed.getAndSet(false);
                        }
                        latch.countDown();

                    }
                });
                // Counter update
                counter--;
            }
            // Waiting all threads to finish
            latch.await();
            // Ensure that all the threads have found the tile
            Assert.assertTrue(passed.get());
        } finally {
            try {
                if (stream_in != null) {
                    stream_in.flush();
                    stream_in.close();
                }
            } catch (Throwable t) {
                //
            }
        }
    }
    
    @Test
    public void testRemoveTile() throws InterruptedException, FileNotFoundException, IOException {
        // Input stream to use
        ImageInputStream stream_in = null;
        try {
            stream_in = new FileImageInputStream(TestData.file(this, "world.tiff"));
            // Input RenderedImage to use
            final RenderedOp input = ImageReadDescriptor.create(stream_in, 0, false, false, false,
                    null, null, null, null, null);

            // Boolean used for checking if the conditions are passed
            final AtomicBoolean passed = new AtomicBoolean(true);
            // Cache creation
            final ConcurrentTileCacheMultiMap cache = new ConcurrentTileCacheMultiMap(1000 * 1000,
                    false, 1f, 4);
            // Selection of one tile from the image
            Raster data = input.getTile(input.getMinTileX(), input.getMinTileY());
            // Setting the tile inside the cache
            cache.add(input, input.getMinTileX(), input.getMinTileY(), data);
            // Removing tile
            cache.remove(input, input.getMinTileX(), input.getMinTileY());
            // Thread pool to use for doing concurrent access on the cache
            ThreadPoolExecutor executor = new ThreadPoolExecutor(TOTAL, TOTAL, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1000000));
            // Latch used for waiting all the threads to end their work
            final CountDownLatch latch = new CountDownLatch(TOTAL);
            // Cycle for launching various requests
            int counter = TOTAL;
            while (counter > 0) {

                executor.execute(new Runnable() {

                    public void run() {
                        // Get the tile to use
                        Raster data = cache.getTile(input, input.getMinTileX(), input.getMinTileY());
                        if (data != null) {
                            passed.getAndSet(false);
                        }
                        latch.countDown();

                    }
                });
                // Counter update
                counter--;
            }
            // Waiting all threads to finish
            latch.await();
            // Ensure that all the threads have found the tile
            Assert.assertTrue(passed.get());
        } finally {
            try {
                if (stream_in != null) {
                    stream_in.flush();
                    stream_in.close();
                }
            } catch (Throwable t) {
                //
            }
        }
    }  
}
