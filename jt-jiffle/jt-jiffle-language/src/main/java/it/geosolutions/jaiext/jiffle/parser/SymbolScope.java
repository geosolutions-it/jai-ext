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

import it.geosolutions.jaiext.jiffle.JiffleException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class for symbol scope levels. 
 * <p>
 * Adapted from "Language Implementation Patterns" by Terence Parr,
 * published by The Pragmatic Bookshelf, 2010.
 * 
 * @author michael
 */
/**
 * A symbol scope level. 
 * <p>
 * Adapted from "Language Implementation Patterns" by Terence Parr,
 * published by The Pragmatic Bookshelf, 2010.
 * 
 * @author michael
 */
public abstract class SymbolScope {
    
    protected final String name;
    
    /** Parent scope or {@code null} if top level. */
    protected final SymbolScope enclosingScope;
    
    /** Symbols defined within this scope, keyed by name. */
    protected final Map<String, Symbol> symbols;
    
    
    /**
     * Creates a new instance.
     * 
     * @param name label for this scope
     * @param parent scope or {@code null} if top level
     */
    public SymbolScope(String name, SymbolScope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        symbols = new LinkedHashMap<String, Symbol>();
    }
    
    /**
     * Gets the name of this scope instance.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parent scope or {@code null} if this is the
     * top scope.
     */
    public SymbolScope getEnclosingScope() {
        return enclosingScope;
    }

    /**
     * Adds a symbol to this scope. If a symbol with the
     * same name is already defined, an exception is thrown.
     */
    public void add(Symbol symbol) {
        add(symbol, false);
    }

    /**
     * Adds a symbol to this scope, optionally allowing 
     * replacement of any existing symbol with the same name.
     */
    public void add(Symbol symbol, boolean allowReplacement) {
        if (symbols.containsKey(symbol.getName()) && !allowReplacement) {
            throw new IllegalArgumentException(
                    "Symbol " + symbol.getName() + " is already defined");
        }
        
        symbols.put(symbol.getName(), symbol);
    }
    
    /**
     * Tests if a symbol is defined in this scope or any
     * enclosing scope.
     */
    public boolean has(String name) {
        if (symbols.containsKey(name)) {
            return true;
        } else if (enclosingScope != null) {
            return enclosingScope.has(name);
        }
        return false;
    }

    /**
     * Searches for a symbol in this scope and, if not found,
     * any enclosing scopes.
     * 
     * @return the symbol
     * @throws IllegalArgumentException if the symbol is not found
     */
    public Symbol get(String name) {
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        } else if (enclosingScope != null) {
            return enclosingScope.get(name);
        } else {
            throw new JiffleParserException("Unknown symbol " + name);
        }
    }

    /**
     * Searches for a symbol in this scope and, if not found,
     * any enclosing scopes.
     *
     * @return the symbol
     * @throws IllegalArgumentException if the symbol is not found
     */
    public SymbolScope getDeclaringScope(String name) {
        if (symbols.containsKey(name)) {
            return this;
        } else if (enclosingScope != null) {
            return enclosingScope.getDeclaringScope(name);
        } else {
            throw new IllegalArgumentException(
                    "Unknown symbol " + name + " in scope " + getName());
        }
    }

    /**
     * Returns name of all symbols with the given type, in the current scope and enclosing scopes
     * @param type
     * @return
     */
    public Set<String> getByType(Symbol.Type type) {
        Set<String> result = new LinkedHashSet<>();
        for (Symbol symbol : symbols.values()) {
            if (type.equals(symbol.getType())) {
                result.add(symbol.getName());
            }
        }
        if (enclosingScope != null) {
            result.addAll(enclosingScope.getByType(type));
        }
        
        return result;
    }

}
