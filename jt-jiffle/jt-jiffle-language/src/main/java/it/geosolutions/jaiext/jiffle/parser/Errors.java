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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.geosolutions.jaiext.jiffle.parser;

/**
 *
 * @author michael
 */
public enum Errors {
    
    ASSIGNMENT_LIST_TO_SCALAR("Attempting to assign a list to a scalar variable"),
    
    ASSIGNMENT_SCALAR_TO_LIST("Attempting to assign a scalar to a list variable"),
    
    ASSIGNMENT_TO_CONSTANT("Attempting to assign a value to constant"),
    
    ASSIGNMENT_TO_LOOP_VAR("Cannot assign a new value to loop variable"),
    
    CON_CONDITION_MUST_BE_SCALAR("The first (condition) arg in a con expression must be a scalar variable"),
    
    CON_RESULTS_MUST_BE_SAME_TYPE("Alternative return values in a con expression must have same type"),
    
    DUPLICATE_VAR_DECL("Duplicate variable declaration"),
    
    EXPECTED_SCALAR("Expected a scalar value or expression (e.g. 42)"),
    
    IMAGE_VAR_INIT_BLOCK("Image variable cannot be used in init block"),
    
    INVALID_ASSIGNMENT_OP_WITH_DEST_IMAGE(
            "Invalid assignment operator with destination image variable"),
    
    INVALID_BINARY_EXPRESSION("Invalid binary expression"),
    
    LIST_AS_TERNARY_CONDITION("A list variable cannot be used as a condition in a ternary expression"),
    
    LIST_IN_RANGE("A range specifier must have scalar end-points, not list"),
    
    NOT_OP_IS_INVALID_FOR_LIST("Logical negation is not valid with a list variable"),
    
    POW_EXPR_WITH_LIST_EXPONENT("A list variable cannot be used as the exponent in a power expression"),

    INVALID_OPERATION_FOR_LIST("Invalid operation for list variable"),

    READING_FROM_DEST_IMAGE("Attempting to read from destination image"),
    
    UNKNOWN_FUNCTION("Uknown function"),
    
    VAR_UNDEFINED("Variable not initialized prior to use"),
    
    WRITING_TO_SOURCE_IMAGE("Attempting to write to source image"),

    UNDEFINED_SOURCE("Unknown source image"),

    UNINIT_VAR("Variable used before being assigned a value"),

    IMAGE_POS_ON_NON_IMAGE("Image position specifier(s) used with a non-image variable"),

    INVALID_ASSIGNMENT_NOT_DEST_IMAGE("var[x] assignment can only be performed on the output image variable");

    
    private final String msg;
    
    private Errors(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return msg;
    }
    
}
