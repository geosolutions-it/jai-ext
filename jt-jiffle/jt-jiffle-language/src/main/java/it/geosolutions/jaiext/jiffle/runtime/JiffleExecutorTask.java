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
 *  Copyright (c) 2009-2011, Michael Bedward. All rights reserved. 
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

import java.util.concurrent.Callable;


/**
 * Executes a runtime object in a thread provided by a {@link JiffleExecutor}.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleExecutorTask implements Callable<JiffleExecutorResult> {
    
    private final JiffleExecutor executor;
    private final int id;
    private final JiffleDirectRuntime runtime;
    private final JiffleProgressListener progressListener;
    
    private boolean completed;

    
    /**
     * Creates a new task. The image variable names (keys) in {@code images}
     * must correspond to those known by the runtime object.
     * 
     * @param executor the {@code JiffleExecutor} running this task
     * @param id job ID allocated by the {@link JiffleExecutor}.
     * @param runtime the {@link JiffleDirectRuntime} instance
     * @param progressListener  
     */
    public JiffleExecutorTask(
            JiffleExecutor executor,
            int id, 
            JiffleDirectRuntime runtime, 
            JiffleProgressListener progressListener) {
        
        this.executor = executor;
        this.id = id;
        this.runtime = runtime;
        this.progressListener = progressListener;
        
        completed = false;
    }

    /**
     * Called by the system to execute this task on a thread provided by the
     * {@link JiffleExecutor}.
     * 
     * @return a result object with references to the {@code Jiffle} object,
     *         the images, and the job completion status
     */
    public JiffleExecutorResult call() {
        boolean gotEx = false;
        try {
            runtime.evaluateAll(progressListener);
            
        } catch (Exception ex) {
            gotEx = true;
        }

        completed = !gotEx;
        return new JiffleExecutorResult(id, runtime, completed);
    }

}

