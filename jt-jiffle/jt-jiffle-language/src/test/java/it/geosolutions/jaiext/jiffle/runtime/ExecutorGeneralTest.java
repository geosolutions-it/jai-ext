/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package it.geosolutions.jaiext.jiffle.runtime;

import java.awt.image.WritableRenderedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import it.geosolutions.jaiext.jiffle.Jiffle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * General tests for {@link JiffleExecutor}.
 * 
 * @see ExecutorSimpleTaskTest
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class ExecutorGeneralTest {
    private static final int WIDTH = 100;
    private static final double TOL = 1.0e-8;
    
    private JiffleExecutor executor;
    private StreamHandler handler;
    private ByteArrayOutputStream out;

    @Before
    public void setup() {
        executor = new JiffleExecutor();
        
        Logger logger = Logger.getLogger(JiffleExecutor.class.getName());  
        Formatter formatter = new SimpleFormatter();  
        out = new ByteArrayOutputStream();  
        handler = new StreamHandler(out, formatter);  
        logger.addHandler(handler);  
        logger.setUseParentHandlers(false);
    }
    
    @After
    public void cleanup() {
        Logger logger = Logger.getLogger(JiffleExecutor.class.getName());
        logger.removeHandler(handler);
        logger.setUseParentHandlers(true);
        
        executor.shutdownAndWait(1, TimeUnit.SECONDS);
    }
    
    
    @Test
    public void submitRuntimeInstance() throws Exception {
        System.out.println("   submit a JiffleDirectRuntime instance");
        
        JiffleDirectRuntime runtime = getRuntime("dest = 42;");
        executor.submit(runtime, null);
    }
    
    @Test
    public void defaultPollingInterval() throws Exception {
        System.out.println("   default polling interval");
        assertEquals(JiffleExecutor.DEFAULT_POLLING_INTERVAL, executor.getPollingInterval());
    }

    @Test
    public void setPollingInterval() throws Exception {
        System.out.println("   set polling interval");
        
        executor.setPollingInterval(50);
        assertEquals(50L, executor.getPollingInterval());
    }
    
    @Test
    public void invalidPollingIntervalIgnored() throws Exception {
        System.out.println("   invalid polling interval is ignored");
        executor.setPollingInterval(-1);
        assertEquals(JiffleExecutor.DEFAULT_POLLING_INTERVAL, executor.getPollingInterval());
    }
    
    @Test
    public void invalidPollingIntervalWarning() throws Exception {
        System.out.println("   invalid polling interval warning received");
        assertWarningMessage(-1, "polling interval ignored");
    }
    
    @Test
    public void pollingIntervalAfterFirstTaskIgnored() throws Exception {
        System.out.println("   polling interval set after first task is ignored");
        
        JiffleDirectRuntime runtime = getRuntime("dest = 42;");
        executor.submit(runtime, null);
        executor.setPollingInterval(JiffleExecutor.DEFAULT_POLLING_INTERVAL * 2);
        
        // polling interval set after task should have been ignored
        assertEquals(JiffleExecutor.DEFAULT_POLLING_INTERVAL, executor.getPollingInterval());
    }
    
    @Test
    public void pollingIntervalAfterFirstTaskWarning() throws Exception {
        System.out.println("   polling interval set after first task warning received");
        
        JiffleDirectRuntime runtime = getRuntime("dest = 42;");
        executor.submit(runtime, null);
        assertWarningMessage(JiffleExecutor.DEFAULT_POLLING_INTERVAL * 2, 
                "polling interval ignored");
    }
    
    private void assertWarningMessage(long pollingInterval, String expectedMsg) {
        executor.setPollingInterval(pollingInterval);

        handler.flush();
        String logMsg = out.toString();

        assertNotNull(logMsg);
        assertTrue(logMsg.toLowerCase().contains(expectedMsg.toLowerCase()));
    }
    
    
    @Test
    public void addEventListener() throws Exception {
        System.out.println("   add event listener");
        
        JiffleEventListener listener = createListener();
        executor.addEventListener(listener);
        assertTrue(executor.isListening(listener));
    }
    
    @Test
    public void removeEventListener() throws Exception {
        System.out.println("   remove event listener");
        
        JiffleEventListener listener = createListener();
        executor.addEventListener(listener);
        
        executor.removeEventListener(listener);
        assertFalse(executor.isListening(listener));
    }
    
    @Test
    public void speculativeRemoveEventListener() throws Exception {
        System.out.println("   ok to speculatively call removeEventListener");
        executor.removeEventListener(createListener());
    }
    
    @Test
    public void taskCompletedOnShutdown() throws Exception {
        System.out.println("   task completed after shutdown request");
        WaitingListener listener = new WaitingListener();
        executor.addEventListener(listener);
        listener.setNumTasks(1);
        
        executor.submit(new MockJiffleRuntime(100, 5), null);

        executor.shutdown();
        if (!listener.await(2, TimeUnit.SECONDS)) {
            fail("Listener time-out period elapsed");
        }
        
        List<JiffleExecutorResult> results = listener.getResults();
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompleted());
    }
    
    @Test
    public void taskDiscardedOnImmediateShutdown() throws Exception {
        System.out.println("   task discarded after shutdownNow request");
        
        WaitingListener listener = new WaitingListener();
        executor.addEventListener(listener);
        listener.setNumTasks(1);
        
        executor.submit(new MockJiffleRuntime(100, 5), null);

        executor.shutdownNow();
        boolean receivedEvent = listener.await(1, TimeUnit.SECONDS);
        assertFalse(receivedEvent);
    }
    
    
    private JiffleDirectRuntime getRuntime(String script) throws Exception {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        
        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        
        jiffle.setImageParams(imageParams);

        jiffle.compile();
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        WritableRenderedImage destImg = ImageUtilities.createConstantImage(WIDTH, WIDTH, 0d);
        runtime.setDestinationImage("dest", destImg);
        
        return runtime;
    }
    
    private JiffleEventListener createListener() {
        return new JiffleEventListener() {
            public void onCompletionEvent(JiffleEvent ev) {
            }

            public void onFailureEvent(JiffleEvent ev) {
            }
        };
    }
    
}
