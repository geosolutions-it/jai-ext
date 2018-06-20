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

import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import it.geosolutions.jaiext.jiffle.Jiffle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * Tests running a basic task with the executor. Can be run multiple times with
 * {@code JiffleExecutorTestRunner} to check for concurrency problems.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 *
 */
@RunWith(ExecutorTestRunner.class)
public class ExecutorSimpleTaskTest {
    private static final int WIDTH = 100;
    private static final double TOL = 1.0e-8;
    
    private JiffleExecutor executor;
    private final JiffleProgressListener nullListener = new NullProgressListener();
    

    @Before
    public void setup() {
        executor = new JiffleExecutor();
    }
    
    @After
    public void cleanup() {
        executor.shutdownAndWait(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void simpleTask() throws Exception {
        Map<String, Jiffle.ImageRole> imageParams;
        imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        
        Jiffle jiffle = new Jiffle("dest = x() + y();", imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        WritableRenderedImage destImage = ImageUtilities.createConstantImage(WIDTH, WIDTH, 0d);
        runtime.setDestinationImage("dest", destImage);
        
        WaitingListener listener = new WaitingListener();
        executor.addEventListener(listener);
        
        listener.setNumTasks(1);

        int jobID = executor.submit(runtime, nullListener);
        
        if (!listener.await(2, TimeUnit.SECONDS)) {
            fail("Listener time-out period elapsed");
        }
        
        JiffleExecutorResult result = listener.getResults().get(0);
        assertNotNull(result);
        
        RenderedImage dest = result.getImages().get("dest");
        assertNotNull(dest);
        
        RectIter iter = RectIterFactory.create(dest, null);
        for (int y = 0; y < WIDTH; y++) {
            for (int x = 0; x < WIDTH; x++) {
                assertEquals((double)x + y, iter.getSampleDouble(), TOL);
                iter.nextPixel();
            }
            iter.startPixels();
            iter.nextLine();
        }
    }
    
}
