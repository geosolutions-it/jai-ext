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

package it.geosolutions.jaiext.jiffle.parser;

import it.geosolutions.jaiext.jiffle.JiffleException;
import org.junit.Test;

/**
 * Tests basic parsing of scripts with and without init and options blocks.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class HeaderBlockParsingTest extends ParserTestBase {

    @Test
    public void noInitBlock() throws Exception {
        String script =
                "images {result = write;}\n" //
                        + "dest = 42;";
        compileScript(script);
    }

    @Test
    public void emptyInitBlock() throws Exception {
        String script =
                "init { } \n" //
                        + "images {result = write;}\n" //
                        + "dest = 42;";

        compileScript(script);
    }

    @Test
    public void simpleInitBlock() throws Exception {
        String script =
                "init { foo = 42; } \n" //
                        + "images {result = write;}" //
                        + "dest = 42;";

        compileScript(script);
    }

    @Test(expected = JiffleException.class)
    public void misplacedInitBlock() throws Exception {
        String script =
                "dest = 42;" //
                        + "init { foo = 42; } \n";

        compileScript(script);
    }

    @Test
    public void initBlockWithNewLinesAndWhitespace() throws Exception {
        String script =
                "init { \n\n" //
                        + "    n1 = 0; \n\n" //
                        + "    n2 = 42; \n\n" //
                        + "} \n" //
                        + "images {result = write;}" //
                        + "dest = n2 - n1;";

        compileScript(script);
    }

    @Test
    public void noOptionsBlock() throws Exception {
        String script =
                "images {dest = write;}\n" //
                        + "dest = 42;\n";

        compileScript(script);
    }

    @Test
    public void emptyOptionsBlock() throws Exception {
        String script =
                "options { } \n" //
                        + "images {result = write;}" //
                        + "dest = 42;";

        compileScript(script);
    }

    @Test
    public void simpleOptionsBlock() throws Exception {
        String script =
                "options { outside = 0; } \n" //
                        + "images {result = write;}" //
                        + "dest = 42;";

        compileScript(script);
    }

    @Test(expected = JiffleException.class)
    public void misplacedOptionsBlock1() throws Exception {
        String script =
                "dest = 42;" //
                        + "images {result = write;}" //
                        + "options { outside = 0; } \n";

        compileScript(script);
    }

    @Test
    public void optionsBlockWithNewLinesAndWhitespace() throws Exception {
        String script =
                "options { \n\n" //
                        + "    outside = 0; \n\n" //
                        + "} \n" //
                        + "images {result = write;}" //
                        + "dest = 42;";

        compileScript(script);
    }

    @Test
    public void optionsAndInitBlock() throws Exception {
        String script =
                "options { outside = 0; }" //
                        + "images {result = write;}" //
                        + "init { n = 0; }" //
                        + "dest = 42;";

        compileScript(script);
    }
}
