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
 * A trivial <code>OpImage</code> subclass that simply transmits its
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
     * Create a new ImageLayout from the source image optionally
     * overriding a ColorModel supplied via the layout.
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
     * object with all of its fields filled in.  The <code>ColorModel</code>
     * may be overridden via the supplied <code>ImageLayout</code>; all
     * other layout fields are derived from the source image.  Any
     * specified <code>ColorModel</code> will be used if and only if it
     * is compatible with the source image <code>SampleModel</code>.
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
        // cobbleSources is irrelevant since we override getTile().
        super(vectorize(source),layout,configuration,true);
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
        
        String[] propertyNames = getPropertyNames();
        
        RenderedImage sourceImage = getSourceImage(0);
        
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        
        for(String property : propertyNames){
            
            properties.put(property, sourceImage.getProperty(property));
            
        }
        return properties;
    }

    /**
     * Set the properties <code>Hashtable</code> of the source image
     * to the supplied <code>Hashtable</code>.
     */
    protected synchronized void setProperties(Hashtable properties) {
        
        PlanarImage sourceImage = getSourceImage(0);

        Hashtable<String, Object> propertyTable = properties;
        
        for(String property : propertyTable.keySet()){
            
            sourceImage.setProperty(property, propertyTable.get(property));
            
        }
    }

    /**
     * Returns the property names from the source image or <code>null</code>
     * if no property names are recognized.
     */
    public String[] getPropertyNames() {
        return getSource(0).getPropertyNames();
    }

    /**
     * Returns the property names with the supplied prefix from
     * the source image or <code>null</code> if no property names
     * are recognized.
     */
    public String[] getPropertyNames(String prefix) {
        return getSource(0).getPropertyNames(prefix);
    }

    /**
     * Returns the class of the specified property from the source image.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        return getSource(0).getPropertyClass(name);
    }

    /**
     * Retrieves a property from the source image by name or
     * <code>java.awt.Image.UndefinedProperty</code> if the property
     * with the specified name is not defined.
     */
    public Object getProperty(String name) {
        return getSource(0).getProperty(name);
    }

    /**
     * Sets a property on the source image by name.
     */
    public void setProperty(String name, Object value) {
        getSource(0).setProperty(name, value);
    }

    /**
     * Removes a property from the source image by name.
     *
     * @since JAI 1.1
     */
    public void removeProperty(String name) {
        getSource(0).removeProperty(name);
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
