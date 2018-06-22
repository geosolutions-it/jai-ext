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

import java.util.Arrays;
import java.util.Objects;

import it.geosolutions.jaiext.jiffle.parser.Errors;
import it.geosolutions.jaiext.jiffle.parser.FunctionInfo;
import it.geosolutions.jaiext.jiffle.parser.FunctionLookup;
import it.geosolutions.jaiext.jiffle.parser.JiffleType;
import it.geosolutions.jaiext.jiffle.parser.UndefinedFunctionException;
import it.geosolutions.jaiext.jiffle.util.Strings;

/**
 *
 * @author michael
 */
public class FunctionCall extends Expression {

    private final String runtimeName;
    private final boolean proxy;
    private final Expression[] args;
    
    public static FunctionCall of(String jiffleName, Expression ...args) 
            throws NodeException {
        
        JiffleType[] argTypes =
                (args == null) ? new JiffleType[0] : new JiffleType[args.length];
        
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = args[i].getType();
        }
        
        try {
            FunctionInfo info = FunctionLookup.getInfo(jiffleName, argTypes);
            return new FunctionCall(info, args);
        } catch (UndefinedFunctionException ex) {
            throw new NodeException(Errors.UNKNOWN_FUNCTION);
        }
    }

    private FunctionCall(FunctionInfo info, Expression ...args)
            throws NodeException {
        
        super(info.getReturnType());
        
        this.runtimeName = info.getRuntimeName();
        this.proxy = info.isProxy();
        this.args = args == null ? new Expression[0] : args;
    }

    @Override
    public String toString() {
        // All other functions
        String tail = proxy ? "()" : String.format("(%s)", Strings.commas((Object []) args));
        return runtimeName + tail;
    }

    public void write(SourceWriter w) {
        w.append(runtimeName);
        if (!proxy) {
            w.append("(");
            for (int i = 0; i <args.length; i++) {
               Expression arg = args[i];
               arg.write(w);
               if (i < args.length - 1) {
                   w.append(", ");
               }
            }
            w.append(")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FunctionCall that = (FunctionCall) o;
        return proxy == that.proxy &&
                Objects.equals(runtimeName, that.runtimeName) &&
                Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), runtimeName, proxy);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    public boolean isProxy() {
        return proxy;
    }

    public Expression[] getArgs() {
        return args;
    }
}
