package it.geosolutions.jaiext.mosaic;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.registry.RenderedRegistryMode;
import org.jaitools.JAITools;

import com.sun.media.jai.opimage.MosaicOpImage;

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
 * for(int i=0; i< sources.length(); i++){
 *      if(!SourceNoDataRange[i].contains(s[i][x][y]){ 
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
 * for(int i=0; i< sources.length(); i++){
 *      if(!SourceNoDataRange[i].contains(s[i][x][y]){ 
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
 * {@link JAITools} package. This class contains one value or a group of
 * contiguous values and it is used for checking if every source pixel is
 * contained into. If <code>True</code>, it means that the selected pixel is a
 * no data value.
 * </p>
 * <p>
 * In this Mosaic implementation the no data support has been added for
 * geospatial images mosaic elaborations. In that images the there could be
 * different type of nodata and a simple thresholding operation couldn't be
 * enough for avoiding image artifacts.
 * <p>
 * The ROI and alpha mosaic type are equal to those of the classic MosaicOp.
 * 
 * @see MosaicOpImage2
 */
public class MosaicDescriptor extends OperationDescriptorImpl {

/** serialVersionUID */
private static final long serialVersionUID = 2718297230579888333L;

/**
 * With this mosaic type, the destination pixel value is calculated as weigthed
 * sum of all the source pixels in the same position. (This mosaic type is
 * equals to that of the standard Mosaic operation).
 */
public static final MosaicType MOSAIC_TYPE_BLEND = MosaicDescriptor.MOSAIC_TYPE_BLEND;

/**
 * With this mosaic type, the destination pixel value is calculated as the first
 * pixel source in the same position without no data. (This mosaic type is
 * equals to that of the standard Mosaic operation).
 */
public static final MosaicType MOSAIC_TYPE_OVERLAY = MosaicDescriptor.MOSAIC_TYPE_OVERLAY;

/**
 * The resource strings that indicates the global name, local name, vendor, a
 * simple operation description, the documentation URL, the version number and a
 * simple description of the operation parameters.
 */
private static final String[][] resources = {
        { "GlobalName", "MosaicNoData" },
        { "LocalName", "MosaicNoData" },
        { "Vendor", "it.geosolutions.jaiext.mosaic" },
        {
                "Description",
                "A different mosaic operation which supports noData and doesn't supports threshold" },
        { "DocURL", "wiki github non already available" },
        { "Version", "1.0" }, { "arg0Desc", "ImageMosaicBean " },
        { "arg1Desc", "Mosaic Type" },
        { "arg2Desc", "Destination no data  Values" } };

/** The parameter class. Used for the constructor. */
private static final Class[] paramClasses = { ImageMosaicBean[].class,
        MosaicType.class, double[].class };

/** The parameter name list. Used for the constructor. */
private static final String[] paramNames = { "imageMosaicBean", "mosaicType",
        "destinationNoData" };

/** The parameter values. Used for the constructor. */
private static final Object[] paramDefaults = { null, MOSAIC_TYPE_OVERLAY,
        new double[] { 0.0 } };

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
 * This method check if the parameters are suitable for the operation.
 * 
 * @param pb The ParameterBlock containing the values to check
 * @return <code>True</code> only if all the parameters are valid.
 */
public boolean validateParameters(ParameterBlock pb) {
    // All the parameters are listed
    List params = pb.getParameters();
    for (int i = 0; i < params.size(); i++) {
        switch (i) {
        /*
         * If the ImageBeam array has not the same length of the source list,
         * this method returns false.
         */
        case 1:
            ImageMosaicBean[] bean = (ImageMosaicBean[]) pb
                    .getObjectParameter(i);
            int numSource = pb.getNumSources();
            if (bean.length != numSource) {
                return false;
            }
            break;
        /*
         * If the Mosaic type is not MOSAIC_TYPE_BLEND or MOSAIC_TYPE_OVERLAY
         * this method returns false.
         */
        case 2:
            MosaicType mosaic = (MosaicType) pb.getObjectParameter(i);
            if (mosaic != MOSAIC_TYPE_BLEND || mosaic != MOSAIC_TYPE_OVERLAY) {
                return false;
            }
            break;
        default:
        }

    }
    // Otherwise it returns true
    return true;
}

/**
 * RenderedOp creation method that takes all the parameters, passes them to the
 * ParameterBlockJAI and then call the JAI create method for the mosaic
 * operation with no data support.
 * 
 * @param sources The RenderdImage source array used for the operation.
 * @param bean The Java Bean used for storing image data, ROI and alpha channel
 *        if present, and no data Range.
 * @param mosaicType This field sets which type of mosaic operation must be
 *        executed.
 * @param destinationNoData This value fills the image pixels that contain no
 *        data.
 * @param renderingHints This value sets the rendering hints for the operation.
 * @return A RenderedOp that performs the mosaic operation with no data support.
 */
public static RenderedOp create(RenderedImage[] sources,
        ImageMosaicBean[] bean, MosaicType mosaicType,
        double[] destinationNoData, RenderingHints renderingHints) {
    ParameterBlockJAI pb = new ParameterBlockJAI("MosaicNoData", RenderedRegistryMode.MODE_NAME);

    // All the source images are added to the parameter block.
    int numSources = sources.length;
    for (int i = 0; i < numSources; i++) {
        pb.addSource(sources[i]);
    }
    // Then the parameters are passed to the parameterblockJAI.
    pb.setParameter("imageMosaicBean", bean);
    pb.setParameter("mosaicType", mosaicType);
    pb.setParameter("destinationNoData", destinationNoData);
    // JAI operation performed.
    return JAI.create("MosaicNoData", pb, renderingHints);
}

}
