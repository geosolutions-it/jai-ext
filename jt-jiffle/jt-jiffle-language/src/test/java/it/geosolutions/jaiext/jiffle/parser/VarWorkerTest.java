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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class VarWorkerTest extends AbstractWorkerTest<VarWorker> {

    @Test
    public void scriptNodeIsAnnotatedWithGlobalScope() throws Exception {
        Pair<ParseTree, VarWorker> result = parseFileAndDoWork("ValidScript.jfl");

        ParseTree tree = result.a;
        VarWorker worker = result.b;

        SymbolScope scope = worker.getProperties().get(tree);
        assertThat(scope, instanceOf(GlobalScope.class));
    }

    @Test
    public void imageVarsInGlobalScope() throws Exception {
        Pair<ParseTree, VarWorker> result = parseFileAndDoWork("ValidScript.jfl");

        ParseTree tree = result.a;
        VarWorker worker = result.b;
        assertFalse(worker.messages.isError());

        SymbolScope scope = worker.getProperties().get(tree);

        String[] names = { "src", "dest" };
        Symbol.Type[] types = { Symbol.Type.SOURCE_IMAGE, Symbol.Type.DEST_IMAGE };

        int i = 0;
        for (String name : names) {
            assertTrue( scope.has(name) );
            assertTrue( scope.get(name).getType() == types[i++] );
        }
    }

    @Test
    public void initBlockVarsInGlobalScope() throws Exception {
        Pair<ParseTree, VarWorker> result = parseFileAndDoWork("InitBlockFooBar.jfl");

        ParseTree tree = result.a;
        VarWorker worker = result.b;
        assertFalse(worker.messages.isError());

        SymbolScope scope = worker.getProperties().get(tree);

        assertTrue( scope.has("foo") );
        assertTrue( scope.has("bar") );
    }

    @Test
    public void initBlockWithDuplicateVar() throws Exception {
        assertFileHasError(
                "InitBlockDuplicateVar.jfl",
                Errors.DUPLICATE_VAR_DECL + ": foo");
    }

    @Test
    public void initBlockWithImageVarOnLHS() throws Exception {
        assertFileHasError(
                "InitBlockImageVarLHS.jfl",
                Errors.IMAGE_VAR_INIT_BLOCK + ": src");
    }

    @Test
    public void readingFromDestinationImage() throws Exception {
        assertFileHasError(
                "ReadingFromDestImage.jfl",
                Errors.READING_FROM_DEST_IMAGE + ": dest");
    }

    @Test
    public void writingToSourceImage() throws Exception {
        assertFileHasError(
                "WritingToSourceImage.jfl",
                Errors.WRITING_TO_SOURCE_IMAGE + ": src");
    }

    @Test
    public void assignmentToConstant() throws Exception {
        assertFileHasError(
                "AssignmentToConstant.jfl",
                Errors.ASSIGNMENT_TO_CONSTANT + ": M_PI");
    }

    @Test
    public void assignmentToLoopVar() throws Exception {
        assertFileHasError(
                "AssignmentToLoopVar.jfl",
                Errors.ASSIGNMENT_TO_LOOP_VAR + ": i");
    }

    @Test
    public void invalidAssignmentOpForDestinationImage() throws Exception {
        assertFileHasError(
                "InvalidAssignmentOpForDestinationImage.jfl",
                Errors.INVALID_ASSIGNMENT_OP_WITH_DEST_IMAGE + ": dest");
    }

    @Test
    public void undefinedVariable() throws Exception {
        assertFileHasError(
                "UndefinedVariable.jfl",
                "Variable not initialized prior to use: b");
    }

    protected Pair<ParseTree, VarWorker> runWorker(ParseTree tree) throws Exception {
        ImagesBlockWorker ib = new ImagesBlockWorker(tree);
        return new Pair(tree, new VarWorker(tree, ib.imageVars));
    }
    
    @Test
    public void arrayIndexAssignment() throws Exception {
        assertFileHasError(
                "listArrayAssignment.jfl",
                "var[x] assignment can only be performed on the output image variable: list");
    }

}
