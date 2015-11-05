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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

//import com.digitalglobe.util.security.SecurityValidator;


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
    //US9834:SUPPRESSION:Dead Code: Unused Field (Code Quality, Structural):Low:STIGCAT2
    private int dataType;
    private StringBuffer sb = new StringBuffer();
    private RenderedImage image;
    protected File lastDirectory;

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
        final JToggleButton tileGrid = new JToggleButton("Tile grid");
        JButton save = new JButton("Save...");
        final JButton showChain = new JButton("Show chain in separate window");
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonBar.add(zoomIn);
        buttonBar.add(zoomOut);
        buttonBar.add(tileGrid);
        buttonBar.add(save);
        buttonBar.add(showChain);

        // actual image viewer
        display = new ZoomableImageDisplay();
//              display.setBackground(Color.BLACK);
        tileGrid.setSelected(display.isTileGridVisible());

        // the "status bar"
        status = new JLabel("Move on the image to display pixel values... ");

        // compose
        add(buttonBar, BorderLayout.NORTH);
        add(new JScrollPane(display), BorderLayout.CENTER);
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
                                return "*.png";
                            }

                            @Override
                            public boolean accept(File file)
                            {
                                return file.isDirectory() || file.getName().toLowerCase().endsWith(".png");
                            }
                        });

                    int result = chooser.showSaveDialog(ImageViewer.this);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        File selected = chooser.getSelectedFile();
                        if (!selected.getName().toLowerCase().endsWith(".png"))
                        {
                            selected = new File(selected.getParentFile(), selected.getName() + ".png");
                        }
                        lastDirectory = selected.getParentFile();
                        try
                        {
                            ImageIO.write(image, "PNG", selected);
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
                            if ((dataType == DataBuffer.TYPE_DOUBLE) || (dataType == DataBuffer.TYPE_FLOAT))
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

//    protected File getStartupLocation()
//    {
//        if (lastDirectory != null)
//        {
//            // REMEDIATION:Path Manipulation:Critical:STIGCAT1
//            SecurityValidator.getDirectoryPathValidator().validate(
//                "PropertyUtil.getFileFromClasspath", lastDirectory.getAbsolutePath(), false);
//
//            return lastDirectory;
//        }
//        else
//        {
//            // REMEDIATION:Path Manipulation:Critical:STIGCAT1
//            return new File(SecurityValidator.getFileNameValidator().validate(
//                        "ImageViewer.getStartupLocation", "/tmp", false));
//        }
//    }

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
