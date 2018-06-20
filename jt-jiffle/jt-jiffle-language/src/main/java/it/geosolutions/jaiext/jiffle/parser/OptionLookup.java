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
 *  Copyright (c) 2011-2013, Michael Bedward. All rights reserved. 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lookup service used by the Jiffle compiler when parsing script options.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class OptionLookup {

    private static final List<OptionInfo> options;
    private static final Map<String, String> activeRuntimeExpr;
    private static final List<String> names;
    
    static {
        options = new ArrayList<>();
        names = new ArrayList<>();
        activeRuntimeExpr = new HashMap<>();
        
        OptionInfo info;
        String name;
        
        name = "outside";
        
        info = new OptionInfo(name,
                new String[] { OptionInfo.ANY_NUMBER, OptionInfo.NULL_KEYWORD });
        
        options.add(info);
        names.add(name);
        
        activeRuntimeExpr.put(name, 
                "_outsideValueSet = true;\n"
                + "_outsideValue = _VALUE_;");
    }
    
    /**
     * Tests if an option name is defined.
     * 
     * @param optionName the name
     * @return {@code true} if the name is defined; {@code false} otherwise
     */
    public static boolean isDefined(String optionName) {
        try {
            getInfo(optionName);
            return true;
            
        } catch (UndefinedOptionException ex) {
            return false;
        }
    }
    
    /**
     * Tests if a value is valid for the given option.
     * @param optionName option name
     * @param value the value as a String
     * @return {@code true} if the value is valid; {@code false} otherwise
     * @throws UndefinedOptionException if the name is not recognized
     */
    public static boolean isValidValue(String optionName, String value) 
            throws UndefinedOptionException {
        
        return getInfo(optionName).isValidValue(value);
    }
    
    /**
     * Gets the names known to the lookup service.
     * 
     * @return option names as an unmodifiable list
     */
    public static Iterable<String> getNames() {
        return Collections.unmodifiableList(names);
    }
    
    /**
     * Gets the runtime source for the given option name:value pair.
     * 
     * @param name option name
     * @param value option value
     * 
     * @return the runtime source
     * @throws UndefinedOptionException if the name is not recognized
     */
    public static String getActiveRuntimExpr(String name, String value) 
            throws UndefinedOptionException {
        
        String key = name.toLowerCase();
        String expr = activeRuntimeExpr.get(key);
        if (expr == null) {
            throw new UndefinedOptionException(name);
        }
        return expr.replace("_VALUE_", value);
    }

    /**
     * Get the info for a given option.
     * @param optionName option name
     * @return option info
     * @throws UndefinedOptionException if the name is not recognized
     */
    private static OptionInfo getInfo(String optionName) throws UndefinedOptionException {
        for (OptionInfo info : options) {
            if (info.getName().equalsIgnoreCase(optionName)) {
                return info;
            }
        }
        
        throw new UndefinedOptionException(optionName);
    }

}
