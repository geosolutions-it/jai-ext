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

import java.awt.Component;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.RenderedOp;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

//import com.digitalglobe.util.Log4jUtil;

//import org.apache.log4j.Logger;


/**
 * Extends DefaultTreeCellRenderer to provide a good looking label for rendered
 * images and RenderedOp instances in particular
 *
 * @author Andrea Aime
 *
 */
class ImageTreeRenderer extends DefaultTreeCellRenderer
{

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, selected, expanded,
            leaf, row, hasFocus);

        RenderedImage ri = (RenderedImage) value;
        if (ri instanceof RenderedOp)
        {
            RenderedOp op = (RenderedOp) ri;
            // force op to have a "rendering"
            String operationName = op.getOperationName();
            String renderingName = "null";
            try
            {
                op.getWidth();
                renderingName = op.getCurrentRendering().getClass().getSimpleName();
            }
            catch (Exception ignored)
            {
//                Logger logger = Logger.getLogger(getClass());
//                if (logger.isTraceEnabled())
//                {
//                    Log4jUtil.trace(logger, ignored.getMessage(), ignored);
//                }
                
                
                Logger logger = Logger.getLogger(getClass().toString());
                
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING, ignored.getMessage());                   
                }
                
            }
            setText("<html><body><b>RenderedOp:</b> <i>" +
                operationName + '(' + renderingName + ")</i></body></html>");
        }
        else
        {
            setText(ri.getClass().getSimpleName());
        }

        return this;
    }

}
