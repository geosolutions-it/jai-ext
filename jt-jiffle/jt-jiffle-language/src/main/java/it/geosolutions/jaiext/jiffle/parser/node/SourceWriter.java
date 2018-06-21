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

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleProperties;
import it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime;
import it.geosolutions.jaiext.jiffle.runtime.AbstractIndirectRuntime;

import java.util.Arrays;

/**
 * Helper class for writing source code, handles proper indentation
 */
public class SourceWriter {

    private Jiffle.RuntimeModel runtimeModel;
    private StringBuilder sb = new StringBuilder();
    private int indentStep = 4;
    private int indentAmount = 0;
    private String indentation = "";
    private String script;
    private String baseClassName;

    public SourceWriter(Jiffle.RuntimeModel model) {
        this.runtimeModel = model;
    }

    public Jiffle.RuntimeModel getRuntimeModel() {
        return runtimeModel;
    }

    /**
     * Writes out a node to a SourceWriter and returns the resulting script
     * 
     * @param node
     * @return
     */
    public String writeToString(Expression node) {
        SourceWriter sw = new SourceWriter(runtimeModel);
        node.write(sw);
        return sw.getSource();
    }

    /**
     * Increases indentation by one indentation step
     */
    public void inc() {
        indentAmount += indentStep;
    }

    /**
     * Decreases indentation by one indentation step, or reduce indentation to zero otherwise
     */
    public void dec() {
        if (indentStep < indentAmount) {
            indentAmount -= indentStep;
        } else {
            indentAmount = 0;
        }
    }

    /**
     * Method to add a line in the source code.
     * Writes the indentation, the line provided, and adds a newline at the end 
     * @param line
     */
    public SourceWriter line(String line) {
        String indentation = getIndentation();
        sb.append(indentation).append(line).append("\n");
        return this;
    }

    /**
     * Method to add text in the source, without any indentation or newline 
     * @param line
     */
    public SourceWriter append(String text) {
        sb.append(text);
        return this;
    }

    /**
     * Returns the source code built so far
     * @return
     */
    public String getSource() {
        return sb.toString();
    }

    private String getIndentation() {
        if (indentation.length() < indentAmount) {
            char[] charArray = new char[indentAmount];
            Arrays.fill(charArray, ' ');
            indentation = new String(charArray);
        } else if (indentation.length() > indentAmount) {
            indentation = indentation.substring(0, indentAmount);
        }
        return indentation;
    }

    public void newLine() {
        sb.append("\n");
    }

    public SourceWriter indent() {
        sb.append(getIndentation());
        return this;
    }

    public SourceWriter append(Node node) {
        node.write(this);
        return this;
        
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getScript() {
        return this.script;
    }

    public void setBaseClassName(String baseClassName) {
        this.baseClassName = baseClassName;
    }

    public String getBaseClassName() {
        if (this.baseClassName == null) {
            if (runtimeModel == Jiffle.RuntimeModel.DIRECT) {
                return JiffleProperties.DEFAULT_DIRECT_BASE_CLASS.getName();
            } else {
                return JiffleProperties.DEFAULT_INDIRECT_BASE_CLASS.getName();
            }
        }
        return this.baseClassName;
    }

    /**
     * Returns true if the runtime is either {@link it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime}
     * or {@link it.geosolutions.jaiext.jiffle.runtime.AbstractIndirectRuntime}
     * @return
     */
    public boolean isInternalBaseClass() {
        String baseClassName = getBaseClassName();
        return AbstractDirectRuntime.class.getName().equals(baseClassName) ||
                AbstractIndirectRuntime.class.getName().equals(baseClassName);
    }
}
