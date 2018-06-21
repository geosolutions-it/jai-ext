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

import org.junit.Test;

/**
 * Tests for parsing list expressions.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class ListTest extends RuntimeTestBase {

    @Test
    public void createEmptyList() throws Exception {
        System.out.println("   create empty list with []");
        String script = "foo = []; dest = 42;" ;
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return 42;
            }
        };
        testScript(script, e);
    }

    @Test
    public void createInitList() throws Exception {
        System.out.println("   create list with initial values");
        String script = "foo = [1, 2, 3]; dest = sum(foo);" ;
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return 6;
            }
        };
        testScript(script, e);
    }
    
    @Test
    public void reassignListVar() throws Exception {
        System.out.println("   reassign list var");
        String script = "foo = [1, 2, 3]; foo = [4, 5, 6]; dest = sum(foo);";
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return 15;
            }
        };
        testScript(script, e);
    }
    
    @Test
    public void intermediateListVar() throws Exception {
        System.out.println("   intermediate list var");
        String script = "foo = [1, 2, 3, 4]; bar=foo; dest = sum(bar);";
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return 10;
            }
        };
        testScript(script, e);
    }
    
    @Test
    public void listVarAsFunctionArg() throws Exception {
        System.out.println("   list var as function arg");
        String script = "foo = [1, 2, 3, 4]; dest = src + sum(foo);" ;
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return val + 10;
            }
        };
        testScript(script, e);
    }
    
    @Test
    public void listLiteralAsFunctionArg() throws Exception {
        System.out.println("   list literal as function arg");
        String script = "dest = src + sum([1, 2, 3, 4]);" ;
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return val + 10;
            }
        };
        testScript(script, e);
    }

    @Test
    public void appendWithOperator() throws Exception {
        System.out.println("   append to list with << operator in loop");
        String script = "options { outside=0; } \n"
                + "values = []; \n"
                + "foreach (dy in -1:1) { \n"
                + "  foreach (dx in -1:1) { \n"
                + "    values << src[dx, dy]; \n"
                + "  } \n"
                + "} \n"
                + "dest = sum(values);";
        
        assertListAppend(script);
    }
    
    @Test
    public void appendWithConcat() throws Exception {
        System.out.println("   append to list with concat in loop");
        String script = "options { outside=0; } \n"
                + "values = []; \n"
                + "foreach (dy in -1:1) { \n"
                + "  foreach (dx in -1:1) { \n"
                + "    values = concat(values, src[dx, dy]); \n"
                + "  } \n"
                + "} \n"
                + "dest = sum(values);";
        
        assertListAppend(script);
    }

    @Test
    public void prependWithConcat() throws Exception {
        System.out.println("   prepend to list with concat in loop");
        String script = "options { outside=0; } \n"
                + "values = []; \n"
                + "foreach (dy in -1:1) { \n"
                + "  foreach (dx in -1:1) { \n"
                + "    values = concat(src[dx, dy], values); \n"
                + "  } \n"
                + "} \n"
                + "dest = sum(values);";
        
        assertListAppend(script);
    }
    
    @Test
    public void concatToNewVar() throws Exception {
        System.out.println("   assign concat return to new var");
        String script = 
                  "foo = [1, 2, 3]; \n"
                + "bar = concat(foo, 4); \n"
                + "dest = src + sum(bar);" ;

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return val + 10;
            }
        };
        
        testScript(script, e);
    }

    private void assertListAppend(String script) throws Exception {
        RenderedImage srcImg = createRowValueImage();
        
        Evaluator e = new Evaluator() {
            final int MAX = IMG_WIDTH - 1;
            
            public double eval(double val) {
                int sum = 0;
                int n = x == 0 || x == MAX ? 2 : 3;
                if (y > 0) sum += n*(y-1);
                sum += n*y;
                if (y < MAX) sum += n*(y+1);
                
                move();
                return sum;
            }
        };
        
        testScript(script, srcImg, e);
    }

}
