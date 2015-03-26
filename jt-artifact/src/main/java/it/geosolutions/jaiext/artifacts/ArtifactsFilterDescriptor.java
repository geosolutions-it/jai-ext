/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 GeoSolutions


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
package it.geosolutions.jaiext.artifacts;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * A Artifacts Filter operation descriptor.
 * 
 * Given an input image and a ROI, set the values of pixels outside the ROI to the background value and transform the pixels along the BORDER of the
 * ROI, if less than a specified Luminance threshold value, to a mean of all surrounding pixels within ROI, having Luminance greater than threshold.
 * It should be pointed out that users may specify a NoData Range to use in order to avoid to calculate NoData values.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 * 
 * 
 * @source $URL$
 */
public class ArtifactsFilterDescriptor extends OperationDescriptorImpl {

    private static final String[] srcImageNames = { "sourceImage" };

    private static final Class<?>[][] srcImageClasses = { { RenderedImage.class } };

    static final int ROI_ARG = 0;

    static final int BACKGROUND_ARG = 1;

    static final int THRESHOLD_ARG = 2;

    static final int FILTERSIZE_ARG = 3;

    static final int NODATA_ARG = 4;

    private static final int DEFAULT_FILTER_SIZE = 3;

    private static final int DEFAULT_THRESHOLD = 10;

    private static final String[] paramNames = { "roi", "backgroundValues", "threshold",
            "filterSize", "nodata" };

    private static final Class<?>[] paramClasses = { ROI.class, double[].class, Integer.class,
            Integer.class, it.geosolutions.jaiext.range.Range.class };

    private static final Object[] paramDefaults = { (ROI) null, (double[]) null, DEFAULT_THRESHOLD,
            DEFAULT_FILTER_SIZE, null };

    /** Constructor. */
    public ArtifactsFilterDescriptor() {
        super(
                new String[][] {
                        { "GlobalName", "ArtifactsFilter" },
                        { "LocalName", "ArtifactsFilter" },
                        { "Vendor", "it.geosolutions.jaiext" },
                        { "Description",
                                "Filter pixels along the ROI BORDER with Luminance value less than threshold" },
                        { "DocURL", "" },
                        { "Version", "1.0.0" },
                        {
                                "arg0Desc",
                                String.format("%s (default %s) - a ROI defining working area",
                                        paramNames[ROI_ARG], paramDefaults[ROI_ARG]) },
                        {
                                "arg1Desc",
                                String.format(
                                        "%s (default %s) - an array of double that define values "
                                                + "for the background ",
                                        paramNames[BACKGROUND_ARG], paramDefaults[BACKGROUND_ARG]) },
                        {
                                "arg2Desc",
                                String.format(
                                        "%s (default %s) - an integer defining the luminance threshold value",
                                        paramNames[THRESHOLD_ARG], paramDefaults[THRESHOLD_ARG]) },
                        {
                                "arg3Desc",
                                String.format(
                                        "%s (default %s) - an integer defining the filterSize",
                                        paramNames[FILTERSIZE_ARG], paramDefaults[FILTERSIZE_ARG]) },
                        {
                                "arg4Desc",
                                String.format(
                                        "%s (default %s) - a Range defining the image nodata",
                                        paramNames[NODATA_ARG], paramDefaults[NODATA_ARG]) }, },

                new String[] { RenderedRegistryMode.MODE_NAME }, // supported modes
                srcImageNames, srcImageClasses, paramNames, paramClasses, paramDefaults, null // valid values (none defined)
        );
    }

    public static RenderedImage create(RenderedImage sourceImage, ROI sourceRoi,
            double[] backgroundValues, final int threshold, RenderingHints hints) {
        return create(sourceImage, sourceRoi, backgroundValues, threshold, DEFAULT_FILTER_SIZE,
                null, hints);
    }

    public static RenderedImage create(RenderedImage sourceImage, ROI sourceRoi,
            double[] backgroundValues, RenderingHints hints) {
        return create(sourceImage, sourceRoi, backgroundValues, DEFAULT_THRESHOLD,
                DEFAULT_FILTER_SIZE, null, hints);
    }

    public static RenderedImage create(RenderedImage sourceImage, ROI sourceRoi,
            double[] backgroundValues, final int threshold, Range nodata, RenderingHints hints) {
        return create(sourceImage, sourceRoi, backgroundValues, threshold, DEFAULT_FILTER_SIZE,
                nodata, hints);
    }

    public static RenderedImage create(RenderedImage sourceImage, ROI sourceRoi,
            double[] backgroundValues, Range nodata, RenderingHints hints) {
        return create(sourceImage, sourceRoi, backgroundValues, DEFAULT_THRESHOLD,
                DEFAULT_FILTER_SIZE, nodata, hints);
    }

    /**
     * Convenience method which constructs a {@link ParameterBlockJAI} and invokes {@code JAI.create("ArtifactsFilter", params) }
     * 
     * @param sourceImage the image to be restored
     * 
     * @param sourceRoi a {@link ROI} defining the working area
     * @param backgroundValues double array used for defining background
     * @param threshold integer value used for defining the luminance threshold
     * @param filterSize size of the filter used for filtering artifacts
     * @param nodata a {@link Range} object defining input NoData
     * 
     * @param hints an optional RenderingHints object
     * 
     * @return a RenderedImage
     */
    public static RenderedImage create(RenderedImage sourceImage, ROI sourceRoi,
            double[] backgroundValues, final int threshold, final int filterSize, Range nodata,
            RenderingHints hints) {

        ParameterBlockJAI pb = new ParameterBlockJAI("ArtifactsFilter",
                RenderedRegistryMode.MODE_NAME);

        pb.setSource(srcImageNames[0], sourceImage);
        pb.setParameter(paramNames[ROI_ARG], sourceRoi);
        pb.setParameter(paramNames[BACKGROUND_ARG], backgroundValues);
        pb.setParameter(paramNames[THRESHOLD_ARG], threshold);
        pb.setParameter(paramNames[FILTERSIZE_ARG], filterSize);
        pb.setParameter(paramNames[NODATA_ARG], nodata);

        return JAI.create("ArtifactsFilter", pb, hints);
    }

    /**
     * Returns true to indicate that properties are supported
     */
    @Override
    public boolean arePropertiesSupported() {
        return true;
    }

    /**
     * Checks parameters for the following:
     * <ul>
     * <li>Number of sources is 1
     * <li>Data image bands are valid
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean validateArguments(String modeName, ParameterBlock pb, StringBuffer msg) {
        if (pb.getNumSources() == 0 || pb.getNumSources() > 1) {
            msg.append("ArtifactsFilter operator takes 1 source image");
            return false;
        }

        // CHECKING Background values
        Object backgroundValues = pb.getObjectParameter(BACKGROUND_ARG);
        double[] bgValues = null;
        if (!(backgroundValues instanceof double[])) {
            msg.append(paramNames[BACKGROUND_ARG] + " arg has to be of type double[]");
            return false;
        } else {
            bgValues = (double[]) backgroundValues;
        }

        // CHECKING DATA IMAGE
        RenderedImage dataImg = pb.getRenderedSource(0);
        Rectangle dataBounds = new Rectangle(dataImg.getMinX(), dataImg.getMinY(),
                dataImg.getWidth(), dataImg.getHeight());

        // CHECKING ROI
        Object roiObject = pb.getObjectParameter(ROI_ARG);
        if (roiObject != null) {
            if (!(roiObject instanceof ROI)) {
                msg.append("The supplied ROI is not a supported class");
                return false;
            }
            final ROI roi = (ROI) roiObject;
            final Rectangle roiBounds = roi.getBounds();
            if (!roiBounds.intersects(dataBounds)) {
                msg.append("The supplied ROI does not intersect the source image");
                return false;
            }
        } else {
            msg.append("The ROI parameter is missing ");
            return false;
        }

        return true;
    }

}
