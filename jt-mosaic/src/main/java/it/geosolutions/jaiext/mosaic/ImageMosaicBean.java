/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 - 2015 GeoSolutions


* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.geosolutions.jaiext.mosaic;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.Serializable;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterFormatTag;

import it.geosolutions.jaiext.range.Range;

/**
 * This class implements a Java Bean used as input values container of the for the MosaicOpImage2 class. Inside this class are contained the
 * following values:
 * 
 * <ul>
 * <li>The input image</li>
 * <li>an eventual alpha channel</li>
 * <li>an eventual ROI</li>
 * <li>an eventual Range of No Data values</li>
 * </ul>
 * 
 * The all class methods are getters and setters of all the private variables. The constructor is parameterless and the class implements the
 * Serializable interface. These characteristics are fundamental for creating a Java Bean.
 */
public class ImageMosaicBean implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -8333010416931087950L;

    private RenderedImage image;

    private PlanarImage alphaChannel;
    
    private RenderedImage roiImage;

    private ROI roi;

    private Range sourceNoData;

    RasterFormatTag rasterFormatTag;

    public RenderedImage getImage() {
        return image;
    }

    public ColorModel getColorModel() {
        return image.getColorModel();
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

    public ROI getRoi() {
        return roi;
    }

    public void setRoi(ROI roi) {
        this.roi = roi;
    }

    public Range getSourceNoData() {
        return sourceNoData;
    }

    public void setSourceNoData(Range sourceNoData) {
        this.sourceNoData = sourceNoData;
    }
    
    public RenderedImage getRoiImage() {
        return roiImage;
    }

    public void setRoiImage(RenderedImage roiImage) {
        this.roiImage = roiImage;
    }

    // default constructor
    public ImageMosaicBean() {
    }

    /**
     * @return the rasterFormatTag
     */
    public RasterFormatTag getRasterFormatTag() {
        return rasterFormatTag;
    }

    /**
     * @param rasterFormatTag the rasterFormatTag to set
     */
    public void setRasterFormatTag(RasterFormatTag rasterFormatTag) {
        this.rasterFormatTag = rasterFormatTag;
    }
}
