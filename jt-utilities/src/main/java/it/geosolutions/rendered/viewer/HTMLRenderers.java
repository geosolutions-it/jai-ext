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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


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
            StringBuffer sb = new StringBuffer();
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
