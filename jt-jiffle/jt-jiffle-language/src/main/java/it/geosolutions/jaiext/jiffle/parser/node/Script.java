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

import static java.lang.String.format;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.parser.JiffleParserException;
import it.geosolutions.jaiext.jiffle.parser.OptionLookup;
import it.geosolutions.jaiext.jiffle.parser.RepeatedReadOptimizer;
import it.geosolutions.jaiext.jiffle.parser.UndefinedOptionException;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** @author michael */
public class Script implements Node {

    /** <pre>ID      : (Letter) (Letter | UNDERSCORE | Digit | Dot)*</pre> */
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z][_a-zA-Z0-9\\.]*$");

    private final StatementList stmts;
    private final RepeatedReadOptimizer readOptimizer;
    private Map<String, String> options;
    private Set<String> sourceImages;
    private Set<String> destImages;
    private final GlobalVars globals;

    public Script(
            Map<String, String> options,
            Set<String> sourceImages,
            Set<String> destImages,
            GlobalVars globals,
            StatementList stmts,
            RepeatedReadOptimizer readOptimizer) {
        this.options = options;
        this.sourceImages = sourceImages;
        this.destImages = destImages;
        this.globals = globals;
        this.stmts = stmts;
        this.readOptimizer = readOptimizer;
        validate();
    }

    private void validate() {
        for (String name : sourceImages) {
            if (!VALID_IDENTIFIER.matcher(name).matches()) {
                throw new JiffleParserException("Invalid source image name: " + name);
            }
        }
        for (String name : destImages) {
            if (!VALID_IDENTIFIER.matcher(name).matches()) {
                throw new JiffleParserException("Invalid dest image name: " + name);
            }
        }
    }

    public void write(SourceWriter w) {
        // class header
        String packageName = "it.geosolutions.jaiext.jiffle.runtime";
        w.line("package " + packageName + ";");
        w.newLine();
        w.line("import java.util.List;");
        w.line("import java.util.ArrayList;");
        w.line("import java.util.Arrays;");
        w.newLine();

        // add the script source, if available
        String script = w.getScript();
        if (script != null) {
            String[] lines = script.split("\n");
            w.line("/**");
            w.line(" * Java runtime class generated from the following Jiffle script: ");
            w.line(" *<pre>");
            for (String line : lines) {
                // In case the script itself includes comments, they best to be escaped
                String escaped = line.replace("*/", "*&#47;").replace("/*", "&#47;*");
                w.append(" * ").append(escaped).newLine();
            }
            w.line(" *</pre>");
            w.line(" */");
        }

        // class declaration
        String template = "public class %s extends %s {";
        String className;
        Jiffle.RuntimeModel model = w.getRuntimeModel();
        if (model == Jiffle.RuntimeModel.DIRECT) {
            className = "JiffleDirectRuntimeImpl";
        } else {
            className = "JiffleIndirectRuntimeImpl";
        }
        w.line(format(template, className, w.getBaseClassName()));

        // writing class fields
        w.inc();
        // ... if we are using a internal class, dodge map lookups while working on pixels
        if (w.isInternalBaseClass()) {
            for (String sourceImage : sourceImages) {
                w.indent().append("SourceImage s_").append(sourceImage).append(";").newLine();
            }
            if (model == Jiffle.RuntimeModel.DIRECT) {
                for (String destImage : destImages) {
                    w.indent()
                            .append("DestinationImage d_")
                            .append(destImage)
                            .append(";")
                            .newLine();
                }
            }
        }
        globals.writeFields(w);
        w.newLine();

        // adding the constructor
        w.indent().append("public ").append(className).append("() {").newLine();
        w.inc();
        w.indent().append("super(new String[] {");
        globals.listNames(w);
        w.append("});").newLine();
        w.dec();
        w.line("}");
        w.newLine();

        // add the options init, if required
        if (options != null && !options.isEmpty()) {
            w.line("protected void initOptionVars() {");
            w.inc();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                try {
                    String activeExpr = OptionLookup.getActiveRuntimExpr(name, value);
                    w.line(activeExpr);
                } catch (UndefinedOptionException e) {
                    throw new JiffleParserException(e);
                }
            }
            w.dec();
            w.line("}");
        }

        // and field initializer method
        w.line("protected void initImageScopeVars() {");
        w.inc();
        if (w.isInternalBaseClass()) {
            for (String sourceImage : sourceImages) {
                w.indent()
                        .append("s_")
                        .append(sourceImage)
                        .append(" = (SourceImage) _images.get(\"")
                        .append(sourceImage)
                        .append("\");")
                        .newLine();
            }
            if (model == Jiffle.RuntimeModel.DIRECT) {
                for (String destImage : destImages) {
                    w.indent()
                            .append("d_")
                            .append(destImage)
                            .append("= (DestinationImage) _destImages.get(\"")
                            .append(destImage)
                            .append("\");")
                            .newLine();
                }
            }
        }
        globals.write(w);
        w.line("_imageScopeVarsInitialized = true;");
        w.dec();
        w.line("}");
        w.newLine();

        // the evaluate method
        if (model == Jiffle.RuntimeModel.DIRECT) {
            w.line("public void evaluate(double _x, double _y) {");
        } else {
            w.line("public void evaluate(double _x, double _y, double[] result) {");
        }
        w.inc();

        // basic checks at the beginning of pixel evaluation
        w.line("if (!isWorldSet()) {");
        w.inc();
        w.line("setDefaultBounds();");
        w.dec();
        w.line("}");
        w.line("if (!_imageScopeVarsInitialized) {");
        w.inc();
        w.line("initImageScopeVars();");
        w.dec();
        w.line("}");
        w.line("_stk.clear();");

        // centralize the source reads to avoid repeated reads
        readOptimizer.declareRepeatedReads(w);

        // the actual script
        w.newLine();
        stmts.write(w);

        w.dec();
        w.line("}");

        // closing class
        w.dec();
        w.line("}");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Script script = (Script) o;
        return Objects.equals(stmts, script.stmts) &&
                Objects.equals(options, script.options) &&
                Objects.equals(sourceImages, script.sourceImages) &&
                Objects.equals(destImages, script.destImages) &&
                Objects.equals(globals, script.globals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stmts, options, sourceImages, destImages, globals);
    }

    public StatementList getStmts() {
        return stmts;
    }

    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public Set<String> getSourceImages() {
        return Collections.unmodifiableSet(sourceImages);
    }

    public Set<String> getDestImages() {
        return Collections.unmodifiableSet(destImages);
    }

    public GlobalVars getGlobals() {
        return globals;
    }
}
