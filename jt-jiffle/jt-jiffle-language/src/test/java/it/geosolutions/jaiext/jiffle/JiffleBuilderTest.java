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

package it.geosolutions.jaiext.jiffle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;
import it.geosolutions.jaiext.jiffle.runtime.RuntimeTestBase;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.io.File;
import java.net.URL;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * Unit tests for the JiffleBuilder helper class.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleBuilderTest extends RuntimeTestBase {
    
    private JiffleBuilder jb;
    
    @Before
    public void setup() {
        jb = new JiffleBuilder();
    }

    @Test
    public void runBasicScript() throws Exception {
        System.out.println("   basic script with provided dest image");
        String script = "dest = con(src1 > 10, src1, null);" ;

        RenderedImage srcImg1 = createSequenceImage();
        WritableRenderedImage destImg = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0d);

        jb.script(script).source("src1", srcImg1).dest("dest", destImg);
        JiffleDirectRuntime runtime = jb.getRuntime();
        runtime.evaluateAll(null);

        Evaluator e = new Evaluator() {
            public double eval(double val) {
                return val > 10 ? val : Double.NaN;
            }
        };

        assertImage(srcImg1, destImg, e);
    }
    
    @Test
    public void builderCreatesDestImage() throws Exception {
        System.out.println("   builder creating dest image");
        String script = "init { n = 0; } dest = n++ ;" ;

        jb.dest("dest", IMG_WIDTH, IMG_WIDTH).script(script).getRuntime().evaluateAll(null);
        RenderedImage img = jb.getImage("dest");

        assertNotNull(img);
        
        RandomIter iter = RandomIterFactory.create(img, null);
        int k = 0;
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                assertEquals(k, iter.getSample(x, y, 0));
                k++ ;
            }
        }
    }

    @Test
    public void destImageWithRect() throws Exception {
        System.out.println("   dest image from Rectangle bounds");

        int w = 10;
        int h = 20;
        jb.dest("dest", new Rectangle(0, 0, w, h));
        RenderedImage img = jb.getImage("dest");
        assertNotNull(img);
        assertEquals(w, img.getWidth());
        assertEquals(h, img.getHeight());
    }

    @Test
    public void clearingDestImage() throws Exception {
        System.out.println("   clear builder dest image");

        int w = 10;
        int h = 20;
        jb.dest("dest", new Rectangle(0, 0, w, h));
        RenderedImage img = jb.getImage("dest");
        assertNotNull(img);
        
        jb.clear();
        img = jb.getImage("dest");
        assertNull(img);
    }
    
    @Test
    public void runMethod() throws Exception {
        System.out.println("   using run() method");
        String script = "dest = x();";
        
        jb.script(script).dest("dest", 10, 10).run();
        RenderedImage img = jb.getImage("dest");
        assertNotNull(img);
        
        Evaluator e = new Evaluator() {
            int x = 0;
            public double eval(double val) {
                int xx = x;
                x = (x + 1) % IMG_WIDTH;
                return xx;
            }
        };
        
        assertImage(null, img, e);
    }

    @Test
    public void scriptFile() throws Exception {
        System.out.println("   loading script file");
        URL url = JiffleBuilderTest.class.getResource("constant.jfl");
        File scriptFile = new File(url.toURI());
        
        jb.script(scriptFile).dest("dest", 10, 10).run();
        
        // no checking of dest image - just as long as we didn't
        // get an exception we are happy
    }
    
    @Test
    public void removeImage() throws Exception {
        System.out.println("   remove image");
        String script = "dest = 42;" ;
        
        jb.script(script).dest("dest", 10, 10).run();
        RenderedImage image = jb.removeImage("dest");
        assertNotNull(image);
        
        image = jb.getImage("dest");
        assertNull(image);
    }
    
}
