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
 *  Copyright (c) 2009-2011, Michael Bedward. All rights reserved. 
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

import static org.junit.Assert.assertThat;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hamcrest.CoreMatchers;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Lexes and parses Jiffle scripts for unit tests.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class ParserTestBase {
    static final int DOWN = -3;
    static final int UP = -4;

    /**
     * Helper function to scan and parse an input script and
     * prepare a tree node stream for a tree walker
     *
     * @param input input jiffle script
     * @return the AST as a tree node stream with attached tokens
     * @throws java.lang.Exception
     */
    protected int[] getAST(String script) throws Exception {
        return getAST(script, parser -> parser.script());
    }
    
    /**
     * Helper function to scan and parse an input script and
     * prepare a tree node stream for a tree walker
     * 
     * @param input input jiffle script
     * @return the AST as a tree node stream with attached tokens
     * @throws java.lang.Exception
     */
    protected int[] getAST(String script, Function<JiffleParser, ParseTree> parseDriver) throws Exception {
        

        ParseTree tree = getParseTree(script, parseDriver);
        List<Integer> types = new ArrayList<>();
        walk(tree, types);
        
        int[] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            int type = types.get(i);
            result[i] = type;
        }
        
        return result;
    }

    protected ParseTree getParseTree(String script, Function<JiffleParser, ParseTree> parseDriver) {
        CharStream input = CharStreams.fromString(script);

        JiffleLexer lexer = new JiffleLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);

        JiffleParser parser = new JiffleParser(tokens);

        return parseDriver.apply(parser);
    }

    private void walk(ParseTree tree, List<Integer> types) {
        if (tree instanceof RuleContext) {
            RuleContext ruleContext = (RuleContext) tree;
            int ruleIndex = ruleContext.getRuleIndex();
            if (ruleIndex == JiffleParser.RULE_atom
                    || ruleIndex == JiffleParser.RULE_identifiedAtom
                    || ruleIndex == JiffleParser.RULE_literal) {
                // ignore these, they just add noise
                for (int i = 0; i < ruleContext.getChildCount(); i++) {
                    walk(ruleContext.getChild(i), types);
                }
            } else {
                types.add(ruleIndex);
                if (ruleContext.getChildCount() > 0) {
                    types.add(DOWN);
                    for (int i = 0; i < ruleContext.getChildCount(); i++) {
                        walk(ruleContext.getChild(i), types);
                    }
                    types.add(UP);
                }
            }
        } else if (tree instanceof TerminalNode) {
            int tokenType = ((TerminalNode) tree).getSymbol().getType();
            types.add(tokenType);
        } else {
            throw new IllegalArgumentException("Don't know how to handle " + tree);
        }
    }
    
    protected void assertAST(int[] ast, int[] expected) {
        assertThat(ast, CoreMatchers.equalTo(expected));
    }

    protected void compileScript(String script) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
    }
}
