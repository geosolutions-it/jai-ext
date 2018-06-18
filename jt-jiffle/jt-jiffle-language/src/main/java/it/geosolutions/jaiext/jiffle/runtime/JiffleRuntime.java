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

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.Map;

import it.geosolutions.jaiext.jiffle.Jiffle.ImageRole;
import it.geosolutions.jaiext.jiffle.JiffleException;


/**
 * The root interface for Jiffle runtime classes.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public interface JiffleRuntime {

    /**
     * Sets the world (processing area) bounds and resolution (pixel dimensions).
     * 
     * @param bounds outer bounds of the processing area
     * @param xres pixel width in world units
     * @param yres pixel height in world units
     * 
     * @throws IllegalArgumentException if {@code bounds} is {@code null} or empty
     */
    void setWorldByResolution(Rectangle2D bounds, double xres, double yres);
    
    /**
     * Sets the world (processing area) bounds and the number of pixels in the
     * X and Y directions.
     * 
     * @param bounds outer bounds of the processing area
     * @param numX number of pixels in the X direction
     * @param numY number of pixels in the Y direction
     * 
     * @throws IllegalArgumentException if {@code bounds} is {@code null} or empty
     */
    void setWorldByNumPixels(Rectangle2D bounds, int numX, int numY);
    
    /**
     * Sets a coordinate transform to be used by any source and destination images
     * submitted to the runtime object without their own transforms. This 
     * includes any images submitted prior to calling this method. If {@code tr}
     * is {@code null} the system default transform ({@link IdentityCoordinateTransform})
     * will be used.
     * 
     * @param tr the coordinate transform to use as the default; or {@code null}
     *        for the system default
     * 
     * @throws JiffleException if the world bounds and resolution have not
     *         been set previously
     */
    void setDefaultTransform(CoordinateTransform tr) throws JiffleException, WorldNotSetException;
    
    /**
     * Gets the min X ordinate of the processing area.
     * 
     * @return min X ordinate in world units
     */
    double getMinX();

    /**
     * Gets the max X ordinate of the processing area.
     * 
     * @return max X ordinate in world units
     */
    double getMaxX();

    /**
     * Gets the min Y ordinate of the processing area.
     * 
     * @return min Y ordinate in world units
     */
    double getMinY();

    /**
     * Gets the max Y ordinate of the processing area.
     * 
     * @return max Y ordinate in world units
     */
    double getMaxY();
    
    /**
     * Gets the width of the processing area.
     * 
     * @return the width in world units
     */
    double getWidth();
    
    /**
     * Gets the height of the processing area.
     * 
     * @return the height in world units
     */
    double getHeight();
    
    /**
     * Gets the pixel width (resolution in X direction) in world units.
     * 
     * @return pixel width
     */
    double getXRes();
    
    /**
     * Gets the pixel height (resolution in Y direction) in world units.
     * 
     * @return pixel height
     */
    double getYRes();
    
    /**
     * Gets the total number of pixels in the processing area.
     * 
     * @return number of pixels
     * @throws IllegalStateException if the processing area has not been set
     */
    long getNumPixels();
    
    /**
     * Checks whether the world bounds and pixel dimensions have been set.
     * 
     * @return {@code true} if set; {@code false} otherwise
     */
    boolean isWorldSet();
    
    /**
     * Returns the names of image scope variables that can be used with
     * the {@link #getVar(String)} and {@link #setVar(String, Double)}
     * methods.
     * 
     * @return array of variable names; may be empty but not {@code null}
     */
    String[] getVarNames();
    
    /**
     * Returns the value of a variable that was declared in the
     * script's <i>init</i> block.
     *
     * @param varName variable name
     *
     * @return the values or {@code null} if the variable name is
     *         not found
     */
    Double getVar(String varName);

    /**
     * Sets the value of a variable that was declared in the script's
     * <i>init</i> block, overriding the default value in the script
     * if present. Setting {@code value} to {@code null} results in the
     * default script value being used.
     * 
     * @param varName variable name
     * @param value the new value
     * 
     * @throws JiffleRuntimeException if the variable name is not found
     */
    void setVar(String varName, Double value) throws JiffleRuntimeException;

    /**
     * Supplies the runtime object with the names and roles if image variables
     * used in the script. Although this is a public method it is not intended
     * for general use. It is called by the {@link jaitools.jiffle.Jiffle} 
     * instance that is creating the runtime object so that clients can use 
     * the {@link #getSourceVarNames()} and {@link #getDestinationVarNames()}
     * methods.
     * 
     * @param imageParams a {@code Map} of variable names (key) and roles (value)
     */
    void setImageParams(Map<String, ImageRole> imageParams);
    
    /**
     * Gets the variable names associated with source images.
     * 
     * @return an array of names; may be empty but not {@code null}
     */
    String[] getSourceVarNames();
    
    /**
     * Gets the variable names associated with destination images.
     * 
     * @return an array of names; may be empty but not {@code null}
     */
    String[] getDestinationVarNames();

    /**
     * Associates a variable name with a source image and coordinate transform.
     * The transform defines how to convert from processing area coordinates
     * to image (pixel) coordinates. If {@code tr} is {@code null} the default
     * identify transform will be used.
     * <p> 
     * Note that Jiffle uses rounding to reduce the transformed coordinates to 
     * integers.
     * 
     * @param varName script variable representing the source image
     * @param image writable image
     * @param tr transform for processing area to image coordinates
     * 
     * @throws JiffleException if the world bounds and resolution have not
     *         been set previously
     */
    void setSourceImage(String varName, RenderedImage image, CoordinateTransform tr)
            throws JiffleException;

    /**
     * Associates a variable name with a source image. Equivalent to:
     * <pre><code>
     * setSourceImage(varName, image, null)
     * </code></pre>
     * This convenience method is defined in the interface because it will be
     * commonly when working directly with image coordinates.
     * 
     * @param varName script variable representing the source image
     * @param image writable image
     */
    void setSourceImage(String varName, RenderedImage image);

    /**
     * Gets a value from a source image for a given world position and
     * image band.
     * 
     * @param srcImageName the source image
     * @param x source X ordinate in world units
     * @param y source Y ordinate in world units
     * @param band source band
     * 
     * @return image value
     */
    double readFromImage(String srcImageName, double x, double y, int band);

    /**
     * Gets the images used by this object and returns them as a {@code Map}
     * with variable names as keys and images as values.
     * 
     * @return images keyed by variable name
     */
    Map<String, RenderedImage> get_images();
}
