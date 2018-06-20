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

package it.geosolutions.jaiext.jiffle.runtime;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import it.geosolutions.jaiext.jiffle.JiffleException;

/**
 * The default abstract base class for runtime classes that implement direct evaluation.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class AbstractDirectRuntime extends AbstractJiffleRuntime
        implements JiffleDirectRuntime {

    private static final double EPS = 1.0e-10d;

    protected Map<String, DestinationImage> _destImages = new HashMap<>();

    protected class DestinationImage {
        final String imageName;
        final WritableRenderedImage image;
        CoordinateTransform transform;
        boolean defaultTransform;
        final WritableRandomIter iterator;

        public DestinationImage(String imageName, WritableRenderedImage image) {
            this.imageName = imageName;
            this.image = image;
            this.iterator = RandomIterFactory.createWritable(image, null);
        }

        public void write(double x, double y, int band, double value) {
            int posx, posy;
            if (transform != null && !(transform instanceof IdentityCoordinateTransform)) {
                Point imgPos = transform.worldToImage(x, y, null);
                posx = imgPos.x;
                posy = imgPos.y;
            } else {
                posx = (int) x;
                posy = (int) y;
            }

            iterator.setSample(posx, posy, band, value);
        }

        public void setTransform(CoordinateTransform transform, boolean defaultTransform)
                throws WorldNotSetException {
            if (transform != null && !isWorldSet()) {
                throw new WorldNotSetException();
            }
            this.transform = transform;
            this.defaultTransform = defaultTransform;
        }
    }

    /** Creates a new instance and initializes script-option variables. */
    public AbstractDirectRuntime() {
        super(new String[0]);
        initOptionVars();
    }

    /** Creates a new instance and initializes script-option variables. */
    public AbstractDirectRuntime(String[] variableNames) {
        super(variableNames);
        initOptionVars();
    }

    /** {@inheritDoc} */
    public void setDestinationImage(String varName, WritableRenderedImage image) {
        try {
            doSetDestinationImage(varName, image, null);
        } catch (WorldNotSetException ex) {
            // No exception can be caused by a null transform
        }
    }

    /** {@inheritDoc} */
    public void setDestinationImage(
            String varName, WritableRenderedImage image, CoordinateTransform tr)
            throws JiffleException {

        try {
            doSetDestinationImage(varName, image, tr);

        } catch (WorldNotSetException ex) {
            throw new JiffleException(
                    String.format(
                            "Setting a coordinate tranform for a destination (%s) without"
                                    + "having first set the world bounds and resolution",
                            varName));
        }
    }

    private void doSetDestinationImage(
            String varName, WritableRenderedImage image, CoordinateTransform tr)
            throws WorldNotSetException {
        DestinationImage destinationImage = new DestinationImage(varName, image);
        boolean defaultTransform = tr == null;
        CoordinateTransform tt = defaultTransform ? _defaultTransform : tr;
        destinationImage.setTransform(tt, defaultTransform);
        _destImages.put(varName, destinationImage);
    }

    /** {@inheritDoc} */
    public void evaluateAll(JiffleProgressListener pl) {
        JiffleProgressListener listener = pl == null ? new NullProgressListener() : pl;

        if (!isWorldSet()) {
            setDefaultBounds();
        }

        final long numPixels = getNumPixels();
        listener.setTaskSize(numPixels);

        long count = 0;
        long sinceLastUpdate = 0;
        final long updateInterval = listener.getUpdateInterval();

        final double minX = getMinX();
        final double maxX = getMaxX();
        final double resX = getXRes();

        final double minY = getMinY();
        final double maxY = getMaxY();
        final double resY = getYRes();

        listener.start();
        for (double y = minY; y < maxY - EPS; y += resY) {
            for (double x = minX; x < maxX - EPS; x += resX) {
                evaluate(x, y);

                if (pl != null)
                count++;
                sinceLastUpdate++;
                if (sinceLastUpdate >= updateInterval) {
                    listener.update(count);
                    sinceLastUpdate = 0;
                }
            }
        }
        listener.finish();
    }

    /** {@inheritDoc} */
    public void writeToImage(String destImageName, double x, double y, int band, double value) {
        DestinationImage image = _destImages.get(destImageName);
        image.write(x, y, band, value);
    }

    /** {@inheritDoc} */
    public void setDefaultBounds() {
        RenderedImage refImage = null;
        String imageName = null;

        if (!_destImages.isEmpty()) {
            imageName = (String) _destImages.keySet().iterator().next();
            refImage = _destImages.get(imageName).image;
        } else {
            imageName = (String) _images.keySet().iterator().next();
            refImage = _images.get(imageName).image;
        }

        Rectangle rect =
                new Rectangle(
                        refImage.getMinX(), refImage.getMinY(),
                        refImage.getWidth(), refImage.getHeight());

        setWorldByResolution(rect, 1, 1);
    }

    public void setDefaultTransform(CoordinateTransform tr) throws JiffleException {
        super.setDefaultTransform(tr);

        for (DestinationImage destImage : _destImages.values()) {
            if (destImage.defaultTransform) {
                destImage.setTransform(tr, true);
            }
        }
    }

    @Override
    public Map get_images() {
        Map<String, RenderedImage> images = super.get_images();
        for (DestinationImage destImage : _destImages.values()) {
            images.put(destImage.imageName, destImage.image);
        }
        return images;
    }
}