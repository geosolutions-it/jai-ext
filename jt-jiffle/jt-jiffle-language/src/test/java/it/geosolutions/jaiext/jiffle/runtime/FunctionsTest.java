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
import static org.junit.Assert.assertTrue;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import it.geosolutions.jaiext.numeric.CompareOp;
import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * Unit tests for general functions
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class FunctionsTest extends RuntimeTestBase {
    
    @Test(expected=JiffleException.class)
    public void undefinedFunctionName() throws Exception {
        System.out.println("   undefined function name");
        String script = "dest = foo(src);" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                throw new UnsupportedOperationException("Should not be called");
            }
        };
        
        testScript(script, e);
    }
    
    @Test(expected=JiffleException.class)
    public void wrongNumArgs() throws Exception {
        System.out.println("   wrong number of args");
        String script = "dest = sqrt(src, 2, 3);" ;
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                throw new UnsupportedOperationException("Should not be called");
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void abs() throws Exception {
        String script = "dest = abs(src - 50);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.abs(val - 50);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void acos() throws Exception {
        String script = "dest = acos(x() / width());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = Math.acos((double)x / IMG_WIDTH);
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void asin() throws Exception {
        String script = "dest = asin(x() / width());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = Math.asin((double)x / IMG_WIDTH);
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void atan() throws Exception {
        String script = "dest = atan(x() / width());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = Math.atan((double)x / IMG_WIDTH);
                move();
                return z;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void degToRad() throws Exception {
        String script = "dest = degToRad(src);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.PI * val / 180;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void exp() throws Exception {
        String script = "dest = exp(src);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.exp(val);
            }
        };
    }
    
    @Test
    public void floor() throws Exception {
        String script = "dest = floor(src / 10);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.floor(val / 10);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void isinf() throws Exception {
        String script = "dest = isinf(1 / x());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                Double z = 1.0 / x;
                move();
                return z.isInfinite() ? 1.0 : 0.0;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void isnan() throws Exception {
        String script = "dest = isnan(y() / x());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                Double z = (double)y / x;
                move();
                return z.isNaN() ? 1.0 : 0.0;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void isnull() throws Exception {
        String script = "dest = isnull(y() / x());" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                Double z = (double)y / x;
                move();
                return z.isNaN() ? 1.0 : 0.0;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void log1Arg() throws Exception {
        String script = "dest = log(src + 1);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.log(val + 1);
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void log2Arg() throws Exception {
        String script = "dest = log(src + 1, 10);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.log10(val + 1);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void radToDeg() throws Exception {
        String script = "dest = radToDeg(src / 10);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return (val / 10) * 180 / Math.PI;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void rand() throws Exception {
        String script = "dest = src + rand(src);" ;
        System.out.println("   " + script);
        
        imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);
        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        RenderedImage src = createRowValueImage();
        WritableRenderedImage dest = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0d);
        
        runtime.setSourceImage("src", src);
        runtime.setDestinationImage("dest", dest);
        runtime.evaluateAll(null);

        RectIter srcIter = RectIterFactory.create(src, null);
        RectIter destIter = RectIterFactory.create(dest, null);
        
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                double val = srcIter.getSampleDouble();
                double z = destIter.getSampleDouble();
                assertTrue(CompareOp.acompare(z, val) >= 0);
                assertTrue(CompareOp.acompare(z, 2*val) <= 0);
                
                srcIter.nextPixelDone();
                destIter.nextPixelDone();
            }
            srcIter.nextLineDone();
            srcIter.startPixels();
            destIter.nextLineDone();
            destIter.startPixels();
        }
    }
    
    @Test
    public void randInt() throws Exception {
        String script = "dest = src + randInt(src + 1);" ;
        System.out.println("   " + script);
        
        imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);
        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        RenderedImage src = createRowValueImage();
        WritableRenderedImage dest = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0d);
        
        runtime.setSourceImage("src", src);
        runtime.setDestinationImage("dest", dest);
        runtime.evaluateAll(null);

        RectIter srcIter = RectIterFactory.create(src, null);
        RectIter destIter = RectIterFactory.create(dest, null);
        
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                double val = srcIter.getSampleDouble();
                double z = destIter.getSampleDouble();
                assertEquals(Math.round(z), z, CompareOp.DTOL);
                assertTrue(CompareOp.acompare(z, val) >= 0);
                assertTrue(CompareOp.acompare(z, 2*val + 1) <= 0);
                
                srcIter.nextPixelDone();
                destIter.nextPixelDone();
            }
            srcIter.nextLineDone();
            srcIter.startPixels();
            destIter.nextLineDone();
            destIter.startPixels();
        }
    }
    
    @Test
    public void round1Arg() throws Exception {
        String script = "dest = round(src / (width() - 1));" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return Math.round(val / (IMG_WIDTH - 1));
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void round2Arg() throws Exception {
        String script = "dest = round(src / (width() - 1), 2);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {
            public double eval(double val) {
                double z = val / (IMG_WIDTH - 1);
                return Math.round(z / 2) * 2;
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void sin() throws Exception {
        String script = "dest = sin(src);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.sin(val);
            }
        };
        
        testScript(script, e);
    }

    @Test
    public void sqrt() throws Exception {
        String script = "dest = sqrt(src);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.sqrt(val);
            }
        };
        
        testScript(script, e);
    }
    
    @Test
    public void tan() throws Exception {
        String script = "dest = tan(src);" ;
        System.out.println("   " + script);
        
        Evaluator e = new Evaluator() {

            public double eval(double val) {
                return Math.tan(val);
            }
        };
        
        testScript(script, e);
    }

}
