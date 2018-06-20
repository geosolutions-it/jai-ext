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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.Matchers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for worker tests
 * @param <T> The worker type
 */
public abstract class AbstractWorkerTest<T extends BaseWorker> {
    final Logger LOGGER = Logger.getLogger(getClass().getName());

    /**
     * Checks the script has no errors
     *
     * @param scriptFileName
     * @param error
     * @throws Exception
     */
    protected void assertFileHasNoErrors(String scriptFileName) throws Exception {
        Pair<ParseTree, T> result = parseFileAndDoWork(scriptFileName);
        assertThat(result.b.messages, Matchers.hasProperty("error", equalTo(false)));
    }

    /**
     * Checks the script has the expected error type (among others)
     *
     * @param scriptFileName
     * @param errorMessage
     * @throws Exception
     */
    protected void assertFileHasError(String scriptFileName, Errors errorMessage) throws Exception {
        Pair<ParseTree, T> result = parseFileAndDoWork(scriptFileName);
        assertWorkerHasError(result.b, errorMessage.toString());
    }


    /**
     * Checks the script has the expected error type (among others)
     * 
     * @param scriptFileName
     * @param errorMessage
     * @throws Exception
     */
    protected void assertFileHasError(String scriptFileName, String errorMessage) throws Exception {
        Pair<ParseTree, T> result = parseFileAndDoWork(scriptFileName);
        assertWorkerHasError(result.b, errorMessage);
    }

    /**
     * Checks the script has no errors
     *
     * @param scriptFileName
     * @param error
     * @throws Exception
     */
    protected void assertScriptHasNoErrors(String script) throws Exception {
        Pair<ParseTree, T> result = parseStringAndDoWork(script);
        assertThat(result.b.messages, Matchers.hasProperty("error", equalTo(false)));
    }

    /**
     * Checks the script has the expected error type (among others)
     *
     * @param errorMesssage
     * @param scriptFileName
     * @throws Exception
     */
    protected void assertScriptHasError(String script, Errors errorMesssage) throws Exception {
        Pair<ParseTree, T> result = parseStringAndDoWork(script);
        assertWorkerHasError(result.b, errorMesssage.toString());
    }

    /**
     * Checks the script has the expected error type (among others)
     *
     * @param errorMesssage
     * @param scriptFileName
     * @throws Exception
     */
    protected void assertScriptHasError(String script, String errorMesssage) throws Exception {
        Pair<ParseTree, T> result = parseStringAndDoWork(script);
        assertWorkerHasError(result.b, errorMesssage);
    }

    protected Pair<ParseTree, T> parseFileAndDoWork(String scriptFileName) throws Exception {
        InputStream input = getClass().getResourceAsStream(scriptFileName);
        ParseTree tree = ParseHelper.parse(input);
        
        return runWorker(tree);
    }

    protected Pair<ParseTree, T> parseStringAndDoWork(String script) throws Exception {
        ParseTree tree = ParseHelper.parse(script);

        return runWorker(tree);
    }

    protected abstract Pair<ParseTree,T> runWorker(ParseTree tree) throws Exception;

    protected void assertWorkerHasError(BaseWorker worker, String errorMessage) {
        List<String> errors = new ArrayList<>();
        for (Message cm : worker.messages.getMessages()) {
            LOGGER.info(cm.msg);

            if (cm.level == Message.Level.ERROR) {
                errors.add(cm.msg);
            }
        }

        assertThat(errors, hasItems(equalTo(errorMessage)));
    }
}
