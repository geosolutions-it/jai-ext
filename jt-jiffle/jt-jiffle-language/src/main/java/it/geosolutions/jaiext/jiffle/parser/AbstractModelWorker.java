/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *  
 */
package it.geosolutions.jaiext.jiffle.parser;

import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AndExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AtomContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.AtomExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.BandSpecifierContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.CompareExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ConCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.EqExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExprStmtContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ExpressionListContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.FunctionCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImageCallContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.ImagePosContext;
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
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.TernaryExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.TimesDivModExprContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.VarIDContext;
import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.XorExprContext;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.geosolutions.jaiext.jiffle.parser.node.Band;
import it.geosolutions.jaiext.jiffle.parser.node.BinaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.ConFunction;
import it.geosolutions.jaiext.jiffle.parser.node.ConstantLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.DoubleLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.Expression;
import it.geosolutions.jaiext.jiffle.parser.node.FunctionCall;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;
import it.geosolutions.jaiext.jiffle.parser.node.IntLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.ListLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.Node;
import it.geosolutions.jaiext.jiffle.parser.node.NodeException;
import it.geosolutions.jaiext.jiffle.parser.node.ParenExpression;
import it.geosolutions.jaiext.jiffle.parser.node.Pixel;
import it.geosolutions.jaiext.jiffle.parser.node.PostfixUnaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.PrefixUnaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.SimpleStatement;
import it.geosolutions.jaiext.jiffle.parser.node.Variable;

/**
 * Base class to generate a Java model representing the script, limited to expressions
 */
abstract class AbstractModelWorker extends PropertyWorker<Node> {

    /**
     * Locates the source positions
     */
    public AbstractModelWorker(ParseTree tree) {
        super(tree);
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
                            "Compiler error: invalid type for variable '" + name + "'");
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
    protected abstract SymbolScope getScope(ParseTree ctx);

    protected <N extends Node> N getAsType(ParseTree ctx, Class<N> clazz) {
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

    protected Expression[] asExpressions(List<ExpressionContext> ctxs) {
        if (ctxs == null) {
            return new Expression[0];
        }

        Expression[] exprs = new Expression[ctxs.size()];
        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = getAsType(ctxs.get(i), Expression.class);
        }

        return exprs;
    }


}
