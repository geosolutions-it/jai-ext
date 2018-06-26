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
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import it.geosolutions.jaiext.jiffle.runtime.BandTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.JiffleIndirectRuntime;
import it.geosolutions.jaiext.range.NoDataContainer;

/**
 * Jiffle operation.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleOpImage extends OpImage {

    static class ImageSpecification {
        RenderedImage image;
        CoordinateTransform coordinateTransform;
        BandTransform bandTransform;

        public ImageSpecification(
                RenderedImage image,
                CoordinateTransform coordinateTransform,
                BandTransform bandTransform) {
            this.image = image;
            this.coordinateTransform = coordinateTransform;
            this.bandTransform = bandTransform;
        }
    }

    private final JiffleIndirectRuntime runtime;

    private final int band = 0;

    public JiffleOpImage(
            Map<String, ImageSpecification> sourceImages,
            ImageLayout layout,
            Map configuration,
            String script,
            String destVarName) {

        super(
                specsToImages(sourceImages),
                layout,
                configuration,
                false);

        try {
            Jiffle jiffle = new Jiffle();
            jiffle.setScript(script);

            Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
            for (String varName : sourceImages.keySet()) {
                imageParams.put(varName, Jiffle.ImageRole.SOURCE);
            }
            imageParams.put(destVarName, Jiffle.ImageRole.DEST);

            jiffle.setImageParams(imageParams);
            jiffle.compile();
            runtime =
                    (JiffleIndirectRuntime) jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT);

            for (Map.Entry<String, ImageSpecification> entry : sourceImages.entrySet()) {
                String name = entry.getKey();
                ImageSpecification spec = entry.getValue();
                runtime.setSourceImage(name, spec.image, spec.coordinateTransform);
                if (spec.bandTransform != null) {
                    runtime.setSourceImageBandTransform(name, spec.bandTransform);
                }
            }

            Rectangle bounds =
                    new Rectangle(
                            layout.getMinX(null),
                            layout.getMinY(null),
                            layout.getWidth(null),
                            layout.getHeight(null));
            runtime.setWorldByResolution(bounds, 1, 1);

        } catch (JiffleException ex) {
            throw new RuntimeException(ex);
        }

        // by default Jiffle does nodata with NaN
        setProperty(NoDataContainer.GC_NODATA, new NoDataContainer(Double.NaN));
    }

    private static Vector specsToImages(
            Map<String, ImageSpecification> sourceImages) {
        return new Vector(
                sourceImages
                        .values()
                        .stream()
                        .map(is -> is.image)
                        .collect(Collectors.toList()));
    }

    /**
     * For testing: returns null to indicate that all of the destination could be affected.
     *
     * @param sourceRect
     * @param sourceIndex
     * @return
     */
    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int sourceIndex) {
        return null;
    }

    /**
     * For testing: returns the source image bounds.
     *
     * @param destRect
     * @param sourceIndex
     * @return
     */
    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        return getSourceImage(sourceIndex).getBounds();
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        for (int y = destRect.y, iy = 0; iy < destRect.height; y++, iy++) {
            for (int x = destRect.x, ix = 0; ix < destRect.width; x++, ix++) {
                final double value = runtime.evaluate(x, y);
                dest.setSample(x, y, band, value);
            }
        }
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        super.computeRect(sources, dest, destRect);
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        return super.computeTile(tileX, tileY);
    }
}
