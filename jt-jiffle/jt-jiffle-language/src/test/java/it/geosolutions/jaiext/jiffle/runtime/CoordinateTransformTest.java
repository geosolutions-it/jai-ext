/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
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

package it.geosolutions.jaiext.jiffle.runtime;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Unit tests for CoordinateTransform class and the CoordinateTransforms helper
 * class.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class CoordinateTransformTest {
    
    @Test
    public void identity_direct() throws Exception {
        System.out.println("   identity transform created directly");
        
        CoordinateTransform tr = IdentityCoordinateTransform.INSTANCE;
        Point pt = tr.worldToImage(10.4, 10.6, null);
        assertPoint(10, 11, pt);
    }
    
    @Test
    public void identity_fromHelper() throws Exception {
        System.out.println("   identity transform from helper");
        
        CoordinateTransform tr = CoordinateTransforms.identity();
        Point pt = tr.worldToImage(10.4, 10.6, null);
        assertPoint(10, 11, pt);
    }
    
    @Test
    public void translation() throws Exception {
        System.out.println("   translation transform");
        
        CoordinateTransform tr = CoordinateTransforms.translation(10, -10);
        Point pt = tr.worldToImage(100, 100, null);
        assertPoint(110, 90, pt);
    }
    
    @Test
    public void scale() throws Exception {
        System.out.println("   scaling transform");
        
        CoordinateTransform tr = CoordinateTransforms.scale(0.1, 0.2);
        Point pt = tr.worldToImage(100, 100, null);
        assertPoint(10, 20, pt);
    }

    @Test
    public void unitBounds() throws Exception {
        System.out.println("   unit bounds transform");
        
        Rectangle r = new Rectangle(-100, 100, 1000, 2000);
        CoordinateTransform tr = CoordinateTransforms.unitBounds(r);
        
        assertPoint(r.x, r.y, tr.worldToImage(0, 0, null));
        assertPoint(r.x + r.width, r.y + r.height, tr.worldToImage(1, 1, null));
    }
    
    @Test
    public void getTransformDefault() throws Exception {
        System.out.println("   getTransform method");
        
        Rectangle world = new Rectangle(0, 0, 10000, 10000);
        Rectangle image = new Rectangle(10, -10, 100, 100);
        CoordinateTransform tr = CoordinateTransforms.getTransform(world, image);
        
        assertPoint(image.x, image.y, tr.worldToImage(world.x, world.y, null));
        
        assertPoint(image.x + image.width, image.y + image.height, 
                tr.worldToImage(world.x + world.width, world.y + world.height, null));
    }
    
    @Test
    public void getTransformReverseYDir() throws Exception {
        System.out.println("   getTransform with reversed Y axis");
        
        Rectangle world = new Rectangle(5000, 4000, 10000, 10000);
        Rectangle image = new Rectangle(10, -10, 100, 100);
        CoordinateTransform tr = CoordinateTransforms.getTransform(world, image, false, true);
        
        assertPoint(image.x, image.y, tr.worldToImage(world.x, world.y + world.height, null));
        
        assertPoint(image.x + image.width, image.y + image.height, 
                tr.worldToImage(world.x + world.width, world.y, null));
    }
    
    @Test
    public void getTransformReverseXDir() throws Exception {
        System.out.println("   getTransform with reversed X axis");
        
        Rectangle world = new Rectangle(5000, 4000, 10000, 10000);
        Rectangle image = new Rectangle(10, -10, 100, 100);
        CoordinateTransform tr = CoordinateTransforms.getTransform(world, image, true, false);
        
        assertPoint(image.x, image.y, tr.worldToImage(world.x + world.width, world.y, null));
        
        assertPoint(image.x + image.width, image.y + image.height, 
                tr.worldToImage(world.x, world.y + world.height, null));
    }
    
    @Test
    public void affineRotation() throws Exception {
        System.out.println("   affine rotation");

        // 90 degrees anti-clockwise rotation
        AffineTransform affine = AffineTransform.getRotateInstance(Math.PI/2, 50, 50);
        CoordinateTransform tr = new AffineCoordinateTransform(affine);
        
        assertPoint(100, 0, tr.worldToImage(0, 0, null));
        assertPoint(0, 100, tr.worldToImage(100, 100, null));
    }
    
    private void assertPoint(int expectedX, int expectedY, Point pt) {
        assertEquals(expectedX, pt.x);
        assertEquals(expectedY, pt.y);
    }
}
