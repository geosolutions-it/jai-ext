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


package it.geosolutions.jaiext.jiffle.docs;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;
import java.util.Map;

import it.geosolutions.jaiext.utilities.ImageUtilities;
import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import it.geosolutions.jaiext.jiffle.JiffleException;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransforms;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;
import it.geosolutions.jaiext.swing.ImageFrame;


public class RunProportionalRipples {
    
    public static void main(String[] args) throws JiffleException {
        RunProportionalRipples self = new RunProportionalRipples();
        
        String script = 
                "init { C = M_PI * 8; }"
                + "dx = 2*(x() - 0.5); \n"
                + "dy = 2*(y() - 0.5); \n"
                + "d = sqrt(dx*dx + dy*dy); \n"
                + "destImg = sin(C * d);" ;
        
        WritableRenderedImage destImage = ImageUtilities.createConstantImage(500, 500, 0d);
        self.runScriptWithJiffle(script, "destImg", destImage);
        
        ImageFrame frame = new ImageFrame(destImage, "Ripples");
        frame.setSize(550, 550);
        frame.setVisible(true);
    }

    // docs start jiffle method
    public void runScriptWithJiffle(String script, String destVar, WritableRenderedImage destImage) 
            throws JiffleException {
        
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        
        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put(destVar, Jiffle.ImageRole.DEST);
        jiffle.setImageParams(imageParams);
        
        jiffle.compile();
        JiffleDirectRuntime runtimeObj = jiffle.getRuntimeInstance();
        
        // Image bounds are taken from the destination image
        Rectangle imageBounds = new Rectangle(
                destImage.getMinX(), destImage.getMinY(),
                destImage.getWidth(), destImage.getHeight());
        
        // The world bounds are the unit rectangle
        Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, 1, 1);
        
        // We use the CoordinateTransforms helper class to create a transform that
        // will convert proportional coordinates into pixel positions.
        CoordinateTransform transform = CoordinateTransforms.unitBounds(imageBounds);

        // Set the world bounds and resolution
        runtimeObj.setWorldByNumPixels(worldBounds, destImage.getWidth(), destImage.getHeight());
        
        // Associate the image and its transform with the destination variable 
        // name used in the script
        runtimeObj.setDestinationImage(destVar, destImage, transform);
        
        // Execute the runtime object. This will write results into destImage.
        runtimeObj.evaluateAll(null);
    }
    // docs end jiffle method
    
    // docs start builder method
    public void runScriptWithBuilder(String script, String destVar, WritableRenderedImage destImage) 
            throws JiffleException {
        
        // Image bounds are taken from the destination image
        Rectangle imageBounds = new Rectangle(
                destImage.getMinX(), destImage.getMinY(),
                destImage.getWidth(), destImage.getHeight());
        
        // The world bounds are the unit rectangle
        Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, 1, 1);
        
        CoordinateTransform transform = CoordinateTransforms.unitBounds(imageBounds);
        
        JiffleBuilder builder = new JiffleBuilder();
        
        // Set the processing area (world units)
        builder.worldAndNumPixels(worldBounds, destImage.getWidth(), destImage.getHeight());
        
        // Set the script and the destination image with its transform
        builder.script(script).dest(destVar, destImage, transform);
        
        // This executes the script and writes the results into destImage
        builder.run();
    }
    // docs end builder method
}
