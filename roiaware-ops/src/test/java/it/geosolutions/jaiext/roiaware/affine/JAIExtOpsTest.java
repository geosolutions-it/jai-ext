package it.geosolutions.jaiext.roiaware.affine;

import it.geosolutions.jaiext.roiaware.warp.ROIAwareWarpDescriptor;

import org.junit.Test;

public class JAIExtOpsTest {
	
@Test
public void testRegistration() {
	
	// register our own operations
    ROIAwareWarpDescriptor.register();
    ScaleDescriptor.register();
    AffineDescriptor.register();
    
    
	}
}
