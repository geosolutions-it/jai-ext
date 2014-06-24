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


/**
 * Simple helper for building an HTML document
 *
 * @author Andrea Aime
 *
 */
public class HTMLBuilder
{

    public static String render(Object value)
    {
        if (value == null)
        {
            return "-";
        }

        String result = null;
        for (HTMLRenderer renderer : HTMLRenderers.getRenderers())
        {
            if (renderer.canRender(value))
            {
                result = renderer.render(value);
            }
        }
        if (result == null)
        {
            result = value.toString();
        }

        return result.replace("\n", "<br>");
    }

    private StringBuffer sb = new StringBuffer();

    public HTMLBuilder()
    {
        sb.append("<html><body>");
    }

    public void title(String title)
    {
        sb.append("<h2>").append(title).append("</h2><hr>");
    }

    public void dataLine(String label, Object value)
    {
        sb.append("<b>").append(label).append(":</b> ").append(render(value)).append("<br>");
    }

    public String getHtml()
    {
        sb.append("</html></body>");

        String result = sb.toString();
        sb = null;

        return result;
    }

}
