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

package it.geosolutions.jaiext.jiffle;

import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.IdentityCoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

import it.geosolutions.jaiext.utilities.ImageUtilities;

/**
 * A builder class which makes it easier to compile and run basic Jiffle scripts.
 * <p>
 * When working with Jiffle objects directly you end up writing a certain 
 * amount of boiler-plate code for image parameters etc. JiffleBuilder offers
 * concise, chained methods to help you get the job done with fewer keystrokes.
 * <pre><code>
 * // A script to sum values from two source images
 * String sumScript = "dest = foo + bar;" ;
 *
 * RenderedImage fooImg = ...
 * RenderedImage barImg = ...
 *
 * JiffleBuilder jb = new JiffleBuilder();
 * jb.script(sumScript).source("foo", fooImg).script("bar", barImg);
 *
 * // We can get the builder to create the destination image for us
 * jb.dest("dest", fooImg.getWidth(), fooImg.getHeight());
 *
 * // Run the script
 * jb.getRuntime().run();
 *
 * // Since we asked the builder to create the destination image we
 * // now need to get a reference to it
 * RenderedImage destImg = jb.getImage("dest");
 * </code></pre>
 * When a script does not use any source images, {@code JiffleBuilder} makes
 * for very concise code:
 * <pre><code>
 * String script = "waves = sin( 4 * M_PI * x() / width() );" ;
 * JiffleBuilder jb = new JiffleBuilder();
 * RenderedImage wavesImg = jb.script(script).dest("waves", 500, 200).run().getImage("waves");
 * </code></pre>
 * {@code JiffleBuilder} also provides support for setting world units and 
 * coordinate transforms.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleBuilder {
    
    private static final double EPS = 1.0e-8;
    
    private static class WorldInfo {
        Rectangle2D bounds;
        double xres;
        double yres;
    }
    
    private WorldInfo worldInfo;
    
    /*
     * We use this class, rather than a private collection of WeakReferences,
     * to ensure that both weak and strong references are visible to the
     * client. Otherwise, the weak references get garbage collected too soon.
     */
    private static class ImageRef {
        Object ref;
        boolean weak;
        
        ImageRef(RenderedImage image, boolean weak) {
            if (weak) {
                ref = new WeakReference<>(image);
            } else {
                ref = image;
            }
            this.weak = weak;
        }

        RenderedImage get() {
            if (weak) {
                RenderedImage image = ((WeakReference<RenderedImage>) ref).get();
                return image;
            } else {
                return (RenderedImage) ref;
            }
        }
    }

    private String script;
    private final Map<String, Jiffle.ImageRole> imageParams;
    private final Map<String, ImageRef> images;
    
    private CoordinateTransform _defaultTransform;
    private final Map<String, CoordinateTransform> transforms;

    /**
     * Creates a new JiffleBuilder instance.
     */
    public JiffleBuilder() {
        imageParams = new LinkedHashMap<>();
        images = new LinkedHashMap<>();
        transforms = new LinkedHashMap<>();
    }

    /**
     * Clears all attributes in this builder. If destination images
     * were created using the {@code dest} methods with image bounds
     * arguments they will also be freed.
     */
    public void clear() {
        worldInfo = null;
        _defaultTransform = null;
        script = null;
        imageParams.clear();
        images.clear();
        transforms.clear();
    }
    
    /**
     * Sets the bound and resolution of the processing area. If the client 
     * does not explicitly set the processing area the default is used
     * (first destination or source image bounds and resolution).
     * 
     * @param worldBounds bounds in world units
     * @param xres pixel width in world units
     * @param yres pixel height in world units
     * 
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder worldAndRes(Rectangle2D worldBounds, double xres, double yres) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("bounds must not be null or empty");
        }
        if (xres <= EPS) {
            throw new IllegalArgumentException("xres must be greater than zero");
        }
        if (xres <= EPS) {
            throw new IllegalArgumentException("xres must be greater than zero");
        }
        
        return doSetWorld(worldBounds, xres, yres);
    }
    
    /**
     * Sets the bound and resolution of the processing area. If the client 
     * does not explicitly set the processing area the default is used
     * (first destination or source image bounds and resolution).
     * 
     * @param worldBounds bounds in world units
     * @param numX number of pixels in the X direction
     * @param numY number of pixels in the Y direction
     * 
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder worldAndNumPixels(Rectangle2D worldBounds, int numX, int numY) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("bounds must not be null or empty");
        }
        if (numX <= 0) {
            throw new IllegalArgumentException("numX must be greater than zero");
        }
        if (numY <= 0) {
            throw new IllegalArgumentException("numY must be greater than zero");
        }
        
        double xres = worldBounds.getWidth() / numX;
        double yres = worldBounds.getHeight() / numY;
        return doSetWorld(worldBounds, xres, yres);
    }
    
    private JiffleBuilder doSetWorld(Rectangle2D worldBounds, double xres, double yres) {
        worldInfo = new WorldInfo();
        worldInfo.bounds = worldBounds;
        worldInfo.xres = xres;
        worldInfo.yres = yres;
        return this;
    }

    /**
     * Sets the script to be compiled.
     *
     * @param script the script
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder script(String script) {
        this.script = script;
        return this;
    }

    /**
     * Reads the script from {@code scriptFile}.
     *
     * @param scriptFile file containing the script
     *
     * @return the instance of this class to allow method chaining
     * @throws JiffleException if there were problems reading the file
     */
    public JiffleBuilder script(File scriptFile) throws JiffleException {
        script = readScriptFile(scriptFile);
        return this;
    }

    /**
     * Associates a variable name with a source image. The default coordinate
     * system will be used for this image.
     * The image will be stored by the builder as a weak reference.
     *
     * @param varName variable name
     * @param sourceImage the source image
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder source(String varName, RenderedImage sourceImage) {
        return source(varName, sourceImage, null);
    }

    /**
     * Associates a variable name with a source image and coordinate transform.
     * The image will be stored by the builder as a weak reference.
     *
     * @param varName variable name
     * @param sourceImage the source image
     * @param transform the transform to convert world coordinates to this image's
     *        pixel coordinates
     * 
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder source(String varName, RenderedImage sourceImage,
            CoordinateTransform transform) {
        
        imageParams.put(varName, Jiffle.ImageRole.SOURCE);
        // store as weak reference
        images.put(varName, new ImageRef(sourceImage, true));
        transforms.put(varName, transform);
        return this;
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param destBounds the bounds of the new destination image
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, Rectangle destBounds) {
        if (destBounds == null || destBounds.isEmpty()) {
            throw new IllegalArgumentException("destBounds argument cannot be null or empty");
        }

        return dest(varName, destBounds.x, destBounds.y, destBounds.width, destBounds.height);
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param destBounds the bounds of the new destination image
     * @param transform the transform to convert world coordinates to this image's
     *        pixel coordinates
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, Rectangle destBounds,
            CoordinateTransform transform) {
        
        if (destBounds == null || destBounds.isEmpty()) {
            throw new IllegalArgumentException("destBounds argument cannot be null or empty");
        }

        return dest(varName, destBounds.x, destBounds.y, 
                destBounds.width, destBounds.height, transform);
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script. The minimum pixel X and Y ordinates of the destination
     * image will be 0.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param width image width (pixels)
     * @param height image height (pixels)
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, int width, int height) {
        return dest(varName, 0, 0, width, height);
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script. The minimum pixel X and Y ordinates of the destination
     * image will be 0.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param width image width (pixels)
     * @param height image height (pixels)
     * @param transform the transform to convert world coordinates to this image's
     *        pixel coordinates
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, int width, int height,
            CoordinateTransform transform) {
        return dest(varName, 0, 0, width, height, transform);
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param minx minimum pixel X ordinate
     * @param miny minimum pixel Y ordinate
     * @param width image width (pixels)
     * @param height image height (pixels)
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, int minx, int miny, int width, int height) {
        return dest(varName, minx, miny, width, height, null);
    }

    /**
     * Creates a new destination image and associates it with a variable name
     * in the script.
     * <p>
     * Note: a {@code JiffleBuilder} maintains only {@code WeakReferences}
     * to all source images and any destination _images passed to it via
     * the {@link #dest(String, WritableRenderedImage)} method. However,
     * a strong reference is stored to any destination images created with this
     * method. This can be freed later by calling {@link #clear()} or
     * {@link #removeImage(String varName)}.
     *
     * @param varName variable name
     * @param minx minimum pixel X ordinate
     * @param miny minimum pixel Y ordinate
     * @param width image width (pixels)
     * @param height image height (pixels)
     * @param transform the transform to convert world coordinates to this image's
     *        pixel coordinates
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, int minx, int miny, 
            int width, int height, CoordinateTransform transform) {
        
        WritableRenderedImage image = ImageUtilities.createConstantImage(minx, miny, width, height, 0d);
        imageParams.put(varName, Jiffle.ImageRole.DEST);
        // store as strong reference
        images.put(varName, new ImageRef(image, false));
        transforms.put(varName, transform);
        return this;
    }

    /**
     * Sets a destination image associated with a variable name in the script.
     * <p>
     * See {@link #dest(String, WritableRenderedImage, CoordinateTransform)}
     * for more details about this method.
     * 
     * @param varName variable name
     * @param destImage the destination image
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, WritableRenderedImage destImage) {
        return dest(varName, destImage, null);
    }
    
    /**
     * Sets a destination image associated with a variable name in the script.
     * <p>
     * Note: The builder will only hold a Weak reference to {@code destImg} so
     * it's not a good idea to create an image on the fly when calling this
     * method...
     * <pre><code>
     * // Creating image on the fly
     * builder.dest("foo", ImageUtils.createConstantImage(width, height, 0d), transform);
     *
     * // Later - oops, null is returned here
     * RenderedImage img = builder.getImage("foo");
     * </code></pre>
     * To avoid this problem, create your image locally...
     * <pre><code>
     * WritableRenderedImage img = ImageUtils.createConstantImage(width, height, 0d);
     * builder.dest("foo", img, transform);
     * </code></pre>
     * Or use on of the {@code dest} methods with image bounds arguments to
     * create it for you
     * <pre><code>
     * builder.dest("foo", width, height, transform);
     *
     * // Now, we can retrieve the image successfully
     * RenderedImage img = builder.getImage("foo");
     * </code></pre>
     *
     * @param varName variable name
     * @param destImage the destination image
     * @param transform the transform to convert world coordinates to this image's
     *        pixel coordinates
     *
     * @return the instance of this class to allow method chaining
     */
    public JiffleBuilder dest(String varName, WritableRenderedImage destImage, 
            CoordinateTransform transform) {
        imageParams.put(varName, Jiffle.ImageRole.DEST);
        // store as weak reference
        images.put(varName, new ImageRef(destImage, true));
        transforms.put(varName, transform);
        return this;
    }
    
    /**
     * Sets a default {@code CoordinateTransform} instance to use with all
     * images that are passed to the builder without an explicit transform
     * of their own. If {@code transform} is {@code null}, the system default
     * transform will be used for any such images.
     * 
     * @param transform a transform to use as the default; or {@code null} for
     *        the system default transform
     * 
     * @return the instance of this class to allow method chaining
     * 
     * @see it.geosolutions.jaiext.jiffle.runtime.JiffleRuntime#setDefaultTransform(CoordinateTransform) 
     */
    public JiffleBuilder defaultTransform(CoordinateTransform transform) {
        _defaultTransform = transform == null ? IdentityCoordinateTransform.INSTANCE : transform;
        return this;
    }
    
    /**
     * Runs the script. Equivalent to calling 
     * {@code builder.getRuntime().evaluateAll(null)}.
     * 
     * @return the instance of this class to allow method chaining
     * 
     * @throws JiffleException if the script has not been set yet or if
     *         compilation errors occur
     */
    public JiffleBuilder run() throws JiffleException {
        getRuntime().evaluateAll(null);
        return this;
    }

    /**
     * Creates a runtime object for the currently set script and images.
     *
     * @return an instance of {@link JiffleDirectRuntime}
     *
     * @throws JiffleException if the script has not been set yet or if
     *         compilation errors occur
     */
    public JiffleDirectRuntime getRuntime() throws JiffleException {
        if (script == null) {
            throw new IllegalStateException("Jiffle script has not been set yet");
        }

        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        
        runtime.setDefaultTransform(_defaultTransform);
        if (worldInfo != null) {
            runtime.setWorldByResolution(worldInfo.bounds, worldInfo.xres, worldInfo.yres);
        }
        
        for (String var : images.keySet()) {
            RenderedImage img = images.get(var).get();
            if (img == null) {
                throw new JiffleException(
                        "Image for variable " + var + " has been garbage collected");
            }
            
            CoordinateTransform transform = transforms.get(var);
            switch (imageParams.get(var)) {
                case SOURCE:
                    runtime.setSourceImage(var, img, transform);
                    break;

                case DEST:
                    runtime.setDestinationImage(var, (WritableRenderedImage)img, transform);
                    break;
            }
        }

        return runtime;
    }
    
    /**
     * Gets the Java run-time class code generated from the compiled script.
     *
     * @return the run-time source code
     *
     * @throws JiffleException if the script has not been set yet or if
     *         compilation errors occur
     */
    public String getRuntimeSource() throws JiffleException {
        if (script == null) {
            throw new IllegalStateException("Jiffle script has not been set yet");
        }
        
        Jiffle jiffle = new Jiffle(script, imageParams);

        return jiffle.getRuntimeSource(Jiffle.RuntimeModel.DIRECT, true);
    }

    /**
     * Get an image associated with a script variable name. The image must
     * have been previously suppolied to the builder using the (@code source}
     * method or one of the {@code dest} methods.
     * <p>
     * In the case of a destination image the object returned can be cast
     * to {@link WritableRenderedImage}.
     * 
     * @param varName variable name
     * 
     * @return the associated image or {@code null} if the variable name is
     *         not recognized or the image has since been garbage collected
     */
    public RenderedImage getImage(String varName) {
        ImageRef ref = images.get(varName);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    /**
     * Removes an image associated with a script variable name. The image should
     * have been previously suppolied to the builder using the (@code source}
     * method or one of the {@code dest} methods.
     * <p>
     * In the case of a destination image the object returned can be cast
     * to {@link WritableRenderedImage}.
     * <p>
     * <strong>Note:</strong> Thie method also removes any {@code CoordinateTransform}
     * associated with the image.
     *
     * @param varName variable name
     *
     * @return the associated image or {@code null} if the variable name is
     *         not recognized or the image has since been garbage collected
     */
    public RenderedImage removeImage(String varName) {
        ImageRef ref = images.remove(varName);
        transforms.remove(varName);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    private String readScriptFile(File scriptFile) throws JiffleException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(scriptFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    sb.append(line);
                    sb.append('\n');  // put the newline back on for the parser
                }
            }

            return sb.toString();

        } catch (IOException ex) {
            throw new JiffleException("Could not read the script file", ex);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
