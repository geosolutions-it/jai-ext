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
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for runtime object meta-functions.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class RuntimeMetafunctionsTest {

    @Test
    public void getSourceVarNames_ImageParamsMap() throws Exception {
        System.out.println("   setup with map and get source var names");
        
        JiffleDirectRuntime runtime = getRuntimeWithImageParams();
        String[] names = runtime.getSourceVarNames();
        assertEquals(3, names.length);
        
        List<String> expected = new ArrayList<>();
        expected.add("src1");
        expected.add("src2");
        expected.add("src3");
        for (int i = 0; i < names.length; i++) {
            assertTrue(expected.contains(names[i]));
            expected.remove(names[i]);
        }
    }

    @Test
    public void getDestinationVarNames_ImageParamsMap() throws Exception {
        System.out.println("   setup with map and get dest var names");
        
        JiffleDirectRuntime runtime = getRuntimeWithImageParams();
        String[] names = runtime.getDestinationVarNames();
        assertEquals(3, names.length);
        
        List<String> expected = new ArrayList<>();
        expected.add("dest1");
        expected.add("dest2");
        expected.add("dest3");
        for (int i = 0; i < names.length; i++) {
            assertTrue(expected.contains(names[i]));
            expected.remove(names[i]);
        }
    }
    
    @Test
    public void getSourceVarNames_ImagesBlock() throws Exception {
        System.out.println("   use images block and get source var names");
        
        JiffleDirectRuntime runtime = getRuntimeWithImagesBlock();
        String[] names = runtime.getSourceVarNames();
        assertEquals(3, names.length);
        
        List<String> expected = new ArrayList<>();
        expected.add("src1");
        expected.add("src2");
        expected.add("src3");
        for (int i = 0; i < names.length; i++) {
            assertTrue(expected.contains(names[i]));
            expected.remove(names[i]);
        }
    }

    @Test
    public void getDestinationVarNames_ImagesBlock() throws Exception {
        System.out.println("   use images block and get dest var names");
        
        JiffleDirectRuntime runtime = getRuntimeWithImagesBlock();
        String[] names = runtime.getDestinationVarNames();
        assertEquals(3, names.length);
        
        List<String> expected = new ArrayList<>();
        expected.add("dest1");
        expected.add("dest2");
        expected.add("dest3");
        for (int i = 0; i < names.length; i++) {
            assertTrue(expected.contains(names[i]));
            expected.remove(names[i]);
        }
    }
    
    @Test
    public void getImageScopeVarNames() throws Exception {
        String script = 
                  "images { dest=write; } "
                + "init { foo = 1; bar = 2; foz = 3; baz = 4; } "
                + "dest = 42;" ;

        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        String[] names = runtime.getVarNames();
        
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        expected.add("foz");
        expected.add("baz");
        
        assertEquals(expected.size(), names.length);
        for (int i = 0; i < names.length; i++) {
            assertTrue(expected.contains(names[i]));
            expected.remove(names[i]);
        }
    }
    

    private JiffleDirectRuntime getRuntimeWithImageParams() throws Exception {
        String script = "dest1 = src1; dest2 = src2; dest3 = src3;" ;

        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("dest1", Jiffle.ImageRole.DEST);
        imageParams.put("dest2", Jiffle.ImageRole.DEST);
        imageParams.put("dest3", Jiffle.ImageRole.DEST);
        imageParams.put("src1", Jiffle.ImageRole.SOURCE);
        imageParams.put("src2", Jiffle.ImageRole.SOURCE);
        imageParams.put("src3", Jiffle.ImageRole.SOURCE);

        Jiffle jiffle = new Jiffle(script, imageParams);
        return jiffle.getRuntimeInstance();
    }
    
    private JiffleDirectRuntime getRuntimeWithImagesBlock() throws Exception {
        String script = 
                  "images {"
                + "  src1=read; src2=read; src3=read;"
                + "  dest1=write; dest2=write; dest3=write;"
                + "}"
                + "dest1 = src1; dest2 = src2; dest3 = src3;" ;

        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        return jiffle.getRuntimeInstance();
    }
    
}
