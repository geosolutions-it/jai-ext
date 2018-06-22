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

package it.geosolutions.jaiext.jiffle;

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.Map;

import it.geosolutions.jaiext.jiffle.Jiffle.ImageRole;
import it.geosolutions.jaiext.jiffle.runtime.BandTransform;
import it.geosolutions.jaiext.jiffle.runtime.CoordinateTransform;
import it.geosolutions.jaiext.jiffle.runtime.JiffleRuntime;
import it.geosolutions.jaiext.jiffle.runtime.JiffleRuntimeException;

/**
 * A stub class used in unit tests.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class NullRuntime implements JiffleRuntime {

    public Double getVar(String varName) {
        return null;
    }

    public void setVar(String varName, Double value) throws JiffleRuntimeException {}

    public void setWorldByResolution(Rectangle2D bounds, double xres, double yres) {}

    public void setWorldByNumPixels(Rectangle2D bounds, int nx, int ny) {}

    public boolean isWorldSet() {
        return true;
    }

    public double getMinX() {
        return 0;
    }

    public double getMaxX() {
        return 0;
    }

    public double getMinY() {
        return 0;
    }

    public double getMaxY() {
        return 0;
    }

    public double getWidth() {
        return 0;
    }

    public double getHeight() {
        return 0;
    }

    public double getXRes() {
        return 0;
    }

    public double getYRes() {
        return 0;
    }

    public long getNumPixels() {
        return 0;
    }

    public void setDefaultTransform(CoordinateTransform tr) {}

    public void setImageParams(Map<String, ImageRole> imageParams) {}

    public String[] getSourceVarNames() {
        return new String[0];
    }

    public String[] getDestinationVarNames() {
        return new String[0];
    }

    @Override
    public void setSourceImage(String varName, RenderedImage image, CoordinateTransform tr)
            throws JiffleException {
        
    }

    @Override
    public void setSourceImage(String varName, RenderedImage image) {

    }

    @Override
    public void setSourceImageBandTransform(String varName, BandTransform br)
            throws JiffleException {
        
    }

    @Override
    public void setSourceImageCoordinateTransform(String varName, CoordinateTransform tr)
            throws JiffleException {
        
    }

    @Override
    public double readFromImage(String srcImageName, double x, double y, int band) {
        return 0;
    }

    @Override
    public Map<String, RenderedImage> get_images() {
        return null;
    }

    public String[] getVarNames() {
        return new String[0];
    }

}
