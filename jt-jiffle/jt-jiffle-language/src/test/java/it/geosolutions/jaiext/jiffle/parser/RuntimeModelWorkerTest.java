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

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.janino.SimpleCompiler;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.Jiffle.RuntimeModel;
import it.geosolutions.jaiext.jiffle.parser.node.Script;
import it.geosolutions.jaiext.jiffle.parser.node.SourceWriter;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

/**
 *
 * @author michael
 */
public class RuntimeModelWorkerTest {
    
    @Test
    public void mandelbrot() throws Exception {
        assertGeneratedSource("mandelbrot.jfl", RuntimeModel.DIRECT, "result");
        assertGeneratedSource("mandelbrot.jfl", RuntimeModel.INDIRECT, "result");
    }

    @Test
    public void interference() throws Exception {
        assertGeneratedSource("interference.jfl", RuntimeModel.DIRECT, "result");
    }

    @Test
    public void ripple() throws Exception {
        assertGeneratedSource("ripple.jfl", RuntimeModel.DIRECT, "result");
    }

    @Test
    public void squircle() throws Exception {
        assertGeneratedSource("squircle.jfl", RuntimeModel.DIRECT, "result");
    }

    @Test
    public void chessboard() throws Exception {
        assertGeneratedSource("chessboard.jfl", RuntimeModel.DIRECT, "result");
    }

    @Test
    public void lifeEdges() throws Exception {
        assertGeneratedSource("life-edges.jfl", RuntimeModel.DIRECT, "nextworld", "world");
    }

    @Test
    public void lifeToroid() throws Exception {
        assertGeneratedSource("life-toroid.jfl", RuntimeModel.DIRECT, "nextworld", "world");
    }

    @Test
    public void aspect() throws Exception {
        assertGeneratedSource("aspect.jfl", RuntimeModel.DIRECT, "result", "dtm");
        // this one is interesting, has multiple assignements to the output
        assertGeneratedSource("aspect.jfl", RuntimeModel.INDIRECT, "result", "dtm");
    }

    @Test
    public void flow() throws Exception {
        assertGeneratedSource("flow.jfl", RuntimeModel.DIRECT, "result", "dtm");
    }

    @Test
    public void ndvi() throws Exception {
        assertGeneratedSource("ndvi.jfl", RuntimeModel.DIRECT, "res", "nir", "red");
    }

    private void assertGeneratedSource(String scriptFileName, RuntimeModel model) throws Exception {
        assertGeneratedSource(scriptFileName, model, null);
    }
    
    private void assertGeneratedSource(String scriptFileName, RuntimeModel model, String outputName, String... inputNames) throws Exception {
        InputStream input = getClass().getResourceAsStream(scriptFileName);
        ParseTree tree = ParseHelper.parse(input);
        
        ImagesBlockWorker ibw = new ImagesBlockWorker(tree);
        
        // set input and outputs if missing from script
        if (outputName != null) {
            ibw.imageVars.put(outputName, Jiffle.ImageRole.DEST);
        }
        if (inputNames != null) {
            for (String inputName : inputNames) {
                ibw.imageVars.put(inputName, Jiffle.ImageRole.SOURCE);
            }
        }
        
        OptionsBlockWorker ow = new OptionsBlockWorker(tree);
        VarWorker vw = new VarWorker(tree, ibw.imageVars);
        ExpressionWorker ew = new ExpressionWorker(tree, vw);
        
        RuntimeModelWorker rsw = new RuntimeModelWorker(tree, ow.options, ew.getProperties(), ew.getScopes());
        
        Script script = rsw.getScriptNode();
        SourceWriter writer = new SourceWriter(model);
        script.write(writer);

        String referenceName = FilenameUtils.getBaseName(scriptFileName) + "-" + model + ".java";
        String generatedSource = writer.getSource();
        SourceAssert.compare(new File("./src/test/resources/reference", referenceName),
                generatedSource);
        
        // make sure it compiles too
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(generatedSource);

    }
    
}
