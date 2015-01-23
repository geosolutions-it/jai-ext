package it.geosolutions.jaiext.colorconvert;

import java.awt.image.RenderedImage;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;

public class ColorConvertOpImage extends PointOpImage {

    public ColorConvertOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            boolean cobbleSources) {
        
        super(source, layout, configuration, cobbleSources); // TODO Auto-generated constructor stub
        
    }

}
