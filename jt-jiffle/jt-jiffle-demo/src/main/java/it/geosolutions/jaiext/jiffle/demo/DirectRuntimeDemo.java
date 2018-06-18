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



package it.geosolutions.jaiext.jiffle.demo;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.TiledImage;

import it.geosolutions.jaiext.swing.ImageFrame;
import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * Demonstrates how to retrieve and use a runtime object from a compiled 
 * Jiffle script.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class DirectRuntimeDemo extends it.geosolutions.jaiext.jiffle.demo.JiffleDemoBase {

    /**
     * Run the demonstration. The optional {@code arg} can be either
     * the path to a user-supplied script file or one of "chessboard",
     * "interference", "ripple" or "squircle".
     * 
     * @param args (optional) the script to run
     * @throws Exception on an error in the Jiffle compiler
     */
    public static void main(String[] args) throws Exception {
        DirectRuntimeDemo demo = new DirectRuntimeDemo();
        File f = it.geosolutions.jaiext.jiffle.demo.JiffleDemoHelper.getScriptFile(args, ImageChoice.RIPPLES);
        demo.compileAndRun(f);
    }

    /**
     * Compiles a script read from a file and submits it for execution.
     * 
     * @param scriptFile file containing the Jiffle script
     * @throws Exception on an error in the Jiffle compiler
     */
    public void compileAndRun(File scriptFile) throws Exception {
        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("result", Jiffle.ImageRole.DEST);

        Jiffle jiffle = new Jiffle(scriptFile, imageParams);

        Map<String, RenderedImage> images = new HashMap<>();
        images.put("result",
                ImageUtilities.createConstantImage(WIDTH, HEIGHT, Double.valueOf(0d)));

        if (jiffle.isCompiled()) {
            JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();

            final TiledImage destImg = ImageUtilities.createConstantImage(WIDTH, HEIGHT, 0d);
            runtime.setDestinationImage("result", destImg);
            
            runtime.evaluateAll(null);
            
            ImageFrame frame = new ImageFrame(destImg, "Jiffle image demo");
            frame.setVisible(true);
        }
    }

}
