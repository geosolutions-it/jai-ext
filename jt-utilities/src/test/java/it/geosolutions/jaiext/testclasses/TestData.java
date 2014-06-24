package it.geosolutions.jaiext.testclasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Provides access to {@code test-data} directories associated with JUnit tests. Similar to the Geotools TestData class
 */
public class TestData {

    /**
     * The test data directory.
     */
    private static final String DIRECTORY = "test-data";

    /**
     * Encoding of URL path.
     */
    private static final String ENCODING = "UTF-8";  
    
    /**
     * Access to <code>{@linkplain #getResource getResource}(caller, path)</code> as a non-null
     * {@link File}. You can access the {@code test-data} directory with:
     *
     * <blockquote><pre>
     * TestData.file(MyClass.class, null);
     * </pre></blockquote>
     *
     * @param  caller Calling class or object used to locate {@code test-data}.
     * @param  path Path to file in {@code test-data}.
     * @return The file to the {@code test-data} resource.
     * @throws FileNotFoundException if the file is not found.
     * @throws IOException if the resource can't be fetched for an other reason.
     */
    public static File file(final Object caller, final String path)
            throws FileNotFoundException, IOException
    {
        final URL url = getResource(caller, path);
        if (url == null) {
            throw new FileNotFoundException("Can not locate test-data for \"" + path + '"');
        }
        final File file = new File(URLDecoder.decode(url.getPath(), ENCODING));
        if (!file.exists()) {
            throw new FileNotFoundException("Can not locate test-data for \"" + path + '"');
        }
        return file;
    }
    
    /**
     * Locates named test-data resource for caller. <strong>Note:</strong> Consider using the
     * <code>{@link #url url}(caller, name)</code> method instead if the resource should always
     * exists.
     *
     * @param  caller Calling class or object used to locate {@code test-data}.
     * @param  name resource name in {@code test-data} directory.
     * @return URL or {@code null} if the named test-data could not be found.
     *
     * @see #url
     */
    public static URL getResource(final Object caller, String name) {
        if (name == null || (name=name.trim()).length() == 0) {
            name = DIRECTORY;
        } else {
            name = DIRECTORY + '/' + name;
        }
        if (caller != null) {
            final Class c = (caller instanceof Class) ? (Class) caller : caller.getClass();
            return c.getResource(name);
        } else {
            return Thread.currentThread().getContextClassLoader().getResource(name);
        }
    }
}
