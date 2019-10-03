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
package it.geosolutions.rendered.viewer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageReadParam;
import javax.media.jai.WarpAffine;


/**
 * HTML renderers registry
 *
 * @author Andrea Aime
 *
 */
public final class HTMLRenderers
{
    private static List<HTMLRenderer> renderers = new ArrayList<HTMLRenderer>();

    static
    {
        renderers.add(new ArrayRenderer());
        renderers.add(new ImageReadParamRenderer());
        renderers.add(new WarpAffineRenderer());
        //renderers.add(new ROIGeometryRenderer());
    }

    public static List<HTMLRenderer> getRenderers()
    {
        return renderers;
    }

    public static void addRendered(HTMLRenderer renderer)
    {
        renderers.add(renderer);
    }

    private HTMLRenderers()
    {
    }

    private static class ArrayRenderer implements HTMLRenderer
    {

        public boolean canRender(Object o)
        {
            return (o != null) && o.getClass().isArray();
        }

        public String render(Object o)
        {
            int length = Array.getLength(o);
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < length; i++)
            {
                sb.append(HTMLBuilder.render(Array.get(o, i)));
                if (i < (length - 1))
                {
                    sb.append(", ");
                }
                else
                {
                    sb.append(']');
                }
            }

            return sb.toString();
        }

    }

    /** 
     * Renderer for ImageReadParam parameters, such as source subsampling,
     * source region rectangle, source bands, ...
     */
    private static class ImageReadParamRenderer implements HTMLRenderer {

        public boolean canRender(Object o) {
            return (o != null) && ImageReadParam.class.isAssignableFrom(o.getClass());
        }

        public String render(Object o) {
            ImageReadParam param = (ImageReadParam) o;
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(o.getClass().toString()).append("\n");
            int ssx = param.getSourceXSubsampling();
            int ssy = param.getSourceYSubsampling();
            Rectangle rect = param.getSourceRegion();
            Point p = param.getDestinationOffset();
            int[] bands = param.getSourceBands();
            if (rect != null) {
                sb.append(String.format(
                        "    SourceRegion(Rectangle)[x:%d, y:%d, width:%d, height:%d]\n", rect.x,
                        rect.y, rect.width, rect.height));
            }
            sb.append(String.format("    SourceSubsampling[ssx:%d, ssy:%d]\n", ssx, ssy));
            if (bands != null) {
                sb.append("    SourceBands[").append(HTMLBuilder.render(bands)).append("]\n");
            }
            if (p != null) {
                sb.append(String.format("    DestinationOffset(Point)[x:%d, y:%d]", p.x, p.y));
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Renderer for WarpAffine parameters
     */
    private static class WarpAffineRenderer implements HTMLRenderer {

        public boolean canRender(Object o) {
            return (o != null) && WarpAffine.class.isAssignableFrom(o.getClass());
        }

        public String render(Object o) {
            WarpAffine warpAffine = (WarpAffine) o;
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(o.getClass().toString()).append("\n");
            AffineTransform transform = warpAffine.getTransform();
            if (transform != null) {
                double [] matrix = new double[6];
                transform.getMatrix(matrix);
                sb.append(String.format("\t m00 (scaleX):%f" +
                                "\n\t m11 (scaleY):%f" +
                                "\n\t m01 (shearX):%f" +
                                "\n\t m10 (shearY):%f" +
                                "\n\t m02 (translateX):%f" +
                                "\n\t m12 (translateY):%f]",
                        matrix[0], matrix[3], matrix[2], matrix[1], matrix[4], matrix[5]));
            }
            sb.append("]");
            return sb.toString();
        }
    }
//
//    private static class ROIGeometryRenderer implements HTMLRenderer
//    {
//
//        public boolean canRender(Object o)
//        {
//            return false;
//        }
//
//        public String render(Object o)
//        {
//            return ((ROIGeometry) o).getAsGeometry().toString();
//        }
//
//    }

}
