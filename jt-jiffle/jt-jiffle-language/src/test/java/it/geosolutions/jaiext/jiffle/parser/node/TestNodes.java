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

import static org.hamcrest.CoreMatchers.*;
import it.geosolutions.jaiext.jiffle.parser.DirectSources;
import it.geosolutions.jaiext.jiffle.parser.FunctionInfo;
import it.geosolutions.jaiext.jiffle.parser.FunctionLookup;
import it.geosolutions.jaiext.jiffle.parser.JiffleType;
import it.geosolutions.jaiext.jiffle.parser.UndefinedFunctionException;
import it.geosolutions.jaiext.jiffle.util.Strings;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class TestNodes {
    
    @Test
    public void intLiteral() throws Exception {
        Node node = new IntLiteral("42");
        assertThat( node.toString(), is("42") );
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void intLiteralRejectsFloatArg() throws Exception {
        Node node = new IntLiteral("1.2");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void intLiteralRejectsNonNumericArg() throws Exception {
        Node node = new IntLiteral("foo");
    }
    
    @Test
    public void bandDefault() throws Exception {
        assertThat( Band.DEFAULT.toString(), is("0") );
    }
    
    @Test
    public void bandWithIntLiteral() throws Exception {
        Band b = new Band( new IntLiteral("1") );
        assertThat( b.toString(), is("1") );
    }
    
    @Test
    public void pixelDefault() throws Exception {
        assertThat( Pixel.DEFAULT.toString(), is("_x,_y") );
    }
    
    @Test
    public void pixel() throws Exception {
        Expression x = new IntLiteral("42");
        Expression y = FunctionCall.of("y");
        Pixel p = new Pixel(x, y);
        
        String expected = Strings.commas(x, y);
        assertThat( p.toString(), is(expected) );
    }
    
    @Test
    public void proxyFunction() throws Exception {
        String name = "xres";
        FunctionInfo info = getFnInfo(name);
        
        FunctionCall fn = FunctionCall.of(name);
        assertThat(fn.toString(), is(info.getRuntimeName()));
    }

    @Test
    public void mathFunction() throws Exception {
        String name = "min";
        
        Expression[] args = { mockDExpr("a"), mockDExpr("b") };
        JiffleType[] argTypes = { JiffleType.D, JiffleType.D };
        
        FunctionCall fn = FunctionCall.of(name, args);
        FunctionInfo info = getFnInfo(name, argTypes);
        
        String expected = info.getRuntimeName() + 
                String.format("(%s)", Strings.commas((Object[])args));
        
        assertThat(fn.toString(), is(expected));
    }
    
    @Test
    public void conFunction() throws Exception {
        Expression[] args = { mockDExpr("a"), mockDExpr("b"), mockDExpr("c") };
        String[] argStrs = {"a", "b", "c"};
        
        Node node = new ConFunction(args);
        String expected = DirectSources.conCall(argStrs);
        assertThat(node.toString(), is(expected));
    }
    
    @Test
    public void imageRead() throws Exception {
        Expression e = new GetSourceValue("src", ImagePos.DEFAULT);
        assertThat(e.toString(), is("readFromImage(src,_x,_y,0)"));
    }

    
    private FunctionInfo getFnInfo(String name, JiffleType ...argTypes) {
        try {
            return FunctionLookup.getInfo(name, argTypes);
        } catch (UndefinedFunctionException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private Expression mockDExpr(final String name) {
        return new Expression(JiffleType.D) {
            @Override
            public void write(SourceWriter w) {
                // do nothing
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
