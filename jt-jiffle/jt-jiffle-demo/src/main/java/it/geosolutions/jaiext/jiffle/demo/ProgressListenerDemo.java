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

package it.geosolutions.jaiext.jiffle.demo;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime;
import it.geosolutions.jaiext.jiffle.runtime.AbstractProgressListener;
import it.geosolutions.jaiext.jiffle.runtime.JiffleExecutor;
import it.geosolutions.jaiext.jiffle.runtime.JiffleExecutorException;


/**
 * Demonstrates using a JiffleProgressListener with JiffleExecutor.
 * <p>
 * Rather than running a real Jiffle task that will take long enough to 
 * be worth using a progress listener, we cheat and use mock Jiffle and
 * JiffleRuntime classes (see bottom of source code). The runtime class
 * pretends process pixels by just having a little sleep each time its
 * {@code evaluate()} method is called.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class ProgressListenerDemo {

    // Number of pixels in the pretend task
    private static final int NUM_PIXELS = 500;
    
    // Milliseconds to spend pretending to process a pixel
    private static final long PIXEL_TIME = 10;

    /**
     * Runs the demo.
     */
    public static void main(String[] args) throws Exception {
        ProgressListenerDemo me = new ProgressListenerDemo();
        me.demo();
    }

    /**
     * This method shows how you might use a progress listener with
     * JiffleExecutor.
     * 
     * @throws JiffleExecutorException 
     */
    private void demo() throws JiffleExecutorException {
        MyProgressListener listener = new MyProgressListener();
        
        /* 
         * The update interval can be set as number of pixels or 
         * a proportion of total task size. Here we use the latter
         * method to request that the listener is notified after
         * each 10% of the task has been completed.
         */
        listener.setUpdateInterval(0.1);
        
        JiffleExecutor executor = new JiffleExecutor();
        executor.submit(new PretendJiffleRuntime(), listener);
    }
    

    /**
     * Our progress listener class extends {@link AbstractProgressListener}
     * and provides start, update and finish methods which we will use to
     * display and update a Swing widget.
     */
    class MyProgressListener extends AbstractProgressListener {
        ProgressMeter meter;

        public MyProgressListener() {
            meter = new ProgressMeter();
            meter.setLocationByPlatform(true);
        }

        public void start() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    meter.setVisible(true);
                }
            });
        }

        public void update(long done) {
            int percent = (int) (100 * (double) done / taskSize);
            meter.update(percent);
        }

        public void finish() {
            meter.done();
        }
    }
    

    /**
     * Simple Swing widget with a progress bar and a button
     * that is enabled when the task is finished.
     */
    class ProgressMeter extends JDialog {
        private JProgressBar bar;
        private JButton btn;

        public ProgressMeter() {
            setTitle("Trying to look busy");
            setSize(400, 80);
            
            initComponents();
            setModal(true);
        }
        
        private void initComponents() {
            JPanel panel = new JPanel(new BorderLayout());
            
            bar = new JProgressBar();
            bar.setMaximum(100);
            panel.add(bar, BorderLayout.NORTH);
            
            btn = new JButton("Working...");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            btn.setEnabled(false);
            panel.add(btn, BorderLayout.SOUTH);
            
            getContentPane().add(panel);
        }

        void update(final int progress) {
            if (EventQueue.isDispatchThread()) {
                bar.setValue(progress);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        bar.setValue(progress);
                    }
                });
            }
        }
        
        void done() {
            final String msg = "Finished";
            
            if (EventQueue.isDispatchThread()) {
                btn.setText(msg);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        btn.setText(msg);
                    }
                });
            }
            btn.setEnabled(true);
        }
    }

    
    /**
     * Mock runtime object that pretends to process pixels by
     * having a little sleep each time.
     */
    class PretendJiffleRuntime extends AbstractDirectRuntime {

        public PretendJiffleRuntime() {
            // set the pretend processing area
            Rectangle bounds = new Rectangle(0, 0, NUM_PIXELS, 1);
            setWorldByResolution(bounds, 1, 1);
        }

        @Override
        protected void initImageScopeVars() {}

        @Override
        protected void initOptionVars() {}

        /**
         * Pretends to process a pixel (very slowly).
         */
        public void evaluate(double x, double y) {
            try {
                Thread.sleep(PIXEL_TIME);

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public long getNumPixels() {
            return NUM_PIXELS;
        }

    }
}
