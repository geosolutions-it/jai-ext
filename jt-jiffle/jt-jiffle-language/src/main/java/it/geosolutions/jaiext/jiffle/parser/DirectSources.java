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
 * Copyright (c) 2018, Michael Bedward. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package it.geosolutions.jaiext.jiffle.parser;

import java.util.Arrays;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;

/**
 *
 * @author michael
 */
public class DirectSources {

    public static String setDestValue(
            Jiffle.RuntimeModel runtimeModel, String destVar, String expr) {
        
        switch (runtimeModel) {
            case DIRECT:
                return String.format("writeToImage(%s, %s, %s)",
                        destVar, ImagePos.DEFAULT, expr);
                
            case INDIRECT:
                return "return " + expr;
                
            default:
                throw new IllegalArgumentException("Invalid runtime model: " + runtimeModel);
        }
    }

    public static String conCall(String ...args) {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("No args provided");
        }
        if (args.length > 4) {
            throw new IllegalArgumentException("Too many args: " + Arrays.toString(args));
        }
        switch (args.length) {
            case 1: return con1(args[0]);
            case 2: return con2(args[0], args[1]);
            case 3: return con3(args[0], args[1], args[2]);
            default: return con4(args[0], args[1], args[2], args[3]);
        }
    }

    private static String con1(String x) {
        return String.format(
                "(_stk.push(_FN.sign(%s)) == null ? "
                + "Double.NaN : (_stk.peek() != 0 ? 1.0 : 0.0))",
                x);
    }

    private static String con2(String x, String a) {
        return String.format(
                "(_stk.push(_FN.sign(%s)) == null ? "
                + "Double.NaN : (_stk.peek() != 0 ? (%s) : 0.0))",
                x, a);
    }
    
    private static String con3(String x, String a, String b) {
        return String.format(
                "(_stk.push(_FN.sign(%s)) == null ? "
                + "Double.NaN : (_stk.peek() != 0 ? (%s) : (%s)))",
                x, a, b);
    }

    private static String con4(String x, String a, String b, String c) {
        return String.format(
                "(_stk.push(_FN.sign(%s)) == null ? "
                + "Double.NaN : (_stk.peek() == 1 ? (%s) : " 
                + "(_stk.peek() == 0 ? (%s) : (%s))))",
                x, a, b, c);
    }

}
