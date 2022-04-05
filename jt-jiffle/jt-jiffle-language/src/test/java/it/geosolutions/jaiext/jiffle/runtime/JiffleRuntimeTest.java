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
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JiffleRuntimeTest {

    @Test
    public void testSkipSource() throws Exception {
        Jiffle jiffle = getJiffleWithComment();
        String source = jiffle.getRuntimeSource(false);

        // by default, it does not contain the source
        assertFalse(source.contains("This is a comment"));
    }

    @Test
    public void testEscapeSource() throws Exception {
        Jiffle jiffle = getJiffleWithComment();
        String source = jiffle.getRuntimeSource(true);

        // source is in the javadocs, and properly escaped
        assertTrue(source.contains("* &#47;* This is a comment *&#47;"));
        assertTrue(source.contains("* dest=10;"));
    }

    private Jiffle getJiffleWithComment() throws JiffleException {
        String script = "/* This is a comment */\ndest=10;";
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        Map<String, Jiffle.ImageRole> params = new HashMap<>();
        params.put("dest", Jiffle.ImageRole.DEST);
        jiffle.setImageParams(params);

        jiffle.compile();
        return jiffle;
    }
}
