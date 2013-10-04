package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * Simple class that provides the RenderedImage create operation by calling the SimpleStatsOpImage. The input parameters are: ParameterBlock,
 * RenderingHints. The first one stores all the input parameters, the second stores eventual hints used for changing the image settings. The only one
 * method of this class returns a new instance of the SimpleStatsOpImage operation.
 */
public class StatisticsRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the parameters
        int xPeriod = pb.getIntParameter(0);
        int yPeriod = pb.getIntParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        boolean useROIAccessor = (Boolean) pb.getObjectParameter(4);
        int[] bands = (int[]) pb.getObjectParameter(5);
        StatsType[] statsTypes = (StatsType[]) pb.getObjectParameter(6);
        
        boolean isSimpleStat = true;
        
        for(int i = 0 ; i< statsTypes.length; i++){
            if(statsTypes[i].getStatsId()>6){
                isSimpleStat = false;
                break;
            }
        }
        
     // Creation of the OpImage 
        if(isSimpleStat){       
            return new SimpleStatsOpImage(source, layout, hints, xPeriod, yPeriod, roi, noData, useROIAccessor, bands, statsTypes);
        }else{
            double[] minBounds = (double[]) pb.getObjectParameter(7);
            double[] maxBounds = (double[]) pb.getObjectParameter(8);
            int[] numBins = (int[]) pb.getObjectParameter(9);
            return new ComplexStatsOpImage(source, layout, hints, xPeriod, yPeriod, roi, noData, useROIAccessor, bands, statsTypes, minBounds, maxBounds, numBins);
        }
    }

}
