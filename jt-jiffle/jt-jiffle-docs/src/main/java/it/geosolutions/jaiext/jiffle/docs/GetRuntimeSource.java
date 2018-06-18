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


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.geosolutions.jaiext.jiffle.docs;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import it.geosolutions.jaiext.jiffle.JiffleException;

/**
 *
 * @author michael
 */
public class GetRuntimeSource {
    
    // docs start getSourceFromJiffleBuilder
    public void getSourceFromJiffleBuilder(String script) throws JiffleException {
        JiffleBuilder builder = new JiffleBuilder();
        builder.script(script);
        
        // Set source and destination parameters, then...
        
        String runtimeSource = builder.getRuntimeSource();
    }
    // docs end getSourceFromJiffleBuilder
    
    
    // docs start getSourceFromJiffleObject
    public void getSourceFromJiffleObject(String script) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        
        // You have to compile the script before getting the runtime
        // source otherwise an Exception will be thrown
        jiffle.compile();
        
        // Get the Java source. The boolean argument specifies that we
        // want the input script copied into the class javadocs
        String runtimeSource = jiffle.getRuntimeSource(true);
    }
    // docs end getSourceFromJiffleObject
    
}
