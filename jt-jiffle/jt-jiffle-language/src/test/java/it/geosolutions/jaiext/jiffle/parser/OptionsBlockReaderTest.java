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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the options block parser.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class OptionsBlockReaderTest extends ParserTestBase {
    private String script;
    private Messages msgTable;
    private Map<String, String> options;
    private Map<String, String> expectedOptions;

    @Before
    public void setup() {
        expectedOptions = new HashMap<>();
    }

    @Test
    public void simpleBlock() throws Exception {
        script = "options { outside = 0; } dest = 42;";

        parseOptions(script);
        assertMessages();
        expectedOptions.put("outside", "0");
        assertOptions();
    }

    @Test
    public void blockWithNewLines() throws Exception {
        script = "options { \n" + "  outside = 0; \n" + "} \n" + "dest = 42;";

        parseOptions(script);
        assertMessages();
        expectedOptions.put("outside", "0");
        assertOptions();
    }

    @Test
    public void emptyBlock() throws Exception {
        script = "options { } dest = 42;";

        parseOptions(script);
        assertMessages();
        assertOptions();
    }

    @Test
    public void outsideNull() throws Exception {
        script = "options { outside = null; } dest = 42;";

        parseOptions(script);
        assertMessages();
        expectedOptions.put("outside", "Double.NaN");
        assertOptions();
    }

    @Test
    public void outsideNaN() throws Exception {
        script = "options { outside = NaN; } dest = 42;";

        parseOptions(script);
        assertMessages();
        expectedOptions.put("outside", "Double.NaN");
        assertOptions();
    }

    @Test
    public void invalidOptionName() throws Exception {
        script = "options { foo = 0; } dest = 42;";

        parseOptions(script);
        assertMessages(new CompilerMessage(Message.Level.ERROR, 1, 11, "Unknown option foo"));
    }

    @Test
    public void invalidOutsideOptionValue() throws Exception {
        script = "options { outside = foo; } dest = 42;";

        parseOptions(script);
        assertMessages(
                new CompilerMessage(
                        Message.Level.ERROR, 1, 11, "Invalid value (foo) for option outside"));
    }

    @Test
    public void constantUsage() throws Exception {
        script = "options { outside = M_PI; } dest = 42;";

        parseOptions(script);
        assertMessages();
        expectedOptions.put("outside", Double.toString(Math.PI));
        assertOptions();
    }

    private void assertOptions() {
        assertThat(options, CoreMatchers.equalTo(expectedOptions));
    }

    private void assertMessages(Message... expectedMessages) {
        if (expectedMessages == null) {
            assertTrue(
                    "Expected no messages but found: " + msgTable,
                    msgTable.getMessages().isEmpty());
        }
        assertThat(msgTable.getMessages(), CoreMatchers.hasItems(expectedMessages));
    }

    private void parseOptions(String script) throws Exception {
        ParseTree tree = getParseTree(script, parser -> parser.optionsBlock());
        OptionsBlockWorker reader = new OptionsBlockWorker(tree);
        this.msgTable = reader.messages;
        this.options = reader.options;
    }
}
