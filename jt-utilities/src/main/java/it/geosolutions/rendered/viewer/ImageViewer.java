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

import static java.awt.image.DataBuffer.TYPE_DOUBLE;
import static java.awt.image.DataBuffer.TYPE_FLOAT;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;


/**
 * Simple rendered image browser, allows to zoom in, out, display tile grid and
 * view pixel values on mouse over
 *
 * @author Andrea Aime
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class ImageViewer extends JPanel
{
    private ZoomableImageDisplay display;
    private ImageViewer relatedViewer;
    private JLabel status;
    private RandomIter pixelIter;
    private int[] ipixel;
    private double[] dpixel;
    JToggleButton tileGrid;
    JToggleButton roiSync;
    private StringBuffer sb = new StringBuffer();
    private RenderedImage image;
    protected File lastDirectory;
    private JScrollBar vScrollBar;
    private JScrollBar hScrollBar;

    public ImageViewer(ImageViewer relatedViewer)
    {
        this();
        this.relatedViewer = relatedViewer;
    }

    public ImageViewer()
    {
        setLayout(new BorderLayout());

        // build the button bar
        JButton zoomIn = new JButton("Zoom in");
        JButton zoomOut = new JButton("Zoom out");
        tileGrid = new JToggleButton("Tile grid");
        roiSync = new JToggleButton("ROI sync");
        JButton save = new JButton("Save...");
        final JButton showChain = new JButton("Show chain in separate window");
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonBar.add(zoomIn);
        buttonBar.add(zoomOut);
        buttonBar.add(tileGrid);
        buttonBar.add(roiSync);
        buttonBar.add(save);
        buttonBar.add(showChain);

        // actual image viewer
        display = new ZoomableImageDisplay();
//              display.setBackground(Color.BLACK);
        tileGrid.setSelected(display.isTileGridVisible());

        // the "status bar"
        status = new JLabel("Move on the image to display pixel values... ");

        // compose
        JScrollPane scrollPane = new JScrollPane(display);
        vScrollBar = scrollPane.getVerticalScrollBar();
        hScrollBar = scrollPane.getHorizontalScrollBar();

        add(buttonBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // events
        zoomIn.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    display.setScale(display.getScale() * 2.0);
                    if (relatedViewer != null)
                    {
                        relatedViewer.display.setScale(relatedViewer.display.getScale() * 2.0);
                    }
                }

            });
        zoomOut.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    display.setScale(display.getScale() / 2.0);
                    if (relatedViewer != null)
                    {
                        relatedViewer.display.setScale(relatedViewer.display.getScale() / 2.0);
                    }
                }

            });
        tileGrid.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    display.setTileGridVisible(tileGrid.isSelected());
                    if (relatedViewer != null)
                    {
                        relatedViewer.display.setTileGridVisible(tileGrid.isSelected());
                        relatedViewer.tileGrid.setSelected(tileGrid.isSelected());
                    }
                }

            });
        roiSync.addChangeListener(new ChangeListener()
        {

            @Override
            public void stateChanged(ChangeEvent e)
            {
                if (relatedViewer != null)
                {
                    relatedViewer.roiSync.setSelected(roiSync.isSelected());
                }
            }

        });
        vScrollBar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e)
            {
                if (roiSync.isSelected() && relatedViewer != null) {
                    relatedViewer.vScrollBar.setValue(vScrollBar.getValue());
                }
            }
        });
        hScrollBar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e)
            {
                if (roiSync.isSelected() && relatedViewer != null) {
                    relatedViewer.hScrollBar.setValue(hScrollBar.getValue());
                }
            }
        });

        save.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent arg0)
                {
                    // File location = getStartupLocation();
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileFilter()
                        {

                            @Override
                            public String getDescription()
                            {
                                return "images";
                            }

                            @Override
                            public boolean accept(File file)
                            {
                                if (file.isDirectory()) {
                                    return true;
                                }
                                String name = file.getName().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".tif") || name.endsWith(".tiff");
                            }
                        });

                    int result = chooser.showSaveDialog(ImageViewer.this);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        File selected = chooser.getSelectedFile();
                        String name = selected.getName().toLowerCase();
                        if (!(name.endsWith(".png") || name.endsWith(".tif") || name.endsWith(".tiff"))) {
                            selected = new File(selected.getParentFile(), selected.getName() + ".png");
                        }
                        lastDirectory = selected.getParentFile();
                        try
                        {
                            String format = "PNG";
                            if (name.endsWith(".tif") || name.endsWith(".tiff")) {
                                format = "TIF";
                            }
                            ImageIO.write(image, format, selected);
                            status.setText("File successfully saved");
                        }
                        catch (IOException e)
                        {
                            status.setText("Failed to save file: " + e.getMessage());
                        }
                    }

                }
            });
        showChain.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent e)
                {
                    RenderedImageBrowser.showChain(image);
                }

            });
        display.addMouseMotionListener(new MouseMotionAdapter()
            {

                @Override
                public void mouseMoved(MouseEvent e)
                {
                    if (pixelIter != null)
                    {
                        int x = (int) Math.round(e.getX() / display.getScale());
                        int y = (int) Math.round(e.getY() / display.getScale());
                        sb.setLength(0);

                        if ((x < image.getMinX()) || (x >= (image.getMinX() + image.getWidth())) ||
                                (y < image.getMinY()) || (y >= (image.getMinY() + image.getHeight())))
                        {
                            sb.append("Outside of image bounds");
                        }
                        else
                        {
                            sb.append("Value at ");
                            sb.append(x).append(", ").append(y).append(": [");
                            int dataType = image.getSampleModel().getDataType();
                            if ((dataType == TYPE_DOUBLE) || (dataType == TYPE_FLOAT))
                            {
                                pixelIter.getPixel(x, y, dpixel);
                                for (int i = 0; i < dpixel.length; i++)
                                {
                                    sb.append(dpixel[i]);
                                    if (i < (dpixel.length - 1))
                                    {
                                        sb.append(", ");
                                    }
                                }
                            }
                            else
                            { // integer samples
                                pixelIter.getPixel(x, y, ipixel);
                                for (int i = 0; i < ipixel.length; i++)
                                {
                                    sb.append(ipixel[i]);
                                    if (i < (ipixel.length - 1))
                                    {
                                        sb.append(", ");
                                    }
                                }
                            }
                            sb.append(']');
                        }
                        status.setText(sb.toString());
                    }
                }

            });
    }

    public void setImage(RenderedImage image)
    {
        this.image = image;
        if(image == null) {
            display.setVisible(false);
            pixelIter = null;
        } else {
            display.setImage(image);
            display.setVisible(true);
            pixelIter = RandomIterFactory.create(image, null);
            ipixel = new int[image.getSampleModel().getNumBands()];
            dpixel = new double[image.getSampleModel().getNumBands()];
        }
    }

    public ImageViewer getRelatedViewer()
    {
        return relatedViewer;
    }

    public void setRelatedViewer(ImageViewer relatedViewer)
    {
        this.relatedViewer = relatedViewer;
    }

    public void setStatusMessage(String message) {
        status.setText(message);
        
    }

}

