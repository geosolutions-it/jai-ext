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
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AndExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AssignExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AssignmentContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AtomContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AtomExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.BandSpecifierContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.BodyContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.CompareExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ConCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.EqExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExprStmtContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionListContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.FunctionCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImageCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImagePosContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.InitBlockContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.LiteralContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.NotExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.OrExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ParenExpressionContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PixelPosContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PixelSpecifierContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PlusMinusExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PostExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PowExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PreExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ScriptContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.StatementContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.TernaryExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.TimesDivModExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.VarDeclarationContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.VarIDContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.XorExprContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import it.geosolutions.jaiext.jiffle.parser.node.Band;
import it.geosolutions.jaiext.jiffle.parser.node.BinaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.Break;
import it.geosolutions.jaiext.jiffle.parser.node.BreakIf;
import it.geosolutions.jaiext.jiffle.parser.node.ConFunction;
import it.geosolutions.jaiext.jiffle.parser.node.ConstantLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.DefaultScalarValue;
import it.geosolutions.jaiext.jiffle.parser.node.DoubleLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.Expression;
import it.geosolutions.jaiext.jiffle.parser.node.FunctionCall;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.GlobalVars;
import it.geosolutions.jaiext.jiffle.parser.node.IfElse;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;
import it.geosolutions.jaiext.jiffle.parser.node.IntLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.ListAppend;
import it.geosolutions.jaiext.jiffle.parser.node.ListLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInLiteralList;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInRange;
import it.geosolutions.jaiext.jiffle.parser.node.LoopInVariable;
import it.geosolutions.jaiext.jiffle.parser.node.Node;
import it.geosolutions.jaiext.jiffle.parser.node.NodeException;
import it.geosolutions.jaiext.jiffle.parser.node.ParenExpression;
import it.geosolutions.jaiext.jiffle.parser.node.Pixel;
import it.geosolutions.jaiext.jiffle.parser.node.PostfixUnaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.PrefixUnaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.Script;
import it.geosolutions.jaiext.jiffle.parser.node.SetDestValue;
import it.geosolutions.jaiext.jiffle.parser.node.SimpleStatement;
import it.geosolutions.jaiext.jiffle.parser.node.Statement;
import it.geosolutions.jaiext.jiffle.parser.node.StatementList;
import it.geosolutions.jaiext.jiffle.parser.node.Until;
import it.geosolutions.jaiext.jiffle.parser.node.Variable;
import it.geosolutions.jaiext.jiffle.parser.node.While;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generates a Java model representing the script, from which sources can be generated
 *
 * @author michael
 */
public class RuntimeModelWorker extends PropertyWorker<Node> {

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
    public void exitAtomExpr(AtomExprContext ctx) {
        set( ctx, get(ctx.atom()) );
    }

    @Override
    public void exitPowExpr(PowExprContext ctx) {
        Expression a = getAsType(ctx.expression(0), Expression.class);
        Expression b = getAsType(ctx.expression(1), Expression.class);
        
        try {
            set(ctx, new BinaryExpression(JiffleParser.POW, a, b));
        } catch (NodeException ex) {
            throw new InternalCompilerException();
        }
    }

    @Override
    public void exitPostExpr(PostExprContext ctx) {
        String op = ctx.getChild(1).getText();
        Expression e = getAsType(ctx.expression(), Expression.class);
        set(ctx, new PostfixUnaryExpression(e, op));
    }

    @Override
    public void exitPreExpr(PreExprContext ctx) {
        String op = ctx.getChild(0).getText();
        Expression e = getAsType(ctx.expression(), Expression.class);
        set(ctx, new PrefixUnaryExpression(op, e));
    }

    @Override
    public void exitNotExpr(NotExprContext ctx) {
        setFunctionCall(ctx, "NOT", Collections.singletonList(ctx.expression()));
    }

    @Override
    public void exitTimesDivModExpr(TimesDivModExprContext ctx) {
        Expression left = getAsType(ctx.expression(0), Expression.class);
        Expression right = getAsType(ctx.expression(1), Expression.class);
        
        try {
            set(ctx, new BinaryExpression(ctx.op.getType(), left, right));
        } catch (NodeException ex) {
            throw new InternalCompilerException();
        }
    }
    
    @Override
    public void exitPlusMinusExpr(PlusMinusExprContext ctx) {
        Expression left = getAsType(ctx.expression(0), Expression.class);
        Expression right = getAsType(ctx.expression(1), Expression.class);
        
        try {
            set(ctx, new BinaryExpression(ctx.op.getType(), left, right));
        } catch (NodeException ex) {
            throw new InternalCompilerException();
        }
    }

    @Override
    public void exitCompareExpr(CompareExprContext ctx) {
        String op;
        
        switch (ctx.op.getType()) {
            case JiffleParser.LT: op = "LT"; break;
            case JiffleParser.LE: op = "LE"; break;
            case JiffleParser.GE: op = "GE"; break;
            case JiffleParser.GT: op = "GT"; break;
            default: throw new IllegalStateException("Unknown op: " + ctx.op.getText());
        }

        setFunctionCall(ctx, op, ctx.expression());
    }
    
    @Override
    public void exitEqExpr(EqExprContext ctx) {
        String op;
        
        switch (ctx.op.getType()) {
            case JiffleParser.EQ: op = "EQ"; break;
            case JiffleParser.NE: op = "NE"; break;
            default: throw new IllegalStateException("Unknown op: " + ctx.op.getText());
        }

        setFunctionCall(ctx, op, ctx.expression());
    }

    @Override
    public void exitAndExpr(AndExprContext ctx) {
        setFunctionCall(ctx, "AND", ctx.expression());
    }
    
    @Override
    public void exitOrExpr(OrExprContext ctx) {
        setFunctionCall(ctx, "OR", ctx.expression());
    }

    @Override
    public void exitXorExpr(XorExprContext ctx) {
        setFunctionCall(ctx, "XOR", ctx.expression());
    }
    
    private void setFunctionCall(ParseTree ctx, String fnName, List<ExpressionContext> ecs) {
        try {
            set(ctx, FunctionCall.of(fnName, asExpressions(ecs)));
        } catch (NodeException ex) {
            throw new InternalCompilerException();
        }
    }

    @Override
    public void exitTernaryExpr(TernaryExprContext ctx) {
        Expression[] args = {
            getAsType(ctx.expression(0), Expression.class),
            getAsType(ctx.expression(1), Expression.class),
            getAsType(ctx.expression(2), Expression.class)
        };
        
        try {
            set(ctx, new ConFunction(args));
        } catch (NodeException ex) {
            messages.error(ctx.getStart(), ex.getError());
        }
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

    @Override
    public void exitAtom(AtomContext ctx) {
        set( ctx, get(ctx.getChild(0)) );
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        ExpressionListContext expressionList = ctx.argumentList().expressionList();
        if (expressionList == null) {
            setFunctionCall(ctx, ctx.start.getText(), new ArrayList<ExpressionContext>());
        } else {
            setFunctionCall(ctx, ctx.start.getText(), expressionList.expression()); 
        }
    }

    @Override
    public void exitConCall(ConCallContext ctx) {
        List<ExpressionContext> es = ctx.argumentList().expressionList().expression();
        Expression[] args = new Expression[es.size()];
        
        for (int i = 0; i < args.length; i++) {
            args[i] = getAsType(es.get(i), Expression.class);
        }
        
        try {
            set(ctx, new ConFunction(args));
        } catch (NodeException ex) {
            messages.error(ctx.getStart(), ex.getError());
        }
    }

    @Override
    public void exitParenExpression(ParenExpressionContext ctx) {
        Expression e = getAsType(ctx.expression(), Expression.class);
        set(ctx, new ParenExpression(e));
    }

    @Override
    public void exitImageCall(ImageCallContext ctx) {
        String name = ctx.ID().getText();
        ImagePos pos = getAsType(ctx.imagePos(), ImagePos.class);
        set(ctx, new GetSourceValue(name, pos));
    }

    @Override
    public void exitImagePos(ImagePosContext ctx) {
        BandSpecifierContext bandCtx = ctx.bandSpecifier();
        Band band = bandCtx == null ? 
                Band.DEFAULT : getAsType(bandCtx, Band.class);
        
        PixelSpecifierContext pixelCtx = ctx.pixelSpecifier();
        Pixel pixel = pixelCtx == null ?
                Pixel.DEFAULT : getAsType(pixelCtx, Pixel.class);
        
        set(ctx, new ImagePos(band, pixel));
    }

    @Override
    public void exitBandSpecifier(BandSpecifierContext ctx) {
        Expression e = getAsType(ctx.expression(), Expression.class);
        set(ctx, new Band(e));
    }

    @Override
    public void exitPixelSpecifier(PixelSpecifierContext ctx) {
        
        final Expression x, y;
        Expression e;
        
        try {
            PixelPosContext xpos = ctx.pixelPos(0);
            e = getAsType(xpos.expression(), Expression.class);
            if (xpos.ABS_POS_PREFIX() == null) {
                // relative position
                x = new BinaryExpression(JiffleParser.PLUS, FunctionCall.of("x"), e);
            } else {
                // absolute position
                x = e;
            }

            PixelPosContext ypos = ctx.pixelPos(1);
            e = getAsType(ypos.expression(), Expression.class);
            if (ypos.ABS_POS_PREFIX() == null) {
                // relative position
                y = new BinaryExpression(JiffleParser.PLUS, FunctionCall.of("y"), e);
            } else {
                // absolute position
                y = e;
            }
            
            set(ctx, new Pixel(x, y));
            
        } catch (NodeException ex) {
            // definitely should not happen
            throw new InternalCompilerException(ex.getError().toString());
        }
    }

    @Override
    public void exitVarID(VarIDContext ctx) {
        String name = ctx.ID().getText();
        // variable or constant?
        if (ConstantLookup.isDefined(name)) {
            set (ctx, new DoubleLiteral(Double.toString(ConstantLookup.getValue(name))));
        } else {
            Symbol symbol = getScope(ctx).get(name);
            
            switch( symbol.getType() ) {
                case SOURCE_IMAGE:
                    // Source image with default pixel / band positions
                    set(ctx, new GetSourceValue(name, ImagePos.DEFAULT));
                    break;
                    
                case LIST:
                    set(ctx, new Variable(name, JiffleType.LIST));
                    break;
                    
                case LOOP_VAR:
                case SCALAR:
                    set(ctx, new Variable(name, JiffleType.D));
                    break;
                    
                default:  
                    // DEST_IMAGE and UNKNOWN
                    // This should have been picked up in earlier stages
                    throw new IllegalArgumentException(
                            "Compiler error: invalid type for variable" + name);
            } 
        }
    }

    @Override
    public void exitLiteral(LiteralContext ctx) {
        Token tok = ctx.getStart();
        switch (tok.getType()) {
            case JiffleParser.INT_LITERAL:
                set(ctx, new IntLiteral(tok.getText())); 
                break;
                
            case JiffleParser.FLOAT_LITERAL:
                set(ctx, new DoubleLiteral(tok.getText()));
                break;
                
            case JiffleParser.TRUE:
                set(ctx, ConstantLiteral.trueValue());
                break;
                
            case JiffleParser.FALSE:
                set(ctx, ConstantLiteral.falseValue());
                break;
                
            case JiffleParser.NULL:
                set(ctx, ConstantLiteral.nanValue());
                break;
                
            default:
                throw new JiffleParserException("Unrecognized literal type: " + tok.getText());
        }
    }

    @Override
    public void exitListLiteral(JiffleParser.ListLiteralContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        if (ctx.expressionList() != null && ctx.expressionList().expression() != null) {
            for (ExpressionContext ec : ctx.expressionList().expression()) {
                Expression expression = getAsType(ec, Expression.class);
                expressions.add(expression);
            }
        }
        set(ctx, new ListLiteral(expressions));
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

    private <N extends Node> N getAsType(ParseTree ctx, Class<N> clazz) {
        if (get(ctx) == null) {
            // bummer - node property should have been set but wasn't
            String lineColumn = "(unknown)";
            if (ctx instanceof ParserRuleContext) {
                ParserRuleContext prc = (ParserRuleContext) ctx;
                Token start = prc.getStart();
                lineColumn = "(" + start.getLine() + ":" + start.getCharPositionInLine() + ")";
            }
            throw new IllegalStateException(
                    "Internal compiler error: no property set for node of type "
                    + ctx.getClass().getSimpleName() + " at " + lineColumn);
        }
        
        try {
            // have to assign to a local variable to allow
            // for possible class cast exception
            return clazz.cast(get(ctx));
            
        } catch (ClassCastException ex) {
            // Bummer - internal error
            String msg = String.format(
                    "Internal compiler error: cannot cast %s to %s",
                    get(ctx).getClass().getSimpleName(), clazz.getSimpleName());
            
            throw new IllegalStateException(msg);
        }
        
    }
    
    private Expression[] asExpressions(List<ExpressionContext> ctxs) {
        if (ctxs == null) {
            return new Expression[0];
        }
        
        Expression[] exprs = new Expression[ctxs.size()];
        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = getAsType(ctxs.get(i), Expression.class);
        }
        
        return exprs;
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
