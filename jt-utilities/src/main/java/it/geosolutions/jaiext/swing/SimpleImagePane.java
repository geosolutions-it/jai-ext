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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;

import javax.swing.*;


/**
 * A basic Swing widget to display a {@code RenderedImage}. Used with JAITools
 * example applications.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class SimpleImagePane extends JPanel {

    private RenderedImage image;
    private AffineTransform imageToDisplay;
    private AffineTransform displayToImage;
    private int margin;
    
    private final Object lock = new Object();
    

    /**
     * Creatss a new instance.
     */
    public SimpleImagePane() {
        margin = 0;
        
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent ce) {
                setTransform();
            }
        });
    }
    
    /**
     * Sets the image to display and repaints.
     * 
     * @param image the image to display
     */
    public void setImage(RenderedImage image) {
        this.image = image;
        setTransform();
        repaint();
    }
    
    /**
     * Removes the current display image and repaints.
     */
    public void clear() {
        image = null;
        repaint();
    }
    
    /**
     * Forces recalculation of the {@code AffineTransform} used to
     * scale the image display.
     */
    public void resetTransform() {
        setTransform();
    }
    
    /**
     * Converts a window position into the corresponding image position.
     * If {@code imageCoords} is not {@code null} it will be set to the
     * image position, otherwise a new {@code Point} object will be created.
     * In either case, the image position is returned for convenience.
     * <p>
     * If no image is currently set, {@code null} is returned.
     * 
     * @param paneCoords window position
     * @param imageCoords object to receive image position, or {@code null}
     * 
     * @return image position or {@code null} if no image is set
     */
    public Point getImageCoords(Point paneCoords, Point imageCoords) {
        if (image != null) {
            Point2D p = displayToImage.transform(paneCoords, null);

            if (imageCoords != null) {
                imageCoords.x = (int) p.getX();
                imageCoords.y = (int) p.getY();
                return imageCoords;
            }
            return new Point((int) p.getX(), (int) p.getY());
        }

        return null;
    }
    
    /**
     * Converts an image position into the corresponding window position.
     * If {@code paneCoords} is not {@code null} it will be set to the
     * window position, otherwise a new {@code Point} object will be created.
     * In either case, the window position is returned for convenience.
     * <p>
     * If no image is currently set, {@code null} is returned.
     * 
     * @param imageCoords image position
     * @param paneCoords object to receive window position, or {@code null}
     * 
     * @return window position or {@code null} if no image is set
     */
    public Point getPaneCoords(Point imageCoords, Point paneCoords) {
        if (image != null) {
            Point2D p = imageToDisplay.transform(imageCoords, null);

            if (paneCoords != null) {
                paneCoords.x = (int) p.getX();
                paneCoords.y = (int) p.getY();
                return paneCoords;
            }
            return new Point((int) p.getX(), (int) p.getY());
        }

        return null;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized(lock) {
            if (image != null) {
                if (imageToDisplay == null) {
                    setTransform();
                }
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawRenderedImage(image, imageToDisplay);
            }
        }
    }
    
    private void setTransform() {
        synchronized(lock) {
            if (image != null) {
                Rectangle visr = getVisibleRect();
                if (visr.isEmpty()) {
                    return;
                }
            
                if (imageToDisplay == null) {
                    imageToDisplay = new AffineTransform();
                }
            
                double xscale = (visr.getWidth() - 2*margin) / image.getWidth();
                double yscale = (visr.getHeight() - 2*margin) / image.getHeight();
                double scale = Math.min(xscale, yscale);
        
                double xoff = margin - (scale * image.getMinX());
                double yoff = margin - (scale * image.getMinY());
                
                imageToDisplay.setTransform(scale, 0, 0, scale, xoff, yoff);
                
                try {
                    displayToImage = imageToDisplay.createInverse();
                } catch (NoninvertibleTransformException ex) {
                    // we shouldn't ever be here
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    
}
