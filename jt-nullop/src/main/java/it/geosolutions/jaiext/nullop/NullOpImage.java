/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


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
package it.geosolutions.jaiext.nullop;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Hashtable;
import java.util.Map;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;

import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An<code>OpImage</code> subclass that simply transmits its
 * source unchanged.  This may be useful when an interface requires an
 * <code>OpImage</code> but another sort of <code>RenderedImage</code>
 * (such as a <code>BufferedImage</code> or <code>TiledImage</code>)
 * is available.  Additionally, <code>NullOpImage</code> is able to
 * make use of JAI's tile caching mechanisms.
 *
 * <p> Methods that get or set properties are implemented to forward
 * the requests to the source image; no independent property information
 * is stored in the <code>NullOpImage</code> itself.
 *
 * @see PointOpImage
 */
public class NullOpImage extends OpImage {

    /**
     * Create a new ImageLayout from the source image optionally overriding a ColorModel supplied via the layout.
     */
    private static ImageLayout layoutHelper(RenderedImage source,
                                            ImageLayout layout) {
        // Create basic layout from the source.
        ImageLayout il = new ImageLayout(source);

        // If a layout containing a valid ColorModel field is supplied then
        // reset the ColorModel if it is compatible with the SampleModel.
        if(layout != null && layout.isValid(ImageLayout.COLOR_MODEL_MASK)) {
            ColorModel colorModel = layout.getColorModel(null);
            if(JDKWorkarounds.areCompatibleDataModels(source.getSampleModel(),
                                                      colorModel)) {
                il.setColorModel(colorModel);
            }
        }

        return il;
    }

    /**
     * Constructs a <code>NullOpImage</code>.  The superclass
     * constructor will be passed a new <code>ImageLayout</code>
     * object with all of its fields filled in.
     *
     * @param layout An <code>ImageLayout</code> optionally specifying
     *        the image <code>ColorModel</code>; all other fields are
     *        ignored.  This parameter may be <code>null</code>.
     * @param source A <code>RenderedImage</code>; must not be
     *        <code>null</code> or a <code>IllegalArgumentException</code>
     *        will be thrown.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     *
     * @throws <code>IllegalArgumentException</code> if <code>source</code>
     *        is <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if <code>computeType</code>
     *        is not one of the known <code>OP_*_BOUND</code> values.
     *
     */
    public NullOpImage(RenderedImage source,
                       ImageLayout layout,
                       Map configuration) {
        super(vectorize(source),layoutHelper(source, layout),configuration,true);
    }

    /**
     * Returns a tile for reading.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {
        return getSourceImage(0).getTile(tileX, tileY);
    }

    /**
     * Returns false as NullOpImage can return via computeTile()
     * tiles that are internally cached.
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * Returns the properties from the source image.
     */
    protected synchronized Hashtable getProperties() {
        // Selection of all the image properties
        String[] propertyNames = getPropertyNames();
        // Selection of the source image
        RenderedImage sourceImage = getSourceImage(0);
        // Creation of a Map containing all the source image properties
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        // Cycle on all the properties
        if(propertyNames != null){
            for(String property : propertyNames){
                // Addition of the selected property
                properties.put(property, sourceImage.getProperty(property));
                
            }
            return properties;
        }else{
            return null;
        }
        
    }

    /**
     * Set the properties <code>Hashtable</code> of the source image
     * to the supplied <code>Hashtable</code>.
     */
    protected synchronized void setProperties(Hashtable properties) {
        // Selection of the source image
        PlanarImage sourceImage = getSourceImage(0);

        Hashtable<String, Object> propertyTable = properties;
        if(propertyTable != null){
            // Cycle on all the properties
            for(String property : propertyTable.keySet()){
                // Setting of the selected property
                sourceImage.setProperty(property, propertyTable.get(property));
                
            }
        }
    }

    /**
     * Returns the property names from the source image or <code>null</code>
     * if no property names are recognized.
     */
    public String[] getPropertyNames() {
        return getSourceImage(0).getPropertyNames();
    }

    /**
     * Returns the property names with the supplied prefix from
     * the source image or <code>null</code> if no property names
     * are recognized.
     */
    public String[] getPropertyNames(String prefix) {
        return getSourceImage(0).getPropertyNames(prefix);
    }

    /**
     * Returns the class of the specified property from the source image.
     */
    public Class getPropertyClass(String name) {
        return getSourceImage(0).getPropertyClass(name);
    }

    /**
     * Retrieves a property from the source image by name or
     * <code>java.awt.Image.UndefinedProperty</code> if the property
     * with the specified name is not defined.
     */
    public Object getProperty(String name) {
        return getSourceImage(0).getProperty(name);
    }

    /**
     * Sets a property on the source image by name.
     */
    public void setProperty(String name, Object value) {
        getSourceImage(0).setProperty(name, value);
    }

    /**
     * Removes a property from the source image by name.
     */
    public void removeProperty(String name) {
        getSourceImage(0).removeProperty(name);
    }

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        return destRect;
    }

    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int sourceIndex) {
        return sourceRect;
    }

}
