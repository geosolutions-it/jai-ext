package it.geosolutions.jaiext.changematrix;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
//import java.io.File;
//import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;

import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;


/*
 * JCuda declarations: I need the "JCuda Runtime API"
 */
import static jcuda.driver.JCudaDriver.*;
import java.io.*; // delete or replace with already given java.io.File/IOException
import jcuda.*;
import jcuda.driver.*;

/**
 * This test-class is used for testing images in the directory
 * src/test/resources/it/geosolutions/jaiext/changematrix/test-data-expanded. If
 * the data are not present, the test are not performed and a WARNING is shown,
 * or the user can change the current image directory to test-data. After the
 * addition of the images, the user can change some parameters:
 * DEFAULT_THREAD_NUMBER,which indicates the number of thread retrieving the
 * result image-tiles; DEFAULT_TILE_HEIGHT, the height of every tile of the 2
 * initial images; DEFAULT_TILE_WIDTH, the width of every tile of the 2 initial
 * images; NUM_CYCLES_BENCH, the number of the benchmark cycles for every test;
 * NUM_CYCLES_WARM, the number of the initial cycles for every test that are not
 * considered inside the statistics. The initial method startUp is used for reading
 * the 2 images, expanding the tileCache, fill the tile cache with the tile of both
 * images and calculation of the Histogram of the images for the changeMatrix. The first
 * test calculates the changematrix from the current and reference images and only the result
 * image tiles are removed from the cache. The second test simply adds the tiles of the 2 
 * images to the cache and then remove them from it, continuously.
 * 
 * TODO add multithreading
 */
public class CudaChangeMatrixTest extends AbstractBenchmark {
    
    private final class MyRunnable implements Runnable {
        private final int tileX;
        
        private final int tileY;
        
        private final CountDownLatch latch;

        /**
         * @param tileX
         * @param tileY
         * @param latch
         */
        public MyRunnable(int tileX, int tileY, CountDownLatch latch) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.latch = latch;
        }
     
        @Override
        public void run() {
            
            // get the raster for reference and current
            Raster ref=reference.getTile(tileX, tileY),cur=null;
            Rectangle rect= ref.getBounds();
            rect=rect.intersection(reference.getBounds());
            ref=reference.getData(rect);
            cur=current.getData(rect);
            
            // transform into byte array
            final DataBufferByte dbRef = (DataBufferByte) ref.getDataBuffer();
            final DataBufferByte dbCurrent = (DataBufferByte) cur.getDataBuffer();
            byte dataRef[]=dbRef.getData(0);
            byte dataCurrent[]=dbCurrent.getData(0);
            
            assert dataRef.length==rect.width*rect.height;
            assert dataCurrent.length==rect.width*rect.height;
            
            System.out.println("Calling JCUDA tileX:"+tileX+" tileY:"+tileY);
            // call CUDA and get result
            // I am expecting the host_oMap as the first array and host_chMat as the second array
            final List<int[]> result=JCudaChangeMat(dataRef,dataCurrent);
            System.out.println("Cuda kernels work fine !!");
            System.out.println("");
            
            // build output image and save
            System.out.println("build output image	1");
            final BufferedImage biImage= new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_BYTE_GRAY);
            System.out.println("build output image	2");
            final DataBufferInt dbFinal= new DataBufferInt(result.get(0),result.get(0).length);
            System.out.println("build output image	3");
            final Raster finalR= RasterFactory.createRaster(biImage.getSampleModel(), dbFinal, new Point(0,0));
            System.out.println("build output image	4");
            biImage.setData(finalR);
            try {
                //ImageIO.write(biImage, "tiff", new File("d:/data/unina/test/row"+tileY+"_col"+tileX+"_"+".tif"));
            	ImageIO.write(biImage, "tiff", new File("/home/giuliano/git/jai-ext/out/row"+tileY+"_col"+tileX+"_"+".tif"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }    
            
            latch.countDown();
            
        }

    }

	//private final static String REFERENCE_PATH_FOR_TESTS = "d:/data/unina";
    private final static String REFERENCE_PATH_FOR_TESTS = "/home/giuliano/work/Projects/LIFE_Project/LUC_gpgpu/rasterize";

	private final static int DEFAULT_TILE_HEIGHT = 1024;

	private final static int DEFAULT_TILE_WIDTH = 1024;

	// The first NUM_CYCLES_WARM cycles are not considered because they simply
	// allows the
	// Java Hotspot to compile the code. Then the other NUM_CYCLES_BENCH cycles
	// are calculated
	private static final int NUM_CYCLES_BENCH = 1;//=3
	private static final int NUM_CYCLES_WARM = 1;

    private RenderedOp current;

    private RenderedOp reference;

    private final static int DEFAULT_THREAD_NUMBER = 1;

	@Before
	public void init() {
		// Source and reference images acquisition
		/*
		 * final File file0 = new File(REFERENCE_PATH_FOR_TESTS,
		 * 				"clc2000_L3_100m.tif");
		 * final File file6 = new File(REFERENCE_PATH_FOR_TESTS,
		 * 				"clc2006_L3_100m.tif");
		 */
		final File file0 = new File(REFERENCE_PATH_FOR_TESTS,
				"clc2000_L3_100m.tif");
		final File file6 = new File(REFERENCE_PATH_FOR_TESTS,
				"clc2006_L3_100m.tif");

		if(!file0.exists()||!file0.canRead()||!file6.exists()||!file6.canRead()){
		    throw new IllegalArgumentException("Input files are not present!");
		}

                int tileH = DEFAULT_TILE_HEIGHT;
                int tileW = DEFAULT_TILE_WIDTH;
        
                // Tile dimension settings
                ImageLayout layout = new ImageLayout();
                layout.setTileHeight(tileH).setTileWidth(tileW).setTileGridXOffset(0).setTileGridYOffset(0);
                RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        
                // TileCache used by JAI
                TileCache defaultTileCache = JAI.getDefaultInstance().getTileCache();
        
                // Setting of the tile cache memory capacity 800M
                defaultTileCache.setMemoryCapacity(64 * 1024 * 1024);
                defaultTileCache.setMemoryThreshold(1.0f);
                JAI.getDefaultInstance().setTileCache(defaultTileCache);
                // new TCTool((SunTileCache)defaultTileCache);
        
                // Forcing the cache to store the current and reference tiles
        
                final ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
                pbj.setParameter("Input", file0);
        
                current = JAI.create("ImageRead", pbj, hints);
        
                pbj.setParameter("Input", file6);
                reference = JAI.create("ImageRead", pbj, hints);        
	}


    @Test
    @BenchmarkOptions(benchmarkRounds = NUM_CYCLES_BENCH, warmupRounds = NUM_CYCLES_WARM)
    public void testCUDA() throws Exception {

        // prepare tiles layout for input images
        final int numTileX=reference.getNumXTiles();
        final int numTileY=reference.getNumYTiles();
        final int minTileX=reference.getMinTileX();
        final int minTileY=reference.getMinTileY();
        
        
        //System.out.println(numTileX*numTileY);
        final ExecutorService ex = Executors.newFixedThreadPool(DEFAULT_THREAD_NUMBER);
        final CountDownLatch sem = new CountDownLatch(numTileX * numTileY);
        System.out.println();
        // cycle on tiles to call the CUDA code
        //for(int i=minTileY;i<minTileY+numTileY;i++){
        for(int i=1;i<2;i++){
            //for(int j=minTileX;j<minTileX+numTileX;j++){
        	for(int j=1;j<2;j++){
                ex.execute(new MyRunnable(j, i, sem));
            }
        }
        sem.await(10,TimeUnit.MINUTES);
        ex.shutdown();
    }


    /**
     * Stub method to be replaced with CUDA code
     * @param dataRef the reference data
     * @param dataCurrent the current data
     * @return a list of byte arrays containing the results
     */
    private List<int[]> JCudaChangeMat(byte[] host_iMap1,byte[] host_iMap2)
	{
        //return Arrays.asList(dataRef,dataCurrent);
        
        
        /*
         * Copyright 2013 Massimo Nicolazzo & Giuliano Langella:
         * 		(1) correct
         * 		(2) compile
         */
        
        /*
         * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
         * http://www.jcuda.org
         */

        /**
         * This uses the JCuda driver bindings to load and execute two 
         * CUDA kernels:
         * (1)
         * The first kernel executes the change matrix computation for
         * the whole ROI given as input in the form of iMap1 & iMap2.
         * It returns a 3D change matrix in which every 2D array 
         * corresponds to a given CUDA-tile (not GIS-tile). 
         * (2)
         * The second kernel sum up the 3D change matrix returning one
         * 2D array being the accountancy for the whole ROI. 
         */
/*public void main(final int[] host_iMap1,final int[] host_iMap2,
 * 				   int tiledimX, int tiledimY, int ntilesX, int ntilesY,
                   int host_oMap, int host_chMat) throws IOException
*/                
        // number of classes: !DYNAMIC!
        int crossdim 	= 45;
        // ----
        // opt function for different SIZEs
        /*
        int tiledimX	= 150;
        int tiledimY	= 158;
        int ntilesX		= 86;
        int ntilesY 	= 64;
         */
        int tiledimX	= 16;
        int tiledimY	= 16;
        int ntilesX		= 2;
        int ntilesY 	= 2;
        // ----
        int mapsize 	= tiledimX * tiledimY * Integer.SIZE;
        int mapsizeb 	= tiledimX * tiledimY * Byte.SIZE;
        // change iMap or data*? about (i) type of data, (ii) string, (iii) duplication of data
//        host_iMap1		= dataRef;
//        host_iMap2 		= dataCurrent;
        // ----
        
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = "/home/giuliano/work/Projects/LIFE_Project/LUC_gpgpu/gpgpu/changemat.ptx";
        // DOVE METTIAMO IL .ptx ??

        // Initialize the driver and create a context for the first device.
        //System.out.println("Initializing driver:");
        //System.out.println("	-cuInit(0)");
        cuInit(0);
        //System.out.println("	-CUdevice()");
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        //System.out.println("	-CUcontext()");
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        //System.out.println("Loading ptx FILE...");
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);
        
        // Obtain a function pointer to the "add" function.
        //System.out.println("changemap MOD");
        CUfunction changemap = new CUfunction();
        //System.out.println("...here...");
        cuModuleGetFunction(changemap, module, "_Z9changemapPKhS0_iiiiiPjS1_");
        //System.out.println("changemat MOD");
        CUfunction changemat = new CUfunction();
        cuModuleGetFunction(changemat, module, "_Z9changematPjii");

        // Allocate the device input data, and copy the
        // host input data to the device
        //System.out.println("dev_iMap1");
        CUdeviceptr dev_iMap1 = new CUdeviceptr();
        cuMemAlloc(dev_iMap1, mapsizeb );
        cuMemcpyHtoD(dev_iMap1, Pointer.to(host_iMap1), mapsizeb);
        //System.out.println("dev_iMap2");
        CUdeviceptr dev_iMap2 = new CUdeviceptr();
        cuMemAlloc(dev_iMap2, mapsizeb );
        cuMemcpyHtoD(dev_iMap2, Pointer.to(host_iMap2), mapsizeb);

        // Allocate device output memory
        //System.out.println("dev_oMap");
        CUdeviceptr dev_oMap = new CUdeviceptr();
        cuMemAlloc(dev_oMap, mapsize);
        //System.out.println("dev_chMat");
        CUdeviceptr dev_chMat = new CUdeviceptr();
        cuMemAlloc(dev_chMat, crossdim * crossdim * ntilesX * ntilesY);
        

        System.out.println("first kernel");
        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.
        Pointer kernelParameters1 = Pointer.to(
            Pointer.to(dev_iMap1),
            Pointer.to(dev_iMap2),
            Pointer.to(new int[]{tiledimX}),
            Pointer.to(new int[]{tiledimY}),
            Pointer.to(new int[]{ntilesX}),
            Pointer.to(new int[]{ntilesY}),
            Pointer.to(new int[]{crossdim}),
            Pointer.to(dev_chMat),
            Pointer.to(dev_oMap)
        );

        //System.out.println("pointers done");
        // Call the kernel function.
        int blockSizeX = 1;
        int blockSizeY = 1;
        int blockSizeZ = 1;
        int gridSizeX = 75;
        int gridSizeY = 75;
        int gridSizeZ = 1;
        //System.out.println("launch cuda kernel");
        int status_k1 = cuLaunchKernel(changemap,
            gridSizeX,  blockSizeY, blockSizeZ,   // Grid dimension
            blockSizeX, gridSizeY, gridSizeZ,     // Block dimension
            0, null,               // Shared memory size and stream
            kernelParameters1, null // Kernel- and extra parameters
        );
        System.out.println("	k1 = "+status_k1);
        //System.out.println("synchro");
        int status_syn1 = cuCtxSynchronize();
        System.out.println("	synchro_1 = "+status_syn1);

        System.out.println("second kernel");
        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.
        Pointer kernelParameters2 = Pointer.to(
            Pointer.to(dev_chMat),
            Pointer.to(new int[]{crossdim * crossdim}),
            Pointer.to(new int[]{ntilesX * ntilesY})
            );
        //System.out.println("pointers done");
        //System.out.println("launch cuda kernel");
        int status_k2 = cuLaunchKernel(changemat,
            gridSizeX,  blockSizeY, blockSizeZ,   // Grid dimension
            blockSizeX, gridSizeY, gridSizeZ,     // Block dimension
            0, null,               // Shared memory size and stream
            kernelParameters2, null // Kernel- and extra parameters
        );
        System.out.println("	k2 = "+status_k2);
        int status_syn2 = cuCtxSynchronize();
        System.out.println("	synchro_2 = "+status_syn2);

        // Allocate host output memory and copy the device output
        // to the host.
        int host_chMat[] = new int[crossdim * crossdim];
        cuMemcpyDtoH(Pointer.to(host_chMat), dev_chMat, crossdim * crossdim * Sizeof.INT);
        int host_oMap[] = new int[tiledimX * tiledimY];
        cuMemcpyDtoH(Pointer.to(host_oMap), dev_oMap, mapsize);

        // Clean up.
        cuMemFree(dev_iMap1);
        cuMemFree(dev_iMap2);
        cuMemFree(dev_oMap);
        cuMemFree(dev_chMat);
        //System.out.println("...here...");
        return Arrays.asList(host_oMap,host_chMat);
    }
}