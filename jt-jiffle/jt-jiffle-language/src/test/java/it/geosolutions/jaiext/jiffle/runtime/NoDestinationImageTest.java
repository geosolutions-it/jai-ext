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
import java.util.Random;

import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * Unit tests for scripts with no destination image.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class NoDestinationImageTest {
    
    private static final int WIDTH = 10;

    private class TestData {
        RenderedImage image;
        int expectedCount;
    }
    
    @Test
    public void countValues() throws Exception {
        System.out.println("   counting pixels that meet a condition");
        
        final int testVal = 10;
        
        String script = String.format(
                  "images { src=read; } \n"
                + "init { count = 0; } \n"
                + "count += src > %d;",
                testVal);
        
        TestData testData = createTestData(testVal);
        
        Jiffle jiffle = getCompiledJiffle(script);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        runtime.setSourceImage("src", testData.image);
        runtime.evaluateAll(null);
        
        Double count = runtime.getVar("count");
        assertNotNull(count);
        assertEquals(testData.expectedCount, count.intValue());
    }
    
    @Test(expected=JiffleException.class)
    public void noImagesAtAll() throws Exception {
        System.out.println("   no source or destination images causes exception");
        
        String script = "answer = 42;" ;
        getCompiledJiffle(script);
    }
    
    private Jiffle getCompiledJiffle(String script) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        
        return jiffle;
    }
    
    private TestData createTestData(int midPoint) {
        Integer[] data = new Integer[WIDTH * WIDTH];
        Random rr = new Random();
        int n = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = (int) (2 * midPoint * rr.nextDouble());
            if (data[i] > midPoint) {
                n++;
            }
        }
        
        TestData testData = new TestData();
        testData.image = ImageUtilities.createImageFromArray(data, WIDTH, WIDTH);
        testData.expectedCount = n;
        return testData;
    }
}
