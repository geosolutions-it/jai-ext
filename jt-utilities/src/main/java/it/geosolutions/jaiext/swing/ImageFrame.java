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

package it.geosolutions.jaiext.swing;

import java.awt.*;
import java.awt.image.RenderedImage;

import javax.swing.*;


/**
 * A simple display widget with an image pane and a status bar that
 * shows the image location and data value(s) of the mouse cursor.
 * <p>
 * Typical use is:
 *
 * <pre><code>
 * ImageFrame frame = new ImageFrame();
 * frame.displayImage(imageToLookAt, imageWithData, "My beautiful image");
 * </code></pre>
 *
 * Note: the default close operation for the frame is JFrame.EXIT_ON_CLOSE.
 * 
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class ImageFrame extends JFrame {

    private JTextField statusBar;
    private StringBuilder sb;
    
    /**
     * Displays the given image in a new ImageFrame. This method can be
     * safely called from any thread. The frame's closing behaviour will be
     * {@linkplain #EXIT_ON_CLOSE}.
     * 
     * @param img image to display
     * @param title frame title
     */
    public static void showImage(
            final RenderedImage img, 
            final String title) {
        
        showImage(img, title, true);
    }
    
    /**
     * Displays the given image in a new ImageFrame. This method can be
     * safely called from any thread. The frame's closing behaviour will be
     * either {@linkplain #EXIT_ON_CLOSE} or {@linkplain #DISPOSE_ON_CLOSE}
     * depending on the {@code exitOnClose} argument.
     * 
     * @param img image to display
     * @param title frame title
     * @param exitOnClose true for exit on close; false for dispose on close
     */
    public static void showImage(
            final RenderedImage img, 
            final String title, 
            boolean exitOnClose) {
        
        if (SwingUtilities.isEventDispatchThread()) {
            doShowImage(img, title);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    doShowImage(img, title);
                }
            });
        }
    }
    
    private static void doShowImage(RenderedImage img, String title) {
        ImageFrame f = new ImageFrame(img, title);
        f.setVisible(true);
    }

    /**
     * Constructor to display and draw data from a single image
     *
     * @param img the image to display
     * @param title title for the frame
     */
    public ImageFrame(RenderedImage img, String title) {
        this(img, null, title);
    }

    /**
     * Constructor for separate display and data images.
     *
     * @param displayImg image to be displayed
     *
     * @param dataImg an image with bounds equal to, or enclosing, those of
     * displayImg and which contains data that will be reported in the status
     * bar; if null data will be drawn from the display image
     *
     * @param title title for the frame
     */
    public ImageFrame(RenderedImage displayImg, RenderedImage dataImg, String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        ImagePane pane = new ImagePane(this, displayImg, dataImg);
        getContentPane().add(pane, BorderLayout.CENTER);
        
        sb = new StringBuilder();

        statusBar = new JTextField();
        statusBar.setEditable(false);
        statusBar.setMinimumSize(new Dimension(100, 30));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());

        getContentPane().add(statusBar, BorderLayout.SOUTH);

        pack();
        setSize(500, 500);
    }
    
    void setCursorInfo(Point imagePos, int[] imageValues) {
        Integer[] values = new Integer[imageValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.valueOf(imageValues[i]);
        }
        doSetCursorInfo(imagePos, values, "%d");
    }

    void setCursorInfo(Point imagePos, double[] imageValues) {
        Double[] values = new Double[imageValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Double(imageValues[i]);
        }
        doSetCursorInfo(imagePos, values, "%.4f");
    }
    
    private void doSetCursorInfo(Point imagePos, Number[] imageValues, String fmt) {
        sb.setLength(0);
        sb.append("x:").append(imagePos.x).append(" y:").append(imagePos.y);
        
        final int len = imageValues.length;
        if (len == 1) {
            sb.append("  value:");
        } else {
            sb.append("  values:");
        }
        
        for (int i = 0; i < len; i++) {
            sb.append(" ").append(String.format(fmt, imageValues[i]));
        }
        
        setStatusText(sb.toString());
    }

    /**
     * Set the status bar contents. This is used by {@linkplain ImagePane}
     * @param text the text to display
     */
    public void setStatusText(String text) {
        statusBar.setText(text);
    }

}
