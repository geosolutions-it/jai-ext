package it.geosolutions.jaiext.lookup;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.LookupTableJAI;

public class LookupTableWrapper extends LookupTable {
	
	private LookupTableJAI lut;

	public LookupTableWrapper(LookupTableJAI lut){
		// Fake constructor
		super(new byte[1]);
		this.lut = lut;
	}

	@Override
	protected void lookup(Raster source, WritableRaster dst, Rectangle rect,
			Raster roi) {
		lut.lookup(source, dst, rect);
	}
	
	@Override
	public int getNumBands() {
		return lut.getNumBands();
	}

}
