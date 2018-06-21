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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.geosolutions.jaiext.jiffle.parser.JiffleParserException;
import it.geosolutions.jaiext.jiffle.runtime.JiffleRuntime;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for basic Jiffle object creation, setting attributes and compiling.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleBasicTest {
    
    private Jiffle jiffle;
    private Map<String, Jiffle.ImageRole> imageParams;
    
    @Before
    public void setup() {
        jiffle = new Jiffle();
        imageParams = new HashMap<>();
    }
    
    @Test
    public void blankInstance() {
        System.out.println("   creating an empty Jiffle object");
        
        assertEquals("", jiffle.getScript());
        assertTrue(jiffle.getImageParams().isEmpty());
        assertFalse(jiffle.isCompiled());
    }
    
    @Test
    public void setScript() throws Exception {
        System.out.println("   set and get the script");
        
        String script = "dest = 42;";
        jiffle.setScript(script);
        
        String result = jiffle.getScript();
        assertTrue(result.contains(script));
    }
    
    @Test
    public void setImageParams() {
        System.out.println("   set and get image params");
        
        imageParams.put("src1", Jiffle.ImageRole.SOURCE);
        imageParams.put("src2", Jiffle.ImageRole.SOURCE);
        imageParams.put("dest1", Jiffle.ImageRole.DEST);
        imageParams.put("dest2", Jiffle.ImageRole.DEST);
        jiffle.setImageParams(imageParams);
        
        Map<String, Jiffle.ImageRole> result = jiffle.getImageParams();
        assertEquals(imageParams.size(), result.size());
        for (String name : imageParams.keySet()) {
            assertTrue(imageParams.get(name).equals(result.get(name)));
        }
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void tryToModifyImageParams() {
        System.out.println("   trying to modify map returned by getImageParams");
        
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle.setImageParams(imageParams);
        
        Map<String, Jiffle.ImageRole> unmodifiableMap = jiffle.getImageParams();
        
        // this should provoke an exception
        unmodifiableMap.clear();
    }

    @Test
    public void resetScript() throws Exception {
        System.out.println("   resetScript");
        
        String script1 = "dest = 42;";
        String script2 = "dest = foo;";
        
        jiffle.setScript(script1);
        jiffle.setScript(script2);
        
        String result = jiffle.getScript();
        assertFalse(result.contains(script1));
        assertTrue(result.contains(script2));
    }
    
    @Test
    public void compileValidScript() throws Exception {
        System.out.println("   compile valid script");
        
        String script = "dest = 42;";
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        
        jiffle.setScript(script);
        jiffle.setImageParams(imageParams);
        jiffle.compile();
        
        assertTrue(jiffle.isCompiled());
    }

    @Test(expected=JiffleParserException.class)
    public void compileInvalidScriptThrowsException() throws Exception {
        System.out.println("   compile invalid script and get exception");
        
        // script with an uninitialized variable
        String script = "dest = x;";
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        
        jiffle.setScript(script);
        jiffle.setImageParams(imageParams);
        jiffle.compile();
    }

    @Test
    public void compileInvalidScriptAndCheckStatus() throws Exception {
        System.out.println("   compile invalid script and check status");
        
        // script with an uninitialized variable
        String script = "dest = x;";
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        
        jiffle.setScript(script);
        jiffle.setImageParams(imageParams);
        
        try {
            jiffle.compile();
        } catch (JiffleParserException ignored) {}
        
        assertFalse(jiffle.isCompiled());
    }
    
    @Test(expected=JiffleException.class)
    public void compileWithNoScript() throws Exception {
        System.out.println("   compile with no script set");
        
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle.setImageParams(imageParams);
        jiffle.compile();
    }

    @Test(expected=JiffleException.class)
    public void compileScriptWithoutImageParams() throws Exception {
        System.out.println("   compile script with missing image params");
        
        String script = "dest = 42;";
        
        jiffle.setScript(script);
        jiffle.compile();
    }
    
    @Test
    public void setName() {
        System.out.println("   set name");
        
        String name = "foo";
        jiffle.setName(name);
        assertEquals(name, jiffle.getName());
    }
    
    @Test
    public void scriptWithParamsConstructor() throws Exception {
        System.out.println("   Jiffle(script, imageParams)");
        
        String script = "dest = 42;";
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle = new Jiffle(script, imageParams);
        
        assertTrue(jiffle.isCompiled());
    }
    
    @Test(expected=JiffleException.class)
    public void passingEmptyScriptToConstructor() throws Exception {
        System.out.println("   Jiffle(script, imageParams) with empty script");
        
        String script = "";
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle = new Jiffle(script, imageParams);
    }
    
    
    @Test
    public void fileWithParamsConstructor() throws Exception {
        System.out.println("   Jiffle(scriptFile, imageParams)");
        
        URL url = JiffleBasicTest.class.getResource("constant.jfl");
        File file = new File(url.toURI());
        
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle = new Jiffle(file, imageParams);
        
        assertTrue(jiffle.isCompiled());
    }
    
    @Test(expected=JiffleException.class)
    public void getRuntimeBeforeCompiling() throws Exception {
        System.out.println("   getRuntimeInstance before compiling");
        
        String script = "dest = 42;";
        jiffle.setScript(script);
        
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        jiffle.setImageParams(imageParams);
        
        JiffleRuntime runtime = jiffle.getRuntimeInstance();
    }    
}
