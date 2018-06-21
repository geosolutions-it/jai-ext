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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

public class ExpressionWorkerTest extends AbstractWorkerTest<ExpressionWorker> {

    @Test
    public void testValid() throws Exception {
        Pair<ParseTree, ExpressionWorker> pair = parseFileAndDoWork("ValidScript.jfl");
        ExpressionWorker worker = pair.b;
        assertFalse(worker.messages.isError());
    }

    @Test
    public void testAssignListToScalar() throws Exception {
        assertFileHasError("AssignListToScalar.jfl", Errors.ASSIGNMENT_LIST_TO_SCALAR);
    }

    @Test
    public void testAssignScalarToList() throws Exception {
        assertFileHasError("AssignScalarToList.jfl", Errors.ASSIGNMENT_SCALAR_TO_LIST);
    }

    @Test
    public void testCombinedAssignToList() throws Exception {
        String template = "list = [1, 2, 3];\n" +
                "list <op> 3;";
        // invalid
        String[] operators = new String[] {"+=", "-=", "*=", "/=", "%="};
        for (String op : operators) {
            String script = template.replace("<op>", op);
            LOGGER.info("Testing operator " + op);
            assertScriptHasError(script, Errors.INVALID_OPERATION_FOR_LIST);    
        }
        // valid
        assertScriptHasNoErrors(template.replace("<op>", "="));
    }

    @Test
    public void testListInTernary() throws Exception {
        assertFileHasError("ListInTernary.jfl", Errors.LIST_AS_TERNARY_CONDITION);
    }

    @Test
    public void testListInTernaryComparison() throws Exception {
        assertFileHasError("ListInTernaryComparison.jfl", Errors.LIST_AS_TERNARY_CONDITION);
    }

    @Test
    public void testScalarInTernary() throws Exception {
        assertFileHasNoErrors("ScalarInTernary.jfl");
    }

    @Test
    public void testUninitializedVariable() throws Exception {
        assertFileHasError("UninitializedVariable.jfl", Errors.UNINIT_VAR + ": b");
    }

    @Test
    public void testUndefinedFunctionUse() throws Exception {
        String script = "x = 1; myVar = func123(x);";
        assertScriptHasError(script, "Undefined function: func123(D)");
    }

    @Test
    public void testInvalidScalarFunctionArgument() throws Exception {
        String script = "myVar = sin([1, 2, 3]);";
        assertScriptHasError(script, "Undefined function: sin(LIST)");
    }

    @Test
    public void testAssignListFunctionToScalar() throws Exception {
        String script = "var = 1; var = concat([1, 2, 3], [4, 5]);";
        assertScriptHasError(script, Errors.ASSIGNMENT_LIST_TO_SCALAR);
    }

    @Test
    public void testAssignScalarFunctionToList() throws Exception {
        String script = "var = [1, 2, 3]; var = sin(1.35);";
        assertScriptHasError(script, Errors.ASSIGNMENT_SCALAR_TO_LIST);
    }
    
    @Override
    protected Pair<ParseTree, ExpressionWorker> runWorker(ParseTree tree)
            throws Exception {
        ImagesBlockWorker ibw = new ImagesBlockWorker(tree);
        VarWorker vw = new VarWorker(tree, ibw.imageVars);
        ExpressionWorker ew = new ExpressionWorker(tree, vw);
        return new Pair(tree, ew);
    }

    @Test
    public void imagePosOnNonImage() throws Exception {
        assertFileHasError(
                "ImagePosOnNonImage.jfl",
                Errors.IMAGE_POS_ON_NON_IMAGE + ": x");
    }
}
