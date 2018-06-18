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

import java.util.HashMap;
import java.util.Map;

/**
 * A lookup for named constants used by the Jiffle compiler.
 * <p>
 * The following constants are recognized:
 * <pre>
 * M_E     The base of natural logarithms (e)
 * M_PI    Pi
 * M_PI_2  Pi / 2
 * M_PI_4  Pi / 4
 * M_SQRT2 Squre root of 2
 * </pre>
 * In addition, any of the following can be used for {@code Double.NaN}
 * <pre>
 * M_NaN
 * M_NAN
 * NaN
 * NAN
 * </pre>
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class ConstantLookup {

    private static final Map<String, Double> constants;
    static {
        constants = new HashMap<>();
        
        constants.put("M_E", Math.E);
        constants.put("M_PI", Math.PI);
        constants.put("M_PI_2", Math.PI / 2);
        constants.put("M_PI_4", Math.PI / 4);
        constants.put("M_SQRT2", Math.sqrt(2.0));
        
        // be generous with NaN names
        constants.put("M_NaN", Double.NaN);
        constants.put("M_NAN", Double.NaN);
        constants.put("NaN", Double.NaN);
        constants.put("NAN", Double.NaN);
    }
    
    /**
     * Checks if a constant is recognized by Jiffle.
     * 
     * @param name the name
     * 
     * @return {@code true} if the constant is recognized;
     *         {@code false} otherwise
     */
    public static boolean isDefined(String name) {
        return constants.containsKey(name);
    }

    /**
     * Gets the value of a named constant.
     * 
     * @param name the constant
     * 
     * @return the value
     * 
     * @throws IllegalArgumentException if {@code name} is not recognized
     */
    public static double getValue(String name) {
        Double value = constants.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Unknown constant: " + name);
        }
        return value;
    }
 
}
