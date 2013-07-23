/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2007, GeoTools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.renderedimage.viewer;

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
