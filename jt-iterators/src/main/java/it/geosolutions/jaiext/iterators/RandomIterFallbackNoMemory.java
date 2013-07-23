/*
 * $RCSfile: RandomIterFallback.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:43 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.iterators;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;


/**
 * @since EA2
 */
public class RandomIterFallbackNoMemory implements RandomIter
{

    protected RenderedImage im;
    protected Rectangle boundsRect;

    protected SampleModel sampleModel;

    protected int boundsX;
    protected int boundsY;

    public RandomIterFallbackNoMemory(RenderedImage im, Rectangle bounds)
    {
        this.im = im;

        Rectangle imBounds = new Rectangle(im.getMinX(), im.getMinY(),
                im.getWidth(), im.getHeight());
        this.boundsRect = imBounds.intersection(bounds);
        this.sampleModel = im.getSampleModel();


        this.boundsX = boundsRect.x;
        this.boundsY = boundsRect.y;
    }

    /**
     * Sets dataBuffer to the correct buffer for the pixel
     * (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     *
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    private Raster makeCurrent(int xLocal, int yLocal)
    {
        final int tileWidth = im.getTileWidth();
        final int tileHeight = im.getTileHeight();
        final int tileGridXOffset = im.getTileGridXOffset();
        final int tileGridYOffset = im.getTileGridYOffset();
        final int tileX = PlanarImage.XToTileX(xLocal, tileGridXOffset, tileWidth);
        final int tileY = PlanarImage.YToTileY(yLocal, tileGridYOffset, tileHeight);

        return im.getTile(tileX, tileY);
    }

    public int getSample(int x, int y, int b)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSample(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                b,
                tile.getDataBuffer());
    }

    public float getSampleFloat(int x, int y, int b)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleFloat(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                b,
                tile.getDataBuffer());
    }

    public double getSampleDouble(int x, int y, int b)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleDouble(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                b,
                tile.getDataBuffer());
    }

    public int[] getPixel(int x, int y, int[] iArray)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                iArray,
                tile.getDataBuffer());
    }

    public float[] getPixel(int x, int y, float[] fArray)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                fArray,
                tile.getDataBuffer());
    }

    public double[] getPixel(int x, int y, double[] dArray)
    {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY,
                dArray,
                tile.getDataBuffer());
    }

    public void done(){
    }
}
