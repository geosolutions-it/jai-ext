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



package it.geosolutions.jaiext.jiffle.demo;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.RenderedImage;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import it.geosolutions.jaiext.jiffle.JiffleException;

import it.geosolutions.jaiext.swing.SimpleImagePane;

/**
 * A browser for Jiffle example scripts. Displays the script in a text
 * window and the destination image in an adjacent window.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class JiffleDemo extends JFrame {
    private SimpleImagePane imagePane;
    private JTextArea scriptPane;
    private JSplitPane splitPane;
    
    private int imageWidth = 400;
    private int imageHeight = 400;

    
    public static void main(String[] args) {
        JiffleDemo me = new JiffleDemo();
        me.setSize(800, 500);
        me.setVisible(true);
    }


    private JiffleDemo() {
        super("Jiffle scripting language");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    
    @Override
    public void setVisible(boolean vis) {
        if (vis) {
            splitPane.setDividerLocation((int) (getWidth() * 0.45));
        }
        super.setVisible(vis);
    }
    

    private void initComponents() {
        imagePane = new SimpleImagePane();
        
        scriptPane = new JTextArea();
        scriptPane.setEditable(false);
        Font font = new Font("Courier", Font.PLAIN, 12);
        scriptPane.setFont(font);
        
        JScrollPane imageScroll = new JScrollPane(imagePane);
        JScrollPane scriptScroll = new JScrollPane(scriptPane);
        
        splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scriptScroll, imageScroll);
        
        Dimension minSize = new Dimension(100, 100);
        imageScroll.setMinimumSize(minSize);
        scriptScroll.setMinimumSize(minSize);
        
        getContentPane().add(splitPane);
        
        JMenuItem item;
        JMenuBar menuBar = new JMenuBar();
        JMenu mainMenu = new JMenu("File");
        
        JMenu scriptMenu = new JMenu("Example scripts");
        
        for (final ImageChoice choice : ImageChoice.values()) {
            item = new JMenuItem(choice.toString());
        
            item.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    loadScript(choice);
                }
            } );
            
            scriptMenu.add(item);
        }
        
        mainMenu.add(scriptMenu);
        menuBar.add(mainMenu);
        
        JMenu viewMenu = new JMenu("View");
        
        item = new JMenuItem("Bigger font");
        item.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setFontSize(1);
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_UP, 
                KeyEvent.SHIFT_DOWN_MASK|KeyEvent.CTRL_DOWN_MASK));
        viewMenu.add(item);
        
        item = new JMenuItem("Smaller font");
        item.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setFontSize(-1);
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, 
                KeyEvent.SHIFT_DOWN_MASK|KeyEvent.CTRL_DOWN_MASK));
        viewMenu.add(item);
        
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }
    
    private void loadScript(ImageChoice imageChoice) {
        try {
            String script = JiffleDemoHelper.getScript(imageChoice);
            runScript(script, imageChoice.getDestImageVarName());
            
        } catch (JiffleException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Problem loading the example script", 
                    "Bummer", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runScript(String script, String destVarName) {
        try {
            scriptPane.setText(script);
            
            JiffleBuilder builder = new JiffleBuilder();
            builder.script(script).dest(destVarName, imageWidth, imageHeight);
            RenderedImage image = builder.run().getImage(destVarName);
            imagePane.setImage(image);
            
        } catch (JiffleException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Errors compiling or running the script", 
                    "Bummer", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setFontSize(int delta) {
        Font font = scriptPane.getFont();
        Font font2 = font.deriveFont((float) font.getSize() + delta);
        scriptPane.setFont(font2);
    }
}
