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

package it.geosolutions.jaiext.jiffle.parser;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;
import it.geosolutions.jaiext.jiffle.parser.node.Node;

/**
 * Support class helping to list all the unique positions read in the various sources
 */
public class SourcePositionsWorker extends AbstractModelWorker {

    private final SymbolScope scope;
    private final Set<String> names;
    private final Set<GetSourceValue> positions = new HashSet<>();

    public SourcePositionsWorker(ParseTree tree, List<String> sourceImageNames) {
        super(tree);
        this.names = new HashSet<String>(sourceImageNames);
        this.scope = new SymbolScope("fake", null) {
            @Override
            public Symbol get(String name) {
                if (names.contains(name)) {
                    return new Symbol(name, Symbol.Type.SOURCE_IMAGE);
                } else {
                    // fake a scalar variable
                    return new Symbol(name, Symbol.Type.SCALAR);
                }
            }
        };
        sourceImageNames
                .stream()
                .map(name -> new Symbol(name, Symbol.Type.SOURCE_IMAGE))
                .forEach(symbol -> scope.add(symbol));
        
        walkTree();
    }

    @Override
    public void exitVarID(JiffleParser.VarIDContext ctx) {
        super.exitVarID(ctx);
        Node node = get(ctx);
        if (node instanceof GetSourceValue) {
            this.positions.add((GetSourceValue) node);
        }
    }

    @Override
    public void exitImageCall(JiffleParser.ImageCallContext ctx) {
        super.exitImageCall(ctx);
        Node node = get(ctx);
        if (node instanceof GetSourceValue) {
            this.positions.add((GetSourceValue) node);
        }
    }

    @Override
    protected SymbolScope getScope(ParseTree ctx) {
        return scope;
    }

    /**
     * Returns all the unique read calls found in the source
     * @return
     */
    public Set<GetSourceValue> getPositions() {
        return positions;
    }
}
