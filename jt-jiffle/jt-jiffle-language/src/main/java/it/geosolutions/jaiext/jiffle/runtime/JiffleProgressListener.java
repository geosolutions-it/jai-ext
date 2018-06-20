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

/**
 * An interface implemented by classes wishing to receive task progress information
 * from a {@link JiffleExecutor}.
 * <p>
 * At run-time, the executor passes the progress listener to the 
 * {@link JiffleRuntime} object and it is the this object that updates the 
 * listener in its {@link it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime#evaluateAll}
 * method.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public interface JiffleProgressListener {

    /**
     * Called by the client to request that the listener be notified
     * of task progress after each {@code numPixels} number of destination
     * pixels have been processed by the runtime object.
     *
     * @param numPixels number of pixels between listener updates
     */
    public void setUpdateInterval(long numPixels);

    /**
     * Called by the client to request that the listener be notified
     * of task progress after each {@code propPixels} proportion of the
     * destination pixels has been processed by the runtime object.
     *
     * @param propPixels proportion of pixels between listener updates
     */
    public void setUpdateInterval(double propPixels);

    /**
     * Called by the runtime object before processing begins to get
     * the update interval as number of destination image pixels.
     *
     * @return update interval as number of pixels
     */
    public long getUpdateInterval();

    /**
     * Called by the runtime object to inform the listener of the total
     * number of pixels in the largest destination image that will be
     * processed.
     * 
     * @param numPixels number of destination image pixels
     */
    public void setTaskSize(long numPixels);
    
    /**
     * Called by the runtime object when the task starts.
     */
    public void start();
    
    /**
     * Called by the runtime object at update intervals as specified by
     * either {@link #setUpdateInterval(long)} or {@link #setUpdateInterval(double)}.
     * <p>
     * It is important to keep the amount of processing done in this method
     * to a minimum.
     * 
     * @param done number of pixels processed
     */
    public void update(long done);

    /**
     * Called by the runtime object when the task finishes.
     */
    public void finish();

}
