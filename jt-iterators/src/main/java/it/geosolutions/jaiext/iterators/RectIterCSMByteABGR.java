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

public class RectIterCSMByteABGR extends RectIterCSMByte {

    public RectIterCSMByteABGR(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    @Override
    public int[] getPixel(int[] iArray) {
        if (iArray == null) {
            iArray = new int[numBands];
        }
        
        iArray[3] = bankData[3][offset + 0] & 0xff;
        iArray[2] = bankData[2][offset + 1] & 0xff;
        iArray[1] = bankData[1][offset + 2] & 0xff;
        iArray[0] = bankData[0][offset + 3] & 0xff;
        return iArray;
    }
}
