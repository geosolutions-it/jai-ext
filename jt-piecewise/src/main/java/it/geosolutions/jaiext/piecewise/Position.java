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
package it.geosolutions.jaiext.piecewise;

/**
 * Class used for indicating a position on a selected axis. Note that the position is single dimensional
 * 
 * @author Nicola Lagomarsini
 * 
 */
public class Position {

    /** Value indicating the position on the axis */
    double ordinatePosition;

    public Position() {
    }

    public Position(double ordinatePosition) {
        this.ordinatePosition = ordinatePosition;
    }

    /**
     * @return the current ordinate position
     */
    public double getOrdinatePosition() {
        return ordinatePosition;
    }

    /**
     * Sets the current ordinate position parameter
     * 
     * @param ordinatePosition
     */
    public void setOrdinatePosition(double ordinatePosition) {
        this.ordinatePosition = ordinatePosition;
    }

    /**
     * @return the element dimension
     */
    public int getDimension() {
        return 1;
    }
}
