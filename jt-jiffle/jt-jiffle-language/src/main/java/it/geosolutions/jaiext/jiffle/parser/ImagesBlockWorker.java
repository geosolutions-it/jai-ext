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
 *  Copyright (c) 2013, Michael Bedward. All rights reserved. 
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

import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImageVarDeclarationContext;
import it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImagesBlockContext;

/**
 * /**
 * Collects image var declarations from the script image block (if present).
 *
 * @author michael
 */
public class ImagesBlockWorker extends BaseWorker {

    public final Map<String, Jiffle.ImageRole> imageVars;
    private boolean readBlock = false;

    /**
     * Creates a new reader which immediately walks the given tree.
     *
     * @param tree a parse tree
     */
    public ImagesBlockWorker(ParseTree tree) {
        super(tree);
        imageVars = new HashMap<String, Jiffle.ImageRole>();
        walkTree();
    }

    @Override
    public void enterImagesBlock(ImagesBlockContext ctx) {
        if (readBlock) {
            messages.error(ctx.start, "Script has more than one image block");
        }
    }

    @Override
    public void exitImagesBlock(ImagesBlockContext ctx) {
        if (!readBlock) {
            for (ImageVarDeclarationContext imgDecl : ctx.imageVarDeclaration()) {
                String imageName = imgDecl.ID().getText();

                switch (imgDecl.role().getStart().getType()) {
                    case JiffleParser.READ:
                        imageVars.put(imageName, Jiffle.ImageRole.SOURCE);
                        break;

                    case JiffleParser.WRITE:
                        imageVars.put(imageName, Jiffle.ImageRole.DEST);
                        break;

                    default:
                        Token tok = imgDecl.role().getStart();
                        messages.error(tok, "Invalid image var type ("
                                + tok.getText()
                                + "). Should be read or write.");
                }
            }
            readBlock = true;
        }
    }
}
