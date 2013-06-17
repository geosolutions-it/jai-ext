package it.geosolutions.opimage;

import java.awt.image.RenderedImage;
import java.io.Serializable;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.jaitools.numeric.Range;


/** This class implements a Java Bean*/
public class ImageMosaicBean implements Serializable {

/** serialVersionUID */
    private static final long serialVersionUID = -8333010416931087950L;

private RenderedImage image;

private PlanarImage alphaChannel;

private ROI imageRoi;

private Range sourceNoData;

public RenderedImage getImage() {
    return image;
}

public void setImage(RenderedImage image) {
    this.image = image;
}

public PlanarImage getAlphaChannel() {
    return alphaChannel;
}

public void setAlphaChannel(PlanarImage alphaChannel) {
    this.alphaChannel = alphaChannel;
}

public ROI getImageRoi() {
    return imageRoi;
}

public void setImageRoi(ROI imageRoi) {
    this.imageRoi = imageRoi;
}

public Range getSourceNoData() {
    return sourceNoData;
}

public void setSourceNoData(Range sourceNoData) {
    this.sourceNoData = sourceNoData;
}



// default constructor
public ImageMosaicBean() {
}

}
