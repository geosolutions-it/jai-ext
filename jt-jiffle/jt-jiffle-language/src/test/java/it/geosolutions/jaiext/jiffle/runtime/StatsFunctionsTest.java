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

import org.junit.Test;

/**
 * Unit tests for stats functions
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class StatsFunctionsTest extends RuntimeTestBase {

    @Test
    public void max2Arg() throws Exception {
        System.out.println("   max(D, D)");
        
        int z = IMG_WIDTH * IMG_WIDTH / 2;
        String script = String.format("init { z = %d; } dest = max(src, z);", z);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.max(val, 50);
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void min2Arg() throws Exception {
        System.out.println("   min(D, D)");
        
        int z = IMG_WIDTH * IMG_WIDTH / 2;
        String script = String.format("init { z = %d; } dest = min(src, z);", z);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.min(val, 50);
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void maxListArg() throws Exception {
        System.out.println("   max(List)");
        
        String script = "options { outside = 0; } \n"
                + "z = [ src[0,-1], src, src[0,1] ]; \n"
                + "dest = max(z);";
        
        Evaluator e = new Evaluator() {
            final int MAXY = IMG_WIDTH - 1;
            
            public double eval(double val) {
                double z = Math.min(MAXY, y + 1);
                move();
                return z;
            }
        };
        
        testScript(script, createRowValueImage(), e);
    }

    @Test
    public void minListArg() throws Exception {
        System.out.println("   min(List)");
        
        String script = "options { outside = 0; } \n"
                + "z = [ src[0,-1], src, src[0,1] ]; \n"
                + "dest = min(z);";
        
        Evaluator e = new Evaluator() {
            final int MAXY = IMG_WIDTH - 1;

            public double eval(double val) {
                double z = y == MAXY ? 0 : Math.max(0, y-1);
                move();
                return z;
            }
        };
        
        testScript(script, createRowValueImage(), e);
    }

}
