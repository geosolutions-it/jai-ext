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

import java.awt.BorderLayout;
import java.awt.Transparency;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.TileScheduler;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

//import com.digitalglobe.util.Log4jUtil;

//import org.apache.log4j.Logger;


/**
 * Full rendered image browser, made up of tree for rendered image source
 * hierarchy navigation and a info panel with various information on the
 * selected {@link RenderedImage}
 *
 * @author Andrea Aime
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class RenderedImageBrowser extends JPanel
{
    private static final Map<Integer, String> TYPE_MAP = new HashMap<Integer, String>();

    static
    {
        TYPE_MAP.put(DataBuffer.TYPE_BYTE, "Byte");
        TYPE_MAP.put(DataBuffer.TYPE_DOUBLE, "Double");
        TYPE_MAP.put(DataBuffer.TYPE_FLOAT, "Float");
        TYPE_MAP.put(DataBuffer.TYPE_INT, "Int");
        TYPE_MAP.put(DataBuffer.TYPE_SHORT, "Short");
        TYPE_MAP.put(DataBuffer.TYPE_UNDEFINED, "Undefined");
        TYPE_MAP.put(DataBuffer.TYPE_USHORT, "Short");
    }

    public static void showChain(RenderedImage image, String title)
    {
        showChain(image, true, true, title);
    }

    public static void showChain(RenderedImage image)
    {
        showChain(image, "");
    }

    public static void showChain(RenderedImage image, final boolean showHistogram)
    {
        showChain(image, showHistogram, false);
    }

    public static void showChain(RenderedImage image, final boolean showHistogram, final boolean showRoi)
    {
        showChain(image, showHistogram, showRoi, "");
    }

    /**
     * Dumps a text description of an image chain, useful to for headless debugging
     * or logging purposes
     * @param image
     * @return
     */
    public static String dumpChain(RenderedImage image)
    {
        TextTreeBuilder builder = new TextTreeBuilder();
        dumpChain(image, builder);

        return builder.toString();
    }

    private static void dumpChain(RenderedImage image, TextTreeBuilder builder)
    {
        String name;
        TileCache tcache = null;
        TileScheduler tscheduler = null;
        if (image instanceof RenderedOp)
        {
            RenderedOp op = (RenderedOp) image;
            String operationName = op.getOperationName();
            String renderingName = "null";
            try
            {
                op.getWidth();
                renderingName = op.getCurrentRendering().getClass().getSimpleName();
            }
            catch (Exception ignored)
            {
                // can happen if the op has trouble setting up its rendering
//                Logger logger = Logger.getLogger(RenderedImageBrowser.class);
//                if (logger.isTraceEnabled())
//                {
//                    Log4jUtil.trace(logger, ignored.getMessage(), ignored);
//                }
                
                Logger logger = Logger.getLogger(RenderedImageBrowser.class.toString());
                
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING, ignored.getMessage());                   
                }
                
                
            }
            name = "JAI op: " + operationName + '(' + renderingName + ')';
            tcache = (TileCache) op.getRenderingHint(JAI.KEY_TILE_CACHE);
            tscheduler = (TileScheduler) op.getRenderingHint(JAI.KEY_TILE_SCHEDULER);
        }
        else
        {
            name = "Non op: " + image.getClass();
        }
        builder.append(name);
        builder.append(", offset:");
        builder.append(image.getMinX() + ", " + image.getMinY());
        builder.append(", size:");
        builder.append(image.getWidth() + " x " + image.getHeight());
        builder.append(", tile size:" + image.getTileWidth() + " x " + image.getTileHeight());
        builder.newLine();
        // dump the rendered op params if we have some
        if (image instanceof RenderedOp)
        {
            builder.append("Params. ");

            RenderedOp op = (RenderedOp) image;
            final ParameterBlock block = op.getParameterBlock();
            Vector<Object> paramValues = block.getParameters();
            for (int i = 0; i < paramValues.size(); i++)
            {
                String pname = "Parameter " + (i + 1);
                if (block instanceof ParameterBlockJAI)
                {
                    pname = ((ParameterBlockJAI) block).getParameterListDescriptor().getParamNames()[i];
                }
                builder.append(pname);
                builder.append(":");

                Object value = paramValues.get(i);
                dumpValue(builder, value);
                builder.append("; ");
            }
            builder.newLine();
        }

        SampleModel sm = image.getSampleModel();
        builder.append("Bands: " + sm.getNumBands() + ", type: " + TYPE_MAP.get(sm.getDataType()));
        builder.append("; Color model:" + image.getColorModel().getClass());
        builder.append(", transparency: ");
        switch (image.getColorModel().getTransparency())
        {
        case Transparency.OPAQUE:
            builder.append("Opaque");
            break;
        case Transparency.TRANSLUCENT:
            builder.append("Translucent");
            break;
        case Transparency.BITMASK:
            builder.append("Bitmas  k");
            break;
        }

        builder.newLine();
        builder.append("Tile cache: " + tcache);
        builder.newLine();
        builder.append("Tile scheduler: ");
        if (tscheduler == null)
        {
            builder.append("null");
        }
        else
        {
            builder.append(tscheduler +
                ((tscheduler == JAI.getDefaultInstance().getTileScheduler()) ? "<global>" : "<local>") + ", parallelism " + tscheduler.getParallelism() +
                ", priority " + tscheduler.getPriority());
        }

        if (image.getSources() != null)
        {
            builder.newLine();
            builder.append("Number of children: " + image.getSources().size());
            for (RenderedImage child : image.getSources())
            {
                builder.newChild();
                dumpChain(child, builder);
                builder.endChild();
            }
        }
    }

    private static void dumpValue(TextTreeBuilder builder, Object value)
    {
        if ((value != null) && value.getClass().isArray())
        {
            int length = Array.getLength(value);
            builder.append("[");
            for (int j = 0; j < length; j++)
            {
                dumpValue(builder, Array.get(value, j));
                if (j < (length - 1))
                {
                    builder.append(", ");
                }
                else
                {
                    builder.append("]");
                }
            }
        }
        else if (value instanceof EnumeratedParameter)
        {
            String enumName = ((EnumeratedParameter) value).getName();
            builder.append(enumName);
        }
        else if (value instanceof Interpolation)
        {
            String interpName = value.getClass().getSimpleName();
            builder.append(interpName);
        }
        else
        {
            builder.append(String.valueOf(value));
        }
    }

    public static void showChain(RenderedImage image, final boolean showHistogram, final boolean showRoi,
        final String title)
    {
        String localTitle = (title != null) ? (": " + title) : "";
        JFrame frame = new JFrame("Rendered image information tool" + localTitle);
        RenderedImageBrowser info = new RenderedImageBrowser(showHistogram, showRoi);
        info.setImage(image);
        frame.setContentPane(info);
        frame.setSize(1024, 768);
        frame.setVisible(true);
    }

    ImageTreeModel model;

    JTree imageTree;

    RenderedImageInfoPanel imageInfo;

    JSplitPane split;

    boolean showHistogram;

    boolean showRoi;

    public RenderedImageBrowser()
    {
        this(true, false);
    }

    public RenderedImageBrowser(final boolean showHistogram, final boolean showRoi)
    {
        this.showHistogram = showHistogram;
        this.showRoi = showRoi;
        model = new ImageTreeModel();
        imageTree = new JTree(model);
        imageTree.setCellRenderer(new ImageTreeRenderer());
        imageTree.setShowsRootHandles(true);
        imageTree.putClientProperty("JTree.lineStyle", "Angled");
        imageInfo = new RenderedImageInfoPanel(showHistogram, showRoi);
        split = new JSplitPane();
        split.setLeftComponent(new JScrollPane(imageTree));
        split.setRightComponent(imageInfo);
        split.setResizeWeight(0.2);
        setLayout(new BorderLayout());
        add(split);

        imageTree.addTreeSelectionListener(new TreeSelectionListener()
            {

                public void valueChanged(TreeSelectionEvent e)
                {
                    final TreePath selectedpath = imageTree.getSelectionPath();
                    if (selectedpath == null)
                    {
                        imageTree.setSelectionRow(0);
                    }

                    RenderedImage image = (RenderedImage) imageTree.getSelectionPath().getLastPathComponent();
                    imageInfo.setImage(image);
                }

            });
    }

    public void setImage(RenderedImage image)
    {
        model.setRoot(image);
        imageTree.setSelectionPath(new TreePath(image));

        int rc;
        do
        {
            rc = imageTree.getRowCount();
            for (int x = rc; x >= 0; x--)
            {
                imageTree.expandRow(x);
            }
        }
        while (rc != imageTree.getRowCount());
    }
}
