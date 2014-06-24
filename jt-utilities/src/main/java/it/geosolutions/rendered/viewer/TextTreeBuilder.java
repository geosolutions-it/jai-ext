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

class TextTreeBuilder
{

    StringBuilder sb = new StringBuilder();

    int indent;

    public void newChild()
    {
        indent++;
        newLine();
    }

    public void endChild()
    {
        indent--;
    }

    public void newLine()
    {
        sb.append('\n');
        for (int i = 0; i < indent; i++)
        {
            sb.append("   ");
        }
    }

    public void append(String text)
    {
        sb.append(text);
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
