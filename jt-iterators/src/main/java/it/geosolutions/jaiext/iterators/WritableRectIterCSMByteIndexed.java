/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2019 GeoSolutions


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


package it.geosolutions.jaiext.iterators;

import java.awt.*;
import java.awt.image.RenderedImage;

public class WritableRectIterCSMByteIndexed extends WritableRectIterCSMByte {
    
    public WritableRectIterCSMByteIndexed(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    @Override
    public void setSample(int s) {
        bank[offset] = (byte) s;
    }

    @Override
    public void setSample(int b, int s) {
        if (b != 0) {
            throw new ArrayIndexOutOfBoundsException("Only legal value for band is zero");
        }
        bank[offset] = (byte) s;
    }
}
