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
import static org.junit.Assert.assertNotNull;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;

import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * Tests for use of the images block in scripts.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class SpecifyingImageVarsTest extends RuntimeTestBase {
    
    @Test
    public void destVarInImagesBlock() throws Exception {
        System.out.println("   destination image var name in images block");
        String script = 
                  "images { foo = write; }  foo = 42;";
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return 42;
            }
        };
        
        assertScriptResult(script, e, null, "foo");
    }
    
    @Test
    public void sourceAndDestVarsInImagesBlock() throws Exception {
        System.out.println("   source and destination names in images block");

        String script = 
                "images { inimage = read; outimage = write; } outimage = inimage + 1;" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return val + 1;
            }
        };
        
        assertScriptResult(script, e, "inimage", "outimage");
    }
    
    @Test
    public void noDestImage() throws Exception {
        System.out.println("   destination-less script with images block");

        String script = String.format(
                  "images { inimage = read; } \n"
                + "init { n = 0; } \n"
                + "n += inimage >= %d;",
                NUM_PIXELS - 5);
        
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        
        directRuntimeInstance = jiffle.getRuntimeInstance();
        directRuntimeInstance.setSourceImage("inimage", createSequenceImage());
        directRuntimeInstance.evaluateAll(null);
        
        Double var = directRuntimeInstance.getVar("n");
        assertNotNull(var);
        assertEquals(5, var.intValue());
    }
    
    @Test(expected=JiffleException.class)
    public void emptyImagesBlock() throws Exception {
        System.out.println("   empty images block and no parameters causes exception");
        
        String script = "images { } dest = 42;" ;
        
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
    }
    
    private void assertScriptResult(String script, 
            Evaluator e, String srcVarName, String destVarName) throws Exception {
        
        RenderedImage srcImg = null;
        WritableRenderedImage destImg = null;
        
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        
        directRuntimeInstance = (JiffleDirectRuntime) jiffle.getRuntimeInstance();
        
        if (srcVarName != null && srcVarName.length() > 0) {
            srcImg = createSequenceImage();
            directRuntimeInstance.setSourceImage(srcVarName, srcImg);
        }
        
        if (destVarName != null && destVarName.length() > 0) {
            destImg = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0d);
            directRuntimeInstance.setDestinationImage(destVarName, destImg);
        }
        
        directRuntimeInstance.evaluateAll(null);
        assertImage(srcImg, destImg, e);
    }

}
