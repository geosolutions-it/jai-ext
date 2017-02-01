/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2017 GeoSolutions


 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package it.geosolutions.jaiext.utilities;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.ImageLayout;

/**
 * Extends the standard JAI {@link ImageLayout} to provide a reliable hash function. 
 * {@code ImageLayout} has a bug that will cause an application to crash if doing
 * hashing when some fields have not been initialized.
 * 
 * @see javax.media.jai.ImageLayout
 * 
 * @author Simone Giannecchini, GeoSolutions S.A.S.
 * @author Daniele Romagnoli, GeoSolutions S.A.S.
 * @since 1.1
 * @version $Id$
 */
public class ImageLayout2 extends ImageLayout{

    private static final long serialVersionUID = -7921590012423277029L;

    /** 
     * Default constructor. Constructs an {@link ImageLayout2} without any parameter set.
     */
    public ImageLayout2() {
        super();
    }

    /**
     * Construct an {@link ImageLayout2} with the parameter set.
     * @param minX
     *          the image's minimum X coordinate.
     * @param minY
     *          the image's minimum X coordinate.
     * @param width
     *          the image's width.
     * @param height
     *          the image's height.
     * @param tileGridXOffset
     *          the x coordinate of the tile (0,0)
     * @param tileGridYOffset
     *          the y coordinate of the tile (0,0)
     * @param tileWidth
     *          the tile's width.
     * @param tileHeight
     *          the tile's height.
     * @param sampleModel
     *          the image's {@link SampleModel}
     * @param colorModel
     *          the image's {@link ColorModel}
     */
    public ImageLayout2(
            final int minX, 
            final int minY, 
            final int width, 
            final int height, 
            final int tileGridXOffset,
            final int tileGridYOffset, 
            final int tileWidth, 
            final int tileHeight, 
            final SampleModel sampleModel,
            final ColorModel colorModel) {
        super(minX, minY, width, height, tileGridXOffset, tileGridYOffset, tileWidth, tileHeight,
                sampleModel, colorModel);
    }

    /**
     * Construct an {@link ImageLayout2} with only tiling layout properties, sampleModel and 
     * colorModel set.
     * @param tileGridXOffset
     *          the x coordinate of the tile (0,0)
     * @param tileGridYOffset
     *          the y coordinate of the tile (0,0)
     * @param tileWidth
     *          the tile's width.
     * @param tileHeight
     *          the tile's height.
     * @param sampleModel
     *          the image's {@link SampleModel}
     * @param colorModel
     *          the image's {@link ColorModel}
     */
    public ImageLayout2(
            final int tileGridXOffset, 
            final int tileGridYOffset, 
            final int tileWidth, 
            final int tileHeight,
            final SampleModel sampleModel, 
            final ColorModel colorModel) {
        super(tileGridXOffset, tileGridYOffset, tileWidth, tileHeight, sampleModel, colorModel);
    }

    /**
     * Construct an {@link ImageLayout2} with only the image's properties set.
     * @param minX
     *          the image's minimum X coordinate.
     * @param minY
     *          the image's minimum X coordinate.
     * @param width
     *          the image's width.
     * @param height
     *          the image's height.
     */
    public ImageLayout2(
            final int minX, 
            final int minY, 
            final int width, 
            final int height) {
        super(minX, minY, width, height);
    }

    /**
     * Construct an {@link ImageLayout2} on top of a RenderedImage. The layout parameters are set
     * from the related values of the input image.
     * @param im a {@link RenderedImage} whose layout will be copied.
     */
    public ImageLayout2(RenderedImage im) {
        super(im);
    }

    /**
     * Returns the hash code for this {@link ImageLayout2}.
     * With respect to the super {@link ImageLayout}, this method also does 
     * validity check on the parameters during hashing.
     */
    @Override
    public int hashCode() {
        int code = 0, i = 1;

        if (isValid(ImageLayout2.WIDTH_MASK)){
            code += (getWidth(null) * i++);
        }

        if (isValid(ImageLayout2.HEIGHT_MASK)){
            code += (getHeight(null) * i++);
        }

        if (isValid(ImageLayout2.MIN_X_MASK)){
            code += (getMinX(null) * i++);
        }

        if (isValid(ImageLayout2.MIN_Y_MASK)){
            code += (getMinY(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_HEIGHT_MASK)){
            code += (getTileHeight(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_WIDTH_MASK)){
            code += (getTileWidth(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_GRID_X_OFFSET_MASK)){
            code += (getTileGridXOffset(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_GRID_Y_OFFSET_MASK)){
            code += (getTileGridYOffset(null) * i++);
        }

        if (isValid(ImageLayout2.SAMPLE_MODEL_MASK)){
            code ^= getSampleModel(null).hashCode();
        }

        code ^= validMask;

        if (isValid(ImageLayout2.COLOR_MODEL_MASK))
            code ^= getColorModel(null).hashCode();

        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!ImageLayout.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        
        ImageLayout other = (ImageLayout) obj;
        
        if (getValidMask() != other.getValidMask()) {
            return false;
        }
        if (getWidth(null) != other.getWidth(null)) {
            return false;
        }
        if (getHeight(null) != other.getHeight(null)) {
            return false;
        }
        if (getMinX(null) != other.getMinX(null)) {
            return false;
        }
        if (getMinY(null) != other.getMinY(null)) {
            return false;
        }
        if (getTileWidth(null) != other.getTileWidth(null)) {
            return false;
        }
        if (getTileHeight(null) != other.getTileHeight(null)) {
            return false;
        }
        if (getTileGridXOffset(null) != other.getTileGridXOffset(null)) {
            return false;
        }
        if (getTileGridYOffset(null) != other.getTileGridYOffset(null)) {
            return false;
        }
        
        SampleModel sm = getSampleModel(null);
        if (sm == null) {
            if (other.getSampleModel(null) != null) {
                return false;
            }
        } else {
            if (!sm.equals(other.getSampleModel(null))) {
                return false;
            }
        }

        ColorModel cm = getColorModel(null);
        if (cm == null) {
            if (other.getColorModel(null) != null) {
                return false;
            }
        } else {
            if (!cm.equals(other.getColorModel(null))) {
                return false;
            }
        }

        return true;
    }

}

