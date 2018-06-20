/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

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

import javax.swing.*;

/**
 * A progress bar in a frame with a title and optional label.
 * This was written to be used with JAITools demonstration
 * programs.
 * <p>
 * The update methods {@linkplain #setProgress(float progress)}
 * and {@linkplain #setLabel(String label)} may be called from
 * any thread. If the calling thread is not the AWT event dispatch
 * thread the updates will be passed to the dispatch thread.
 * 
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class ProgressMeter extends JFrame {
    
    private static final int MIN_PROGRESS = 0;
    private static final int MAX_PROGRESS = 100;
    
    JProgressBar progBar;
    JLabel label;
    private boolean preset;


    /**
     * Constructor
     */
    public ProgressMeter() {
        this("Progress");
    }

    /**
     * Creates a new progress bar.
     * 
     * @param title the progress bar title
     */
    public ProgressMeter(String title) {
        this(title, null);
    }

    /**
     * Creates a new progress bar
     * 
     * @param title the progress bar title
     * @param labelText label to display in the body of the progress bar
     */
    public ProgressMeter(String title, String labelText) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        progBar = new JProgressBar(MIN_PROGRESS, MAX_PROGRESS);
        getContentPane().add(progBar, BorderLayout.CENTER);
        
        label = new JLabel("  ");
        if (labelText != null && labelText.length() > 0) {
            label.setText(labelText);
        }
        getContentPane().add(label, BorderLayout.SOUTH);

        setSize(400, 60);
        setLocationByPlatform(true);
    }
    
    /**
     * Updates the progress bar. It is safe to call this method from any thread.
     * 
     * @param progress a proportion value between 0 and 1
     */
    public void setProgress(final float progress) {
        final int barValue = (int)Math.ceil((MAX_PROGRESS - MIN_PROGRESS) * progress);

        if (isVisible()) {
            if (EventQueue.isDispatchThread()) {
                progBar.setValue(barValue);

            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progBar.setValue(barValue);
                    }
                });
            }

        } else {
            progBar.setValue(barValue);
            preset = true;
        }
    }

    /**
     * Updates the progress label. It is safe to call this method from any thread.
     * 
     * @param text the new label text
     */
    public void setLabel(final String text) {
        if (!isVisible() || EventQueue.isDispatchThread()) {
            label.setText(text);

        } else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    label.setText(text);
                }
            });
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b == true) {
            if (!preset) {
                progBar.setValue(MIN_PROGRESS);
            }
        }
        super.setVisible(b);
    }

}
