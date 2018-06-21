/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.shadedrelief;

import com.sun.media.jai.util.AreaOpPropertyGenerator;
import it.geosolutions.jaiext.range.Range;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "ShadedRelief" operation.
 *
 */
public class ShadedReliefDescriptor extends OperationDescriptorImpl {

    public static final double DEFAULT_AZIMUTH = 315;

    public static final double DEFAULT_ALTITUDE = 45;

    public static final double DEFAULT_Z = 100000;

    private static final double DEGREES_TO_METERS = 111120;

    public static final double DEFAULT_SCALE = DEGREES_TO_METERS;

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The resource strings that provide the general documentation and specify the parameter list
     * for a ShadedRelief operation.
     */
    private static final String[][] resources = {
        {"GlobalName", "ShadedRelief"},
        {"LocalName", "ShadedRelief"},
        {"Vendor", "it.geosolutions.jaiext"},
        {"Description", "desc"},
        {"Version", "1.0"},
        {"DocURL", "Not Defined" },
        {"arg0Desc", "Region of interest"},
        {"arg1Desc", "Source NoData"},
        {"arg2Desc", "Destination NoData"},
        {"arg3Desc", "X resolution"},
        {"arg4Desc", "Y resolution"},
        {"arg5Desc", "Zeta factor"},
        {"arg6Desc", "elevation unit to 2D unit scale ratio"},
        {"arg7Desc", "altitude"},
        {"arg8Desc", "azimuth"},
        {"arg9Desc", "algorithm"}
    };

    /**
     * The parameter names for the ShadedRelief operation.
     */
    private static final String[] paramNames = {
        "roi",
        "srcNoData",
        "dstNoData",
        "resX",
        "resY",
        "zetaFactor",
        "scale",
        "altitude",
        "azimuth",
        "algorithm"
    };

    /**
     * The parameter class types for the ShadedRelief operation.
     */
    private static final Class[] paramClasses = {
        javax.media.jai.ROI.class,
        Range.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        ShadedReliefAlgorithm.class
    };

    /**
     * The parameter default values for the ShadedRelief operation.
     */
    private static final Object[] paramDefaults = {
        null,
        null,
        0d,
        NO_PARAMETER_DEFAULT,
        NO_PARAMETER_DEFAULT,
        1d,
        1d,
        DEFAULT_ALTITUDE,
        DEFAULT_AZIMUTH,
        ShadedReliefAlgorithm.ZEVENBERGEN_THORNE_COMBINED
    };

    /** Constructor. */
    public ShadedReliefDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the
     * "ShadedRelief" operation.
     *
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }

    /**
     *
     * @param altitude  is the sun's angle of elevation above the horizon and ranges from 0 to 90 degrees. A value of 0 degrees indicates that the sun is on the horizon, that is, on the same horizontal plane as the frame of reference. A value of 90 degrees indicates that the sun is directly overhead.
     * @param azimuth  is the sun's relative position along the horizon (in degrees). This position is indicated by the angle of the sun measured clockwise from due north. An azimuth of 0 degrees indicates north, east is 90 degrees, south is 180 degrees, and west is 270 degrees.
     * @return
     */
    public static RenderedOp create(
            RenderedImage source0,
            ROI roi,
            Range srcNoData,
            double dstNoData,
            double resX,
            double resY,
            double zetaFactor,
            double scale,
            double altitude,
            double azimuth,
            ShadedReliefAlgorithm algorithm,
            RenderingHints hints) {
        ParameterBlockJAI pb =
                new ParameterBlockJAI("ShadedRelief", RenderedRegistryMode.MODE_NAME);

        // Setting sources
        pb.setSource("source0", source0);

        // Setting params
        pb.setParameter("roi", roi);
        pb.setParameter("srcNoData", srcNoData);
        pb.setParameter("dstNoData", dstNoData);
        pb.setParameter("resX", resX);
        pb.setParameter("resY", resY);
        pb.setParameter("zetaFactor", zetaFactor);
        pb.setParameter("scale", scale);
        pb.setParameter("altitude", altitude);
        pb.setParameter("azimuth", azimuth);
        pb.setParameter("algorithm", algorithm);

        return JAI.create("ShadedRelief", pb, hints);
    }
}
