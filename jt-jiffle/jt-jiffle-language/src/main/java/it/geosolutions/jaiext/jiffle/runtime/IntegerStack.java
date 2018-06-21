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
 * A simple, array-based stack for Integer values used by {@link AbstractJiffleRuntime}.
 * This class is here to avoid using generic collections (which the Janino compiler
 * does not support) or littering the runtime source code with casts.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class IntegerStack {
    /** Initial size of stack and grow increment */
    public static final int CHUNK_SIZE = 1000;
    
    /** 
     * Stack size beyond which the data array will be shrunk 
     * when {@link #clear()} is called. 
     */
    public static final int CLEAR_SIZE = 10 * CHUNK_SIZE;
    
    /** Data array */
    private Integer[] data = new Integer[CHUNK_SIZE];
    private int index = -1;

    /**
     * Push a value onto the stack.
     * @param x the value
     * @return the value
     */
    public synchronized Integer push(Integer x) {
        if (++index == data.length) {
            grow();
        }
        data[index] = x;
        return x;
    }

    /**
     * Pop the top value off the stack.
     * 
     * @return the value
     * @throws RuntimeException if the stack is empty
     */
    public synchronized Integer pop() {
        if (index >= 0) {
            Integer val = data[index];
            index--;
            return val;
        }
        throw new RuntimeException("Stack is empty");
    }

    /**
     * Peek at the top value without removing it.
     * 
     * @return the value
     * @throws RuntimeException if the stack is empty
     */
    public synchronized Integer peek() {
        if (index >= 0) {
            return data[index];
        }
        throw new RuntimeException("Stack is empty");
    }

    /**
     * Clear the stack. If the stack size if above {@link #CLEAR_SIZE}
     * the data array is shrunk to its initial size.
     */
    public synchronized void clear() {
        if (data.length > CLEAR_SIZE) {
            data = new Integer[CHUNK_SIZE];
        }
        index = -1;
    }
    
    /**
     * Gets the number of items on the stack.
     * 
     * @return number of items.
     */
    public int size() {
        return index + 1;
    }

    /**
     * Grow the data array by adding {@link #CHUNK_SIZE} elements.
     */
    private void grow() {
        Integer[] temp = new Integer[data.length + CHUNK_SIZE];
        System.arraycopy(data, 0, temp, 0, data.length);
        data = temp;
    }

}
