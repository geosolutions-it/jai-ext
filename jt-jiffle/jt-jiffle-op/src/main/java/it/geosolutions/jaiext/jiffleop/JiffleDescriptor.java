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


/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package it.geosolutions.jaiext.jiffleop;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import it.geosolutions.jaiext.jiffle.runtime.BandTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;

/**
 * Jiffle operation.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleDescriptor extends OperationDescriptorImpl {

    static final int SOURCE_IMAGE_NAMES_ARG = 0;
    static final int DEST_NAME_ARG = 1;
    static final int SCRIPT_ARG = 2;
    static final int DEST_BOUNDS_ARG = 3;
    static final int DEST_TYPE_ARG = 4;
    static final int SRC_COORDINATE_TRANSFORM_ARG = 5;
    static final int SRC_BAND_TRANSFORM_ARG = 6;

    public static final String SOURCE_NAMES = "sourceNames";
    public static final String DEST_NAME = "destName";
    public static final String SCRIPT = "script";
    public static final String DEST_BOUNDS = "destBounds";
    public static final String DEST_TYPE = "destType";
    public static final String SRC_COORDINATE_TRANSFORMS = "srcCoordinateTransforms";
    public static final String SRC_BAND_TRANSFORMS = "srcBandTransforms";
    
    private static final String[] paramNames = {
            SOURCE_NAMES,
            DEST_NAME,
            SCRIPT,
            DEST_BOUNDS,
            DEST_TYPE,
            SRC_COORDINATE_TRANSFORMS,
            SRC_BAND_TRANSFORMS
    };

    private static final Class[] paramClasses = {
         String[].class,
         String.class,
         String.class,
         Rectangle.class,
         Integer.class,
         CoordinateTransform[].class,
         BandTransform[].class
    };

    private static final Object[] paramDefaults = {
         null,
         "dest",   
         NO_PARAMETER_DEFAULT,
         null,
         DataBuffer.TYPE_DOUBLE,
         null,
         null   
    };

    public JiffleDescriptor() {
        super(new String[][]{
                    {"GlobalName", "Jiffle"},
                    {"LocalName", "Jiffle"},
                    {"Vendor", "it.geosolutions.jaiext"},
                    {"Description", "Execute a Jiffle script"},
                    {"DocURL", ""},
                    {"Version", "1.2.0"},
                    {"arg0Desc", paramNames[0] + " (String[], default {src, src1, src2, ...}):" +
                                "name of the source rasters"},
                    {"arg1Desc", paramNames[1] + " (String, default \"dest\"):" +
                                "the destination variable name"},
                    {"arg2Desc", paramNames[2] + " (String):" +
                             "the Jiffle script"},
                    {"arg3Desc", paramNames[3] + " (Rectangle, default will use the image layout if provided, or the union of the sources otherwise):" +
                                "the output bounds"},
                    {"arg4Desc", paramNames[4] + " (Output data type, default is DataBuffer.TYPE_DOUBLE):" +
                                "the output data type, as a DataBuffer.TYPE_* constant"},
                    {"arg5Desc", paramNames[4] + " (Source coordinate transforms):" +
                                "the world to image source transforms, if needed"},
                    {"arg6Desc", paramNames[4] + " (Source band transforms):" +
                                "the script to image band transforms, if needed"}
                },
                new String[]{RenderedRegistryMode.MODE_NAME},   // supported modes
                1,                                              // number of sources
                paramNames,
                paramClasses,
                paramDefaults,
                null                                            // valid values (none defined)
                );
    }

    @Override
    public int getNumSources() {
        return 0;
    }

    /**
     * RenderedOp creation method that takes all the parameters, passes them to the
     * ParameterBlockJAI and then call the JAI create method for the mosaic
     * operation with no data support.
     *
     * @param sources The RenderdImage source array used for the operation.
     * @param sourceImageNames The array of source image names, that will be referred from the script. Can be null, in such case "src, src1, src2, ..." will be used as image names
     * @param destName The name of the destination image. Can be null, in such case "dest" will be used
     * @param destBounds The output bounds. It is required only if there are no sources, and no {@link javax.media.jai.ImageLayout} is provided in the hints, otherwise can be null.
     * @param destType The destination type. Not required, will default to {@link DataBuffer#TYPE_DOUBLE}
     * @param sourceCoordinateTransforms The world to image coordinate transforms for the sources                
     * @param sourceBandTransforms The band transforms for the source images
     * @param renderingHints This value sets the rendering hints for the operation.
     * @return A RenderedOp that performs the Jiffle operation.
     */
    public static RenderedOp create(RenderedImage[] sources,
            String[] sourceImageNames,
            String destName,
            String script,
            Rectangle destBounds,
            Integer destType,
            CoordinateTransform[] sourceCoordinateTransforms,
            BandTransform[] sourceBandTransforms,
            RenderingHints renderingHints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Jiffle", RenderedRegistryMode.MODE_NAME);

        // All the source images are added to the parameter block.
        int numSources = sources.length;
        for (int i = 0; i < numSources; i++) {
            pb.addSource(sources[i]);
        }
        // Then the parameters are passed to the parameterblockJAI.
        pb.setParameter(SOURCE_NAMES, sourceImageNames);
        pb.setParameter(DEST_NAME, destName);
        pb.setParameter(SCRIPT, script);
        pb.setParameter(DEST_BOUNDS, destBounds);
        pb.setParameter(DEST_TYPE, destType);
        pb.setParameter(SRC_COORDINATE_TRANSFORMS, sourceCoordinateTransforms);
        pb.setParameter(SRC_BAND_TRANSFORMS, sourceBandTransforms);
        // JAI operation performed.
        return JAI.create("Jiffle", pb, renderingHints);
    }

}
