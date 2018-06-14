/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


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

package it.geosolutions.jaiext.shadedrelief.cli;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefDescriptor;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class ShadedReliefRunner
{

    private static final Logger LOGGER = LogManager.getLogger(ShadedReliefRunner.class);

    private final ShadedReliefParams params;

    public ShadedReliefRunner(ShadedReliefParams params) {
        this.params = params;
    }

    public void run() throws IOException {
        LOGGER.info("Running " + getClass().getSimpleName() + " with params " + params);

        File out = params.getOutputFile();
        File in = params.getInputFile();

        BufferedImage bi = ImageIO.read(in);

        Double snd = params.getSrcNoData();
        Range srcNoDataRange = snd == null ? null : RangeFactory.create(snd, true, snd, true, true);

        RenderingHints hints = null;

        JAIExt.initJAIEXT();

        RenderedOp finalImage = ShadedReliefDescriptor.create(bi, null,
                srcNoDataRange, params.getDstNoData(),
                params.getResX(), params.getResY(),
                params.getZetaFactor(), params.getScale(),
                params.getAltitude(), params.getAzimuth(),
                params.getAlgo(), hints);

        ImageIO.write(finalImage, "tif", out);
    }
}
