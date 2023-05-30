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

import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.utilities.ImageLayout2;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.media.jai.*;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.operator.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import static java.awt.image.DataBuffer.*;


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
    private static final int THRESHOLD_2K_X_2K = 2048 * 2048;
    ZoomableImageDisplay display;
    ImageViewer relatedViewer;
    private JLabel status;
    private RandomIter pixelIter;
    private int[] ipixel;
    private double[] dpixel;
    JCheckBox tileGrid;
    JCheckBox roiSync;
    JCheckBox rescaleValues;
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
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        // build the button bar
        String [] zooms = new String[]{"in", "out"};
        for (String zoom: zooms) {
            String alternativeText = "Zoom " + zoom;
            String zoomIconName = "zoom-" + zoom + ".png";
            JButton zoomButton = createDecoratedButton(alternativeText, alternativeText, zoomIconName);
            double scale = zoom.equalsIgnoreCase("in") ? 2.0 : 0.5;
            zoomButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    display.setScale(display.getScale() * scale);
                    if (relatedViewer != null)
                    {
                        relatedViewer.display.setScale(relatedViewer.display.getScale() * scale);
                    }
                }

            });
            buttonBar.add(zoomButton);
        }
        tileGrid = new JCheckBox("Tile grid");
        tileGrid.setToolTipText("Toggle Tile Grid");
        tileGrid.setRolloverEnabled(false);
        roiSync = new JCheckBox("ROI sync");
        roiSync.setToolTipText("Keep the ROIViewer viewport aligned with the main Viewer");
        rescaleValues = new JCheckBox("Rescale");
        rescaleValues.setToolTipText("When needed, rescale image to byte values for viewing. " +
                "(Pixel value at mouse position will still show raw value)");
        rescaleValues.setRolloverEnabled(false);
        JButton save = createDecoratedButton(
                "Save",
                "Save whole image to png/tif",
                "save.png"
        );
        JButton showChain = createDecoratedButton(
                "Show chain in separate window",
                "Open a new viewer with the current image as root of the processing chain",
                "other-window.png"
        );
        JButton showDump = createDecoratedButton(
                "Chain As Text",
                "Show the processing chain dumped as textual info",
                "chain-text.png"
        );
        buttonBar.add(tileGrid);
        buttonBar.add(roiSync);
        buttonBar.add(rescaleValues);
        buttonBar.add(save);
        buttonBar.add(showChain);
        buttonBar.add(showDump);

        // actual image viewer
        display = new ZoomableImageDisplay();
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
        roiSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (relatedViewer != null) {
                    relatedViewer.roiSync.setSelected(roiSync.isSelected());
                }
            }
        });
        rescaleValues.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setImage(image);
            }
        });
        vScrollBar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (roiSync.isSelected() && relatedViewer != null) {
                    relatedViewer.vScrollBar.setValue(vScrollBar.getValue());
                }
                if (rescaleValues.isSelected()) {
                    display.repaint();
                }
            }
        });
        hScrollBar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (roiSync.isSelected() && relatedViewer != null) {
                    relatedViewer.hScrollBar.setValue(hScrollBar.getValue());
                }
                if (rescaleValues.isSelected()) {
                    display.repaint();
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
                                return ".png and .tif/.tiff images";
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
                    String title = "";
                    if (image instanceof RenderedOp) {
                        title = ((RenderedOp) image).getOperationName();
                    }
                    JFrame frame = new JFrame(title);
                    RenderedImageBrowser info = new RenderedImageBrowser(true, true);
                    info.setImage(image);
                    ImageViewer viewer = info.imageInfo.viewer;
                    frame.setContentPane(info);
                    frame.setSize(1024, 768);
                    frame.setVisible(true);
                    // Re-init viewer with current properties
                    double latestScale = display.getScale();
                    viewer.roiSync.setSelected(roiSync.isSelected());
                    viewer.tileGrid.setSelected(tileGrid.isSelected());
                    viewer.rescaleValues.setSelected(rescaleValues.isSelected());
                    viewer.display.setScale(latestScale);
                    if (relatedViewer != null) {
                        ImageViewer openingRelatedViewer = viewer.relatedViewer;
                        openingRelatedViewer.display.setScale(latestScale);
                        openingRelatedViewer.roiSync.setSelected(roiSync.isSelected());
                        openingRelatedViewer.tileGrid.setSelected(tileGrid.isSelected());
                    }
                    // Trigger the painting
                    viewer.setImage(image);
                }

            });
        showDump.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String title = "";
                if (image instanceof RenderedOp) {
                    title = ((RenderedOp) image).getOperationName();
                }

                JFrame frame = new JFrame(title + " dump");
                frame.setLayout(new BorderLayout());
                JCheckBox checkBox = new JCheckBox("Minimal");
                String chainDump = RenderedImageBrowser.dumpChain(image);
                JTextArea textArea = new JTextArea();
                textArea.setText(chainDump);
                checkBox.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        boolean minimal = checkBox.isSelected();
                        String chainDump = RenderedImageBrowser.dumpChain(image, minimal);
                        textArea.setText(chainDump);
                    }
                });
                checkBox.setSelected(false);
                textArea.setEditable(false);
                JScrollPane textScrollPane = new JScrollPane(textArea);
                frame.add(checkBox, BorderLayout.PAGE_START);
                frame.add(textScrollPane, BorderLayout.CENTER);
                frame.setSize(1024, 768);
                frame.setVisible(true);
            }

        });
        display.addMouseMotionListener(new MouseMotionAdapter()
            {

                @Override
                public void mouseMoved(MouseEvent e)
                {
                    if (pixelIter != null)
                    {
                        int x = (int) Math.floor(e.getX() / display.getScale());
                        int y = (int) Math.floor(e.getY() / display.getScale());
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

    private JButton createDecoratedButton(String alternativeText, String toolTipText, String iconResourceName) {
        JButton button = new JButton();
        Image icon = null;
        try {
            InputStream inputStream = getClass()
                    .getClassLoader().getResourceAsStream(iconResourceName);
            icon = ImageIO.read(inputStream);
            button.setIcon(new ImageIcon(icon));
            inputStream.close();

        } catch (IOException e) {
            // Set text
            button.setText(alternativeText);
        }
        button.setToolTipText(toolTipText);
        return button;
    }

    public void setImage(RenderedImage image)
    {
        this.image = image;
        display.setUseRescaled(rescaleValues.isSelected());
        if(image == null) {
            display.setVisible(false);
            pixelIter = null;
        } else {
            if (rescaleValues.isSelected()) {
                rescaleImage(image);
            } else {
                display.setImage(image);
            }
            display.setVisible(true);
            pixelIter = RandomIterFactory.create(image, null);
            ipixel = new int[image.getSampleModel().getNumBands()];
            dpixel = new double[image.getSampleModel().getNumBands()];
        }
    }

    private void rescaleImage(RenderedImage image) {
        int dataType = image.getSampleModel().getDataType();
        switch (dataType) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT:
            case TYPE_SHORT:
            case TYPE_BYTE:
                // Store the unscaled image
                this.display.image = image;
                ImageLayout layout = new ImageLayout2();
                layout.setTileWidth(image.getTileWidth());
                layout.setTileHeight(image.getTileHeight());
                layout.setTileGridYOffset(image.getTileGridYOffset());
                layout.setTileGridXOffset(image.getTileGridXOffset());
                RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

                // look for nodata if any, to setup a ROI for better statistical computations
                Object noData = image.getProperty("GC_NODATA");
                ROI roi = null;
                if (noData != null && noData instanceof NoDataContainer) {
                    double nd = ((NoDataContainer) noData).getAsSingleValue();
                    RenderedImage roiSourceImage = image;
                    if (image.getSampleModel().getNumBands() > 1) {
                        //ROIImage is single band image
                        roiSourceImage = BandSelectDescriptor.create(image, new int[]{0}, hints);
                    }
                    // Create a ROI Image with the threshold value right above the noData value.
                    roi = new ROI(roiSourceImage, (int) (nd + 1));
                } else {
                    // Note that the JAI's ROI does a getData internally, which loads the whole databuffer in memory
                    // Don't do that if the image is bigger than a threshold
                    if (image.getWidth() * image.getHeight() < THRESHOLD_2K_X_2K) {
                        // Use an alternative ROI to exclude any NaN.
                        RenderedImage binarize = BinarizeDescriptor.create(image, Double.NEGATIVE_INFINITY, hints);
                        roi = new ROI(binarize, 1);
                    }
                }

                // Compute min and max to identify the histogram's minValue, maxValue
                RenderedImage extremaImage = ExtremaDescriptor.create(image, roi, 1, 1, false,
                        1, hints);
                double[][] extrema = (double[][]) extremaImage.getProperty("Extrema");

                // Compute histogram on previous min/max range
                RenderedImage histogramImage = HistogramDescriptor.create(
                        image, roi, 1, 1, new int[]{256}, extrema[0], extrema[1], hints);
                Histogram hist = (Histogram) histogramImage.getProperty("Histogram");

                // get 5th and 95th ptiles for contrast stretch
                double[] mins = hist.getPTileThreshold(0.05);
                double[] maxs = hist.getPTileThreshold(0.95);
                int bands = mins.length;
                double[] scales = new double[bands];
                double[] offsets = new double[bands];
                double[] deltas = new double[bands];
                RenderedImage[] rescaled = new RenderedImage[bands];
                for (int i=0; i<bands; i++) {
                    deltas[i] = maxs[i] - mins[i];
                    scales[i] = 255 / deltas[i];
                    offsets[i] = (-scales[i] * mins[i]);
                }


                // rescale values
                image =  RescaleDescriptor.create(image, scales, offsets, hints);
                // force to byte to truncate any values out of the byte range
                image = FormatDescriptor.create(image, TYPE_BYTE, hints);
                // Set rescaled image and trigger repaint
                this.display.setRescaledImage(image);
                break;
            default:
                // Set image and trigger repaint
                this.display.setRescaledImage(null);
                this.display.setImage(image);
        }

    }

    public ImageViewer getRelatedViewer() {
        return relatedViewer;
    }

    public void setRelatedViewer(ImageViewer relatedViewer) {
        this.relatedViewer = relatedViewer;
    }

    public void setStatusMessage(String message) {
        status.setText(message);
    }

}

