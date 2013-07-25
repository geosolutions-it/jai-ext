package it.geosolutions.jaiext.changematrix;

//import static org.junit.Assert.*;
import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.jai.Histogram;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
//import org.junit.Rule;
import org.junit.Test;
//import org.junit.rules.TestRule;

//import tilecachetool.TCTool;

//import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
// com.carrotsearch.junitbenchmarks.BenchmarkOptions;
// com.carrotsearch.junitbenchmarks.BenchmarkRule;
//import com.sun.media.jai.util.SunTileCache;

public class ChangeMatrixSpeedTest {//extends AbstractBenchmark{
    
    private final static String REFERENCE_PATH_FOR_TESTS = "./src/test/resources/it/geosolutions/jaiext/changematrix/test-data-expanded/";
    
    private final static int DEFAULT_THREAD_NUMBER=10;
    
    private final static int DEFAULT_TILE_HEIGHT = 512;
    
    private final static int DEFAULT_TILE_WIDTH = 512;

    private final static SetupClass initializationSetup = new SetupClass();
    
    
//    @Rule
//    public TestRule benchmarkRun = new BenchmarkRule(); 
    
    
    @BeforeClass
    public static void startUp(){
        //Source and reference images acquisition
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS,"clc2000_L3_100m.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS,"clc2006_L3_100m.tif");
        
        
        int tileH= DEFAULT_TILE_HEIGHT;
        int tileW= DEFAULT_TILE_WIDTH;
        
        // Tile dimension settings
        ImageLayout layout = new ImageLayout();
        layout.setTileHeight(tileH).setTileWidth(tileW);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout);        

        //TileCache used by JAI 
        TileCache defaultTileCache = JAI.getDefaultInstance().getTileCache(); 
        
        //Setting of the tile cache memory capacity
        defaultTileCache.setMemoryCapacity(800*1024*1024);   
        defaultTileCache.setMemoryThreshold(1.0f);
        JAI.getDefaultInstance().setTileCache(defaultTileCache);
        //new TCTool((SunTileCache)defaultTileCache);
        
        //Forcing the cache to store the source and reference tiles

        final ParameterBlockJAI pbj= new ParameterBlockJAI("ImageRead");
        pbj.setParameter("Input", file0);
        
        RenderedOp source = JAI.create("ImageRead", pbj,hints);
        
        pbj.setParameter("Input", file6);
        RenderedOp reference = JAI.create("ImageRead", pbj,hints);;  
        
        JAI.getDefaultInstance().getTileScheduler().setParallelism(DEFAULT_THREAD_NUMBER);
        
        //source.getTiles();
        //reference.getTiles();
        
        // ParameterBlock creation
//        final ParameterBlockJAI pbjHist = new ParameterBlockJAI("Histogram");
//        pbjHist.addSource(source);
//        
//        RenderedOp histogramIMG =JAI.create("Histogram",pbjHist, hints);
//        Histogram histogram=(Histogram) histogramIMG.getProperty("Histogram");
//        
//        final ParameterBlockJAI pbjHistRef = new ParameterBlockJAI("Histogram");
//        pbjHistRef.addSource(reference);
//        
//        RenderedOp histogramIMGRef =JAI.create("Histogram",pbjHist, hints);
//        Histogram histogramRef=(Histogram) histogramIMGRef.getProperty("Histogram");

        initializationSetup.setRenderingHints(hints);
        initializationSetup.setSource(source);
        initializationSetup.setReference(reference);  
        
        //Histogram storage
        //initializationSetup.setHistSource(histogram);
        //initializationSetup.setHistRef(histogramRef); 

    }

    
    private static final int NUM_CYCLES=1;
    
    // The first 20 cycles are not considered because they simply allows the Java Hotspot to
    // compile the code. Then the other 50 cycles are calculated    
    @Test
    @Ignore
    //@BenchmarkOptions(benchmarkRounds = 50,warmupRounds=20)
    public void testImageDefaultParametersNoROI() throws InterruptedException { 

        RenderedOp reference = (RenderedOp) initializationSetup.getReference();
        RenderedOp source = (RenderedOp) initializationSetup.getSource();

        RenderingHints hints = initializationSetup.getRenderingHints();

        Histogram histogram= initializationSetup.getHistSource();
        Histogram histogramRef= initializationSetup.getHistRef();
        
        
        
        for(int k=0;k<NUM_CYCLES;k++){
        
        // ChangeMatrix creation
        final Set<Integer> classes = new HashSet<Integer>();
        //Setting of the classes
        
        int[] bins=histogram.getBins(0);
        int[] binsRef=histogramRef.getBins(0);
        
        // Setting of all the source classes
        for(int i = 0; i<bins.length;i++){
            classes.add(bins[i]);
        }
        
     // Setting of all the reference classes
        for(int i = 0; i<binsRef.length;i++){
            classes.add(bins[i]);
        }
        
        final ChangeMatrix cm = new ChangeMatrix(classes);
        
        // ParameterBlock creation
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        // force computation
        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        // Cycle for calculating all the changeMatrix statistics
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(DEFAULT_THREAD_NUMBER);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                @Override
                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written
        
        //cache flushing
        result.dispose();
        }
        //initializationSetup.setResult(result); 
        
        System.out.println("test");
    }

    @Test
    //@BenchmarkOptions(benchmarkRounds = 50,warmupRounds=20)
    public void testImageTileCacheUse() throws InterruptedException { 

        RenderedOp reference = (RenderedOp) initializationSetup.getReference();
        RenderedOp source = (RenderedOp) initializationSetup.getSource();

        //long[] time = new long[NUM_CYCLES_BENCH];
        long mean=0;
        long max=0;
        long min=0;
        //long std = 0;
        
        for(int i=0; i<NUM_CYCLES;i++){
            
            long start = System.nanoTime();                       
            reference.getTiles();
            source.getTiles();
            
            //cache flushing
            //source.dispose();
            //reference.dispose(); 
            JAI.getDefaultInstance().getTileCache().flush();
            
            long end = System.nanoTime();
            
            //New code
            if(i>10){
                long time =  (end-start);
                if(i==11){
                    max=time;
                    min = time;
                }
                
                if(max<time){
                    max = time;
                }
                
                if(min>time){
                    min = time;
                }
                
                mean = mean + time;
                double meanValue= ((mean/(i-10))/1E6);
                
                
                
                System.out.println("mean = " + meanValue);
                System.out.println("max = " + max/1E6);
                System.out.println("min = " + min/1E6);
            }
            
            System.out.println("test cache");
            
//            //Old code              
//            if(i>20){
//                time[i]=end-start;
//                mean +=time[i]; 
//            }
//            
//            
//            
//            
//            System.out.println("test cache");
//            
//            if(i==NUM_CYCLES-1){
//                for(int k = 0; k<NUM_CYCLES_BENCH;k++){
//                    long a = time[k] - mean;
//                    int b = 2;
//                    std += Math.pow(a, b);
//                }
//
//                mean = mean/NUM_CYCLES_BENCH;
//                std = (long) Math.sqrt(std/NUM_CYCLES_BENCH);
//                
//                System.out.println("mean time "+ mean);
//                System.out.println("standard deviation " + std);
//            }            
        }
        
        
        
    }

    
    
    @After
    @Ignore
    public void resultDisposal(){
        //initializationSetup.getResult().dispose();
        
    }
    
    
    
    private static class SetupClass{
        
        private RenderingHints hints;
        
        private RenderedImage source;
        
        private RenderedImage reference;  
        
        private RenderedOp result;

        private Histogram histSource;
        
        private Histogram histRef;

        
        public Histogram getHistSource() {
            return histSource;
        }

        public void setHistSource(Histogram histSource) {
            this.histSource = histSource;
        }

        public Histogram getHistRef() {
            return histRef;
        }

        public void setHistRef(Histogram histRef) {
            this.histRef = histRef;
        }
        
        public RenderingHints getRenderingHints() {
            return hints;
        }

        public void setRenderingHints(RenderingHints hints) {
            this.hints = hints;
        }

        public RenderedImage getSource() {
            return source;
        }

        public void setSource(RenderedImage source) {
            this.source = source;
        }

        public RenderedOp getResult() {
            return result;
        }

        public void setResult(RenderedOp result) {
            this.result = result;
        }
        
        public RenderedImage getReference() {
            return reference;
        }

        public void setReference(RenderedImage reference) {
            this.reference = reference;
        }
        
        SetupClass(){}
    }
    
    
    
}
