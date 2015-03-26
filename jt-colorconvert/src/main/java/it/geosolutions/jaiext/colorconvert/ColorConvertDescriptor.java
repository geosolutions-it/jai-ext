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
package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "ColorConvert" operation.
 * 
 * <p>
 * The "ColorConvert" operation performs a pixel-by-pixel color conversion of the data in a rendered or renderable source image.
 * 
 * <p>
 * The data are treated as having no alpha channel, i.e., all bands are color bands. The color space of the source image is specified by the
 * <code>ColorSpace</code> object of the source image <code>ColorModel</code> which must not be <code>null</code>. The color space of the destination
 * image is specified by the <code>ColorSpace</code> of the "colorModel" parameter which must be a <code>ColorModel</code>. If a
 * <code>ColorModel</code> is suggested via the <code>RenderingHints</code> it is ignored.
 * 
 * <p>
 * The calculation pathway is selected to optimize performance and accuracy based on which <code>ColorSpace</code> subclasses are used to represent
 * the source and destination color spaces. The subclass categories are <code>ICC_ColorSpace</code>, <code>ColorSpaceJAI</code>, and generic
 * <code>ColorSpace</code>, i.e., one which is not an instance of either the two aforementioned subclasses. Note that in the Sun Microsystems
 * implementation, an <code>ICC_ColorSpace</code> instance is what is returned by <code>ColorSpace.getInstance()</code>.
 * 
 * <p>
 * Integral data are assumed to occupy the full range of the respective data type; floating point data are assumed to be normalized to the range
 * [0.0,1.0].
 * 
 * <p>
 * By default, the destination image bounds, data type, and number of bands are the same as those of the source image.
 * 
 * <p>
 * Optionally users may define a NoData Range or a ROI in order to mask unwanted values or to reduce active area calculation
 * 
 * 
 * <p>
 * <table border=1>
 * <caption>Resource List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>GlobalName</td>
 * <td>ColorConvert</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>ColorConvert</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Converts the colorspace of an Image taking into account ROI and NoData.
 * <td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ColorConvertDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The destination <code>ColorModel</code>.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The ROI defining active Area.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>The NoData Range used for checking if a pixel is valid.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>The destination noData parameter.</td>
 * </tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border=1>
 * <caption>Parameter List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Class Type</th>
 * <th>Default Value</th>
 * </tr>
 * <tr>
 * <td>colorModel</td>
 * <td>java.awt.image.ColorModel</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * </tr>
 * <tr>
 * <td>roi</td>
 * <td>javax.media.jai.ROI</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>nodata</td>
 * <td>it.geosolutions.jaiext.Range</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>destNoData</td>
 * <td>double[]</td>
 * <td>new double[]{0.0}</td>
 * </tr>
 * </table>
 * </p>
 * 
 */
public class ColorConvertDescriptor extends OperationDescriptorImpl {

    /** Constructor. */
    public ColorConvertDescriptor() {
        super(
                new String[][] {
                        { "GlobalName", "ColorConvert" },
                        { "LocalName", "ColorConvert" },
                        { "Vendor", "it.geosolutions.jaiext" },
                        { "Description",
                                "Converts the colorspace of an Image taking into account ROI and NoData" },
                        {
                                "DocURL",
                                "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ColorConvertDescriptor.html" },
                        { "Version", "1.0" },
                        { "arg0Desc", "Destination ColorModel" },
                        { "arg1Desc", "Input ROI used in calculations" },
                        { "arg2Desc", "No data range used for checking if the a pixel is a nodata" },
                        { "arg3Desc", "Output value for nodata" } }, 1, new Class[] {
                        java.awt.image.ColorModel.class, javax.media.jai.ROI.class,
                        it.geosolutions.jaiext.range.Range.class, double[].class }, new String[] {
                        "colorModel", "roi", "nodata", "destNoData" }, new Object[] {
                        NO_PARAMETER_DEFAULT, null, null, new double[] { 0.0d } });
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Convert the color space of an image.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param colorModel The destination color space.
     * @param roi Optional ROI defining calculation area
     * @param nodata Optional NoData range used for checking if a pixel is a nodata
     * @param destNoData Optional value used for output NoData
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     */
    public static RenderedOp create(RenderedImage source0, ColorModel colorModel, ROI roi,
            Range nodata, double[] destinationNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("ColorConvert", RenderedRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("colorModel", colorModel);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destinationNoData);

        return JAI.create("ColorConvert", pb, hints);
    }

    /**
     * Convert the color space of an image.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     * 
     * @param source0 <code>RenderableImage</code> source 0.
     * @param colorModel The destination color space.
     * @param roi Optional ROI defining calculation area
     * @param nodata Optional NoData range used for checking if a pixel is a nodata
     * @param destNoData Optional value used for output NoData
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     */
    public static RenderableOp createRenderable(RenderableImage source0, ColorModel colorModel,
            ROI roi, Range nodata, double[] destinationNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("ColorConvert",
                RenderableRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("colorModel", colorModel);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destinationNoData);

        return JAI.createRenderable("ColorConvert", pb, hints);
    }
}
