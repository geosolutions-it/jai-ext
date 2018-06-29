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


/*
 * Copyright (c) 2018, Michael Bedward. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.jiffle.parser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Compares two java sources
 *
 * @author Andrea Aime - GeoSolutions
 * @source $URL$
 */
public class SourceAssert {

    /**
     * Makes the test interactive, showing a Swing dialog with before/after and a choice to
     * overwrite the expected image
     */
    static final boolean INTERACTIVE = Boolean.getBoolean("interactive");

    static final Logger LOGGER = Logger.getLogger("SourceAssert");

    public static void compare(File expectedFile, String actualSource)
            throws IOException {
        // do we have the reference source at all?
        if (!expectedFile.exists()) {

            // see what the user thinks of the image
            boolean useAsReference = INTERACTIVE && ReferenceSourceDialog.show(actualSource);
            if (useAsReference) {
                try {
                    File parent = expectedFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new AssertionError(
                                "Could not create directory that will contain :"
                                        + expectedFile.getParent());
                    }
                    FileUtils.writeStringToFile(expectedFile, actualSource);
                } catch (IOException e) {
                    throw (Error)
                            new AssertionError("Failed to write the source to disk").initCause(e);
                }
            } else {
                throw new AssertionError(
                        "Reference source is missing: "
                                + expectedFile
                                + ", add -Dinteractive=true to show a dialog comparing them (requires GUI support)");
            }
        } else {
            String expectedSource = FileUtils.readFileToString(expectedFile);
            if (!compareSourceStrings(expectedSource, actualSource)) {
                // check with the user
                boolean overwrite = false;
                if (INTERACTIVE) {
                    overwrite = CompareSourceDialog.show(expectedSource, actualSource, true);
                } else {
                    LOGGER.info(
                            "Sources are different, add -interactive=true to show a dialog comparing them (requires GUI support)");
                }

                if (overwrite) {
                    FileUtils.writeStringToFile(expectedFile, actualSource);
                } else {
                    throw new AssertionError(
                            "Sources are different. \nYou can add -Dinteractive=true to show a dialog comparing them (requires GUI support)");
                }
            }
        }
    }

    public static boolean compareSourceStrings(String expectedSource, String actualSource) {
        if( System.lineSeparator().equals("\r\n") ) {
            expectedSource = expectedSource.replaceAll("\\r\\n", "\n");
        }
        return expectedSource.equals(actualSource);
    }
}
