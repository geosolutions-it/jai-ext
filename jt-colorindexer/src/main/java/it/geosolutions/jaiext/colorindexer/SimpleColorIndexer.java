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
package it.geosolutions.jaiext.colorindexer;

import java.awt.image.IndexColorModel;

/**
 * Maps every given color to the closest color in the palette, by exaustive search
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SimpleColorIndexer implements ColorIndexer {

    /** Input colors */
    byte[][] colors;

    public SimpleColorIndexer(byte[][] colors) {
        this.colors = colors;
    }

    public SimpleColorIndexer(IndexColorModel icm) {
        this.colors = new byte[4][icm.getMapSize()];
        icm.getReds(colors[0]);
        icm.getGreens(colors[1]);
        icm.getBlues(colors[2]);
        icm.getAlphas(colors[3]);
    }

    public IndexColorModel toIndexColorModel() {
        int bits = (int) Math.ceil(Math.log(colors[0].length) / Math.log(2));
        if (bits == 0) {
            bits = 1;
        }
        return new IndexColorModel(bits, colors[0].length, colors[0], colors[1], colors[2],
                colors[3]);
    }

    public int getClosestIndex(int r, int g, int b, int a) {
        // find the closest color
        int idx = 0;
        int distance = Integer.MAX_VALUE;
        for (int i = 0; i < colors[0].length; i++) {
            int dr = r - (colors[0][i] & 0xFF);
            int dg = g - (colors[1][i] & 0xFF);
            int db = b - (colors[2][i] & 0xFF);
            int da = a - (colors[3][i] & 0xFF);
            /*
             * This is a simple color distance formula, we might consider checking into the LAB color space, though the conversions would take some
             * more time: http://en.wikipedia.org/wiki/L*a*b* http://www.easyrgb.com/index.php?X=MATH
             */
            int d = 3 * dr * dr + 4 * dg * dg + 2 * db * db + 4 * da * da;
            if (d < distance) {
                distance = d;
                idx = i;
                if (distance == 0) {
                    break;
                }
            }
        }

        return idx;
    }

}
