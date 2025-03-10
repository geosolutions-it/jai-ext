package it.geosolutions.jaiext.scale;

import it.geosolutions.jaiext.range.RangeFactory;
import org.junit.Test;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;

import static org.junit.Assert.assertTrue;

/**
 * This test aims to verify the fix for
 * <a href="https://github.com/geosolutions-it/jai-ext/issues/309">#309</a>.
 * <p>
 * There, a massive performance degrade was reported when scaling images
 * using {@link ScaleOpImage} with large scale ratios. During analysis, it
 * was observed that {@link ScaleOpImage#computeTile(int, int)} requests
 * regions from the source image that are way larger than required, in case
 * border extenders are in use. That leads to massive performance problems,
 * especially if the source of {@link ScaleOpImage} is a very time-consuming
 * operator such as Warp.
 * <p>
 * The problem lies in {@link ScaleOpImage#computeTile(int, int)} that
 * correctly calculates the input regions, but then instead passes the
 * original source rectangle into {@link PlanarImage#getData(Rectangle)}.
 *
 * <p>
 * The way this test works is that it builds a reproducible chain that
 * shows the problem. Then it intercepts the direct source of the scale
 * operator by a special implementation on {@link NullOpImage} that
 * overrides {@link PlanarImage#getData(Rectangle)} in
 * order to assert on the regions that are passed in by the
 * {@link ScaleOpImage}.
 *
 * @author skalesse
 * @since 2025-03-10
 */
public class ScaleOpPerformanceTest extends TestScale {

    /**
     * This test asserts the {@link ScaleOpImage} performance
     * by measuring the time it takes to execute.
     * While this is not a very stable method, the advantage is
     * that it shows the problems (and fix) with an
     * un-altered operation chain.
     */
    @Test(timeout = 10000)
    public void testScaleTilePerformanceUsingTimeout() {

        // create the original source image for this test
        RenderedImage image  = testImage(6500, 5300);
        // wrap in a tile image
        RenderedImage tileImage = tileImage(image, 512, 512);

        // finally, wrap everything in the ScaleOpImage and run the test
        RenderedImage scaledImage = scaleForTest(
                tileImage,
                0.1f, 0.1f,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO)
        );

        scaledImage.getData();
        ((RenderedOp)scaledImage).dispose();
    }

    /**
     * This method asserts the {@link ScaleOpImage} performance by
     * injecting a modified {@link NullOpImage} as the direct source
     * of {@link ScaleOpImage}. That way we can directly check the
     * regions that are passed into the source by {@link ScaleOpImage}.
     * The advantage of this test is that we don't have to measure
     * execution times. The disadvantage is that we are altering the
     * operation chain, which could in theory have side effects.
     */
    @Test
    public void testScaleTilePerformanceUsingAssert() {

        // create the original source image for this test
        RenderedImage image  = testImage(6500, 5300);
        // wrap in a tile image
        RenderedImage tileImage = tileImage(image, 512, 512);
        // wrap in the 'mock' image that asserts for wrong arguments
        // passed into getData()
        RenderedImage testOpImage = new ScaleOpTestImage(tileImage);

        // finally, wrap everything in the ScaleOpImage and run the test
        RenderedImage scaledImage = scaleForTest(
                testOpImage,
                0.1f, 0.1f,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO)
        );

        scaledImage.getData();
        ((RenderedOp)scaledImage).dispose();
    }

    /**
     * Creates the scale operation that is to be tested here.
     * <p>
     * Note that this method only takes the arguments that will have an impact on the test.
     * We do not require translation, background values, no-data ranges, ROI etc. Really,
     * the only important are the scale factors and border extender.
     * @param image the source image
     * @param scaleX the scale-X
     * @param scaleY the scale-Y
     * @param borderExtender the border extender (can be {@code null})
     * @return the scale operation
     */
    private static RenderedImage scaleForTest(RenderedImage image, float  scaleX, float scaleY, BorderExtender borderExtender) {

        RenderingHints renderingHints = borderExtender == null ? null: new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                borderExtender
        );
        return ScaleDescriptor.create(
                image,
                scaleX, scaleY, 1.f, 1.f,
                new InterpolationNearest(),
                null, false,
                RangeFactory.create(-999.0f, true, -999.0f, true, true),
                new double[]{255},
                renderingHints
        );
    }

    /**
     * From the given input image, create a tiled image of the given
     * tile-width and tile-height.
     * <p>
     * The image will be constructed by using the JAI 'format' operator,
     * passing a new image layout with given tile width/height.
     * @param image the source image
     * @param tileWidth the tile width
     * @param tileHeight the tile height
     * @return a tiled image ({@link com.sun.media.jai.opimage.CopyOpImage}
     *         as generated by the 'format' operator)
     */
    private static RenderedOp tileImage(RenderedImage image, int tileWidth, int tileHeight) {
        // create tiled image layout
        ImageLayout tileLayout = new ImageLayout(image);
        tileLayout.setTileWidth(tileWidth);
        tileLayout.setTileHeight(tileHeight);
        // use 'format' Op to create the wrapper
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        return JAI.create(
                "format", pb,
                new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tileLayout)
        );
    }

    /**
     * Create the original source image for this test.
     * <p>
     * The image will be a {@link BufferedImage} of the given size
     * and {@link DataBuffer#TYPE_FLOAT}. The image data will be a single float value.
     * @param imageWidth the image width
     * @param imageHeight the image height
     * @return a {@link BufferedImage} of type float, filled with a constant value
     */
    private static RenderedImage testImage(int imageWidth, int imageHeight) {
        // create a raster and fill
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, imageWidth, imageHeight, 1, null);
        float[] dataBuffer = ((DataBufferFloat) raster.getDataBuffer()).getData();
        for (int rowIdx = 0; rowIdx < imageHeight; rowIdx++) {
            for (int columnIdx = 0; columnIdx < imageWidth; columnIdx++) {
                dataBuffer[(imageHeight - rowIdx - 1) * imageWidth + columnIdx] = 32.2f;
            }
        }
        // simple color model
        int[] dataTypeSize = new int[] {DataBuffer.getDataTypeSize(DataBuffer.TYPE_FLOAT)};
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(colorSpace, dataTypeSize, false, true, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        // create a buffered image
        return new BufferedImage(colorModel, raster, false, null);
    }

    /**
     * The class extends a {@link NullOpImage} and overrides
     * {@link PlanarImage#getData(Rectangle)} so that
     * we can assert on requested regions that are larger than the
     * tile width/height.
     */
    static class ScaleOpTestImage extends NullOpImage {

        public ScaleOpTestImage(RenderedImage source) {
            super(source, new ImageLayout(source),null, OpImage.OP_COMPUTE_BOUND);
        }

        /**
         * The method is overridden in order to assert on the regions
         * that {@link ScaleOpImage} passes to its source when requesting
         * data in {@link ScaleOpImage#computeTile(int, int)}.
         *
         * @param region The rectangular region of this image to be
         * returned, or <code>null</code>.
         *
         * @return calls {@code super.getData()} but asserts on
         * regions that are larger than tile width/height
         */
        @Override
        public Raster getData(Rectangle region) {
            assertTrue("Region width "+region.width+" should be smaller/equal than tileWidth " + getTileWidth(),
                    region.width <= getTileWidth()
            );
            assertTrue("Region height "+region.height+" should be smaller/equal than tileHeight " + getTileHeight(),
                    region.height <= getTileHeight()
            );
            return super.getData(region);
        }
    }
}
