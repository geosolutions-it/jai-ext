package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.List;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class ZonalStatsRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {

        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the classifier
        RenderedImage classifier = (RenderedImage) pb.getObjectParameter(0);
        // Selection of the parameters
        AffineTransform transform;
        if (classifier != null) {
            transform = (AffineTransform) pb.getObjectParameter(1);
        } else {
            transform = null;
        }
        List<ROI> roilist = (List<ROI>) pb.getObjectParameter(2);
        ROI roi = (ROI) pb.getObjectParameter(3);
        Range noData = (Range) pb.getObjectParameter(4);
        boolean useROIAccessor = (Boolean) pb.getObjectParameter(5);
        int[] bands = (int[]) pb.getObjectParameter(6);
        StatsType[] statsTypes = (StatsType[]) pb.getObjectParameter(7);
        double[] minBound = (double[]) pb.getObjectParameter(8);
        double[] maxBound = (double[]) pb.getObjectParameter(9);
        int[] numBins = (int[]) pb.getObjectParameter(10);
        //Image creation
        return new ZonalStatsOpImage(source, layout, hints, classifier, transform, roilist, roi,
                noData, useROIAccessor, bands, statsTypes, minBound, maxBound, numBins);
    }

}
