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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * Extends the JAI widget DisplayJAI. Displays an image gets information
 * about the pixel location and value(s) under the mouse cursor to be
 * displayed by the owning frame (e.g. an ImageFrame object).
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
class ImagePane extends SimpleImagePane implements MouseListener, MouseMotionListener {

    private ImageFrame frame;

    private RenderedImage displayImage;
    private RenderedImage dataImage;
    private RandomIter dataImageIter;
    private boolean integralImageDataType;
    private final Rectangle imageBounds;

    private int[] intData;
    private double[] doubleData;
    
    private void setMouseListener() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    
    /**
     * Constructor.
     *
     * @param frame an object such as an {@linkplain ImageFrame} that implments {@linkplain FrameWithStatusBar}
     * @param displayImg the image to display
     * @param dataImg the image containing data the will be shown in the owning frame's status bar
     * when the mouse is over the pane. If null, data is drawn from the displayImg. If non-null, this
     * image should have bounds equal to, or surrounding, those of the display image.
     */
    public ImagePane(ImageFrame frame, RenderedImage displayImg, RenderedImage dataImg) {
        setImage(displayImg);
        this.frame = frame;
        this.displayImage = displayImg;
        this.imageBounds = new Rectangle(displayImage.getMinX(), displayImage.getMinY(), 
                displayImage.getWidth(), displayImage.getHeight());
        
        this.dataImage = (dataImg == null ? displayImg : dataImg);

        switch (dataImage.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                integralImageDataType = true;
                intData = new int[dataImage.getSampleModel().getNumBands()];
                break;

            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                integralImageDataType = false;
                doubleData = new double[dataImage.getSampleModel().getNumBands()];
                break;
        }

        setMouseListener();
    }

    /**
     * If the mouse cursor is over the image, get the value of the image
     * pixel from band 0
     */
    public void mouseMoved(MouseEvent ev) {
        if (dataImage != null) {
            Point imagePos = getImageCoords(ev.getPoint(), null);
            if (imageBounds.contains(imagePos)) {
                
                if (dataImageIter == null) {
                    dataImageIter = RandomIterFactory.create(dataImage, imageBounds);
                }

                if (integralImageDataType) {
                    dataImageIter.getPixel(imagePos.x, imagePos.y, intData);
                    frame.setCursorInfo(imagePos, intData);
                } else {
                    dataImageIter.getPixel(imagePos.x, imagePos.y, doubleData);
                    frame.setCursorInfo(imagePos, doubleData);
                }
            } else {
                frame.setStatusText("");
            }
        }
    }

    public void mouseExited(MouseEvent ev) {
        frame.setStatusText("");
    }

    /**
     * Empty method.
     * @param e the event
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * Empty method.
     * @param e the event
     */
    public void mousePressed(MouseEvent e) {}

    /**
     * Empty method.
     * @param e the event
     */
    public void mouseReleased(MouseEvent e) {}

    /**
     * Empty method.
     * @param e the event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Empty method.
     * @param e the event
     */
    public void mouseDragged(MouseEvent e) {}

}
