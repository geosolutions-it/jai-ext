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


package it.geosolutions.jaiext.jiffle.docs;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransforms;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;

public class TreeChange {

    JiffleDirectRuntime runtimeObj = null;
    int imageX, imageY, numCols, numRows;

    // DO NOT RUN - SNIPPET FOR DOCS USE ONLY
    public void runtimeExample() throws Exception {
        // docs start
// World bounds
Rectangle2D worldBounds = new Rectangle2D.Double(
        750000, 6500000, 100000, 50000);

// Common image bounds
Rectangle imageBounds = new Rectangle(0, 0, 4000, 2000);

// Set the bounds (world units) and resolution of the 
// processing area
runtimeObj.setWorldByNumPixels(worldBounds, 4000, 2000);

// Create a new transform that converts from world units to
// pixel positions using the CoordinateTransforms helper class
CoordinateTransform tr = CoordinateTransforms.getTransform(
        worldBounds, imageBounds);

// Set this coordinate transform object to be used with all images
runtimeObj.setDefaultTransform(tr);
        // docs end
    }
}
