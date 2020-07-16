/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2020 GeoSolutions
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
package it.geosolutions.jaiext.jiffle.runtime;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.awt.image.WritableRenderedImage;

import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import it.geosolutions.jaiext.utilities.ImageUtilities;

public class MultiBandTest extends RuntimeTestBase {

    @Test
    public void testDestThreeBands() throws Exception {
        String script = "dest[0] = x();\n" + "dest[1] = y();\n" + "dest[2] = x() + y();";

        WritableRenderedImage destImg =
                ImageUtilities.createConstantImage(IMG_WIDTH, IMG_WIDTH, new Number[] {0, 0, 0});
        JiffleBuilder jb = new JiffleBuilder();
        jb.script(script).dest("dest", destImg);
        JiffleDirectRuntime runtime = jb.getRuntime();
        runtime.evaluateAll(null);

        double[] pixel = new double[3];
        double[] expected = new double[3];
        for(int y = 0; y < IMG_WIDTH; y++) {
            for(int x = 0; x < IMG_WIDTH; x++) {
                expected[0] = x;
                expected[1] = y;
                expected[2] = x + y;
                assertArrayEquals(expected, destImg.getData().getPixel(x, y, pixel), 0d);        
            }
        }
        
    }
}
