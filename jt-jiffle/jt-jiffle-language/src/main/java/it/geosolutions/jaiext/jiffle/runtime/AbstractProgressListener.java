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
 * A simple progress listener base. Extend this and provide your own {@code start()},
 * {@code update(long numPixelsDone)} and {@code finish()} methods.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class AbstractProgressListener implements JiffleProgressListener {
    
    /** The number of pixels processed between each call to the listener. */
    protected long updateInterval;

    /** The proportion of pixels processed between each call to the listener. */
    protected Double updateProp;

    /** The total number of pixels to process. */
    protected long taskSize;

    /** 
     * Creates a new instance with an update interval of 1.
     */
    public AbstractProgressListener() {
        updateInterval = 1;
        taskSize = 0;
    }

    /**
     * Sets the update interval.
     * 
     * @param numPixels number of pixels processed between each call to the listener
     */
    public void setUpdateInterval(long numPixels) {
        updateInterval = numPixels;
        updateProp = null;
    }

    /**
     * Sets the update interval expressed a proportion of the total number of
     * pixels.
     * 
     * @param propPixels proportion of pixels processed between each call to the listener
     */
    public void setUpdateInterval(double propPixels) {
        updateProp = Math.min(Math.max(propPixels, 0.0), 1.0);
        init();
    }

    /**
     * Gets the update interval.
     * 
     * @return interval as number of pixels
     */
    public long getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Called by the runtime object at the start of processing to set this
     * listener's task size field.
     * 
     * @param numPixels task size as number of pixels to process
     */
    public void setTaskSize(long numPixels) {
        taskSize = numPixels;
        init();
    }

    private void init() {
        if (taskSize > 0) {
            if (updateProp != null) {
                long n = (long)(taskSize * updateProp);
                updateInterval = Math.max(n, 1);
            }
        }
    }

}
