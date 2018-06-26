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
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

import it.geosolutions.jaiext.jiffle.runtime.BandTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;

/**
 * The image factory for the "Jiffle" operation.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleRIF implements RenderedImageFactory {

    /** Constructor */
    public JiffleRIF() {}

    /**
     * Create a new instance of JiffleOpImage in the rendered layer.
     *
     * @param paramBlock specifies the source image and the parameters WRITE ME
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        String script = (String) paramBlock.getObjectParameter(JiffleDescriptor.SCRIPT_ARG);
        String destVarName = (String) paramBlock.getObjectParameter(JiffleDescriptor.DEST_NAME_ARG);
        Rectangle destBounds =
                (Rectangle) paramBlock.getObjectParameter(JiffleDescriptor.DEST_BOUNDS_ARG);
        int dataType = paramBlock.getIntParameter(JiffleDescriptor.DEST_TYPE_ARG);

        // Build an image layout based on the destination bounds, if provided, or the union of the
        // source bounds
        ImageLayout layout = (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT);
        if (destBounds != null) {
            layout = buildLayout(destBounds, getPreferredTileSize(paramBlock), dataType);
        } else if (layout == null) {
            destBounds = getSourceBounds(paramBlock);
            layout = buildLayout(destBounds, getPreferredTileSize(paramBlock), dataType);
        }

        Map<String, JiffleOpImage.ImageSpecification> sourceImages =
                buildSourceImageMap(paramBlock);

        return new JiffleOpImage(sourceImages, layout, renderHints, script, destVarName);
    }

    private Dimension getPreferredTileSize(ParameterBlock pb) {
        if (pb.getSources() == null || pb.getSources().size() == 0) {
            return JAI.getDefaultTileSize();
        } else {
            // align to the first input, should reduce the computation cost as the source
            // tiles are pulled only once
            RenderedImage ref = (RenderedImage) pb.getSource(0);
            return new Dimension(ref.getWidth(), ref.getHeight());
        }
    }

    private Map<String, JiffleOpImage.ImageSpecification> buildSourceImageMap(ParameterBlock pb) {
        Map<String, JiffleOpImage.ImageSpecification> result = new HashMap<>();
        Vector<Object> sources = pb.getSources();
        String[] names = (String[]) pb.getObjectParameter(JiffleDescriptor.SOURCE_IMAGE_NAMES_ARG);
        CoordinateTransform[] cts =
                (CoordinateTransform[])
                        pb.getObjectParameter(JiffleDescriptor.SRC_COORDINATE_TRANSFORM_ARG);
        BandTransform[] bts =
                (BandTransform[]) pb.getObjectParameter(JiffleDescriptor.SRC_BAND_TRANSFORM_ARG);

        // input-less case (possible, e.g., mandelbrot, surfaces defined by function/algorithm in
        // general)
        if (sources == null || sources.size() == 0) {
            return result;
        }

        // no user assigned names
        if (names == null) {
            for (int i = 0; i < sources.size(); i++) {
                if (i == 0) {
                    result.put("src", getImageSpecification(sources, cts, bts, i));
                } else {
                    result.put("src" + i, getImageSpecification(sources, cts, bts, i));
                }
            }
        } else {
            if (names.length != sources.size()) {
                throw new IllegalArgumentException(
                        "Have "
                                + sources.size()
                                + " sources, but the source name argument contains "
                                + names.length
                                + " entries instead");
            }

            for (int i = 0; i < sources.size(); i++) {
                result.put(names[i], getImageSpecification(sources, cts, bts, i));
            }
        }

        return result;
    }

    private JiffleOpImage.ImageSpecification getImageSpecification(
            Vector<Object> sources, CoordinateTransform[] cts, BandTransform[] bts, int i) {
        RenderedImage image = (RenderedImage) sources.get(i);
        CoordinateTransform ct = null;
        if (cts != null) {
            if (cts.length != sources.size()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Have %d sources, but the coordinate transformation argument contains %d entries instead"
                                        + sources.size()
                                        + cts.length));
            }
            ct = cts[i];
        }
        BandTransform bt = null;
        if (bts != null) {
            if (bts.length != sources.size()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Have %d sources, but the band transformation argument contains %d entries instead"
                                        + sources.size()
                                        + bts.length));
            }
            bt = bts[i];
        }
        
        return new JiffleOpImage.ImageSpecification(image, ct, bt);
    }

    private ImageLayout buildLayout(Rectangle bounds, Dimension tileSize, int dataType) {
        if (bounds == null) {
            throw new IllegalStateException(
                    "Cannot determine output image layout, dest bounds have not been provided");
        }

        ImageLayout layout = new ImageLayout(bounds.x, bounds.y, bounds.width, bounds.height);
        SampleModel sm =
                RasterFactory.createPixelInterleavedSampleModel(
                        dataType, tileSize.width, tileSize.height, 1);
        layout.setSampleModel(sm);
        layout.setColorModel(PlanarImage.createColorModel(sm));

        return layout;
    }

    private Rectangle getSourceBounds(ParameterBlock pb) {
        Rectangle boundsUnion = null;

        if (pb.getNumSources() > 0) {
            boundsUnion = getSourceBounds(pb, 0);
            for (int i = 1; i < pb.getNumSources(); i++) {
                Rectangle imageBounds = getSourceBounds(pb, i);
                boundsUnion = boundsUnion.union(imageBounds);
            }
        }

        return boundsUnion;
    }

    private Rectangle getSourceBounds(ParameterBlock pb, int imageIdx) {
        RenderedImage source = (RenderedImage) pb.getSource(imageIdx);
        return new Rectangle(
                source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight());
    }
}
