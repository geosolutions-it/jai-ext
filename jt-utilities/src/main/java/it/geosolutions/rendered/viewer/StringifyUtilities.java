/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2020 GeoSolutions


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
package it.geosolutions.rendered.viewer;

import javax.imageio.ImageReadParam;
import javax.media.jai.WarpAffine;
import java.awt.*;
import java.awt.geom.AffineTransform;

class StringifyUtilities {

    public static String printWarpAffine(WarpAffine warpAffine, boolean minimal) {
        StringBuilder sb = new StringBuilder();
        String header = minimal ? "" : ("[" + warpAffine.getClass().toString() + " \n");
        sb.append(header);
        String format = null;
        if (minimal) {
            format = "m00:%f; m11:%f; m01:%f; m10:%f; m02:%f; m12:%f";
        } else {
            format = "\t m00 (scaleX):%f" +
                    "\n\t m11 (scaleY):%f" +
                    "\n\t m01 (shearX):%f" +
                    "\n\t m10 (shearY):%f" +
                    "\n\t m02 (translateX):%f" +
                    "\n\t m12 (translateY):%f";
        }

        AffineTransform transform = warpAffine.getTransform();
        if (transform != null) {
            double [] matrix = new double[6];
            transform.getMatrix(matrix);
            sb.append(String.format(format,
                    matrix[0], matrix[3], matrix[2], matrix[1], matrix[4], matrix[5]));
        }
        if (!minimal) {
            sb.append("]");
        }
        return sb.toString();
    }

    public static String printImageReadParam(ImageReadParam param, boolean minimal) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(param.getClass().toString())  ;
        int ssx = param.getSourceXSubsampling();
        int ssy = param.getSourceYSubsampling();
        Rectangle rect = param.getSourceRegion();
        Point p = param.getDestinationOffset();
        int[] bands = param.getSourceBands();
        String nl = minimal ? "" : "\n";
        if (rect != null) {
            sb.append(String.format(
                    "    SourceRegion(Rectangle)[x:%d, y:%d, width:%d, height:%d] %s", rect.x,
                    rect.y, rect.width, rect.height, nl));
        }
        sb.append(String.format("    SourceSubsampling[ssx:%d, ssy:%d] %s", ssx, ssy, nl));
        if (bands != null) {
            sb.append("    SourceBands[").append(HTMLBuilder.render(bands)).append("]" + nl);
        }
        if (p != null) {
            sb.append(String.format("    DestinationOffset(Point)[x:%d, y:%d]", p.x, p.y));
        }
        sb.append("]");
        return sb.toString();
    }
}
