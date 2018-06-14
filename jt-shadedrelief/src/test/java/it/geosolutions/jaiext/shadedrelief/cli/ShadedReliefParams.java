/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


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

package it.geosolutions.jaiext.shadedrelief.cli;

import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm;
import java.io.File;

/**
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class ShadedReliefParams
{
    File inputFile;
    File outputFile;

    Double srcNoData;
    double dstNoData;
    double resX;
    double resY;
    double zetaFactor;
    double scale;
    double altitude;
    double azimuth;
    ShadedReliefAlgorithm algo = ShadedReliefAlgorithm.DEFAULT;

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public Double getSrcNoData() {
        return srcNoData;
    }

    public void setSrcNoData(Double srcNoData) {
        this.srcNoData = srcNoData;
    }

    public double getDstNoData() {
        return dstNoData;
    }

    public void setDstNoData(double dstNoData) {
        this.dstNoData = dstNoData;
    }

    public double getResX() {
        return resX;
    }

    public void setResX(double resX) {
        this.resX = resX;
    }

    public double getResY() {
        return resY;
    }

    public void setResY(double resY) {
        this.resY = resY;
    }

    public double getZetaFactor() {
        return zetaFactor;
    }

    public void setZetaFactor(double zetaFactor) {
        this.zetaFactor = zetaFactor;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    public ShadedReliefAlgorithm getAlgo() {
        return algo;
    }

    public void setAlgo(ShadedReliefAlgorithm algo) {
        this.algo = algo;
    }

    @Override
    public String toString() {
        return "ShadedReliefParams["
                + "inputFile=" + inputFile
                + ", outputFile=" + outputFile
                + ", srcNoData=" + srcNoData
                + ", dstNoData=" + dstNoData
                + ", resX=" + resX
                + ", resY=" + resY
                + ", zeta=" + zetaFactor
                + ", scale=" + scale
                + ", altitude=" + altitude
                + ", azimuth=" + azimuth
                + ", algo=" + algo + ']';
    }

}
