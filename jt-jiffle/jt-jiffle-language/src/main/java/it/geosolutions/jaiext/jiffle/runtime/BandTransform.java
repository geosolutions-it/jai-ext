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
package it.geosolutions.jaiext.jiffle.runtime;

import java.awt.*;

/**
 * A transform used by Jiffle to convert from band indices used in the scripts to raster band
 * indices
 */
public interface BandTransform {
    
    /**
     * Converts from script band indices to image band indices
     * 
     * @param x world X ordinate
     * @param y world Y ordinate
     * @param scriptBand the band index used in the script         
     * 
     * @return image band to be read
     */
    int scriptToImage(double x, double y, int scriptBand);
}
