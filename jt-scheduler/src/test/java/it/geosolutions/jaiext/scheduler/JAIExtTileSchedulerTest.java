package it.geosolutions.jaiext.scheduler;

import java.awt.image.Raster;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileScheduler;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.ScaleDescriptor;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for JAIExtTileScheduler.
 */
public class JAIExtTileSchedulerTest{

    private static final float DEFAUL_DIM = 256f;
    
    private static final float DEFAUL_SCALE = 2f;
    
    private static final float DEFAUL_TRANSLATE = 0;

    @Test(expected = IllegalArgumentException.class)
    public void testWrongParallelism() {
        JAIExtTileScheduler scheduler = new JAIExtTileScheduler();
        // Setting of the Wrong parallelism an exception must be thrown
        scheduler.setParallelism(-1);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPrefetchParallelism() {
        JAIExtTileScheduler scheduler = new JAIExtTileScheduler();
        // Setting of the Wrong parallelism an exception must be thrown
        scheduler.setPrefetchParallelism(-1);
    }

    @Test
    public void testScheduler() {
        TileScheduler scheduler = JAI.getDefaultInstance().getTileScheduler();
        JAIExtTileScheduler tileScheduler = new JAIExtTileScheduler(scheduler.getParallelism(),
                scheduler.getPriority(), scheduler.getPrefetchParallelism(),
                scheduler.getPrefetchPriority());
        JAI.getDefaultInstance().setTileScheduler(tileScheduler);
        // Constant descriptor
        RenderedOp image = ConstantDescriptor.create(DEFAUL_DIM, DEFAUL_DIM, new Byte[]{5}, null);

        //Image scaling
        image = ScaleDescriptor.create(image, DEFAUL_SCALE, DEFAUL_SCALE, DEFAUL_TRANSLATE, DEFAUL_TRANSLATE, null, null);
        
        //Requests to the scheduler (No exception will be thrown)
        Raster[] tiles = image.getTiles();
        // Check if the tiles have been calculated
        Assert.assertNotNull(tiles);
        Assert.assertTrue(tiles.length > 0);
    }
}
