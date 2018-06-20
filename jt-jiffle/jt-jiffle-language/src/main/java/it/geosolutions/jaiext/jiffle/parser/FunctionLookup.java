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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import it.geosolutions.jaiext.jiffle.util.Strings;

/**
 * Looks up function descriptions for the Jiffle parser.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class FunctionLookup {

    private static final String PROPERTIES_FILE = 
            "META-INF/it/geosolutions/jaiext/jiffle/FunctionLookup.properties";

    // lists of function info keyed by function name 
    private static final Map<String, List<FunctionInfo>> lookup = new HashMap<>();
    
    // Indices of attributes in properties file record
    private static final int JIFFLE_NAME = 0;
    private static final int RUNTIME_NAME = 1;
    private static final int PROVIDER = 2;
    private static final int VOLATILE = 3;
    private static final int RETURN = 4;
    private static final int FIRST_ARG = 5;
    
    private static final int MIN_ATTRIBUTES = FIRST_ARG + 1;

    static {
        InputStream in = null;
        try {
            in = FunctionLookup.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
            Properties properties = new Properties();
            properties.load(in);
            
            Enumeration<?> names = properties.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                String value = properties.getProperty(name);

                String[] attr = value.split("[,\\s]+");
                if (attr.length < MIN_ATTRIBUTES) {
                    throw new IllegalArgumentException(
                            "Error reading " + PROPERTIES_FILE + " record: " + name + "=" + value);
                }
                
                FunctionInfo.Provider provider = FunctionInfo.Provider.get( attr[PROVIDER] );
                if (provider == null) {
                    throw new IllegalArgumentException(
                            "Unrecognized Jiffle function provider (" 
                            + attr[PROVIDER] + ") in " + PROPERTIES_FILE);
                }

                boolean isVolatile = Boolean.parseBoolean(attr[VOLATILE]);
                
                final int numArgs = "0".equals(attr[FIRST_ARG]) ? 
                        0 : attr.length - FIRST_ARG;
                
                String[] argTypes = new String[numArgs];
                for (int i = 0, k = FIRST_ARG; i < numArgs; i++, k++) {
                    argTypes[i] = attr[k];
                }

                List<FunctionInfo> fns;
                String fname = attr[JIFFLE_NAME];
                if (lookup.containsKey(fname)) {
                    fns = lookup.get(fname);
                } else {
                    fns = new ArrayList<>();
                    lookup.put(fname, fns);
                }
                
                fns.add(new FunctionInfo(
                            attr[JIFFLE_NAME], attr[RUNTIME_NAME], 
                            provider, isVolatile, attr[RETURN], argTypes) 
                        );
            }

        } catch (Exception ex) {
            throw new IllegalArgumentException("Internal compiler error", ex);

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignore) {}
        }
    }
    
    /**
     * Checks if a function of the given name is defined.
     * 
     * @param jiffleName the name of the function used in a Jiffle script
     * 
     * @return {@code true} if defined
     */
    public static boolean isDefined(String jiffleName) {
        return lookup.containsKey(jiffleName);
    }
    
    /**
     * Checks if a function is defined.
     *
     * @param jiffleName the name of the function used in a Jiffle script
     * @param argTypes argument types; null or empty for no-arg functions
     *
     * @return {@code true} if defined
     */
    public static boolean isDefined(String jiffleName, JiffleType ...argTypes) {
        try {
            getInfo(jiffleName, argTypes);
        } catch (UndefinedFunctionException ex) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the info for a function.
     *
     * @param jiffleName the name of the function used in a Jiffle script
     * @param argTypes argument types; null or empty for no-arg functions
     *
     * @return function info
     * @throws UndefinedFunctionException if {@code jiffleName} is not recognized
     */
    public static FunctionInfo getInfo(String jiffleName, JiffleType ...argTypes)
            throws UndefinedFunctionException {

        List<FunctionInfo> fns = lookup.get(jiffleName);
        if (fns != null) {
            for (FunctionInfo info : fns) {
                if (info.matches(jiffleName, argTypes)) {
                    return info;
                }
            }
        }

        // undefined function
        throw new UndefinedFunctionException( String.format(
                "Undefined function: %s(%s)", 
                jiffleName, Strings.commas((Object[])argTypes)
                ));
    }
    
    /**
     * Gets the runtime source for the function. This will consist of
     * provider name plus function name in the case of {@code JiffleFunction}
     * and {@code java.lang.Math} methods, or runtime class field name in the
     * case of proxy (image info) functions.
     *
     * @param jiffleName the name of the function used in a Jiffle script
     * @param argTypes argument types (if present)
     *
     * @return the runtime source
     * @throws UndefinedFunctionException if {@code jiffleName} is not recognized
     */
    public static String getRuntimeExpr(String jiffleName, JiffleType ...argTypes)
            throws UndefinedFunctionException {
        
        return getInfo(jiffleName, argTypes).getRuntimeName();
    }
    
    /**
     * Searches for a function with a script name that matches {@code jiffleName}
     * and gets its return type. This method relies on the fact that Jiffle 
     * has the same return type for all functions with the same root name.
     * 
     * @param jiffleName name to match
     * @return the return type
     * 
     * @throws UndefinedFunctionException if the name is not matched 
     */
    public static JiffleType getReturnType(String jiffleName) throws UndefinedFunctionException {
        List<FunctionInfo> fns = lookup.get(jiffleName);
        if (fns != null) {
            return fns.get(0).getReturnType();
        }
        
        throw new UndefinedFunctionException("Undefined function: " + jiffleName);
    }
    
}
