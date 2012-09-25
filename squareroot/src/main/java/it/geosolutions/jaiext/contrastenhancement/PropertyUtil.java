/*
 *    JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    (C) 2012, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.contrastenhancement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Vector;


public class PropertyUtil
{

    private static Map<String, ResourceBundle> BUNDLES = new HashMap<String, ResourceBundle>();
    private static String propertiesDir = "it/geosolutions/jaiext/contrastenhancement";

    public static InputStream getFileFromClasspath(String path) throws IOException, FileNotFoundException
    {
        InputStream is;

        final String sep = File.separator;
        String tmpHome = null;
        try
        {
            tmpHome = System.getProperty("java.home");
        }
        catch (Exception e)
        {
            tmpHome = null; // Redundant
        }

        final String home = tmpHome;
        final String urlHeader = (tmpHome == null) ? null : (home + sep + "lib" + sep);

        if (home != null)
        {
            String libExtPath = urlHeader + "ext" + sep + path;
            File libExtFile = new File(libExtPath);
            try
            {
                if (libExtFile.exists())
                {
                    is = new FileInputStream(libExtFile);
                    if (is != null)
                    {
                        return is;
                    }
                }
            }
            catch (java.security.AccessControlException e)
            {
                // When the files are packed into jar files, the
                // permission to access these files in a security environment
                // isn't granted in the policy files in most of the cases.
                // Thus, this java.security.AccessControlException is
                // thrown.  To continue the searching in the jar files,
                // catch this exception and do nothing here.
                // The fix of 4531516.
            }
        }

        is = PropertyUtil.class.getResourceAsStream("/" + path);
        if (is != null)
        {
            return is;
        }

        return null;
    }

    /** Get bundle from .properties files in javax/media/jai dir. */
    private static ResourceBundle getBundle(String packageName)
    {
        ResourceBundle bundle = null;

        InputStream in = null;
        try
        {
            in = getFileFromClasspath(propertiesDir + "/" +
                    packageName + ".properties");
            if (in != null)
            {
                bundle = new PropertyResourceBundle(in);
                BUNDLES.put(packageName, bundle);

                return bundle;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static String getString(String packageName, String key)
    {
        ResourceBundle b = (ResourceBundle) BUNDLES.get(packageName);
        if (b == null)
        {
            b = getBundle(packageName);
        }

        return b.getString(key);
    }

    /**
     * Utility method to search the full list of property names for
     * matches.  If <code>propertyNames</code> is <code>null</code>
     * then <code>null</code> is returned.
     *
     * @exception IllegalArgumentException if <code>prefix</code> is
     * <code>null</code> and <code>propertyNames</code> is
     * non-<code>null</code>.
     */
    public static String[] getPropertyNames(String[] propertyNames,
        String prefix)
    {
        if (propertyNames == null)
        {
            return null;
        }
        else if (prefix == null)
        {
            throw new IllegalArgumentException("The property name prefix may not be null");
        }

        prefix = prefix.toLowerCase();

        Vector names = new Vector();
        for (int i = 0; i < propertyNames.length; i++)
        {
            if (propertyNames[i].toLowerCase().startsWith(prefix))
            {
                names.addElement(propertyNames[i]);
            }
        }

        if (names.size() == 0)
        {
            return null;
        }

        // Copy the strings from the Vector over to a String array.
        String[] prefixNames = new String[names.size()];
        int count = 0;
        for (Iterator it = names.iterator(); it.hasNext();)
        {
            prefixNames[count++] = (String) it.next();
        }

        return prefixNames;
    }
}
