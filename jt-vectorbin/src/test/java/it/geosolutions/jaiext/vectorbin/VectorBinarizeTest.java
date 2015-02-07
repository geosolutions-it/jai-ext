

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
