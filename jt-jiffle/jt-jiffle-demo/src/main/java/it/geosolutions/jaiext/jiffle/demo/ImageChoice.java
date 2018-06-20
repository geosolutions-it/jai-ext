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



package it.geosolutions.jaiext.jiffle.demo;

/**
 * Constants to identify Jiffle scripts used to create example
 * images for JAI-tools demo applications.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public enum ImageChoice {

    /**
     * Chessboard pattern with 0 and 1 values.
     */
    CHESSBOARD("chessboard", "result"), 
    /**
     * Complex interference pattern.
     */
    INTERFERENCE("interference", "result"), 
    /**
     * Binary image of the Mandelbrot set.
     */
    MANDELBROT("mandelbrot", "result"),
    /**
     * Concentric, sinusoidal ripples.
     */
    RIPPLES("ripple", "result"), 
    /**
     * Sort of a square circle thing.
     */
    SQUIRCLE("squircle", "result");

    private String name;
    private String destImageVarName;

    private ImageChoice(String name, String destImageVarName) {
        this.name = name;
        this.destImageVarName = destImageVarName;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public String getDestImageVarName() {
        return destImageVarName;
    }
}
