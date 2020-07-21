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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.geosolutions.jaiext.jiffle.parser.node.Expression;
import it.geosolutions.jaiext.jiffle.parser.node.FunctionCall;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;
import it.geosolutions.jaiext.jiffle.parser.node.ScalarLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.SourceWriter;

/**
 * Support class that helps avoiding repeated reads on the source images, for the common case where
 * the source image reference is the current pixel (no offsets, no absolute references).
 */
public class RepeatedReadOptimizer {

    /**
     * The compiler generates multiple GetSourceValue for the same kind of read, in case it's a
     * repeated one (same source reference in multiple parts of the script). Trying to reduce them
     * all to one causes compile failures, so left them as is, and hid the ugly part in this class.
     */
    Map<GetSourceValue, List<GetSourceValue>> sourceValues = new LinkedHashMap<>();

    /**
     * Adds a source value reference
     *
     * @param node
     */
    public void add(GetSourceValue node) {
        List<GetSourceValue> sourceValues =
                this.sourceValues.computeIfAbsent(node, f -> new ArrayList<>());
        sourceValues.add(node);
    }

    /**
     * Declares a local variable for each read that is repeated at least once, creating local
     * variables, and making the {@link GetSourceValue} instances emit the new local variable
     * reference. Please call {@link #resetVariables()} to allow re-using the script one more time.
     */
    public void declareRepeatedReads(SourceWriter w) {
        for (GetSourceValue sourceValue : sourceValues.keySet()) {
            List<GetSourceValue> valuesList = sourceValues.get(sourceValue);
            if (valuesList.size() > 1) {
                ImagePos pos = sourceValue.getPos();
                Expression band = pos.getBand().getIndex();
                Expression x = pos.getPixel().getX();
                Expression y = pos.getPixel().getY();
                SourceWriter varWriter = new SourceWriter(w.getRuntimeModel());
                if (band instanceof ScalarLiteral && isPosition(x) && isPosition(y)) {
                    // prefixes are used to separate variables "sv_" stands for source value
                    varWriter.append("sv_").append(sourceValue.getVarName()).append("_");
                    x.write(varWriter);
                    varWriter.append("_");
                    y.write(varWriter);
                    varWriter.append("_");
                    band.write(varWriter);
                    String variableName = varWriter.getSource().replace("-", "_");

                    w.indent();
                    w.append("double ").append(variableName).append(" = ");
                    sourceValue.write(w);
                    w.append(";");
                    w.newLine();

                    for (GetSourceValue reference : valuesList) {
                        reference.setVariableName(variableName);
                    }
                }
            }
        }
    }

    /**
     * Checks that the expression is just a proxy to a variable (e.g., <code>_x</code> or <code>_y
     * </code>) or an absolute reference to a fixed pixel.
     */
    private boolean isPosition(Expression x) {
        return (x instanceof FunctionCall && ((FunctionCall) x).isProxy())
                || x instanceof ScalarLiteral;
    }

    /** Resets the local variables references. */
    public void resetVariables() {
        for (GetSourceValue sourceValue : sourceValues.keySet()) {
            List<GetSourceValue> valuesList = sourceValues.get(sourceValue);
            if (valuesList.size() > 1) {
                for (GetSourceValue reference : valuesList) {
                    reference.setVariableName(null);
                }
            }
        }
    }
}
