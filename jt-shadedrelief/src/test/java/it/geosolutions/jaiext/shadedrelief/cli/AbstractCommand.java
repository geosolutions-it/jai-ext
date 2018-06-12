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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class AbstractCommand
{

    private static Logger LOGGER = LogManager.getLogger(AbstractCommand.class);

    protected static List<File> validateFiles(String[] pathArr) throws BadParamException {
        List<File> ret = new ArrayList(pathArr.length);
        for (String path : pathArr) {
            ret.add(validateFile(path));
        }

        return ret;
    }

    protected static File validateFile(String path) throws BadParamException {
        Path p = Paths.get(path);
        File file = p.toFile();
        if (file.exists()) {
            return file;
        }
        throw new BadParamException("File not found: " + path);
    }

    protected static Double validateDouble(String s) throws BadParamException {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            throw new BadParamException("Bad double value: " + s);
        }

    }

    protected static void dumpParams(CommandLine cl, Options options) {
        List<String> deferred = new ArrayList<>();

        LOGGER.info("Options:");

        for (Option option : options.getOptions()) {
            String name = (option.getOpt() != null) ? option.getOpt() : option.getLongOpt();
            if (cl.hasOption(name)) {
                if (option.hasArgs()) {
                    LOGGER.info("Option " + name + " = " + Arrays.toString(cl.getOptionValues(name)));
                } else if (option.hasArg()) {
                    LOGGER.info("Option " + name + " = " + cl.getOptionValue(name));
                } else {
                    LOGGER.info("Option " + name);
                }
            } else {
                deferred.add(name);
            }
        }

        LOGGER.debug("---");

        for (String name : deferred) {
            LOGGER.debug("Option " + name + " not set");
        }
    }

    public static class BadParamException extends Exception
    {
        public BadParamException() {
        }

        public BadParamException(String message) {
            super(message);
        }

        public BadParamException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
