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
import it.geosolutions.jaiext.shadedrelief.ShadedReliefDescriptor;
import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class ShadedReliefCommand extends AbstractCommand
{

    private static Logger LOGGER = LogManager.getLogger(ShadedReliefCommand.class);

    public static String OPT_SRCFILE = "f";
    public static String OPT_OUTFILE = "o";
    public static String OPT_SRCNODATA = "sn";
    public static String OPT_DSTNODATA = "dn";
    public static String OPT_RESX = "x";
    public static String OPT_RESY = "y";
    public static String OPT_ZETA = "z";
    public static String OPT_SCALE = "s";
    public static String OPT_ALT = "alt";
    public static String OPT_AZ = "az";
    public static String OPT_ALG = "alg";

    public static String APP_NAME = "shadedrelief";

    public static void main(String[] args) throws Exception {
        LOGGER.info("");
        LOGGER.info("##############################");
        LOGGER.info(" ShadedRelief COMMAND");
        LOGGER.info("##############################");

        LOGGER.debug("Initializing tool...");

        Options options = createOptions();

        ShadedReliefParams params;
        try {
            params = parseParams(options, args);
        } catch (BadParamException e) {
            LOGGER.warn("Error parsing params: " + e.getMessage());

            final HelpFormatter usageFormatter = new HelpFormatter();
            try (PrintWriter pw = new PrintWriter(System.err)) {
                System.out.println("");
                usageFormatter.printHelp(
                        APP_NAME + ".sh",
                        "hillshading",
                        options, null, true);
            }

            return;
        }

        ShadedReliefRunner runner = new ShadedReliefRunner(params);
        try {
            runner.run();
        } finally {
            LOGGER.info("Exiting...");
//            System.exit(0); // something seems to be hanging...
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder(OPT_SRCFILE).longOpt("in").hasArg().argName("file").desc("Input file").required().build());
        options.addOption(Option.builder(OPT_OUTFILE).longOpt("out").hasArg().argName("file").desc("Output file").required().build());
        options.addOption(Option.builder(OPT_SRCNODATA).longOpt("srcnodata").hasArg().argName("val").desc("Value of nodata in source file").build());
        options.addOption(Option.builder(OPT_DSTNODATA).longOpt("dstnodata").hasArg().argName("val").desc("Value of nodata in out file").build());
        options.addOption(Option.builder(OPT_RESX).longOpt("resx").hasArg().argName("val").desc("X resolutions").build());
        options.addOption(Option.builder(OPT_RESY).longOpt("resy").hasArg().argName("val").desc("Y resolution").build());
        options.addOption(Option.builder(OPT_ZETA).longOpt("zeta").hasArg().argName("val").desc("Vertical relief factor (exaggeration)").build());
        options.addOption(Option.builder(OPT_SCALE).longOpt("scale").hasArg().argName("val").desc("Elevation unit to 2D unit scale ratio").build());
        options.addOption(Option.builder(OPT_ALT).longOpt("alt").hasArg().argName("val").desc("Sun altitude").build());
        options.addOption(Option.builder(OPT_AZ).longOpt("azimuth").hasArg().argName("val").desc("Sun azimuth").build());
        options.addOption(Option.builder(OPT_ALG).longOpt("algorithm").hasArg().argName("algorithm").desc("Algorithm. May be ZT, ZTC, C").build());

        return options;
    }

    private static ShadedReliefParams parseParams(Options options, String[] args) throws BadParamException {
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);

            dumpParams(cl, options);
        } catch (ParseException ex) {
            throw new BadParamException("Error parsing args: " + ex.getMessage(), ex);
        }

        ShadedReliefParams params = new ShadedReliefParams();

        File inputFile = validateFile(cl.getOptionValue(OPT_SRCFILE));
        params.setInputFile(inputFile);

        File outputFile = new File(cl.getOptionValue(OPT_OUTFILE));
        params.setOutputFile(outputFile);

        if (cl.hasOption(OPT_SRCNODATA)) {
            double d = validateDouble(cl.getOptionValue(OPT_SRCNODATA));
            params.setSrcNoData(d);
        } else {
            params.setSrcNoData(null);
        }

        if (cl.hasOption(OPT_DSTNODATA)) {
            double d = validateDouble(cl.getOptionValue(OPT_DSTNODATA));
            params.setDstNoData(d);
        } else {
            params.setDstNoData(0);
        }

        if (cl.hasOption(OPT_RESX)) {
            double d = validateDouble(cl.getOptionValue(OPT_RESX));
            params.setResX(d);
        } else {
            params.setResX(1);
        }

        if (cl.hasOption(OPT_RESY)) {
            double d = validateDouble(cl.getOptionValue(OPT_RESY));
            params.setResY(d);
        } else {
            params.setResY(1);
        }

        if (cl.hasOption(OPT_ZETA)) {
            double d = validateDouble(cl.getOptionValue(OPT_ZETA));
            params.setZetaFactor(d);
        } else {
            params.setZetaFactor(25);
        }

        if (cl.hasOption(OPT_SCALE)) {
            double d = validateDouble(cl.getOptionValue(OPT_SCALE));
            params.setScale(d);
        } else {
            params.setScale(ShadedReliefDescriptor.DEFAULT_SCALE);
        }

        if (cl.hasOption(OPT_ALT)) {
            double d = validateDouble(cl.getOptionValue(OPT_ALT));
            params.setAltitude(d);
        } else {
            params.setAltitude(ShadedReliefDescriptor.DEFAULT_ALTITUDE);
        }

        if (cl.hasOption(OPT_AZ)) {
            double d = validateDouble(cl.getOptionValue(OPT_AZ));
            params.setAzimuth(d);
        } else {
            params.setAzimuth(ShadedReliefDescriptor.DEFAULT_AZIMUTH);
        }

        if (cl.hasOption(OPT_ALG)) {
            switch (cl.getOptionValue(OPT_ALG)) {
                case "ZT":
                    params.setAlgo(ShadedReliefAlgorithm.ZEVENBERGEN_THORNE);
                    break;
                case "ZTC":
                    params.setAlgo(ShadedReliefAlgorithm.ZEVENBERGEN_THORNE_COMBINED);
                    break;
                case "C":
                    params.setAlgo(ShadedReliefAlgorithm.COMBINED);
                    break;
                default:
                    throw new BadParamException("Unknown algorithm " + cl.getOptionValue(OPT_ALG));

            }
        } else {
            params.setAlgo(ShadedReliefAlgorithm.DEFAULT);
        }

        return params;
    }

}
