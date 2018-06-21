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

import java.util.Arrays;
import java.util.List;

/**
 * Holds information about a supported Jiffle script option.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
class OptionInfo {
    
    public static final String ANY_STRING = "ANY_STRING";
    public static final String ANY_NUMBER = "ANY_NUMBER";
    public static final String NULL_KEYWORD = "NULL_KEYWORD";

    private final String name;
    private final List<String> validValues;
    
    public OptionInfo(String name, String[] validValues) {
        this.name = name;
        this.validValues = Arrays.asList(validValues);
    }

    public String getName() {
        return name;
    }

    public boolean isValidValue(String value) {
        // Is it the null keyword ?
        if ("null".equalsIgnoreCase(value)) {
            return validValues.contains(NULL_KEYWORD);
        }
        
        // Is it a named constant ?
        if (ConstantLookup.isDefined(value)) {
            return validValues.contains(ANY_NUMBER);
        }
        
        // Is it a number ?
        boolean numeric = true;
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            numeric = false;
        }
        
        if (numeric) {
            return validValues.contains(ANY_NUMBER);
        }
        
        // Final test
        return validValues.contains(ANY_STRING);
    }
    
}
