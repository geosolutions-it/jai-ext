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

import it.geosolutions.jaiext.jiffle.JiffleException;

import org.junit.Test;

/**
 * Unit tests for Jiffle's loop statements.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class LoopTest extends RuntimeTestBase {

    private static final Exception EXPECTED_EXCEPTION =
            new JiffleRuntimeException("Exceeded maximum allowed loop iterations per pixel");

    @Test
    public void whileLoopWithSimpleStatement() throws Exception {
        System.out.println("   while loop with simple statement");
        String script = 
                  "n = 0; \n"
                + "while (n < x()) n++; \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int xx = x;
                move();
                return xx;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void whileLoopWithBlock() throws Exception {
        System.out.println("   while loop with block");
        String script = 
                  "n = 0; \n"
                + "i = 0; \n"
                + "while (i < x()) { n += i; i++ ; } \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int n = 0;
                for (int i = 0; i < x; i++) n += i;
                
                move();
                return n;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void untilLoopWithSimpleStatement() throws Exception {
        System.out.println("   until loop with simple statement");
        String script = 
                  "n = 0; \n"
                + "until (n > x()) n++; \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int xx = x;
                move();
                return xx + 1;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void untilLoopWithBlock() throws Exception {
        System.out.println("   until loop with block");
        String script = 
                  "n = 0; \n"
                + "i = 0; \n"
                + "until (i > x()) { n += i; i++ ; } \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int n = 0;
                for (int i = 0; i <= x; i++) n += i;
                move();
                return n;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void foreachListLoopWithSimpleStatement() throws Exception {
        System.out.println("   foreach (i in [x(), y(), 3]) simple statement");
        String script =
                  "z = 0;"
                + "foreach (i in [x(), y(), 3]) z += i;"
                + "dest = z;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = x + y + 3;
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void foreachListLoopWithBlock() throws Exception {
        System.out.println("   foreach (i in [x(), y(), 3]) block");
        String script =
                  "z = 0;"
                + "foreach (i in [x(), y(), 3]) \n"
                + "{ \n"
                + "    temp = i * 2; \n"
                + "    z += temp; \n"
                + "} \n"
                + "dest = z;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = 2*(x + y + 3);
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void foreachSequenceLoopWithSimpleStatement() throws Exception {
        System.out.println("   foreach (i in -1:5) simple statement");
        String script =
                  "z = 0; \n"
                + "foreach (i in -1:5) z += i*src; \n"
                + "dest = z;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = 0;
                for (int i = -1; i <= 5; i++) z += val * i;
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void foreachSequenceLoopWithBlock() throws Exception {
        System.out.println("   foreach (i in -1:5) block");
        String script =
                  "z = 0; \n"
                + "foreach (i in -1:5) { \n"
                + "    temp = i * src; \n"
                + "    z += temp; \n"
                + "} \n"
                + "dest = z;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = 0;
                for (int i = -1; i <= 5; i++) z += val * i;
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void breakif() throws Exception {
        System.out.println("   breakif");
        String script = 
                  "n = 0; \n"
                + "i = 0; \n"
                + "while (i < x()) { \n"
                + "  n += i; \n"
                + "  breakif(n >= 10); \n"
                + "  i++ ; \n"
                + "} \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int n = 0;
                for (int i = 0; i < x; i++) n += i;
                move();
                return (n < 10 ? n : 10);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void breakifNestedInIf() throws Exception {
        System.out.println("   breakif nested in if-block");
        
        String script = 
                  "n = 0; \n"
                + "i = 0; \n"
                + "while (i < x()) { \n"
                + "  n += i; \n"
                + "  if (true) { \n"
                + "      breakif(n >= 10); \n"
                + "  } \n"
                + "  i++ ; \n"
                + "} \n"
                + "dest = n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                int n = 0;
                for (int i = 0; i < x; i++) n += i;
                move();
                return (n < 10 ? n : 10);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void unconditionalBreak() throws Exception {
        System.out.println("   unconditional break");
        String script = 
                  "i = 0;"
                + "while (i < src) { \n"
                + "  if (++i >= 5) break;"
                + "} \n"
                + "dest = i;";
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.min(val, 5.0);
            }
        };
        
        testScript(script, e);
    }
    
    @Test(expected=JiffleException.class)
    public void breakifStatementOutsideOfLoop() throws Exception {
        System.out.println("   breakif statement outside loop throws exception");
        String script = 
                  "i = 42;\n"
                + "breakif( i == 42 );\n"
                + "dest = i;" ;

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                throw new IllegalStateException("Should not be called");
            }
        };
        
        testScript(script, e);
    }
    
    @Test(expected=JiffleException.class)
    public void breakStatementOutsideOfLoop() throws Exception {
        System.out.println("   break statement outside loop throws exception");
        String script = 
                  "i = 42;\n"
                + "break;\n"
                + "dest = i;" ;

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                throw new IllegalStateException("Should not be called");
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void nestedForEachLoops() throws Exception {
        System.out.println("   nested foreach loops");
        String script = 
                  "n = 0;"
                + "foreach (i in 1:5) { \n"
                + "  foreach (j in i:(i+5)) { \n"
                + "    n += i + j; \n"
                + "  } \n"
                + "} \n"
                + "dest = src + n;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = val;
                for (int i = 1; i <= 5; i++) {
                    for (int j = i; j <= i+5; j++) {
                        z += i + j;
                    }
                }
                return z;
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void foreachLoopWithListVar() throws Exception {
        System.out.println("   using list var in foreach loop");
        String script = 
                  "options {outside = 0;} \n"
                + "foo = [-1, 0, 1]; \n"
                + "z = 0; \n"
                + "foreach (dx in foo) z += src[dx, 0]; \n"
                + "dest = z;";
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = val;
                if (x > 0) z += val - 1;
                if (x < IMG_WIDTH-1) z += val + 1;
                
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void whileLoopExceedMaxIterations() throws Exception {
        System.out.println("   while loop exceeding max iterations");
        String script = "while (true) dest = 0;";
        testScript(script, EXPECTED_EXCEPTION);
    }

    @Test
    public void untilLoopExceedMaxIterations() throws Exception {
        System.out.println("   until loop exceeding max iterations");
        String script = "until (false) dest = 0;";
        testScript(script, EXPECTED_EXCEPTION);
    }

    @Test
    public void foreachLoopExceedMaxIterations() throws Exception {
        System.out.println("   foreach loop exceeding max iterations");
        String script =
                "foreach (i in 0:100) { \n"
                        + "  foreach (j in 0:100) { \n"
                        + "    foreach (k in 0:100) { \n"
                        + "      dest = 0; \n"
                        + "    } \n"
                        + "  } \n"
                        + "}";
        testScript(script, EXPECTED_EXCEPTION);
    }
}
