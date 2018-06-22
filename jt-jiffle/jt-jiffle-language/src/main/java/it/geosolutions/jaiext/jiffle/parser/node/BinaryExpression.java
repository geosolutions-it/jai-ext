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

package it.geosolutions.jaiext.jiffle.parser.node;

import java.util.Objects;

import it.geosolutions.jaiext.jiffle.parser.Errors;
import it.geosolutions.jaiext.jiffle.parser.JiffleParser;
import it.geosolutions.jaiext.jiffle.parser.JiffleType;

/**
 *
 * @author michael
 */
public class BinaryExpression extends Expression {

    public static enum Operator {

        PLUS(JiffleParser.PLUS, "%s + %s"),
        MINUS(JiffleParser.MINUS, "%s - %s"),
        TIMES(JiffleParser.TIMES, "%s * %s"),
        DIV(JiffleParser.DIV, "%s / %s"),
        MOD(JiffleParser.MOD, "%s %% %s"),
        
        POW(JiffleParser.POW, "Math.pow(%s, %s)"),
        
        ASSIGN(JiffleParser.ASSIGN, "%s = %s"),
        PLUSEQ(JiffleParser.PLUSEQ, "%s += %s"),
        MINUSEQ(JiffleParser.MINUSEQ, "%s -= %s"),
        TIMESEQ(JiffleParser.TIMESEQ, "%s *= %s"),
        DIVEQ(JiffleParser.DIVEQ, "%s /= %s"),
        
        UNKNOWN(-1, "");
        
        private final int code;
        private final String fmt;

        private Operator(int code, String fmt) {
            this.code = code;
            this.fmt = fmt;
        }
        
        public static Operator get(int code) {
            for (Operator o : Operator.values()) {
                if (code == o.code) {
                    return o;
                }
            }
            
            return UNKNOWN;
        }
        
        public String getFormat() {
            return fmt;
        }
    }

    private final boolean declarationNeeded;
    private final Expression left;
    private final Expression right;
    private final Operator op;
    

    private static JiffleType getReturnType(Expression left, Expression right) {
        if (left.getType() == right.getType()) {
            return left.getType();
        } else {
            // types are different so result must be list
            return JiffleType.LIST;
        }
    }

    public BinaryExpression(int opCode, Expression left, Expression right)
            throws NodeException {
        this(opCode, left, right, false);
    }

    public BinaryExpression(int opCode, Expression left, Expression right, boolean declarationNeeded) 
            throws NodeException {
        
        super(getReturnType(left, right));
        
        this.op = Operator.get(opCode);
        if (op == Operator.UNKNOWN) {
            throw new NodeException(Errors.INVALID_BINARY_EXPRESSION);
        }

        this.left = left.forceDouble();
        this.right = right.forceDouble();
        this.declarationNeeded = declarationNeeded;
    }

    public void write(SourceWriter w) {
        if (declarationNeeded && left instanceof Variable) {
            String type = getJavaType();
            w.append(type).append(" ");
        }
        String leftCode = w.writeToString(left);
        String rightCode = w.writeToString(right);
        w.append(String.format(op.getFormat(), leftCode, rightCode));
    }

    private String getJavaType() {
        return left.getType() == JiffleType.D ? "double" : "List";
    }

    private String getInitialValue() {
        return left.getType() == JiffleType.D ? "Double.NaN" : "null";
    }

    public void writeDeclaration(SourceWriter w) {
        if (left instanceof Variable) {
            String type = getJavaType();
            String initialValue = getInitialValue();
            w.indent()
                    .append(type)
                    .append(" ")
                    .append(left)
                    .append(" = ")
                    .append(initialValue)
                    .append(";")
                    .newLine();
        }
    }

    public void appendName(SourceWriter w) {
        if (left instanceof Variable) {
            w.append(left.toString());
        }
    }

    public void writeDefaultValue(SourceWriter w) {
        if (!(left instanceof Variable)) {
            throw new IllegalStateException("Cannot write default value unless this is a declaration");
        }
        w.indent().append("if (Double.isNaN(").append(left).append(")) {").newLine();
        w.inc();
        w.indent().append(this).append(";").newLine();
        w.dec();
        w.line("}");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryExpression that = (BinaryExpression) o;
        return declarationNeeded == that.declarationNeeded &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right) &&
                op == that.op;
    }

    @Override
    public int hashCode() {
        return Objects.hash(declarationNeeded, left, right, op);
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public Operator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return left + " " + op + " " + right;
    }
}
