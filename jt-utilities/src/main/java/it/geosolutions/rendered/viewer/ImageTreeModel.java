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

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/**
 * Turns a RenderedImage into a tree model
 *
 * @author Andrea Aime
 *
 */
class ImageTreeModel implements TreeModel
{
    private List<TreeModelListener> vector = new ArrayList<TreeModelListener>();

    RenderedImage root;

    public void setRoot(RenderedImage root)
    {
        this.root = root;
        fireTreeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
    }

    public Object getChild(Object parent, int index)
    {
        RenderedImage ro = (RenderedImage) parent;

        return ro.getSources().get(index);
    }

    public int getChildCount(Object parent)
    {
        RenderedImage ro = (RenderedImage) parent;
        if (ro.getSources() != null)
        {
            return ro.getSources().size();
        }

        return 0;
    }

    public int getIndexOfChild(Object parent, Object child)
    {
        RenderedImage ro = (RenderedImage) parent;
        Vector sources = ro.getSources();
        for (int i = 0; i < sources.size(); i++)
        {
            if (sources.get(i).equals(child))
            {
                return i;
            }
        }

        return -1;
    }

    public Object getRoot()
    {
        return root;
    }

    public boolean isLeaf(Object node)
    {
        return getChildCount(node) == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue)
    {
        throw new UnsupportedOperationException("This tree is read only");

    }

    public void addTreeModelListener(TreeModelListener listener)
    {
        if ((listener != null) && !vector.contains(listener))
        {
            vector.add(listener);
        }
    }

    public void removeTreeModelListener(TreeModelListener listener)
    {
        if (listener != null)
        {
            vector.remove(listener);
        }
    }

    public void fireTreeStructureChanged(TreeModelEvent e)
    {
        for (TreeModelListener listener : vector)
        {
            listener.treeStructureChanged(e);
        }
    }
}
