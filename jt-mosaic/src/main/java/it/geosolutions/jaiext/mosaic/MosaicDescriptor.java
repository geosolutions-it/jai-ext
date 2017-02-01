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
package it.geosolutions.jaiext.mosaic;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.registry.RenderedRegistryMode;


/**
 * This class is very similar to the Mosaic operation because it returns a
 * composition of various images of the same type (same bands and same
 * dataType). This mosaic implementation has two main differences from the
 * first:
 * <ul>
 * <li>It doesn't support the threshold weight type.</li>
 * <li>It handles source no data values.</li>
 * </ul>
 * This new behavior can be summarized with this code:
 * <ul>
 * <li>Overlay mode</li>
 * 
 * <pre>
 * // s[i][x][y] = pixel value for the source i
 * // d[x][y] = pixel value of the destination 
 * d[x][y] = destinationNoData;
 * for (int i=0; i< sources.length(); i++) {
 *      if (!SourceNoDataRange[i].contains(s[i][x][y]) { 
 *              d[x][y] = s[i][x][y];
 *              break;
 *      } 
 * }
 * </pre>
 * 
 * <li>Blend mode. The destination pixel is calculated as a combination of all
 * the source pixel in the same position.</li>
 * 
 * <pre>
 * // s[i][x][y] = pixel value for the source i
 * // w[i][x][y] = weigthed value of the destination 
 * w[i][x][y] = 0;
 * for (int i=0; i< sources.length(); i++) {
 *      if (!SourceNoDataRange[i].contains(s[i][x][y]) { 
 *              w[i][x][y] = 1;
 *      } 
 * }
 * 
 * </pre>
 * 
 * </ul>
 * <p>
 * The operation parameters are:
 * <ul>
 * <li>A Java Bean used for storing image data, ROI and alpha channel if
 * present, and no data Range.</li>
 * <li>The type of operation executed(Overlay or Blend) .</li>
 * <li>The destination no data value used if all the pixel source in the same
 * location are no data.</li>
 * </ul>
 * </p>
 * <p>
 * The no data support is provided using the <code>Range</code> class in the
 * JAI-EXT package.
 * </p>
 * <p>
 * In this Mosaic implementation the no data support has been added for
 * geospatial images mosaic elaborations. In that images the there could be
 * different type of nodata and a simple thresholding operation couldn't be
 * enough for avoiding image artifacts.
 * <p>
 * The ROI and alpha mosaic type are equal to those of the classic MosaicOp.
 * 
 * @see MosaicOpImage
 */
public class MosaicDescriptor extends OperationDescriptorImpl {

    /** serialVersionUID */
    private static final long serialVersionUID = 2718297230579888333L;

    /**
     * The resource strings that indicates the global name, local name, vendor, 
     * a simple operation description, the documentation URL, the version
     * number and a simple description of the operation parameters.
     */
    private static final String[][] resources = {
            { "GlobalName", "Mosaic" },
            { "LocalName", "Mosaic" },
            { "Vendor", "it.geosolutions.jaiext" },
            {
                    "Description",
                    "A different mosaic operation which supports noData and doesn't supports threshold" },
            { "DocURL", "wiki github non already available" },
            { "Version", "1.0" }, 
            { "arg0Desc", "Mosaic Type" },
            { "arg1Desc", "The source Alpha bands" },
            { "arg2Desc", "The source ROIs" },
            { "arg3Desc", "Thresholds used for the mosaic" },
            { "arg4Desc", "Background values" },
            { "arg5Desc", "No data  Values" }
            };

    /** The parameter class. Used for the constructor. */
    private static final Class[] paramClasses = {         
    	javax.media.jai.operator.MosaicType.class,
        javax.media.jai.PlanarImage[].class,
        javax.media.jai.ROI[].class,
        double[][].class,
        double[].class,
        it.geosolutions.jaiext.range.Range[].class,
    };

    /** The parameter name list. Used for the constructor. */
    private static final String[] paramNames = { 
        "mosaicType",
        "sourceAlpha",
        "sourceROI",
        "sourceThreshold",
        "backgroundValues",
        "nodata"
    };

    /** The parameter values. Used for the constructor. */
    private static final Object[] paramDefaults = { 
        javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
        null,
        null,
        new double[][] {{1.0}},
        new double[] {0.0},
        null };

    /** Constructor. */
    public MosaicDescriptor() {
        super(resources, new String[] { RenderedRegistryMode.MODE_NAME }, 0,
                paramNames, paramClasses, paramDefaults, null);
    }

    /** Check if the Renderable mode is supported */
    public boolean isRenderableSupported() {
        return false;
    }

    /**
     * RenderedOp creation method that takes all the parameters, passes them to the
     * ParameterBlockJAI and then call the JAI create method for the mosaic
     * operation with no data support.
     * 
     * @param sources The RenderdImage source array used for the operation.
     * @param mosaicType This field sets which type of mosaic operation must be
     *        executed.
     * @param sourceAlpha source alpha bands
     * @param sourceROI source ROI
     * @param sourceThreshold source thresholds
     * @param backgroundValues This value fills the image pixels that contain no
     *        data.
     * @param nodata array of NoData {@link Range} used for checking nodata values
     * @param renderingHints This value sets the rendering hints for the operation.
     * @return A RenderedOp that performs the mosaic operation with no data support.
     */
    public static RenderedOp create(RenderedImage[] sources,
            MosaicType mosaicType,
            PlanarImage[] sourceAlpha,
            ROI[] sourceROI,
            double[][] sourceThreshold,
            double[] backgroundValues,
            Range[] nodata,
            RenderingHints renderingHints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Mosaic", RenderedRegistryMode.MODE_NAME);

        // All the source images are added to the parameter block.
        int numSources = sources.length;
        for (int i = 0; i < numSources; i++) {
            pb.addSource(sources[i]);
        }
        // Then the parameters are passed to the parameterblockJAI.
        pb.setParameter("mosaicType", mosaicType);
        pb.setParameter("sourceAlpha", sourceAlpha);
        pb.setParameter("sourceROI", sourceROI);
        pb.setParameter("sourceThreshold", sourceThreshold);
        pb.setParameter("backgroundValues", backgroundValues);
        pb.setParameter("nodata", nodata);
        // JAI operation performed.
        return JAI.create("Mosaic", pb, renderingHints);
    }

}
