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

import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.io.File;

import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import it.geosolutions.jaiext.utilities.ImageUtilities;
import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import it.geosolutions.jaiext.swing.ImageFrame;

public class Ripples {

    public static void main(String[] args) {
        Ripples me = new Ripples();
        TiledImage image = ImageUtilities.createConstantImage(300, 300, 0d);
        me.createRipplesImage(image);
        
        ImageFrame frame = new ImageFrame(image, "ripples");
        frame.setSize(550, 550);
        frame.setVisible(true);
    }

    // docs-begin-method
    public void createRipplesImage(WritableRenderedImage destImg) {

        // image dimensions
        final int width = destImg.getWidth();
        final int height = destImg.getHeight();

        // first pixel coordinates
        int x = destImg.getMinX();
        int y = destImg.getMinY();

        // center pixel coordinates
        final int xc = x + destImg.getWidth() / 2;
        final int yc = y + destImg.getHeight() / 2;

        // constant term
        double C = Math.PI * 8;

        WritableRectIter iter = RectIterFactory.createWritable(destImg, null);
        do {
            double dy = ((double) (y - yc)) / yc;
            do {
                double dx = ((double) (x - xc)) / xc;
                double d = Math.sqrt(dx * dx + dy * dy);
                iter.setSample(Math.sin(d * C));
                x++ ;
            } while (!iter.nextPixelDone());

            x = destImg.getMinX();
            y++;
            iter.startPixels();

        } while (!iter.nextLineDone());
    }
    // docs-end-method
    
    public void runScriptWithBuilder(File scriptFile) throws Exception {
        // docs-begin-builder-example
        JiffleBuilder builder = new JiffleBuilder();
        
        // These chained methods read the script from a file,
        // create a new image for the output, and run the script
        builder.script(scriptFile).dest("destImg", 500, 500).run();
        
        RenderedImage result = builder.getImage("destImg");
        // docs-end-builder-example
    }
}
