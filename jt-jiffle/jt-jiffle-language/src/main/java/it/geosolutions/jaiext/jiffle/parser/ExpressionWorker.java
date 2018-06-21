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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.parser.JiffleParser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Labels expression nodes with their Jiffle types. Expects that
 * the parse tree has been previously annotated by a VarWorker.
 * 
 * @author michael
 */
public class ExpressionWorker extends PropertyWorker<JiffleType> {
    private final TreeNodeProperties<SymbolScope> scopes;
    private final SymbolScope globalScope;

    public ExpressionWorker(ParseTree tree, VarWorker vw) {
        super(tree);
        this.scopes = vw.getProperties();
        this.globalScope = this.scopes.get(tree);
        walkTree();
    }

    /**
     * Gets the scope annotations, which may have been modified when
     * this worker walked the parse tree.
     */
    public TreeNodeProperties<SymbolScope> getScopes() {
        return scopes;
    }
    
    @Override
    public void exitRange(RangeContext ctx) {
        // both left and right must be scalar
        JiffleType left = get(ctx.expression(0));
        JiffleType right = get(ctx.expression(1));
        if (left != JiffleType.D || right != JiffleType.D) {
            messages.error(ctx.getStart(), Errors.LIST_IN_RANGE);
        }
        set(ctx, JiffleType.LIST);
    }
    
    @Override
    public void exitAtomExpr(AtomExprContext ctx) {
        set(ctx, get(ctx.atom()));
    }

    @Override
    public void exitPowExpr(PowExprContext ctx) {
        JiffleType arg = get(ctx.expression(0));
        JiffleType exponent = get(ctx.expression(1));
        
        // exponent type must be scalar
        if (exponent != JiffleType.D) {
            messages.error(ctx.getStart(), Errors.POW_EXPR_WITH_LIST_EXPONENT);
        }
        
        // result type will be that of the arg
        set(ctx, arg);
    }

    @Override
    public void exitPostExpr(PostExprContext ctx) {
        set(ctx, get(ctx.expression()));
    }
    
    @Override
    public void exitPreExpr(PreExprContext ctx) {
        set(ctx, get(ctx.expression()));
    }
    
    @Override
    public void exitNotExpr(NotExprContext ctx) {
        set(ctx, get(ctx.expression()));
    }
    
    @Override
    public void exitTimesDivModExpr(TimesDivModExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }
    
    @Override
    public void exitPlusMinusExpr(PlusMinusExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }
    
    @Override
    public void exitCompareExpr(CompareExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public void exitEqExpr(EqExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public void exitAndExpr(AndExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public void exitOrExpr(OrExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public void exitXorExpr(XorExprContext ctx) {
        setBinaryExprType(ctx, ctx.expression(0), ctx.expression(1));
    }

    @Override
    public void exitTernaryExpr(TernaryExprContext ctx) {
        // first expression (condition) must be scalar (true / false)
        JiffleType condType = get(ctx.expression(0));
        if (condType != JiffleType.D) {
            messages.error(ctx.getStart(), Errors.LIST_AS_TERNARY_CONDITION);
        }
        setBinaryExprType(ctx, ctx.expression(1), ctx.expression(2));
    }

    @Override
    public void exitAssignExpr(AssignExprContext ctx) {
        set(ctx, get(ctx.assignment()));
    }
    
    private void setBinaryExprType(ParseTree ctx, 
            ExpressionContext left, ExpressionContext right) {
        
        JiffleType leftType = get(left);
        JiffleType rightType = get(right);
        
        if (leftType == rightType) {
            // single type expression so result is same type
            set(ctx, rightType);
        } else {
            // if different, they must be scalar and list so
            // result is list
            set(ctx, JiffleType.LIST);
        }
    }

    protected JiffleType get(ExpressionContext ctx) {
        if (ctx instanceof AtomExprContext) {
            AtomExprContext atom = (AtomExprContext) ctx;
            IdentifiedAtomContext identifiedAtomContext = atom.atom().identifiedAtom();
            if (identifiedAtomContext instanceof VarIDContext) {
                VarIDContext var = (VarIDContext) identifiedAtomContext;
                String varName = var.ID().getSymbol().getText();
                // is it a wel known constant?
                if (ConstantLookup.isDefined(varName)) {
                    return JiffleType.D;
                }
                // otherwise search among variables
                Symbol symbol = getScope(ctx).get(varName);
                if (symbol.getType() == null || symbol.getType() == Symbol.Type.UNKNOWN) {
                    return JiffleType.UNKNOWN;
                } else if (symbol.getType() == Symbol.Type.LIST) {
                    return JiffleType.LIST;
                } else {
                    return JiffleType.D; // variable or source image                    
                }
            }
        }
        return super.get(ctx);
    }

    @Override
    public void exitAssignment(AssignmentContext ctx) {
        JiffleType rhsType = get(ctx.expression());
        set(ctx, rhsType);
        
        // Ensure valid type for LHS variable
        String name = ctx.ID().getText();
        SymbolScope scope = getScope(ctx);
        Symbol symbol = scope.get(name);
        
        if (symbol.getType() == Symbol.Type.UNKNOWN) {
            // Symbol was of unknown type.
            // Replace it with a properly typed one.
            //
            Symbol.Type stype = getSymbolType(rhsType);
            
            scope.add(new Symbol(name, stype), true);
            
        } else {
            // Symbol was of known type.
            // Ensure it is compatible with RHS expression type.
            //
            switch (symbol.getType()) {
                case SCALAR:
                case DEST_IMAGE:
                    if (rhsType == JiffleType.LIST) {
                        messages.error(ctx.ID().getSymbol(), Errors.ASSIGNMENT_LIST_TO_SCALAR);
                    }
                    break;
                    
                case LIST:
                    if (JiffleParser.ASSIGN != ctx.op.getType()) {
                        messages.error(ctx.ID().getSymbol(), Errors.INVALID_OPERATION_FOR_LIST);
                    } else if (rhsType == JiffleType.D) {
                        messages.error(ctx.ID().getSymbol(), Errors.ASSIGNMENT_SCALAR_TO_LIST);
                    }
                    break;
            }
        }
    }

    /**
     * This method is a copy of exitAssignment, unfortunately AssignmentContext
     * and VarDeclarationContext share structure but not a base class
     * @param ctx
     */
    @Override
    public void exitVarDeclaration(VarDeclarationContext ctx) {
        JiffleType rhsType = get(ctx.expression());
        set(ctx, rhsType);

        // Ensure valid type for LHS variable
        String name = ctx.ID().getText();
        SymbolScope scope = getScope(ctx);
        Symbol symbol = scope.get(name);

        if (symbol.getType() == Symbol.Type.UNKNOWN) {
            // Symbol was of unknown type.
            // Replace it with a properly typed one.
            //
            Symbol.Type stype = getSymbolType(rhsType);

            scope.add(new Symbol(name, stype), true);

        } else {
            // Symbol was of known type.
            // Ensure it is compatible with RHS expression type.
            //
            switch (symbol.getType()) {
                case SCALAR:
                case DEST_IMAGE:
                    if (rhsType == JiffleType.LIST) {
                        messages.error(ctx.ID().getSymbol(), Errors.ASSIGNMENT_LIST_TO_SCALAR);
                    }
                    break;

                case LIST:
                    if (rhsType == JiffleType.D) {
                        messages.error(ctx.ID().getSymbol(), Errors.ASSIGNMENT_SCALAR_TO_LIST);
                    }
                    break;
            }
        }
    }

    private Symbol.Type getSymbolType(JiffleType type) {
        if (type == null || type == JiffleType.UNKNOWN) {
            return Symbol.Type.UNKNOWN;
        } else if (type == JiffleType.D) {
            return Symbol.Type.SCALAR;
        } else if (type == JiffleType.LIST) {
            return Symbol.Type.LIST;
        } else {
            throw new IllegalArgumentException("Symbol type unknown: " + type);
        }
    }

    @Override
    public void exitAtom(AtomContext ctx) {
        set(ctx, get(ctx.getChild(0)));
    }
    
    @Override
    public void exitParenExpression(ParenExpressionContext ctx) {
        set(ctx, get(ctx.expression()));
    }

    @Override
    public void exitConCall(ConCallContext ctx) {
        set(ctx, JiffleType.D);
    }

    @Override
    public void exitVarID(VarIDContext ctx) {
        // We should be processing the RHS of an expression to be here
        String name = ctx.ID().getText();
        
        // is this a constant reference or a variable reference?
        if (ConstantLookup.isDefined(name)) {
            set(ctx, JiffleType.D);
        } else {
            SymbolScope scope = getScope(ctx);

            // TODO - temp debug
            Symbol symbol = scope.get(name);

            switch (symbol.getType()) {
                case LIST:
                    set(ctx, JiffleType.LIST);
                    break;

                default:
                    set(ctx, JiffleType.UNKNOWN);
                    break;
            }

            Symbol.Type type = scope.get(name).getType();
            if (type == Symbol.Type.UNKNOWN) {
                messages.error(ctx.ID().getSymbol(), Errors.UNINIT_VAR + ": " + name);
            } 
        }
    }
    
    @Override
    public void exitImageCall(ImageCallContext ctx) {
        String name = ctx.ID().getSymbol().getText();
        Symbol symbol = globalScope.get(name);
        if (symbol == null || symbol.getType() == Symbol.Type.UNKNOWN) {
            messages.error(ctx.ID().getSymbol(), Errors.UNDEFINED_SOURCE + ": " + name);
        } else if (symbol.getType() == Symbol.Type.SCALAR || symbol.getType() == Symbol.Type.LIST) {
            messages.error(ctx.ID().getSymbol(), Errors.IMAGE_POS_ON_NON_IMAGE + ": " + name);
        } 
        set(ctx, JiffleType.D);
    }
    
    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        String name = ctx.ID().getText();
        try {
            // do a lookup with full type information
            ExpressionListContext expressionList = ctx.argumentList().expressionList();
            List<JiffleType> argumentTypes = new ArrayList<>();
            if (expressionList != null) {
                List<ExpressionContext> expressions = expressionList.expression();
                for (ExpressionContext expression : expressions) {
                    JiffleType type = get(expression);
                    argumentTypes.add(type);
                }
            }
            JiffleType[] array = argumentTypes.toArray(new JiffleType[argumentTypes.size()]);
            set( ctx, FunctionLookup.getInfo(name, array).getReturnType());
        } catch (UndefinedFunctionException ex) {
            messages.error(ctx.ID().getSymbol(), ex.getMessage());
        }
    }
    
    

    @Override
    public void exitLiteral(LiteralContext ctx) {
        // All plain literals are scalar values
        set(ctx, JiffleType.D);
    }

    @Override
    public void exitListLiteral(ListLiteralContext ctx) {
        set(ctx, JiffleType.LIST);
    }

    

    /*
     * Looks up the inner-most scope for a rule node.
     */
    private SymbolScope getScope(ParseTree ctx) {
        if (ctx != null) {
            SymbolScope s = scopes.get(ctx);
            return s != null ? s : getScope(ctx.getParent());
            
        } else {
            throw new IllegalStateException(
                    "Compiler error: failed to find symbol scope");
        }
    }

}
