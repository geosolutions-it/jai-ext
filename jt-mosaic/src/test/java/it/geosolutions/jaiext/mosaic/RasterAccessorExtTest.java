/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2015 GeoSolutions


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
package it.geosolutions.jaiext.mosaic;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.media.jai.RasterFormatTag;

import org.junit.Test;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.range.RangeUshort;

public class RasterAccessorExtTest {

    @Test
    public void testExpandOpenRange() {
        BufferedImage byteGray = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage ushortGray = new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY);
        RasterFormatTag[] tags = RasterAccessorExt
                .findCompatibleTags(new RenderedImage[] { byteGray }, ushortGray);

        Range range = RangeFactory.create(Float.NEGATIVE_INFINITY, false, 1f, false);
        Range expanded = RasterAccessorExt.expandNoData(range, tags[0], byteGray, ushortGray);
        assertThat(expanded, instanceOf(RangeUshort.class));
        assertEquals(0, expanded.getMin().intValue());
        assertFalse(expanded.isMinIncluded());
        assertEquals(1, expanded.getMax().intValue());
        assertFalse(expanded.isMaxIncluded());
    }
}
