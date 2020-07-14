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

import it.geosolutions.jaiext.jiffle.Jiffle;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * Base class for unit tests of runtime methods.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class RuntimeTestBase {

    protected static final int IMG_WIDTH = 10;
    protected static final int NUM_PIXELS = IMG_WIDTH * IMG_WIDTH;
    protected static final double TOL = 1.0e-8;
    
    private final JiffleProgressListener nullListener = new NullProgressListener();
    
    protected Map<String, Jiffle.ImageRole> imageParams;
    protected JiffleDirectRuntime directRuntimeInstance;
    protected JiffleIndirectRuntime indirectRuntimeInstance;
    
    static {
        System.setProperty("org.codehaus.janino.source_debugging.enable", "true");
        new File("./target/janino").mkdir();
        System.setProperty("org.codehaus.janino.source_debugging.dir", "./target/janino");
    }

    public abstract class Evaluator {
        int x = 0;
        int y = 0;
        
        public void move() {
            if (++x >= IMG_WIDTH) {
                x = 0;
                y++ ;
            }
        }
        
        public abstract double eval(double val);
        
        public void reset() {
            this.x = 0;
            this.y = 0;
        }
    }
    
    protected TiledImage createSequenceImage() {
        TiledImage img = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0.0);
        int k = 0;
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                img.setSample(x, y, 0, k++);
            }
        }
        return img;
    }

    protected TiledImage createTriangleImage() {
        TiledImage img = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0.0);
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                img.setSample(x, y, 0, x > y ? 1 : 0);
            }
        }
        return img;
    }
    
    protected TiledImage createRowValueImage() {
        TiledImage img = ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, 0.0);
        for (int y = 0; y < IMG_WIDTH; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                img.setSample(x, y, 0, y);
            }
        }
        return img;
    }

    protected void testScript(String script, Evaluator evaluator) throws Exception {
        RenderedImage srcImg = createSequenceImage();
        testScript(script, srcImg, evaluator);
    }

    protected void testScript(String script, RenderedImage srcImg, Evaluator evaluator) throws Exception {
        imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);

        // test the direct runtime
        Jiffle jiffle = new Jiffle(script, imageParams);
        directRuntimeInstance = jiffle.getRuntimeInstance();
        testDirectRuntime(srcImg, directRuntimeInstance, evaluator);
        
        // and now the indirect one
        indirectRuntimeInstance =
                (JiffleIndirectRuntime) jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT);
        evaluator.reset();
        testIndirectRuntime(srcImg, indirectRuntimeInstance, evaluator);
    }

    protected void testDirectRuntime(RenderedImage srcImg, JiffleDirectRuntime runtime, Evaluator evaluator) {
        runtime.setSourceImage("src", srcImg);

        TiledImage destImg = ImageUtilities.createConstantImage(
                srcImg.getMinX(), srcImg.getMinY(), srcImg.getWidth(), srcImg.getHeight(), 0.0);
        runtime.setDestinationImage("dest", destImg);

        runtime.evaluateAll(nullListener);
        assertImage(srcImg, destImg, evaluator);
    }

    protected void testIndirectRuntime(RenderedImage srcImg, JiffleIndirectRuntime runtime, Evaluator evaluator) {
        runtime.setSourceImage("src", srcImg);

        double[] actual = new double[1];
        if (srcImg != null) {
            RectIter srcIter = RectIterFactory.create(srcImg, null);

            int x = srcImg.getMinX(), y = srcImg.getMinY();
            do {
                do {
                    double expected = evaluator.eval(srcIter.getSampleDouble());
                    runtime.evaluate(x, y, actual);
                    assertEquals(
                            "Got "
                                    + expected
                                    + " instead of "
                                    + actual[0]
                                    + " at row "
                                    + y
                                    + " and col "
                                    + x,
                            expected,
                            actual[0],
                            TOL);
                    x++;
                    if (x >= (srcImg.getMinX() + srcImg.getWidth())) {
                        x = srcImg.getMinX();
                        y++;
                    }
                } while (!srcIter.nextPixelDone());

                srcIter.startPixels();
            } while (!srcIter.nextLineDone());
        } else {
            final int minX = srcImg.getMinX();
            final int minY = srcImg.getMinY();
            final int maxX = srcImg.getMinX() + srcImg.getWidth();
            final int maxY = srcImg.getMinY() + srcImg.getHeight();

            for (int y = minY; y < maxY; y ++) {
                for (double x = minX; x < maxX; x ++) {
                    double expected = evaluator.eval(0);
                    runtime.evaluate(x, y, actual);
                    assertEquals(
                            "Got "
                                    + expected
                                    + " instead of "
                                    + actual[0]
                                    + " at row "
                                    + y
                                    + " and col "
                                    + x,
                            expected,
                            actual[0],
                            TOL);
                }
            }
        }
        
    }

    protected void assertImage(RenderedImage srcImg, RenderedImage destImg, Evaluator evaluator) {
        RectIter destIter = RectIterFactory.create(destImg, null);
        
        if (srcImg != null) {
            RectIter srcIter = RectIterFactory.create(srcImg, null);
            
            do {
                do {
                    assertEquals(evaluator.eval(srcIter.getSampleDouble()), destIter.getSampleDouble(), TOL);
                    destIter.nextPixelDone();
                } while (!srcIter.nextPixelDone());
                
                srcIter.startPixels();
                destIter.startPixels();
                destIter.nextLineDone();
                
            } while (!srcIter.nextLineDone());
            
        } else {
            do {
                do {
                    assertEquals(evaluator.eval(0), destIter.getSampleDouble(), TOL);
                } while (!destIter.nextPixelDone());
                
                destIter.startPixels();
                
            } while (!destIter.nextLineDone());
        }
    }

}

