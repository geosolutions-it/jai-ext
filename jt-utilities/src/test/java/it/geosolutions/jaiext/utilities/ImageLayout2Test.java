/* 
 *  Copyright (c) 2010-2011, Simone Giannecchini. All rights reserved. 
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
 *  
 *  This class was ported from the JAITools project.
 */

package it.geosolutions.jaiext.utilities;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for equals and hashCode in ImageLayout2
 */
public class ImageLayout2Test {
    
    @Test
    public void nilHash() {
        ImageLayout2 il = new ImageLayout2();
        assertEquals(0, il.hashCode());
    }

    @Test
    public void sameHash() {
        ImageLayout2 il1 = new ImageLayout2(1, 2, 3, 4, 5, 6, 7, 8, null, null);
        ImageLayout2 il2 = new ImageLayout2(1, 2, 3, 4, 5, 6, 7, 8, null, null);
        assertEquals(il1.hashCode(), il2.hashCode());
    }

    @Test
    public void testEquals() {
        ImageLayout2 il1 = new ImageLayout2(1, 2, 3, 4, 5, 6, 7, 8, null, null);
        ImageLayout2 il2 = new ImageLayout2(1, 2, 3, 4, 5, 6, 7, 8, null, null);
        assertTrue(il1.equals(il2));
    }
    
    @Test
    public void testNotEquals() {
        ImageLayout2 il1 = new ImageLayout2(1, 2, 3, 4, 5, 6, 7, 8, null, null);

        int[] z = { 1, 2, 3, 4, 5, 6, 7, 8 };
        for (int i = 0; i < 8; i++) {
            int oldz = z[i];
            z[i] = 99;
            ImageLayout2 il2 = new ImageLayout2(
                    z[0], z[1], z[2], z[3], z[4], z[5], z[6], z[7], null, null);
            
            assertFalse(il1.equals(il2));
            z[i] = oldz;
        }
    }
}

