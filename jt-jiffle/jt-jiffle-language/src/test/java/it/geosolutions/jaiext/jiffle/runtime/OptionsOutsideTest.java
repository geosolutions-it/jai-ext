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

package it.geosolutions.jaiext.jiffle.runtime;

import static org.junit.Assert.assertEquals;

import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * Unit tests for script options.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class OptionsOutsideTest extends RuntimeTestBase {
    
    private JiffleBuilder builder;
    
    @Before
    public void setup() {
        builder = new JiffleBuilder();
    }

    
    @Test
    public void outsideOptionWithNeighbourhoodRefs() throws Exception {
        System.out.println("   using outside option with neighbourhood refs");
        String script = 
                  "options {outside = 0;} \n"
                + "n = 0;"
                + "foreach(iy in -1:1) { \n"
                + "  foreach(ix in -1:1) { \n"
                + "    n += src[ix, iy]; \n"
                + "  } \n"
            + "} \n"
                + "dest = n;";
        
        Integer[] srcData = {
            0, 0, 0, 0,
            0, 1, 0, 0,
            0, 1, 1, 0,
            0, 0, 0, 0
        };
        
        RenderedImage srcImg = ImageUtilities.createImageFromArray(srcData, 4, 4);
        
        builder.script(script).source("src", srcImg).dest("dest", 4, 4).run();
        
        RenderedImage result = builder.getImage("dest");
        
        int[] expectedData = {
            1, 1, 1, 0,
            2, 3, 3, 1,
            2, 3, 3, 1,
            1, 2, 2, 1
        };
        
        int k = 0;
        Raster raster = result.getData();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(expectedData[k++], raster.getSample(x, y, 0));
            }
        }
    }

    @Test(expected=JiffleRuntimeException.class)
    public void readOutsideBoundsWithOptionNotSet() throws Exception {
        System.out.println("   reading outside image bounds with option not set");
        
        String script = "dest = src[$-1, 0];";
        RenderedImage srcImg = ImageUtilities.createConstantImage(4, 4, 0);
        
        builder.script(script).source("src", srcImg).dest("dest", 4, 4).run();
    }
    
    @Test
    public void outsideEqualsNull() throws Exception {
        System.out.println("   outside = null");
        assertOutsideEqualsValue("null", Double.NaN);
    }

    @Test
    public void outsideEqualsNaN() throws Exception {
        System.out.println("   outside = NaN");
        assertOutsideEqualsValue("NaN", Double.NaN);
    }

    @Test
    public void outsideEqualsNamedConstant() throws Exception {
        System.out.println("   outside = M_PI");
        assertOutsideEqualsValue("M_PI", Math.PI);
    }
    
    private void assertOutsideEqualsValue(String stringValue, final Double expectedValue) 
            throws Exception {
        String script = "options { outside = " + stringValue + "; } dest = src[-1, 0];" ;
        RenderedImage srcImg = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 1);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = x == 0 ? expectedValue : 1;
                move();
                return z;
            }
        };
        
        testScript(script, srcImg, e);
    }
}
