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

package it.geosolutions.jaiext.jiffle.parser;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import static java.util.Arrays.asList;

import static it.geosolutions.jaiext.jiffle.parser.JiffleParser.PLUS;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.parser.node.Band;
import it.geosolutions.jaiext.jiffle.parser.node.BinaryExpression;
import it.geosolutions.jaiext.jiffle.parser.node.DoubleLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.FunctionCall;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.ImagePos;
import it.geosolutions.jaiext.jiffle.parser.node.IntLiteral;
import it.geosolutions.jaiext.jiffle.parser.node.NodeException;
import it.geosolutions.jaiext.jiffle.parser.node.Pixel;
import it.geosolutions.jaiext.jiffle.parser.node.Variable;

public class SourcePositionsWorkerTest {

    @Test
    public void noInputs() throws Exception {
        for (String scriptFileName :
                asList(
                        "mandelbrot.jfl",
                        "interference.jfl",
                        "ripple.jfl",
                        "squircle.jfl",
                        "chessboard.jfl")) {
            Set<GetSourceValue> sourcePositions = getSourcePositions(scriptFileName);
            assertThat("Source positions found in " + scriptFileName, sourcePositions, empty());
        }
    }

    @Test
    public void lifeEdges() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("life-edges.jfl", "world");
        GetSourceValue offset =
                new GetSourceValue(
                        "world",
                        new ImagePos(
                                Band.DEFAULT,
                                new Pixel(offsetVariable("x", "ix"), offsetVariable("y", "iy"))));
        GetSourceValue original = new GetSourceValue("world", ImagePos.DEFAULT);
        assertThat(sourcePositions, hasItems(offset, original));
    }

    @Test
    public void lifeToroid() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("life-toroid.jfl", "world");
        GetSourceValue absolute =
                new GetSourceValue(
                        "world",
                        new ImagePos(
                                Band.DEFAULT,
                                new Pixel(
                                        new Variable("xx", JiffleType.D),
                                        new Variable("yy", JiffleType.D))));
        GetSourceValue original = new GetSourceValue("world", ImagePos.DEFAULT);
        assertThat(sourcePositions, hasItems(absolute, original));
    }

    @Test
    public void aspect() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("aspect.jfl", "dtm");
        assertThat(sourcePositions, hasSize(5));
        assertThat(sourcePositions, hasItems(new GetSourceValue("dtm", ImagePos.DEFAULT)));
        for (int[] offsets : new int[][] {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            assertThat(
                    sourcePositions,
                    hasItems(
                            new GetSourceValue(
                                    "dtm",
                                    new ImagePos(
                                            Band.DEFAULT,
                                            new Pixel(
                                                    offsetLiteral("x", offsets[0]),
                                                    offsetLiteral("y", offsets[1]))))));
        }
    }

    @Test
    public void flow() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("flow.jfl", "dtm");
        GetSourceValue offset =
                new GetSourceValue(
                        "dtm",
                        new ImagePos(
                                Band.DEFAULT,
                                new Pixel(offsetVariable("x", "dx"), offsetVariable("y", "dy"))));
        assertThat(sourcePositions, hasSize(1));
        assertThat(sourcePositions, hasItems(offset));
    }

    @Test
    public void ndvi() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("ndvi.jfl", "nir", "red");
        assertThat(sourcePositions, hasSize(2));
        assertThat(sourcePositions, hasItems(new GetSourceValue("nir", ImagePos.DEFAULT)));
        assertThat(sourcePositions, hasItems(new GetSourceValue("red", ImagePos.DEFAULT)));
    }

    @Test
    public void ndvi_s2() throws Exception {
        Set<GetSourceValue> sourcePositions = getSourcePositions("ndvi_s2.jfl", "src");
        assertThat(sourcePositions, hasSize(2));
        assertThat(
                sourcePositions,
                hasItems(
                        new GetSourceValue(
                                "src",
                                new ImagePos(new Band(new IntLiteral("7")), Pixel.DEFAULT))));
        assertThat(
                sourcePositions,
                hasItems(
                        new GetSourceValue(
                                "src",
                                new ImagePos(new Band(new IntLiteral("3")), Pixel.DEFAULT))));
    }

    private Set<GetSourceValue> getSourcePositions(String scriptFileName, String... inputNames)
            throws Exception {
        String script = IOUtils.toString(getClass().getResourceAsStream(scriptFileName));
        List sourceNames = null;
        if (inputNames != null) {
            sourceNames = asList(inputNames);
        }
        return Jiffle.getReadPositions(script, sourceNames);
    }

    private BinaryExpression offsetVariable(String pos, String offsetVar) throws NodeException {
        return new BinaryExpression(
                PLUS, FunctionCall.of(pos), new Variable(offsetVar, JiffleType.D));
    }

    private BinaryExpression offsetLiteral(String pos, double offsetPos) throws NodeException {
        return new BinaryExpression(
                PLUS, FunctionCall.of(pos), new DoubleLiteral(String.valueOf(offsetPos)));
    }
}
