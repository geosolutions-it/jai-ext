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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.jai.TiledImage;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;

import it.geosolutions.jaiext.swing.SimpleImagePane;
import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * John Conway's Game of Life implemented with Jiffle. 
 * <p>
 * The Game of Life is a cellular automaton, ie. a grid based model where
 * the value of each grid cell at time <i>t</i> depends on its state and that
 * of its neighbours at time <i>t-1</i>. 
 * See the Wikipedia article at: http://en.wikipedia.org/wiki/Conway's_Game_of_Life
 * <p>
 * This program is a basic implementation of the game to demonstrate using
 * Jiffle in a simulation setting. It also illustrates the following aspects 
 * of the language:
 * <ul>
 * <li> Use of the <b>foreach</b> loop with an integer sequence (start:end).
 * <li> Pixel neighbour references.
 * <li> The <b>outside</b> script option to set a value returned for
 *      pixel locations beyond the bounds of a source image. </li>
 * <li> Naked conditional expressions to return 0/1 values.
 * </ul>
 *
 * <h3>Jiffle scripts</h3>
 * The program uses two different Jiffle scripts: one which represents a
 * a world with <i>hard</i> edges and a second where the world is a toroid,
 * ie. opposite edges of the image are joined to form a continuous surface.
 * In both scripts the world is an image where an unoccupied location is
 * represented by pixel value 0 and an occupied location by pixel value 1.
 * 
 * <h4>World with hard edges</h4>
 * <pre><code>
 *        options { outside = 0; } 
 *        n = 0; 
 *        foreach (iy in -1:1) { 
 *          foreach (ix in -1:1) { 
 *            n += world[ix, iy]; 
 *          } 
 *        } 
 *        n -= world; 
 *        nextworld = (n == 3) || (world && n==2);
 * </code></pre>
 * 
 * The expression {@code world[ix, iy]} accesses a <u>relative</u> neighbour
 * location. For example {@code world[-1, 1]} would get the value of a pixel
 * at {@code (x-1, y+1)} where x and y are the ordinates of the current pixel.
 * <p>
 * The two foreach loops iterate use <i>integer sequence</i> syntax 
 * ({@code startValue:endValue}) to iterate over the 3x3 neighbourhood centred
 * on the current pixel and count the number of occupied cells (value of 1).
 * The rules of Life are expressed in terms of the number of neighbouring cells
 * occupied, so we adjust the value of {@code n} b subtracting the value of 
 * the current pixel.
 * <p>
 * 
 * The <b>options</b> block at the top of the script sets a value to be returned
 * for any neighbour positions that are beyond the bounds of the image. Without
 * this option, the runtime object would throw a 
 * {@link it.geosolutions.jaiext.jiffle.runtime.JiffleRuntimeException} at the very first pixel
 * when trying to access the relative neighbour position {@code world[-1, -1]}.
 * <p>
 * 
 * The final line of the script expresses all of the Game of Life rules in a
 * single statement ! It uses <i>naked conditional statements</i> which return 
 * 1 or 0.
 * 
 * <h4>Toroidal world</h4>
 * <pre><code>
 *        n = 0; 
 *        foreach (iy in -1:1) { 
 *          yy = y() + iy; 
 *          yy = if (yy &lt; 0, height() - 1, yy); 
 *          yy = if (yy &gt;= height(), 0, yy); 

 *          foreach (ix in -1:1) { 
 *            xx = x() + ix; 
 *            xx = if (xx &lt; 0, width()-1, xx); 
 *            xx = if (xx &gt;= width(), 0, xx); 
 *            n += world[$xx, $yy]; 
 *          } 
 *        } 
 *
 *        n -= world; 
 *        nextworld = (n == 3) || (world && n==2);
 * </code></pre>
 * 
 * This script treats the source image, represented by the {@code world}
 * variable, as a toroid by calculating <u>absolute</u> neighbour positions.
 * These are indicated by the {@code $} prefix. When a neighbour position is
 * beyond an edge, it is adjusted to the corresponding position from the 
 * opposite edge. Note that we don't need the <b>outside</b> option in this
 * script.
 * 
 * <h3>Using the Jiffle runtime objects</h3>
 * The Game of Life is an iterative algorithm where the output for time <i>t</i>
 * becomes the input for time <i>t+1</i>. Here, we accomplish this by simply
 * caching the Jiffle runtime objects and using them repeatedly with two images
 * which are represented by the variables {@code world} and {@code nextworld} 
 * in the scripts. The images are swapped between source and destination roles
 * at each time step as shown in this code fragment...
 * <pre><code>
 *        activeRuntime.setSourceImage(WORLD_NAME, curWorld);
 *        activeRuntime.setDestinationImage(NEXT_WORLD_NAME, nextWorld);
 *        activeRuntime.evaluateAll(null);
 *        
 *        TiledImage temp = curWorld;
 *        curWorld = nextWorld;
 *        nextWorld = temp;
 * </code></pre>
 * 
 * <h3>Acknowledgement</h3>
 * The patterns included with this program are a tiny sample of the pattern
 * collection at LifeWiki: http://www.conwaylife.com/wiki/
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class GameOfLife extends JFrame {

    private static final int WORLD_SIZE = 80;
    
    private static enum WorldType {
        TOROID,
        EDGES;
    }
    
    private WorldType worldType = WorldType.TOROID;
    
    private static final long SHORT_DELAY = 50;
    private static final long NORMAL_DELAY = 1000;
    
    private static long stepDelay;
    
    private static final String WORLD_NAME = "world";
    private static final String NEXT_WORLD_NAME = "nextworld";
    
    private JiffleDirectRuntime toroidRuntime;
    private JiffleDirectRuntime edgeRuntime;
    private JiffleDirectRuntime activeRuntime;

    private static class PatternInfo {
        String name;
        String author;
        String desc;
        String url;
        String data;
    }
    
    private final List<PatternInfo> patterns;
    
    private TiledImage curWorld;
    private TiledImage nextWorld;
    private SimpleImagePane imagePane;
    
    private ScheduledExecutorService runExecutor;
    
    private class StepTask implements Runnable {
        public void run() {
            step();
        }
    }
    
    private AtomicBoolean running;
    
    List<JComponent> itemsDisabledWhenRunning;
    
    
    public static void main(String[] args) {
        GameOfLife me = new GameOfLife();
        me.start(null);
    }

    public GameOfLife() {
        super("Jiffle demo: Conway's Game of Life");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        
        running = new AtomicBoolean(false);
        patterns = new ArrayList<>();

        loadPatterns();
        createRuntimeInstances();
        initializeComponents();
    }
    
    private void createRuntimeInstances() {
        try {
            Jiffle jiffle = new Jiffle();
            Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
            imageParams.put(WORLD_NAME, Jiffle.ImageRole.SOURCE);
            imageParams.put(NEXT_WORLD_NAME, Jiffle.ImageRole.DEST);
            
            // First create a runtime for the toroidal world
            URL url = getClass().getResource("life-toroid.jfl");
            File file = new File(url.toURI());
            jiffle.setScript(file);
            jiffle.setImageParams(imageParams);
            jiffle.compile();
            toroidRuntime = jiffle.getRuntimeInstance();

            // Now create a second runtime for the hard-edged world
            url = getClass().getResource("life-edges.jfl");
            file = new File(url.toURI());
            jiffle.setScript(file);
            jiffle.setImageParams(imageParams);
            jiffle.compile();
            edgeRuntime = jiffle.getRuntimeInstance();

            // Set the active runtime
            activeRuntime = worldType == WorldType.EDGES ? edgeRuntime : toroidRuntime;
            
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void start(String patternName) {
        if (!isVisible()) {
            setVisible(true);
        }
        
        if (patternName == null || patternName.length() == 0) {
            patternName = patterns.get(0).name;
        }
        
        initializeWorld(patternName);
;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                imagePane.setImage(curWorld);
            }
        });
    }
    
    private void run() {
        running.set(true);
        runExecutor = Executors.newScheduledThreadPool(1);
        runExecutor.scheduleWithFixedDelay(new StepTask(), 0, stepDelay, TimeUnit.MILLISECONDS);
    }
    
    private void stop() {
        if (running.get()) {
            runExecutor.shutdown();
            running.set(false);
        }
    }
    
    private void step() {
        activeRuntime.setSourceImage(WORLD_NAME, curWorld);
        activeRuntime.setDestinationImage(NEXT_WORLD_NAME, nextWorld);
        activeRuntime.evaluateAll(null);
        
        TiledImage temp = curWorld;
        curWorld = nextWorld;
        nextWorld = temp;

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                imagePane.setImage(curWorld);
            }
        });
    }

    private void initializeWorld(String patternName) {
        curWorld = ImageUtilities.createConstantImage(WORLD_SIZE, WORLD_SIZE, 0d);
        nextWorld = ImageUtilities.createConstantImage(WORLD_SIZE, WORLD_SIZE, 0d);
        
        setPopulation(patternName);
    }

    private void setPopulation(String patternName) {
        PatternInfo info = getPattern(patternName);
        
        String[] lines = info.data.split("\n");
        final int h = lines.length;
        int maxLen = 0;
        for (String line : lines) {
            if (line.length() > maxLen) {
                maxLen = line.length();
            }
        }
        final int w = maxLen;

        final int ox = curWorld.getMinX() + (curWorld.getWidth() - w) / 2;
        final int oy = curWorld.getMinY() + (curWorld.getHeight() - h) / 2;

        for (int y = oy, iy = 0; iy < h; y++, iy++) {
            String line = lines[iy];
            int len = line.length();
            for (int x = ox, ix = 0; ix < len; x++, ix++) {
                if (line.charAt(ix) != '.') {
                    curWorld.setSample(x, y, 0, 1);
                }
            }
        }
    }
    
    private void initializeComponents() {
        imagePane = new SimpleImagePane();
        getContentPane().add(imagePane);
        
        JMenuItem item;
        itemsDisabledWhenRunning = new ArrayList<>();
        
        JMenu worldMenu = new JMenu("World");
        
        JMenu patternMenu = new JMenu("Set pattern");
        for (final PatternInfo info : patterns) {
            item = new JMenuItem(info.name);
            item.setToolTipText(info.desc);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    start(info.name);
                }
            });
            patternMenu.add(item);
        }
        worldMenu.add(patternMenu);
        itemsDisabledWhenRunning.add(worldMenu);
        
        final JMenuItem edgesItem = new JCheckBoxMenuItem("Join opposite edges (toroid)");
        edgesItem.setSelected(worldType == WorldType.TOROID);
        edgesItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (edgesItem.isSelected()) {
                    activeRuntime = toroidRuntime;
                    worldType = WorldType.TOROID;
                } else {
                    activeRuntime = edgeRuntime;
                    worldType = WorldType.EDGES;
                }
            }
        });
        worldMenu.add(edgesItem);
        itemsDisabledWhenRunning.add(edgesItem);
        
        JMenu runMenu = new JMenu("Run");
        
        item = new JMenuItem("Single step <SPACE>");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                step();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        runMenu.add(item);
        itemsDisabledWhenRunning.add(item);
        
        item = new JMenuItem("Run");
        item.addActionListener(new RunListener(NORMAL_DELAY));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        runMenu.add(item);
        
        item = new JMenuItem("Run fast");
        item.addActionListener(new RunListener(SHORT_DELAY));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        runMenu.add(item);
        
        item = new JMenuItem("Stop");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stop();
                enableMenuItems(false);
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        runMenu.add(item);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(worldMenu);
        menuBar.add(runMenu);
        setJMenuBar(menuBar);
    }

    private void enableMenuItems(boolean isRunning) {
        for (JComponent jc : itemsDisabledWhenRunning) {
            jc.setEnabled(!isRunning);
        }
    }

    private void loadPatterns() {
        // just in case
        patterns.clear();
        
        try {
            URL url = getClass().getResource("patterns");
            File patternDir = new File(url.toURI());
            File[] files = patternDir.listFiles();
            for (File f : files) {
                patterns.add(loadPattern(f));
            }
            
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private PatternInfo loadPattern(File f) throws FileNotFoundException, IOException {
        PatternInfo info = new PatternInfo();
        BufferedReader reader = new BufferedReader(new FileReader(f));
        
        String line = reader.readLine();
        while (line.startsWith("!")) {
            String lwr = line.toLowerCase();
            if (lwr.startsWith("!name:")) {
                info.name = line.substring(6).trim();
            } else if (lwr.startsWith("!author:")) {
                info.author = line.substring(8).trim();
            } else if (lwr.startsWith("!www")) {
                info.url = line.substring(1).trim();
            } else {
                info.desc = line.substring(1).trim();
            }
            
            line = reader.readLine();
        }
        
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (line == null) {
                break;
            }
            
            line = line.trim();
            sb.append(line).append("\n");
            line = reader.readLine();
        }
        
        info.data = sb.toString();
        reader.close();
        return info;
    }

    private PatternInfo getPattern(String patternName) {
        for (PatternInfo info : patterns) {
            if (info.name.equalsIgnoreCase(patternName)) {
                return info;
            }
        }
        
        throw new IllegalArgumentException("Pattern not loaded: " + patternName);
    }


    private class RunListener implements ActionListener {
        private final long delay;

        public RunListener(long delay) {
            this.delay = delay;
        }

        public void actionPerformed(ActionEvent e) {
            if (!running.get()) {
                enableMenuItems(true);
            } else {
                stop();
            }
            stepDelay = delay;
            run();
        }
    }

}
