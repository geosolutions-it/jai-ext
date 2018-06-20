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
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ConstantLookup.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class ConstantLookupTest {
    
    private final double TOL = 1.0e-8;

    @Test
    public void getPI() {
        assertEquals(Math.PI, ConstantLookup.getValue("M_PI"), TOL);
    }

    @Test
    public void getPIOn2() {
        assertEquals(Math.PI / 2.0, ConstantLookup.getValue("M_PI_2"), TOL);
    }

    @Test
    public void getPIOn4() {
        assertEquals(Math.PI / 4.0, ConstantLookup.getValue("M_PI_4"), TOL);
    }

    @Test
    public void getSqrt2() {
        assertEquals(Math.sqrt(2.0), ConstantLookup.getValue("M_SQRT2"), TOL);
    }

    @Test
    public void getE() {
        assertEquals(Math.E, ConstantLookup.getValue("M_E"), TOL);
    }

    @Test
    public void getNanPrefix() {
        assertTrue(Double.isNaN( ConstantLookup.getValue("M_NaN")));
        assertTrue(Double.isNaN( ConstantLookup.getValue("M_NAN")));
    }

    @Test
    public void getNanNoPrefix() {
        assertTrue(Double.isNaN( ConstantLookup.getValue("NaN")));
        assertTrue(Double.isNaN( ConstantLookup.getValue("NAN")));
    }

    @Test(expected=IllegalArgumentException.class)
    public void unknownConstant() {
        ConstantLookup.getValue("NotAConstant");
    }
    
    
}
