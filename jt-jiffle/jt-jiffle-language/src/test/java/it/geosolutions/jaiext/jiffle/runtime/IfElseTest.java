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
 * Unit tests for if-else statements.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class IfElseTest extends RuntimeTestBase {
    
    @Test
    public void ifWithExpression() throws Exception {
        System.out.println("   if statement with simple expression");
        String script = String.format(
                  "z = 0; \n"
                + "if (src > %d) z = 1; \n"
                + "dest = z;", NUM_PIXELS / 2);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val > NUM_PIXELS / 2 ? 1.0 : 0.0);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void ifWithBlock() throws Exception {
        System.out.println("   if statement with block");
        String script = String.format(
                  "z = 0; \n"
                + "if (src > %d) { \n"
                + "  z = src; \n"
                + "  z = z * z; \n"
                + "}\n"
                + "dest = z;", NUM_PIXELS / 2);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val > NUM_PIXELS / 2 ? (val*val) : 0.0);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void ifElseWithExpression() throws Exception {
        System.out.println("   if-else statement with simple expressions");
        String script = String.format(
                  "z = 0; \n"
                + "if (src < %d) \n"
                + "  z = 1; \n"
                + "else \n"
                + "  z = 2; \n"
                + "dest = z;", NUM_PIXELS / 2);

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val < NUM_PIXELS / 2 ? 1.0 : 2.0);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void ifElseWithBlocks() throws Exception {
        System.out.println("   if-else statement with blocks");
        String script = String.format(
                  "z = 0; \n"
                + "if (src < %d) { \n"
                + "  z = src; \n"
                + "  z = 2 * z; \n"
                + "} \n"
                + "else { \n"
                + "  z = src; \n"
                + "  z = 4 * z; \n"
                + "} \n"
                + "dest = z;", NUM_PIXELS / 2);

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val < NUM_PIXELS / 2 ? 2*val : 4*val);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void nestedIf() throws Exception {
        System.out.println("   nested if statement");
        String script = String.format(
                  "z = 0; \n"
                + "if (src < %d) { \n"
                + "  if (src < %d)"
                + "     z = 1; \n"
                + "  else \n"
                + "     z = 2;"
                + "}"
                + "dest = z;", NUM_PIXELS / 2, NUM_PIXELS / 4);

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val < NUM_PIXELS / 4 ? 1 : (val < NUM_PIXELS / 2) ? 2 : 0);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void ifTreatsNullAsFalse() throws Exception {
        System.out.println("   if statement treats null as false");
        String script = 
                  "val = con(src % 2, 1, null); \n"
                + "if (val) dest = 1; \n"
                + "else dest = 0;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                if (((int)val) % 2 == 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        
        testScript(script, e);
    }
    
}
