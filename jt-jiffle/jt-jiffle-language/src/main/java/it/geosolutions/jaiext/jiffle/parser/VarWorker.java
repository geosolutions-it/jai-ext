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

import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import it.geosolutions.jaiext.jiffle.Jiffle;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.*;

/**
 * Inspects variables in the parse tree, labels their type and checks 
 * usage.
 * <p>
 * Parts of this code are adapted from 
 * "The Definitive ANTLR 4 Reference" by Terence Parr, 
 * published by The Pragmatic Bookshelf, 2012
 * 
 * @author michael
 */
public class VarWorker extends PropertyWorker<SymbolScope> {

    private final GlobalScope globalScope;
    private SymbolScope currentScope;
    
    
    public VarWorker(
            ParseTree tree, 
            Map<String, Jiffle.ImageRole> imageParams) {
        
        super(tree);
        this.globalScope = new GlobalScope();
        this.currentScope = globalScope;
        
        addImageVarsToGlobal(imageParams);
        walkTree();
        set(tree, globalScope);
    }
    
    private void addImageVarsToGlobal(Map<String, Jiffle.ImageRole> imageParams) {
        for (Map.Entry<String, Jiffle.ImageRole> e : imageParams.entrySet()) {
            String name = e.getKey();
            
            Symbol.Type type = e.getValue() == Jiffle.ImageRole.SOURCE ?
                    Symbol.Type.SOURCE_IMAGE : Symbol.Type.DEST_IMAGE;
            
            globalScope.add(new Symbol(name, type));
        }
    }

    @Override
    public void exitScript(ScriptContext ctx) {
        // All done. Annotate the root tree node.
        set(ctx, globalScope);
    }

    @Override
    public void enterBody(BodyContext ctx) {
        // All variables that first appear in the bocy of the 
        // script should have pixel-level scope.
        pushScope(ctx, "pixel");
    }
    
    @Override
    public void exitBody(BodyContext ctx) {
        popScope();
    }
    
    @Override
    public void exitInitBlock(InitBlockContext ctx) {
        List<VarDeclarationContext> vars = ctx.varDeclaration();
        for (VarDeclarationContext var : vars) {
            Token tok = var.ID().getSymbol();
            String name = tok.getText();
            
            if (isImage(name)) {
                error(tok, Errors.IMAGE_VAR_INIT_BLOCK, name);
            } else if (currentScope.has(name)) {
                error(tok, Errors.DUPLICATE_VAR_DECL, name);
            } else {
                currentScope.add(new Symbol(name, Symbol.Type.UNKNOWN));
            }
        }
        
    }

    @Override
    public void enterBlock(BlockContext ctx) {
        pushScope(ctx, "block");
    }

    @Override
    public void exitBlock(BlockContext ctx) {
        popScope();
    }

    @Override
    public void enterForeachStmt(ForeachStmtContext ctx) {
        // The foreach statement creates its own scope within
        // which the loop variable is defined.
        pushScope(ctx, "foreach");
        
        // Loop var
        Token tok = ctx.ID().getSymbol();
        String name = tok.getText();

        // Loop var is allowed to shadow any vars of the same name
        // in enclosing scopes
        currentScope.add(new Symbol(name, Symbol.Type.LOOP_VAR));
    }
    
    @Override
    public void exitForeachStmt(ForeachStmtContext ctx) {
        popScope();
    }
    
    @Override
    public void exitAssignment(AssignmentContext ctx) {
        Token tok = ctx.ID().getSymbol();

        if (isValidAssignment(ctx)) {
            String name = tok.getText();

            // Add the symbol if this is its first appearance.
            // We won't know its type yet - could be scalar or list.
            if (!currentScope.has(name)) {
                currentScope.add(new Symbol(name, Symbol.Type.UNKNOWN));
            }
        }
    }
    
    @Override
    public void exitVarID(VarIDContext ctx) {
        Token tok = ctx.ID().getSymbol();
        String name = tok.getText();
        
        if (!currentScope.has(name)) {
             // To get here we must be processing the RHS of
             // an expression, so any vars should be defined.
            error(tok, Errors.VAR_UNDEFINED, name);
        } else if (isDestImage(name)) {
            error(tok, Errors.READING_FROM_DEST_IMAGE, name);
        }
    }

    @Override
    public void exitListLiteral(ListLiteralContext ctx) {
        super.exitListLiteral(ctx);
    }

    
    private boolean isValidAssignment(AssignmentContext ctx) {
        Token tok = ctx.ID().getSymbol();
        String name = tok.getText();
        
        // Short-cut: scalar that is already defined is OK
        if (currentScope.has(name) 
                && currentScope.get(name).getType() == Symbol.Type.SCALAR) {
            return true;
        }
    
        if (isLoopVar(name)) {
            // Trying to assign to a loop var within loop scope
            error(tok, Errors.ASSIGNMENT_TO_LOOP_VAR, name);
        } else if (isSourceImage(name)) {
            // Trying to write to a source image
            error(tok, Errors.WRITING_TO_SOURCE_IMAGE, name);
            return false;
        } else if (isDestImage(name) && ctx.ASSIGN() == null) {
            // Using operator other than simple assignment (=) with 
            // destination image
            error(tok, Errors.INVALID_ASSIGNMENT_OP_WITH_DEST_IMAGE, name);
            return false;
        } else if (ConstantLookup.isDefined(name)) {
            // Trying to write to a built-in constant
            error(tok, Errors.ASSIGNMENT_TO_CONSTANT, name);
            return false;
        }
        
        return true;
    }
    

    /*
     * Pushes a new scope and sets it as a property of the parse
     * tree node.
     */
    private void pushScope(ParseTree ctx, String newScopeLabel) {
        SymbolScope scope = new LocalScope(newScopeLabel, currentScope);
        set(ctx, scope);
        currentScope = scope;
    }
    
    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
    }
    
    private void error(Token tok, Errors error, String varName) {
        messages.error(tok, error + ": " + varName);
    }
    
    private boolean isImage(String name) { 
        return isSourceImage(name) || isDestImage(name);
    }
    
    private boolean isSourceImage(String name) { 
        if (globalScope.has(name)) {
            return globalScope.get(name).getType() == Symbol.Type.SOURCE_IMAGE;
        }
        return false;
    }
    
    private boolean isDestImage(String name) { 
        if (globalScope.has(name)) {
            return globalScope.get(name).getType() == Symbol.Type.DEST_IMAGE;
        }
        return false;
    }
    
    private boolean isLoopVar(String name) {
        if (currentScope.has(name)) {
            return currentScope.get(name).getType() == Symbol.Type.LOOP_VAR;
        }
        return false;
    }
    
}
