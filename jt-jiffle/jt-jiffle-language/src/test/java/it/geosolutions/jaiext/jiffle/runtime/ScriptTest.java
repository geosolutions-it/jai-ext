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

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import it.geosolutions.jaiext.jiffle.parser.JiffleParserException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ScriptTest {

    @Test
    public void testValidateSourceImages() throws Exception {
        checkInvalidSourceName("a b");
        checkInvalidSourceName("99a");
        checkInvalidSourceName("#abc");
        checkInvalidSourceName("/abc");
        checkInvalidSourceName("{abc");
    }

    @Test
    public void testValidateDestinationImages() throws Exception {
        checkInvalidDestinationName("a b");
        checkInvalidDestinationName("99a");
        checkInvalidDestinationName("#abc");
        checkInvalidDestinationName("/abc");
        checkInvalidDestinationName("{abc");
    }

    private void checkInvalidSourceName(String name) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript("dst=0;");
        Map<String, Jiffle.ImageRole> params = new HashMap<>();
        params.put(name, Jiffle.ImageRole.SOURCE);
        jiffle.setImageParams(params);

        JiffleParserException exception =
                Assert.assertThrows(JiffleParserException.class, () -> jiffle.compile());
        assertEquals("Invalid source image name: " + name, exception.getMessage());
    }

    private void checkInvalidDestinationName(String name) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript("dst=0;");
        Map<String, Jiffle.ImageRole> params = new HashMap<>();
        params.put(name, Jiffle.ImageRole.DEST);
        jiffle.setImageParams(params);

        JiffleParserException exception =
                Assert.assertThrows(JiffleParserException.class, () -> jiffle.compile());
        assertEquals("Invalid dest image name: " + name, exception.getMessage());
    }
}
