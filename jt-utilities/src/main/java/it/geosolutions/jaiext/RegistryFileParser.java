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
package it.geosolutions.jaiext;

import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.OperationDescriptor;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.RegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * A class to parse the JAI registry file.
 * 
 * @author Nicola Lagomarsini - GeoSolutions
 */
class RegistryFileParser {

    /** {@link Logger} used for Logging any exception or warning */
    private static final Logger LOGGER = Logger.getLogger(RegistryFileParser.class.toString());

    /**
     * Returns a map with the descriptors, factories from the <code>URL</code>.
     */
    static Map<String, OperationItem> parseFile(ClassLoader cl, URL url) throws IOException {

        return (new RegistryFileParser(cl, url)).parseFile();
    }

    /** Input {@link URL} for the registryFile */
    private URL url;

    /** Input stream for the registryFile */
    private InputStream is;

    /** {@link ClassLoader} used for loading the registryFile resources */
    private ClassLoader classLoader;

    private BufferedReader reader;

    /**
     * Create a {@link RegistryFileParser} from an <code>URL</code>
     */
    private RegistryFileParser(ClassLoader cl, URL url) throws IOException {

        this.is = url.openStream();
        this.url = null;
        this.classLoader = cl;

        // Set up streamtokenizer
        reader = new BufferedReader(new InputStreamReader(is));
    }

    // Aliases for backward compatibility
    private static String[][] aliases = { { "odesc", "descriptor" }, { "rif", "rendered" },
            { "crif", "renderable" }, { "cif", "collection" }, };

    /**
     * Map old keywords to the new keywords
     */
    private String mapName(String key) {
        for (int i = 0; i < aliases.length; i++)
            if (key.equalsIgnoreCase(aliases[i][0]))
                return aliases[i][1];

        return key;
    }

    /**
     * Create an instance given the class name.
     */
    private Object getInstance(String className) {

        try {
            Class descriptorClass = null;
            String errorMsg = null;

            // Since the classes listed in the registryFile can
            // reside anywhere (core, ext, classpath or the specified
            // classloader) we have to try every place.

            // First try the specified classloader
            if (classLoader != null) {
                try {
                    descriptorClass = Class.forName(className, true, classLoader);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    errorMsg = e.getMessage();
                }
            }

            // Next try the callee classloader
            if (descriptorClass == null) {
                try {
                    descriptorClass = Class.forName(className);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    errorMsg = e.getMessage();
                }
            }

            // Then try the System classloader (because the specified
            // classloader might be null and the callee classloader
            // might be an ancestor of the SystemClassLoader
            if (descriptorClass == null) {
                try {
                    descriptorClass = Class.forName(className, true,
                            ClassLoader.getSystemClassLoader());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    errorMsg = e.getMessage();
                }
            }
            // If nothing is found an exception is reported
            if (descriptorClass == null) {
                registryFileError(errorMsg);
                return null;
            }

            return descriptorClass.newInstance();

        } catch (Exception e) {
            registryFileError(e.getMessage());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Parse the entire registry file and stores its informations inside a map.
     */
    Map<String, OperationItem> parseFile() throws IOException {
        // Initialization of the map
        Map<String, OperationItem> operations = new HashMap<String, OperationItem>();

        String[] keys;

        while ((keys = getNextLine()) != null) {

            RegistryMode mode;
            // Mapping of the key
            String key = mapName(keys[0]);

            // Selection of the descriptor
            if (key.equalsIgnoreCase("descriptor")) {
                RegistryElementDescriptor red = (RegistryElementDescriptor) getInstance(keys[1]);
                // Storing the information of the description only if it is an OperationDescriptor
                if (red != null && red instanceof OperationDescriptor) {
                    OperationDescriptor desc = (OperationDescriptor) red;
                    operations.put(desc.getName(), new OperationItem(desc));
                }

                // If it is a rendered registry mode, then get the
                // factory object.
            } else if ((mode = RegistryMode.getMode(key)) != null) {
                Object factory = getInstance(keys[1]);
                if (mode.getName().equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)
                        && factory != null) {
                    operations.get(keys[3]).setFactory(factory);
                }

            } else if (!(key.equalsIgnoreCase("registryMode") || key.equalsIgnoreCase("pref") || key
                    .equalsIgnoreCase("productPref"))) {
                registryFileError("Can not parse line");
            }
        }

        // If this was read in from an URL, we created the InputStream
        // and so we should close it.
        reader.close();
        is.close();

        return operations;
    }

    private String[] getNextLine() throws IOException {
        // TODO Auto-generated method stub
        String line = reader.readLine();
        if (line != null) {
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("\\s")) {
                return getNextLine();
            } else {
                String[] split = line.split("\\s");
                String[] corrected = new String[split.length];
                int count = 0;
                for (int i = 0; i < split.length; i++) {
                    if (!(split[i].equals("\\s") || split[i].isEmpty())) {
                        corrected[count] = split[i];
                        count++;
                    }
                }
                return corrected;
            }
        } else {
            return null;
        }

    }

    /** Boolean indicating that the header line has already been printed */
    private boolean headerLinePrinted = false;

    /**
     * Print the line number and then print the passed in message.
     */
    private void registryFileError(String msg) {

        if (!headerLinePrinted) {

            if (url != null) {
                errorMsg("Error while parsing JAI registry file " + url.getPath(), null);
            }

            headerLinePrinted = true;
        }

        if (msg != null)
            errorMsg(msg, null);
    }

    /**
     * Creates a <code>MessageFormat</code> object and set the <code>Locale</code> to default and formats the message
     */
    private void errorMsg(String key, Object[] args) {
        MessageFormat mf = new MessageFormat(key);
        mf.setLocale(Locale.getDefault());

        LOGGER.log(Level.SEVERE, mf.format(args));
    }

}
