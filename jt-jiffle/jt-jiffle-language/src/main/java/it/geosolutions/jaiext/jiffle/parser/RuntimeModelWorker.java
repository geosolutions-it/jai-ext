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

import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ASSIGN;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AssignExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AssignmentContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.BodyContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExprStmtContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionListContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.InitBlockContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ScriptContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.StatementContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.VarDeclarationContext;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.geosolutions.jaiext.jiffle.parser.node.BinaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.Break;
import it.geosolutions.jaiext.jiffle.parser.node.BreakIf;
import it.geosolutions.jaiext.jiffle.parser.node.DefaultScalarValue;
import it.geosolutions.jaiext.jiffle.parser.node.Expression;
import it.geosolutions.jaiext.jiffle.parser.node.GlobalVars;
import it.geosolutions.jaiext.jiffle.parser.node.IfElse;
import it.geosolutions.jaiext.jiffle.parser.node.ListAppend;
import it.geosolutions.jaiext.jiffle.parser.node.ListLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInLiteralList;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInRange;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInVariable;
import it.geosolutions.jaiext.jiffle.parser.node.NodeException;
import it.geosolutions.jaiext.jiffle.parser.node.Script;
import it.geosolutions.jaiext.jiffle.parser.node.SetDestValue;
import it.geosolutions.jaiext.jiffle.parser.node.SimpleStatement;
import it.geosolutions.jaiext.jiffle.parser.node.Statement;
import it.geosolutions.jaiext.jiffle.parser.node.StatementList;
import it.geosolutions.jaiext.jiffle.parser.node.Until;
import it.geosolutions.jaiext.jiffle.parser.node.Variable;
import it.geosolutions.jaiext.jiffle.parser.node.While;

/**
 * Generates a Java model representing the script, from which sources can be generated
 *
 * @author michael
 */
public class RuntimeModelWorker extends AbstractModelWorker {

    /**
     * A key for the declared variables set
     */
    private static class VariableKey {
        SymbolScope scope;
        String name;

        public VariableKey(SymbolScope scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VariableKey that = (VariableKey) o;
            return Objects.equals(scope, that.scope) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(scope, name);
        }
    }
    
    private final TreeNodeProperties<JiffleType> types;
    private final TreeNodeProperties<SymbolScope> scopes;
    private final Map<String,String> options;
    private final Set<VariableKey> declaredVariables = new HashSet<>();
    
    // Set to a non-null reference if an init block is found
    private InitBlockContext initBlockContext = null;
    private Script script;

    /**
     * Labels the parse tree with Node objects representing elements
     * of the runtime code. Expects that the tree has been previously
     * annotated by an ExpressionWorker.
     */
    public RuntimeModelWorker(ParseTree tree,
            Map<String, String> options,
            TreeNodeProperties<JiffleType> types,
            TreeNodeProperties<SymbolScope> scopes) {
        
        super(tree);
        this.types = types;
        this.scopes = scopes;
        this.options = options;
        
        walkTree();
    }

    @Override
    public void exitScript(ScriptContext ctx) {
        StatementList stmts = getAsType(ctx.body(), StatementList.class);
        
        GlobalVars globals = initBlockContext == null ?
                new GlobalVars() : getAsType(initBlockContext, GlobalVars.class);
        GlobalScope globalScope = (GlobalScope) scopes.get(ctx);
        Set<String> sourceImages = globalScope.getByType(Symbol.Type.SOURCE_IMAGE);
        Set<String> destImages = globalScope.getByType(Symbol.Type.DEST_IMAGE);

        this.script = new Script(options, sourceImages, destImages, globals, stmts);
        set(ctx, this.script);
    }
    
    @Override
    public void exitBody(BodyContext ctx) {
        List<Statement> statements = new ArrayList<>();
        
        for (StatementContext sctx : ctx.statement()) {
            statements.add( getAsType(sctx, Statement.class) );
        }
        
        set(ctx, new StatementList(statements));
    }

    @Override
    public void exitInitBlock(InitBlockContext ctx) {
        List<BinaryExpression> inits = new ArrayList<>();
        
        List<VarDeclarationContext> decls = ctx.varDeclaration();
        if (decls != null) {
            try {
                for (VarDeclarationContext dc : decls) {
                    String name = dc.ID().getText();
                    ExpressionContext exprCtx = dc.expression();

                    final Expression value;
                    if (exprCtx == null) {
                        value = new DefaultScalarValue();
                    } else {
                        value = getAsType(exprCtx, Expression.class);
                    }

                    inits.add(new BinaryExpression(ASSIGN, new Variable(name, JiffleType.D), value));
                }
                
            } catch (NodeException ex) {
                messages.error(ctx.getStart(), ex.getError());
            }
        }

        set(ctx, new GlobalVars(inits));
        initBlockContext = ctx;
    }

    @Override
    public void exitExprStmt(ExprStmtContext ctx) {
        set(ctx, new SimpleStatement(getAsType(ctx.expression(), Expression.class)));
    }

    @Override
    public void exitExpressionList(ExpressionListContext ctx) {
    }

    @Override
    public void exitAssignExpr(AssignExprContext ctx) {
        set(ctx, get(ctx.assignment()));
    }

    @Override
    public void exitAssignment(AssignmentContext ctx) {
        String varName = ctx.ID().getText();
        SymbolScope scope = getScope(ctx);
        Symbol symbol = scope.get(varName);
        SymbolScope declaringScope = scope.getDeclaringScope(varName);
        boolean declare = checkAndSetDeclared(declaringScope, varName);
        
        int opType = ctx.op.getType();
        
        Expression expr = getAsType(ctx.expression(), Expression.class);
        
        try {
            switch (symbol.getType()) {
                case DEST_IMAGE:
                    set(ctx, new SetDestValue(varName, expr));
                    break;

                case LIST:
                    set(ctx, new BinaryExpression(opType, new Variable(varName, JiffleType.LIST), expr, declare));
                    break;

                case SCALAR:
                    set(ctx, new BinaryExpression(opType, new Variable(varName, JiffleType.D), expr, declare));
                    break;
                    
                default:
                    // Invalid RHS var type. This should have been caught
                    // in an earlier stage.
                    throw new InternalCompilerException("Invalid assignment to " + varName);
            }
        } catch (NodeException ex) {
            messages.error(ctx.getStart(), ex.getError());
        }
    }

    /**
     * Checks if a variable has already been declared in this scope, and if not, marks it as such
     * @param scope
     * @param symbol
     * @return True if the variable still needed to be declared in this scope
     */
    private boolean checkAndSetDeclared(SymbolScope scope, String varName) {
        if (scope instanceof GlobalScope) {
            return false;
        }
        return declaredVariables.add(new VariableKey(scope, varName));
    }

    /*
     * Looks up the inner-most scope for a rule node.
     */
    protected SymbolScope getScope(ParseTree ctx) {
        if (ctx != null) {
            SymbolScope s = scopes.get(ctx);
            return s != null ? s : getScope(ctx.getParent());
            
        } else {
            throw new IllegalStateException(
                    "Compiler error: failed to find symbol scope");
        }
    }

    @Override
    public void exitUntilStmt(JiffleParser.UntilStmtContext ctx) {
        Expression condition = getAsType(ctx.parenExpression().expression(), Expression.class);
        Statement statement = getAsType(ctx.statement(), Statement.class);
        set(ctx, new Until(condition, statement));
    }

    @Override
    public void exitBreakifStmt(JiffleParser.BreakifStmtContext ctx) {
        Expression condition = getAsType(ctx.expression(), Expression.class);
        set(ctx, new BreakIf(condition));
        
    }

    @Override
    public void exitBlock(JiffleParser.BlockContext ctx) {
        List<StatementContext> contexts = ctx.statement();
        List<Statement>  statements = new ArrayList<>();
        for (StatementContext context : contexts) {
            Statement st = getAsType(context, Statement.class);
            statements.add(st);
        }
        
        set(ctx, new StatementList(statements));
    }

    @Override
    public void exitBlockStmt(JiffleParser.BlockStmtContext ctx) {
        set(ctx, get(ctx.block()));
    }

    public Script getScriptNode() {
        return this.script;
    }

    @Override 
    public void exitIfStmt(JiffleParser.IfStmtContext ctx) {
        Expression condition = getAsType(ctx.parenExpression().expression(), Expression.class);
        List<StatementContext> statements = ctx.statement();
        Statement ifBlock = getAsType(statements.get(0), Statement.class);
        Statement elseBlock = null;
        if (statements.size() > 1) {
            elseBlock = getAsType(statements.get(1), Statement.class);
        };
        set(ctx, new IfElse(condition, ifBlock, elseBlock));
    }

    @Override
    public void exitListAppendStmt(JiffleParser.ListAppendStmtContext ctx) {
        String varName = ctx.ID().getText();
        Expression expression = getAsType(ctx.expression(), Expression.class);
        set(ctx, new ListAppend(new Variable(varName, JiffleType.LIST), expression));
    }

    @Override
    public void exitForeachStmt(JiffleParser.ForeachStmtContext ctx) {
        String varName = ctx.ID().getText();
        JiffleParser.RangeContext range = ctx.loopSet().range();
        Variable loopVariable = new Variable(varName, JiffleType.D);
        Statement statement = getAsType(ctx.statement(), Statement.class);
        if (ctx.loopSet().ID() != null) {
            Variable listVariable = new Variable(ctx.loopSet().ID().getText(), JiffleType.LIST);
            set(ctx, new LoopInVariable(loopVariable, listVariable, statement));
        } else  if (range != null) {
            Expression low = getAsType(range.expression(0), Expression.class);
            Expression high = getAsType(range.expression(1), Expression.class);
            set(ctx, new LoopInRange(loopVariable, low, high, statement));
        } else {
            ListLiteral listLiteral = getAsType(ctx.loopSet().listLiteral(), ListLiteral.class);
            set(ctx, new LoopInLiteralList(loopVariable, listLiteral, statement));
        }
        
    }

    @Override
    public void exitBreakStmt(JiffleParser.BreakStmtContext ctx) {
        set(ctx, new Break());
    }

    @Override
    public void exitWhileStmt(JiffleParser.WhileStmtContext ctx) {
        Expression condition = getAsType(ctx.parenExpression().expression(), Expression.class);
        Statement statement = getAsType(ctx.statement(), Statement.class);
        set(ctx, new While(condition, statement));

    }
}
