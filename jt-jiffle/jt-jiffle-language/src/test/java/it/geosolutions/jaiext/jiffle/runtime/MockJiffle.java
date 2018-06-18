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

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;

import java.util.Collections;
import java.util.Map;


/**
 * Mock object for unit tests.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class MockJiffle extends Jiffle {
    
    private final int imageWidth;
    private final long pixelTime;

    /**
     * Creates a new instance. The mock runtime object that can be
     * retrieved from this will inherit the image size and pixel
     * processing time values.
     * 
     * @param imageSize image size (number of pixels)
     * @param pixelTime pixel processing time (mS)
     * 
     * @see MockJiffleRuntime
     */
    public MockJiffle(int imageSize, long pixelTime) {
        this.imageWidth = imageSize;
        this.pixelTime = pixelTime;
    }
    
    /**
     * {@inheritDoc}
     * @return always returns {@code true} for the mock object
     */
    @Override
    public boolean isCompiled() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @return always returns an empty map
     */
    @Override
    public Map<String, ImageRole> getImageParams() {
        Map<String, ImageRole> emptyParams = Collections.emptyMap();
        return emptyParams;
    }

    /**
     * Returns a new instance of {@link MockJiffleRuntime} initialized with
     * the image size and pixel processing time values passed of this
     * object.
     * 
     * @return the mock runtime object
     */
    @Override
    public JiffleDirectRuntime getRuntimeInstance() throws JiffleException {
        return new MockJiffleRuntime(imageWidth, pixelTime);
    }
}
