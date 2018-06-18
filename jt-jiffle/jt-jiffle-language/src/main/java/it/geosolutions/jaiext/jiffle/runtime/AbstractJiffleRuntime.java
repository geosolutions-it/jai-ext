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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;


/**
 * Provides default implementations of {@link JiffleRuntime} methods plus 
 * some common fields. The fields include those involved in handling image-scope
 * variables and script options; an instance of {@link JiffleFunctions}; and an
 * integer stack used in evaluating {@code con} statements.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class AbstractJiffleRuntime implements JiffleRuntime {
    private static final double EPS = 1.0e-8d;

    private enum Dim { XDIM, YDIM };
    
    private Map<String, Jiffle.ImageRole> _imageParams;
    
    /** Processing area bounds in world units. */
    private Rectangle2D _worldBounds;
    
    /** Pixel width in world units. */
    private double _xres;
    
    /** Pixel height in world units. */
    private double _yres;

    /** Flags whether bounds and pixel dimensions have been set. */
    private boolean _worldSet;
    
    private String[] _variableNames;
    
    protected boolean _imageScopeVarsInitialized = false;
    
    /** Number of pixels calculated from bounds and pixel dimensions. */
    private long _numPixels;

    /*
     * Note: not using generics here because they are not
     * supported by the Janino compiler.
     */

    /**
     * Maps image variable names ({@link String}) to images
     * ({@link RenderedImage}).
     */
    protected Map<String, SourceImage> _images = new HashMap();

    protected class SourceImage {
        final String imageName;
        final RenderedImage image;
        CoordinateTransform transform;
        boolean defaultTransform;
        final RandomIter iterator;
        final int minX;
        final int maxX;
        final int minY;
        final int maxY;
        
        public SourceImage(String imageName, RenderedImage image) {
            this.imageName = imageName;
            this.image = image;
            this.minX = image.getMinX();
            this.maxX = image.getMinX() + image.getWidth();
            this.minY = image.getMinY();
            this.maxY = image.getMinY() + image.getHeight();
            this.iterator = RandomIterFactory.create(image, null);
        }
        
        public double read(double x, double y, int band) {
            int posx = (int) x;
            int posy = (int) y;
            if (transform != null && !(transform instanceof IdentityCoordinateTransform)) {
                Point imgPos = transform.worldToImage(x, y, null);
                posx = imgPos.x;
                posy = imgPos.y;
            }

            final boolean inside = posx >= minX && posx < maxX && posy >= minY && posy < maxY;
            if (!inside) {
                if (_outsideValueSet) {
                    return _outsideValue;
                } else {
                    throw new JiffleRuntimeException( String.format(
                            "Position %.4f %.4f is outside bounds of image: %s",
                            x, y, imageName));
                }
            }

            return iterator.getSampleDouble(posx, posy, band);
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
    
    
    
    // Used to size / resize the _vars array as required
    private static final int VAR_ARRAY_CHUNK = 100;
    
    /** Whether the <i>outside</i> option is set. */
    protected boolean _outsideValueSet;
    
    /** 
     * The value to return for out-of-bounds image data requests if the
     * <i>outside</i> option is set.
     */
    protected double _outsideValue;

    /** 
     * A stack of integer values used in the evaluation of if statements.
     */
    protected IntegerStack _stk;
    
    /** 
     * Provides runtime function support.
     */
    protected final JiffleFunctions _FN;
    
    protected CoordinateTransform _defaultTransform;

    public AbstractJiffleRuntime() {
        this(new String[0]);
    }

    /**
     * Creates a new instance of this class and initializes its 
     * {@link JiffleFunctions} and {@link IntegerStack} objects.
     */
    public AbstractJiffleRuntime(String[] variableNames) {
        _FN = new JiffleFunctions();
        _stk = new IntegerStack();
        
        _xres = Double.NaN;
        _yres = Double.NaN;
        
        _variableNames = variableNames;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setImageParams(Map imageParams) {
        this._imageParams = new HashMap<String, Jiffle.ImageRole>();
        for (Object oname : imageParams.keySet()) {
            String name = (String) oname;
            Jiffle.ImageRole role = (Jiffle.ImageRole) imageParams.get(oname);
            this._imageParams.put(name, role);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getSourceVarNames() {
        return doGetImageVarNames(Jiffle.ImageRole.SOURCE);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDestinationVarNames() {
        return doGetImageVarNames(Jiffle.ImageRole.DEST);
    }
    
    private String[] doGetImageVarNames(Jiffle.ImageRole role) {
        List<String> names = new ArrayList<String>();
        for (String name : _imageParams.keySet()) {
            if (_imageParams.get(name) == role) {
                names.add(name);
            }
        }

        return names.toArray(new String[0]);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setWorldByResolution(Rectangle2D bounds, double xres, double yres) {
        if (bounds == null || bounds.isEmpty()) {
            throw new IllegalArgumentException("bounds cannot be null or empty");
        }
        if (xres < EPS || yres < EPS) {
            throw new IllegalArgumentException("xres and yres but must be greater than 0");
        }
        
        doSetWorld(bounds, xres, yres);
    }

    /**
     * {@inheritDoc}
     */
    public void setWorldByNumPixels(Rectangle2D bounds, int numX, int numY) {
        if (bounds == null || bounds.isEmpty()) {
            throw new IllegalArgumentException("bounds cannot be null or empty");
        }
        if (numX <= 0 || numY <= 0) {
            throw new IllegalArgumentException("numX and numY must be greater than 0");
        }
        
        doSetWorld(bounds, bounds.getWidth() / numX, bounds.getHeight() / numY);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isWorldSet() {
        return _worldSet;
    }

    /**
     * {@inheritDoc}
     */
    public Double getVar(String varName) {
        Field field = getVariableField(varName);
        if (field == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            return field.getDouble(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected Field getVariableField(String varName) {
        try {
            return getClass().getDeclaredField("v_" + varName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setVar(String varName, Double value) throws JiffleRuntimeException {
        Field field = getVariableField(varName);
        if (field == null) {
            throw new JiffleRuntimeException("Undefined variable: " + varName);
        }
        try {
            field.setAccessible(true);
            field.setDouble(this, value == null ? Double.NaN : value);
            _imageScopeVarsInitialized = false;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getVarNames() {
        String[] names = new String[_variableNames.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = _variableNames[i];
        }
        return names;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinX() {
        return _worldBounds.getMinX();
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxX() {
        return _worldBounds.getMaxX();
    }

    /**
     * {@inheritDoc}
     */
    public double getMinY() {
        return _worldBounds.getMinY();
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxY() {
        return _worldBounds.getMaxY();
    }
    
    /**
     * {@inheritDoc}
     */
    public double getWidth() {
        return _worldBounds.getWidth();
    }
    
    /**
     * {@inheritDoc}
     */
    public double getHeight() {
        return _worldBounds.getHeight();
    }

    /**
     * {@inheritDoc}
     */
    public double getXRes() {
        return _xres;
    }

    /**
     * {@inheritDoc}
     */
    public double getYRes() {
        return _yres;
    }
    
    public long getNumPixels() {
        if (!_worldSet) {
            throw new IllegalStateException("Processing area has not been set");
        }
        return _numPixels;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDefaultTransform(CoordinateTransform tr) throws JiffleException {
        if (tr != null) {
            if (!isWorldSet()) {
                throw new WorldNotSetException();
            }
        }
        _defaultTransform = tr;

        for (SourceImage sourceImage : _images.values()) {
            if (sourceImage.defaultTransform) {
                sourceImage.setTransform(tr, true);
            }
        }
    }
    
    
    
    /**
     * Gets the coordinate transform to use with the image represented by
     * {@code imageVarName}.
     * 
     * @param imageVarName variable name
     * 
     * @return the coordinate transform
     */
    protected CoordinateTransform getTransform(String imageVarName) {
        CoordinateTransform tr = _images.get(imageVarName).transform;
        if (tr == null) {
            tr = IdentityCoordinateTransform.INSTANCE;
        }
        
        return tr;
    }

    /**
     * Initializes image-scope variables. These are fields in the runtime class.
     * They are initialized in a separate method rather than the constructor
     * because they may depend on expressions involving values which are not
     * known until the processing area is set (e.g. Jiffle's width() function).
     * 
     * @throws JiffleRuntimeException if any variables do not have either a
     *         default or provided value
     */
    protected abstract void initImageScopeVars();
    
    /**
     * Initializes runtime class fields related to Jiffle script options.
     */
    protected void initOptionVars() {
        
    };

    /**
     * Helper for {@link #setWorldByNumPixels(Rectangle2D, int, int)} and
     * {@link #setWorldByResolution(Rectangle2D, double, double)} methods.
     * 
     * @param bounds world bounds
     * @param xres pixel width
     * @param yres pixel height
     */
    private void doSetWorld(Rectangle2D bounds, double xres, double yres) {
        checkResValue(xres, Dim.XDIM, bounds);
        checkResValue(yres, Dim.YDIM, bounds);
        
        _worldBounds = new Rectangle2D.Double(
                bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight());
        
        _xres = xres;
        _yres = yres;
        
        _worldSet = true;
    }
    
    /**
     * Helper method for {@link #setWorldByResolution(Rectangle2D, double, double)} to
     * check the validity of a pixel dimension.
     * 
     * @param value dimension in world units
     * @param dim axis: Dim.XDIM or Dim.YDIM
     * @param bounds world area bounds
     */
    private void checkResValue(double value, Dim dim, Rectangle2D bounds) {
        String name = dim == Dim.XDIM ? "xres" : "yres";
        
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " cannot be infinite");
        }
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(name + " cannot be NaN");
        }
        
        if (dim == Dim.XDIM && value > bounds.getWidth()) {
            throw new IllegalArgumentException(name + "should be less than processing area width");
            
        } else if (dim == Dim.YDIM && value > bounds.getHeight()) {
            throw new IllegalArgumentException(name + "should be less than processing area height");
        }
    }

    /**
     * {@inheritDoc}
     */
    public double readFromImage(String srcImageName, double x, double y, int band) {
        SourceImage sourceImage = _images.get(srcImageName);
        return sourceImage.read(x, y, band);
    }

    /**
     * {@inheritDoc}
     */
    public void setSourceImage(String varName, RenderedImage image) {
        try {
            doSetSourceImage(varName, image, null);
        } catch (WorldNotSetException ex) {
            // No exception can be caused by a null transform
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSourceImage(String varName, RenderedImage image, CoordinateTransform tr)
            throws JiffleException {
        try {
            doSetSourceImage(varName, image, tr);
        } catch (WorldNotSetException ex) {
            throw new JiffleException(String.format(
                    "Setting a coordinate tranform for a source (%s) without"
                            + "having first set the world bounds and resolution", varName));
        }
    }

    private void doSetSourceImage(String varName, RenderedImage image, CoordinateTransform tr)
            throws WorldNotSetException {
        SourceImage sourceImage = new SourceImage(varName, image);
        boolean defaultTransform = tr == null;
        CoordinateTransform tt = defaultTransform ? _defaultTransform : tr;
        sourceImage.setTransform(tt, defaultTransform);
        _images.put(varName, sourceImage);
    }

    /**
     * Returns the images set for this runtime object as a {@code Map} with
     * variable name as key and iamge as value. The returned {@code Map} is
     * a copy of the one held by this object, so it can be safely modified
     * by the caller.
     *
     * @return images keyed by variable name
     */
    public Map<String, RenderedImage> get_images() {
        Map copy = new HashMap();
        for (SourceImage sourceImage : _images.values()) {
            copy.put(sourceImage.imageName, sourceImage.image);
        }
        return copy;
    }

    public abstract void setDefaultBounds();
}
