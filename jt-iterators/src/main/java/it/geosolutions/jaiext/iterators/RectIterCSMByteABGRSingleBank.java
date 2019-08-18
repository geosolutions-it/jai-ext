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

public class RectIterCSMByteABGRSingleBank extends RectIterCSMByteABGR {

    public RectIterCSMByteABGRSingleBank(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    @Override
    public int[] getPixel(int[] iArray) {
        if (iArray == null) {
            iArray = new int[4];
        }
        
        // these two checks help the JIT remove array boundary checks in accesses below
        if (offset + 3 > bank.length)
            throw new ArrayIndexOutOfBoundsException(
                    "Max extracted offset "
                            + (offset + 3)
                            + " goes beyond bank size "
                            + bank.length);
        if (iArray.length < 4)
            throw new ArrayIndexOutOfBoundsException(
                    "Pixel should have 4 elements but has " + iArray.length);

        iArray[3] = bank[offset + 0] & 0xff;
        iArray[2] = bank[offset + 1] & 0xff;
        iArray[1] = bank[offset + 2] & 0xff;
        iArray[0] = bank[offset + 3] & 0xff;
        return iArray;
    }
}
