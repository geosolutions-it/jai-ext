/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


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


/*
 * Copyright (c) 2018, Michael Bedward. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */   

package it.geosolutions.jaiext.swing;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * A simple Swing widget to display JTS objects.
 * 
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class JTSFrame extends JFrame {

    private static final int MARGIN = 2;
    private Canvas canvas;
    
    private static enum GeomType {
        POINT,
        LINESTRING,
        POLYGON,
        MULTIPOINT,
        MULTILINESTRING,
        MULTIPOLYGON;
        
        static GeomType get(Geometry geom) {
            if (geom == null) {
                throw new IllegalArgumentException("Geometry arg must not be null");
            }
            if (geom instanceof Point) {
                return POINT;
            }
            if (geom instanceof MultiPoint) {
                return MULTIPOINT;
            }
            if (geom instanceof LineString) {
                return LINESTRING;
            }
            if (geom instanceof MultiLineString) {
                return MULTILINESTRING;
            }
            if (geom instanceof Polygon) {
                return POLYGON;
            }
            if (geom instanceof MultiPolygon) {
                return MULTIPOLYGON;
            }
            throw new IllegalArgumentException("Unsupported geometry type: " + geom);
        }
    }

    /**
     * Creates a new frame.
     * 
     * @param title the frame title
     */
    public JTSFrame(String title) {
        super(title);
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Adds a {@code Geometry} to display
     * 
     * @param geom the geometry
     * @param col the display color
     */
    public void addGeometry(Geometry geom, Color col) {
        canvas.elements.add(new Element(geom, col));
    }
    
    /**
     * Adds a {@code Coordinate} to display as a point.
     * 
     * @param c the {@code Coordinate}
     * @param col the display color
     */
    public void addCoordinate(Coordinate c, Color col) {
        canvas.elements.add(new Element(c, col));
    }

    private void initComponents() {
        canvas = new Canvas();
        getContentPane().add(canvas);
    }

    private static class Element {

        Object geom;
        Color color;

        public Element(Object g, Color c) {
            geom = g;
            color = c;
        }
    }

    private static class Canvas extends JPanel {

        AffineTransform tr;
        List<Element> elements = new ArrayList<Element>();
        
        private static final int POINT_RADIUS = 4;

        @Override
        protected void paintComponent(Graphics g) {
            if (!elements.isEmpty()) {
                setTransform();
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2.0f));
                
                for (Element e : elements) {
                    g2.setColor(e.color);
                    
                    if (e.geom instanceof Coordinate) {
                        drawCoordinate(g2, (Coordinate) e.geom);
                    } else if (e.geom instanceof Geometry) {
                        drawGeometry(g2, (Geometry) e.geom);
                    }
                }
            }
        }
        
        private void drawGeometry(Graphics2D g2, Geometry geom) {
            Coordinate[] coords;
            
            switch (GeomType.get(geom)) {
                case POINT:
                case LINESTRING:
                    drawVertices(g2, geom.getCoordinates());
                    break;

                case POLYGON:
                    drawPolygon(g2, (Polygon) geom);
                    break;
                    
                case MULTIPOINT:
                case MULTILINESTRING:
                    for (int i = 0; i < geom.getNumGeometries(); i++) {
                        drawVertices(g2, geom.getGeometryN(i).getCoordinates());
                    }
                    break;
                    
                case MULTIPOLYGON:
                    for (int i = 0; i < geom.getNumGeometries(); i++) {
                        Polygon px = (Polygon) geom.getGeometryN(i);
                        drawPolygon(g2, px);
                    }
                    break;
            }
            
        }
        
        private void drawPolygon(Graphics2D g2, Polygon poly) {
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            drawVertices(g2, coords);
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                coords = poly.getInteriorRingN(i).getCoordinates();
                drawVertices(g2, coords);
            }
        }
        
        private void drawVertices(Graphics2D g2, Coordinate[] coords) {
            for (int i = 1; i < coords.length; i++) {
                Point2D p0 = new Point2D.Double(coords[i - 1].x, coords[i - 1].y);
                tr.transform(p0, p0);
                Point2D p1 = new Point2D.Double(coords[i].x, coords[i].y);
                tr.transform(p1, p1);
                g2.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY());
            }
        }
        
        private void drawCoordinate(Graphics2D g2, Coordinate coord) {
            Point2D p = new Point2D.Double(coord.x, coord.y);
            tr.transform(p, p);
            g2.fillOval((int)p.getX() - POINT_RADIUS, (int)p.getY() - POINT_RADIUS, 
                    2 * POINT_RADIUS, 2 * POINT_RADIUS );
        }
        
        private void setTransform() {
            Envelope env = new Envelope();
            for (int i = 0; i < elements.size(); i++) {
                Object obj = elements.get(i).geom;
                if (obj instanceof Geometry) {
                    Geometry g = (Geometry) obj;
                    env.expandToInclude(g.getEnvelopeInternal());
                } else if (obj instanceof Coordinate) {
                    Coordinate c = (Coordinate) obj;
                    env.expandToInclude(c);
                }
            }

            Rectangle visRect = getVisibleRect();
            Rectangle drawingRect = new Rectangle(
                    visRect.x + MARGIN, visRect.y + MARGIN, visRect.width - 2 * MARGIN, visRect.height - 2 * MARGIN);

            double scale = Math.min(drawingRect.getWidth() / env.getWidth(), drawingRect.getHeight() / env.getHeight());
            double xoff = MARGIN - scale * env.getMinX();
            double yoff = MARGIN + env.getMaxY() * scale;
            tr = new AffineTransform(scale, 0, 0, -scale, xoff, yoff);
        }
    }
}
