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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Utility class to create {@link CoordinateTransform} objects for simple cases.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class CoordinateTransforms {
    
    /**
     * Creates an identity transform.
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform identity() {
        return IdentityCoordinateTransform.INSTANCE;
    }
    
    /**
     * Creates a scaling transform.
     * 
     * @param xscale scale on the X axis
     * @param yscale scale on the Y axis
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform scale(double xscale, double yscale) {
        return new AffineCoordinateTransform(AffineTransform.getScaleInstance(xscale, yscale));
    }
    
    /**
     * Creates a translation transform.
     * 
     * @param dx translation in the X direction
     * @param dy translation in the Y direction
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform translation(double dx, double dy) {
        return new AffineCoordinateTransform(AffineTransform.getTranslateInstance(dx, dy));
    }

    /**
     * Creates a transform for working in the unit rectangle, ie. proportional
     * image coordinates where both X and Y ordinates vary from 0 to 1.
     * 
     * @param imageBounds the image bounds
     * 
     * @return a new transform instance
     * 
     * @throws IllegalArgumentException if {@code imageBounds} is {@code null} or empty
     */
    public static CoordinateTransform unitBounds(Rectangle imageBounds) {
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }

        return getTransform(new Rectangle(0, 0, 1, 1), imageBounds);
    }

    /**
     * Gets the transform which converts from {@code worldBounds} to {@code imageBounds}.
     * This method is a shortcut for {@code getTransform(worldBounds, imageBounds, false, false)}.
     * 
     * @param worldBounds the coordinate bounds in world (user-defined) units
     * @param imageBounds the image bounds
     * 
     * @return a new transform instance
     * 
     * @throws IllegalArgumentException if either argument is {@code null} or empty
     */
    public static CoordinateTransform getTransform(Rectangle2D worldBounds, Rectangle imageBounds) {
        return getTransform(worldBounds, imageBounds, false, false);
    }
    
    /**
     * Gets the transform which converts from {@code worldBounds} to {@code imageBounds}.
     * The two {@code boolean} arguments provide the option of treating the world X and/or Y
     * axis direction as reversed in relation to the corresponding image axis direction.
     * <p>
     * Example: for an image representing a geographic area, aligned such that the image
     * Y-axis was parallel with the world north-south axis, then setting {@code reverseY}
     * to {@code true} will result in correct transformation of world to image coordinates.
     * 
     * @param worldBounds the coordinate bounds in world (user-defined) units
     * @param imageBounds the image bounds
     * @param reverseX whether to treat the direction of the world X axis as reversed
     *        in relation to the image X axis
     * @param reverseY whether to treat the direction of the world Y axis as reversed
     *        in relation to the image Y axis
     * 
     * @return a new transform instance
     * 
     * @throws IllegalArgumentException if either argument is {@code null} or empty
     */
    public static CoordinateTransform getTransform(Rectangle2D worldBounds, Rectangle imageBounds,
            boolean reverseX, boolean reverseY) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("worldBounds must not be null or empty");
        }
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }

        double xscale = (imageBounds.getMaxX() - imageBounds.getMinX()) / 
                (worldBounds.getMaxX() - worldBounds.getMinX());
        
        double xoff;
        if (reverseX) {
            xscale = -xscale;
            xoff = imageBounds.getMinX() - xscale * worldBounds.getMaxX();
                    
        } else {
            xoff = imageBounds.getMinX() - xscale * worldBounds.getMinX();
        }
        
        double yscale = (imageBounds.getMaxY() - imageBounds.getMinY()) / 
                (worldBounds.getMaxY() - worldBounds.getMinY());
        
        double yoff;
        if (reverseY) {
            yscale = -yscale;
            yoff = imageBounds.getMinY() - yscale * worldBounds.getMaxY();
                    
        } else {
            yoff = imageBounds.getMinY() - yscale * worldBounds.getMinY();
        }
        
        
        return new AffineCoordinateTransform(new AffineTransform(xscale, 0, 0, yscale, xoff, yoff));
    }
}
