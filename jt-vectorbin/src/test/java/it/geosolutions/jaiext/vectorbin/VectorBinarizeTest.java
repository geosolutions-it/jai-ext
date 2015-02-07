/* 
 *  Copyright (c) 2010, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package it.geosolutions.jaiext.vectorbin;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.image.Raster;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

import org.jaitools.jts.CoordinateSequence2D;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the VectorBinarize operation.
 * 
 * @author Michael Bedward
 * @author Andrea Aime
 * @since 1.1
 * @version $Id$
 */
public class VectorBinarizeTest {

	private static final GeometryFactory gf = new GeometryFactory();
	private static final int TILE_WIDTH = 8;

	WKTReader reader = new WKTReader(gf);

	@Before
	public void setup() {
		JAI.setDefaultTileSize(new Dimension(TILE_WIDTH, TILE_WIDTH));
	}

	@Test
	public void rectanglePolyAcrossTiles() throws Exception {
		final int margin = 3;
		final int Ntiles = 3;

		int minx = margin;
		int miny = minx;
		int maxx = TILE_WIDTH * Ntiles - 2 * margin;
		int maxy = maxx;

		String wkt = String.format(
				"POLYGON((%d %d, %d %d, %d %d, %d %d, %d %d))", minx, miny,
				minx, maxy, maxx, maxy, maxx, miny, minx, miny);

		Polygon poly = (Polygon) reader.read(wkt);

		ParameterBlockJAI pb = new ParameterBlockJAI("VectorBinarize");
		pb.setParameter("width", Ntiles * TILE_WIDTH);
		pb.setParameter("height", Ntiles * TILE_WIDTH);
		pb.setParameter("geometry", poly);

		RenderedOp dest = JAI.create("VectorBinarize", pb);

		// uncomment for debugging purposes, remember to comment back before
		// committing
		// ImageIO.write(dest, "png", new java.io.File("/tmp/binarized.png"));

		CoordinateSequence2D testPointCS = new CoordinateSequence2D(1);
		Point testPoint = gf.createPoint(testPointCS);

		for (int ytile = 0; ytile < Ntiles; ytile++) {
			for (int xtile = 0; xtile < Ntiles; xtile++) {
				Raster tile = dest.getTile(xtile, ytile);
				for (int y = tile.getMinY(), iy = 0; iy < tile.getHeight(); y++, iy++) {
					testPointCS.setY(0, y + 0.5);
					for (int x = tile.getMinX(), ix = 0; ix < tile.getWidth(); x++, ix++) {
						testPointCS.setX(0, x + 0.5);
						testPoint.geometryChanged();
						int expected = poly.intersects(testPoint) ? 1 : 0;
						assertEquals("Failed test at position " + x + ", " + y
								+ ", " + "expected " + expected + " but got "
								+ tile.getSample(x, y, 0), expected,
								tile.getSample(x, y, 0));
					}
				}
			}
		}
	}
}
