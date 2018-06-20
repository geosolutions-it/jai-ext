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



package it.geosolutions.jaiext.jiffleop;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import it.geosolutions.jaiext.jiffle.JiffleBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for JiffleOpImage
 * 
 * @author Michael Bedward
 * @since 1.2
 * @version $Id$
 */
public class NoSourceTest {
    
    private static final double TOL = 1.0e-8;
    
    private static final int WIDTH = 10;
    private Dimension savedTileSize;
    
    @Before
    public void init() {
        savedTileSize = JAI.getDefaultTileSize();
        JAI.setDefaultTileSize(new Dimension(WIDTH / 2, WIDTH / 2));
    }
    
    @After
    public void reset() {
        JAI.setDefaultTileSize(savedTileSize);
    }
    
    @Test
    public void createSequentialImage() throws Exception {
        ParameterBlockJAI pb = new ParameterBlockJAI("Jiffle");
        
        String script = "dest = y() * width() + x();" ;
        
        pb.setParameter("script", script);
        pb.setParameter("destName", "dest");
        
        Rectangle bounds = new Rectangle(0, 0, WIDTH, WIDTH);
        pb.setParameter("destBounds", bounds);
        
        RenderedOp op = JAI.create("Jiffle", pb);
        RenderedImage result = op.getRendering();
        
        assertResult(result, script);
    }

    private void assertResult(RenderedImage resultImage, String script) throws Exception {
        JiffleBuilder builder = new JiffleBuilder();
        builder.script(script).dest("dest", WIDTH, WIDTH).run();
        RenderedImage referenceImage = builder.getImage("dest");

        RectIter resultIter = RectIterFactory.create(resultImage, null);
        RectIter referenceIter = RectIterFactory.create(referenceImage, null);
        
        do {
            do {
                assertEquals(resultIter.getSample(), referenceIter.getSample());
                resultIter.nextPixelDone();
            } while (!referenceIter.nextPixelDone());
            
            resultIter.startPixels();
            resultIter.nextLineDone();
            referenceIter.startPixels();
            
        } while (!referenceIter.nextLineDone());
    }
}
