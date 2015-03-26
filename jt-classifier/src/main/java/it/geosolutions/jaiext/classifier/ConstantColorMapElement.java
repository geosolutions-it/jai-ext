/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 GeoSolutions


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
package it.geosolutions.jaiext.classifier;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Color;

/**
 * The {@link ConstantColorMapElement} is a special type of {@link ColorMapTransformElement} that is used to render no data values.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * @todo simplify
 */
public class ConstantColorMapElement extends LinearColorMapElement implements
        ColorMapTransformElement {

    /**
     * UID
     */
    private static final long serialVersionUID = -4754147707013696371L;

    ConstantColorMapElement(CharSequence name, final Color color, final Range inRange,
            final int outVal) throws IllegalArgumentException {
        super(name, new Color[] { color }, inRange, RangeFactory.create(outVal, outVal));
    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    ConstantColorMapElement(final CharSequence name, final Color color, final short value,
            final int sample) throws IllegalArgumentException {
        this(name, color, RangeFactory.create(value, value), sample);

    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    ConstantColorMapElement(final CharSequence name, final Color color, final int value,
            final int sample) throws IllegalArgumentException {
        this(name, color, RangeFactory.create(value, value), sample);

    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    ConstantColorMapElement(final CharSequence name, final Color color, final float value,
            final int sample) throws IllegalArgumentException {
        this(name, color, RangeFactory.create(value, value), sample);

    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    ConstantColorMapElement(final CharSequence name, final Color color, final double value,
            final int sample) throws IllegalArgumentException {
        this(name, color, RangeFactory.create(value, value), sample);

    }

}
