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

package it.geosolutions.jaiext.jiffle.parser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

class CompareSourceDialog extends JDialog {
    private static final long serialVersionUID = -8640087805737551918L;

    boolean accept = false;

    public CompareSourceDialog(String expected, String actual, boolean showCommands) {
        JPanel content = new JPanel(new BorderLayout());
        this.setContentPane(content);
        this.setTitle("SourceAssert");

        JPanel central = new JPanel(new GridLayout(1, 2));
        central.add(new TextArea(expected));
        central.add(new TextArea( actual));
        content.add(central);

        JPanel commands = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton accept = new JButton("Overwrite reference");
        accept.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        CompareSourceDialog.this.accept = true;
                        CompareSourceDialog.this.setVisible(false);
                    }
                });
        JButton reject = new JButton("Sources are different");
        reject.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        CompareSourceDialog.this.accept = false;
                        CompareSourceDialog.this.setVisible(false);
                    }
                });
        commands.add(accept);
        commands.add(reject);
        commands.setVisible(showCommands);
        content.add(commands, BorderLayout.SOUTH);
        pack();
    }

    public static boolean show(String expected, String actual, boolean showCommands) {
        CompareSourceDialog dialog = new CompareSourceDialog(expected, actual, showCommands);
        dialog.setModal(true);
        dialog.setVisible(true);

        return dialog.accept;
    }
}
