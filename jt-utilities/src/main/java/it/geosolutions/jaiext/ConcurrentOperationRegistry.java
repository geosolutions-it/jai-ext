/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2015 GeoSolutions


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

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;

import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationNode;
import javax.media.jai.OperationRegistry;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.PropertySource;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.registry.RenderedRegistryMode;
import javax.media.jai.util.ImagingException;
import javax.media.jai.util.ImagingListener;

import com.sun.media.jai.util.PropertyUtil;

/**
 * A thread safe implementation of OperationRegistry using Java 5 Concurrent {@link ReadWriteLock}
 * Also it is able to substitute JAI operations with JAI-EXT ones and vice versa.
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Nicola Lagomarsini - GeoSolutions
 */
public final class ConcurrentOperationRegistry extends OperationRegistry {
    /** Path to the JAI default registryfile.jai */
    static String JAI_REGISTRY_FILE = "META-INF/javax.media.jai.registryFile.jai";

    /** Name of the other registryfile.jai */
    static String USR_REGISTRY_FILE = "META-INF/registryFile.jaiext";

    /** String associated to the vendor key */
    static final String VENDOR_NAME = "Vendor";

    /** String associated to the JAIEXT product */
    static final String JAIEXT_PRODUCT = "it.geosolutions.jaiext";

    /** String associated to the JAI product */
    static final String JAI_PRODUCT = "com.sun.media.jai";

    /** String associated to the JAI product when the operation is "Null" */
    static final String JAI_PRODUCT_NULL = "javax.media.jai";

    /** Logger associated to the class*/
    private static final Logger LOGGER = Logger.getLogger(ConcurrentOperationRegistry.class.toString());

    public static OperationRegistry initializeRegistry() {
        return initializeRegistry(true);
    }

    public static OperationRegistry initializeRegistry(boolean useJaiExtOps) {
        try {
            // URL associated to the default JAI registryfile.jai
            InputStream url = PropertyUtil.getFileFromClasspath(JAI_REGISTRY_FILE);

            if (url == null) {
                throw new RuntimeException("Could not find the main registry file");
            }
            // Creation of a new registry
            ConcurrentOperationRegistry registry = new ConcurrentOperationRegistry();
            // Registration of the JAI operations
            if (url != null) {
                registry.updateFromStream(url);
            }
            // Registration of the operation defined in any registryFile.jai file
            registry.registerServices(null);
            // Listing of all the registered operations
            List<OperationDescriptor> descriptors = registry
                    .getDescriptors(RenderedRegistryMode.MODE_NAME);
            // Creation of a new OperationCollection object
            OperationCollection input = new OperationCollection(registry);
            input.createMapFromDescriptors(descriptors);
            // Saving of the all initial operations
            Map<String, OperationItem> map = input.copy().filter(JAI_PRODUCT).map;
            map.put("Null", input.get("Null"));
            registry.jaiMap = map;

            // First load all the REGISTRY_FILEs that are found in
            // the specified class loader.
            ClassLoader loader = registry.getClass().getClassLoader();
            Enumeration<URL> en = loader.getResources(USR_REGISTRY_FILE);
            // Creation of another OperationCollection instance to use for storing all the 
            OperationCollection changed = new OperationCollection(registry);
            // Loop on all the registryFile.jai files
            while (en.hasMoreElements()) {
                URL url1 = en.nextElement();
                changed.add(RegistryFileParser.parseFile(null, url1));
            }
            // Filter only the JAIEXT operations
            changed = changed.filter(JAIEXT_PRODUCT);
            // Copy the available JAIEXT operations
            registry.jaiExtMap = changed.copy().map;
            // Substitute the JAIEXT operations only if required
            if (useJaiExtOps) {
                input.substituteOperations(changed);
            } else {
                OperationCollection uniqueOperations = input.getUniqueOperations(changed);
                input.substituteOperations(uniqueOperations);
            }
            // Set the Collection inside the registry file
            registry.setOperationCollection(input);
            // Return the registry
            return registry;

        } catch (IOException ioe) {
            ImagingListener listener = JAI.getDefaultInstance().getImagingListener();
            String message = "Error occurred while initializing JAI";
            listener.errorOccurred(message, new ImagingException(message, ioe),
                    OperationRegistry.class, false);

            return null;
        }
    }

    /** The reader/writer lock for this class. */
    private ReadWriteLock lock;

    /** Collection of the registered Operations */
    OperationCollection collection;

    /** Map of the JAI operations */
    private Map<String, OperationItem> jaiMap;

    /** Map of the JAI-EXT operations */
    private Map<String, OperationItem> jaiExtMap;

    public ConcurrentOperationRegistry() {
        super();

        // Create a concurrent RW lock.
        lock = new ReentrantReadWriteLock();
    }

    public String toString() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.toString();
        } finally {
            readLock.unlock();
        }
    }

    public void writeToStream(OutputStream out) throws IOException {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            super.writeToStream(out);
        } finally {
            readLock.unlock();
        }
    }

    public void initializeFromStream(InputStream in) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.initializeFromStream(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void updateFromStream(InputStream in) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.updateFromStream(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.readExternal(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            super.writeExternal(out);
        } finally {
            readLock.unlock();
        }
    }

    public void removeRegistryMode(String modeName) {

        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.removeRegistryMode(modeName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[] getRegistryModes() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getRegistryModes();
        } finally {
            readLock.unlock();
        }
    }

    public void registerDescriptor(RegistryElementDescriptor descriptor) {
        Lock writeLock = lock.writeLock();
        boolean changed = false;
        try {
            writeLock.lock();
            super.registerDescriptor(descriptor);
            changed = true;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Registered descriptor for the operation: "
                        + descriptor.getName());
            }
            // If the collection is present, then it is updated
            if(collection != null && changed && descriptor instanceof OperationDescriptor){
                OperationItem item = new OperationItem((OperationDescriptor) descriptor);
                collection.substituteSingleOp(item);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Added descriptor for the operation: "
                            + descriptor.getName()  + " to the registry operation list");
                }
            }
        }catch(Exception e){
            // Remove logging for OperationDescriptor registration
            if(e.getMessage().contains("A descriptor is already registered against the name")){
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, e.getMessage());
                }
            } else {
                throw new RuntimeException(e);
            }
        }finally {
            writeLock.unlock();
        }
    }

    public void unregisterDescriptor(RegistryElementDescriptor descriptor) {

        Lock writeLock = lock.writeLock();
        boolean changed = false;
        try {
            writeLock.lock();
            super.unregisterDescriptor(descriptor);
            changed = true;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Unregistered descriptor for the operation: "
                        + descriptor.getName());
            }
            // If the collection is present, then it is updated
            if(collection != null && changed && descriptor instanceof OperationDescriptor){
                OperationItem item = new OperationItem((OperationDescriptor) descriptor);
                collection.removeSingleOp(item);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Removed descriptor for the operation: "
                            + descriptor.getName()  + " from the registry operation list");
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public RegistryElementDescriptor getDescriptor(Class descriptorClass, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptor(descriptorClass, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public List getDescriptors(Class descriptorClass) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptors(descriptorClass);
        } finally {
            readLock.unlock();
        }
    }

    public String[] getDescriptorNames(Class descriptorClass) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptorNames(descriptorClass);
        } finally {
            readLock.unlock();
        }
    }

    public RegistryElementDescriptor getDescriptor(String modeName, String descriptorName) {

        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptor(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public List getDescriptors(String modeName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptors(modeName);
        } finally {
            readLock.unlock();
        }
    }

    public String[] getDescriptorNames(String modeName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptorNames(modeName);
        } finally {
            readLock.unlock();
        }
    }

    public void setProductPreference(String modeName, String descriptorName,
            String preferredProductName, String otherProductName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.setProductPreference(modeName, descriptorName, preferredProductName,
                    otherProductName);
        } finally {
            writeLock.unlock();
        }
    }

    public void unsetProductPreference(String modeName, String descriptorName,
            String preferredProductName, String otherProductName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unsetProductPreference(modeName, descriptorName, preferredProductName,
                    otherProductName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearProductPreferences(String modeName, String descriptorName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearProductPreferences(modeName, descriptorName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[][] getProductPreferences(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getProductPreferences(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Vector getOrderedProductList(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getOrderedProductList(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public void registerFactory(String modeName, String descriptorName, String productName,
            Object factory) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.registerFactory(modeName, descriptorName, productName, factory);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Registered factory for the operation: "
                        + descriptorName);
            }
            // If the collection is present, then it is updated
            if(collection != null && modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)){
                collection.substituteFactory(factory, descriptorName, productName); 
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Added factory for the operation: "
                            + descriptorName  + " to the registry operation list");
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void unregisterFactory(String modeName, String descriptorName, String productName,
            Object factory) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unregisterFactory(modeName, descriptorName, productName, factory);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Unregistered factory for the operation: "
                        + descriptorName);
            }
            // If the collection is present, then it is updated
            if(collection != null && modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)){
                collection.removeFactory(factory, descriptorName, productName); 
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Removed factory for the operation: "
                            + descriptorName  + " from the registry operation list");
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void setFactoryPreference(String modeName, String descriptorName, String productName,
            Object preferredOp, Object otherOp) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.setFactoryPreference(modeName, descriptorName, productName, preferredOp, otherOp);
        } finally {
            writeLock.unlock();
        }
    }

    public void unsetFactoryPreference(String modeName, String descriptorName, String productName,
            Object preferredOp, Object otherOp) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unsetFactoryPreference(modeName, descriptorName, productName, preferredOp,
                    otherOp);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearFactoryPreferences(String modeName, String descriptorName, String productName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearFactoryPreferences(modeName, descriptorName, productName);
        } finally {
            writeLock.unlock();
        }
    }

    public Object[][] getFactoryPreferences(String modeName, String descriptorName,
            String productName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactoryPreferences(modeName, descriptorName, productName);
        } finally {
            readLock.unlock();
        }
    }

    public List getOrderedFactoryList(String modeName, String descriptorName, String productName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getOrderedFactoryList(modeName, descriptorName, productName);
        } finally {
            readLock.unlock();
        }
    }

    public Iterator getFactoryIterator(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactoryIterator(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Object getFactory(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactory(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Object invokeFactory(String modeName, String descriptorName, Object[] args) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            // For Rendered Mode, a check on the interpolations objects is made
            // in order to convert each eventual JAI-EXT interpolation class
            // if the Factory belongs to the JAI API
            if(modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)){
                checkInterpolation(descriptorName, args);
            }

            return super.invokeFactory(modeName, descriptorName, args);
        } finally {
            readLock.unlock();
        }
    }

    public void addPropertyGenerator(String modeName, String descriptorName,
            PropertyGenerator generator) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.addPropertyGenerator(modeName, descriptorName, generator);
        } finally {
            writeLock.unlock();
        }
    }

    public void removePropertyGenerator(String modeName, String descriptorName,
            PropertyGenerator generator) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.removePropertyGenerator(modeName, descriptorName, generator);
        } finally {
            writeLock.unlock();
        }
    }

    public void copyPropertyFromSource(String modeName, String descriptorName, String propertyName,
            int sourceIndex) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.copyPropertyFromSource(modeName, descriptorName, propertyName, sourceIndex);
        } finally {
            writeLock.unlock();
        }
    }

    public void suppressProperty(String modeName, String descriptorName, String propertyName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.suppressProperty(modeName, descriptorName, propertyName);
        } finally {
            writeLock.unlock();
        }
    }

    public void suppressAllProperties(String modeName, String descriptorName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.suppressAllProperties(modeName, descriptorName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearPropertyState(String modeName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearPropertyState(modeName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[] getGeneratedPropertyNames(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getGeneratedPropertyNames(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public PropertySource getPropertySource(String modeName, String descriptorName, Object op,
            Vector sources) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getPropertySource(modeName, descriptorName, op, sources);
        } finally {
            readLock.unlock();
        }
    }

    public PropertySource getPropertySource(OperationNode op) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getPropertySource(op);
        } finally {
            readLock.unlock();
        }
    }

    public void registerServices(ClassLoader cl) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.registerServices(cl);
        } finally {
            writeLock.unlock();
        }
    }

    public void unregisterOperationDescriptor(String operationName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unregisterOperationDescriptor(operationName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearOperationPreferences(String operationName, String productName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearOperationPreferences(operationName, productName);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sets the {@link OperationCollection} containing the list of all the operations contained by the registry.
     * 
     * @param coll
     */
    public void setOperationCollection(OperationCollection coll) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            this.collection = coll;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * This method returns an {@link OperationCollection} object containing all the registered operations.
     * 
     * @return
     */
    OperationCollection getOperationCollection() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            return collection;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns a {@link Collection} object containing a view of the {@link OperationCollection} inside the registry.
     * 
     * @return
     */
    public Collection<OperationItem> getOperations() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            return collection.getOperations();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns a Map containing the {@link OperationItem} objects for each operation. The jai parameter indicates whether must be returned the map of
     * the jai operations or of the Jai-ext ones.
     * 
     * @param jai
     * @return
     */
    public Map<String, OperationItem> getOperationMap(boolean jai) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            if (jai) {
                return jaiMap;
            } else {
                return jaiExtMap;
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * This method internally check if the descriptor used is a
     * 
     * @param descriptorName
     * @param args
     */
    void checkInterpolation(String descriptorName, Object[] args) {
        // First check if the collection is present and then get the OperationItem associated
        OperationItem item = null;
        if (collection != null) {
            item = collection.get(descriptorName);
        }
        // By default we do not change the Interpolation
        boolean jaiext = true;
        // If the item is present we check if it belongs to jaiext
        if (item != null) {
            jaiext = item.isJAIEXTProduct();
        } else {
            // Else we check the Descriptor vendor parameter
            OperationDescriptor op = (OperationDescriptor) getDescriptor(
                    RenderedRegistryMode.MODE_NAME, descriptorName);
            String vendor = op.getResourceBundle(null).getString(VENDOR_NAME);
            jaiext = vendor.equalsIgnoreCase(JAIEXT_PRODUCT);
        }
        // If the operation is not a JAI-EXT one then we start to check if there is any Interpolation
        // instance
        if (!jaiext) {
            // Cycle on the parameterBlock parameters
            ParameterBlock block = (ParameterBlock) args[0];
            Vector<Object> params = block.getParameters();
            int index = 0;
            for (Object param : params) {
                if (param instanceof Interpolation) {
                    Interpolation interp = null;
                    // If the parameter is an instance of one of the JAI-EXT Interpolation classes
                    // then it is transformed into the related JAI Interpolation class.
                    if (param instanceof InterpolationNearest) {
                        interp = new javax.media.jai.InterpolationNearest();
                    } else if (param instanceof InterpolationBilinear) {
                        InterpolationBilinear bil = (InterpolationBilinear) param;
                        interp = new javax.media.jai.InterpolationBilinear(bil.getSubsampleBitsH());
                    } else if (param instanceof InterpolationBicubic) {
                        InterpolationBicubic bic = (InterpolationBicubic) param;
                        if (bic.isBicubic2()) {
                            interp = new javax.media.jai.InterpolationBicubic2(
                                    bic.getSubsampleBitsH());
                        } else {
                            interp = new javax.media.jai.InterpolationBicubic(
                                    bic.getSubsampleBitsH());
                        }
                    }
                    if (interp != null) {
                        block.set(interp, index);
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "Converted JAI-EXT Interpolation object to JAI one");
                        }
                    }
                    break;
                }
                index++;
            }
        }
    }

    /**
     * The {@link OperationItem} class is a wrapper for the {@link OperationDescriptor} class which can store informations about the operations and
     * the associated factory.
     * 
     * @author Nicola Lagomarsini GeoSolutions S.A.S.
     * 
     */
    public static class OperationItem {
        /** {@link OperationDescriptor} instance associated to the operation */
        private OperationDescriptor op;

        /** Descriptor vendor name */
        private String vendor;

        /** Operation Name */
        private String opName;

        /** Factory object (May be null) */
        private Object factory;

        /** MediaLib Factory object, used by JAI. (May be null) */
        private Object mlibFactory;

        /** Boolean indicating if the MediaLib acceleration must be used. By default is set to false */
        private boolean isMediaLibPreferred;

        public OperationItem(OperationDescriptor op) {
            this.op = op;
            this.opName = op.getName();
            this.vendor = op.getResourceBundle(null).getString(VENDOR_NAME);
            this.isMediaLibPreferred = false;
        }

        public OperationItem(OperationItem item) {
            this.op = item.getDescriptor();
            this.opName = item.getName();
            this.vendor = item.getVendor();
            this.factory = item.getFactory();
            this.mlibFactory = item.getMlibFactory();
            this.isMediaLibPreferred = item.isMediaLibPreferred;
        }

        public String getVendor() {
            return vendor;
        }

        public String getName() {
            return opName;
        }

        public OperationDescriptor getDescriptor() {
            return op;
        }

        /***
         * Returns the factory defined by the user. If medialib is preferred, then the MediaLib factory is returned, otherwise the JAI default factory
         * is returned.
         * 
         * @return
         */
        public Object getCurrentFactory() {
            if (mlibFactory != null && isMediaLibPreferred) {
                return mlibFactory;
            }
            return factory;
        }

        public Object getFactory() {
            return factory;
        }

        public Object getMlibFactory() {
            return mlibFactory;
        }

        public OperationDescriptor getOp() {
            return op;
        }

        public boolean isMediaLibPreferred() {
            return isMediaLibPreferred;
        }

        public void setFactory(Object factory) {
            this.factory = factory;
        }

        public void setMlibFactory(Object factory) {
            this.mlibFactory = factory;
        }

        public void setMlibPreference(boolean preferred) {
            this.isMediaLibPreferred = preferred;
        }

        /**
         * Indicates if the {@link OperationItem} operation is a JAI-EXT one
         * 
         * @return
         */
        public boolean isJAIEXTProduct() {
            return vendor.equalsIgnoreCase(JAIEXT_PRODUCT);
        }
    }

    /**
     * This class is a container class which stores internally all the {@link OperationItem}s, each one for an {@link OperationDescriptor}. This class
     * contains all the operations inside an inner map and provides some utility methods for using it.
     * 
     * @author Nicola Lagomarsini GeoSolutions S.A.S.
     * 
     */
    static class OperationCollection {
        /** Inner Map containing all the {@link OperationItem}s for each operation */
        private Map<String, OperationItem> map = new ConcurrentHashMap<String, OperationItem>();

        /** {@link OperationRegistry} used by the collection for registering and unregistering operations */
        private OperationRegistry registry;

        public OperationCollection(OperationRegistry registry, Map<String, OperationItem> map) {
            this.map = map;
            this.registry = registry;
        }

        public OperationCollection(OperationRegistry registry) {
            this.registry = registry;
        }

        /**
         * This method copies all the values inside a new {@link OperationCollection} instance
         */
        public OperationCollection copy() {
            Collection<OperationItem> values = map.values();
            OperationCollection newColl = new OperationCollection(registry);
            for (OperationItem item : values) {
                newColl.add(new OperationItem(item));
            }
            return newColl;
        }

        /**
         * This method populates the inner map with a List of {@link OperationDescriptor}s.
         * 
         * @param listDesc
         */
        public void createMapFromDescriptors(List<OperationDescriptor> listDesc) {
            for (OperationDescriptor desc : listDesc) {
                OperationItem value = createItem(desc);
                map.put(desc.getName(), value);
            }
        }

        /**
         * This method returns a new {@link OperationCollection} instance filtered by the vendor name.
         * 
         * @param vendor
         * @return
         */
        public OperationCollection filter(String vendor) {
            Collection<OperationItem> values = map.values();
            OperationCollection filtered = new OperationCollection(registry);
            for (OperationItem item : values) {
                if (item.getVendor().equalsIgnoreCase(vendor)) {
                    filtered.map.put(item.getName(), item);
                }
            }
            return filtered;
        }

        /**
         * Add a new Item to the map.
         * 
         * @param item
         */
        public void add(OperationItem item) {
            map.put(item.getName(), item);
        }

        /**
         * Add the contents of an external map inside the inner map.
         * 
         * @param items
         */
        public void add(Map<String, OperationItem> items) {
            map.putAll(items);
        }

        /**
         * Returns the {@link OperationItem} associated to the input operation name.
         * 
         * @param decriptorname
         * @return
         */
        public OperationItem get(String decriptorname) {
            return map.get(decriptorname);
        }

        /**
         * Returns a view of the all the {@link OperationItem}s contained by the map.
         * 
         * @return
         */
        public Collection<OperationItem> getOperations() {
            return map.values();
        }

        /**
         * Creates a new {@link OperationItem} from an {@link OperationDescriptor}.
         * 
         * @param desc
         * @return
         */
        public OperationItem createItem(OperationDescriptor desc) {

            OperationItem value = new OperationItem(desc);
            // Selection of a List of the Factories associated to the operation and the vendor
            List<Object> list = registry.getOrderedFactoryList(RenderedRegistryMode.MODE_NAME,
                    desc.getName(), value.getVendor());
            // If the List is not null then we start iterating on it.
            if (list != null) {
                // If there is the MediaLib factory, it is saved inside the OperationItem
                // but it is not used by default
                for (Object factory : list) {
                    if (factory.getClass().getName().contains("Mlib")) {
                        // Ensure Medialib is present
                        if(JAIExt.isMedialibavailable()){
                            value.setMlibFactory(factory);
                        }
                    } else {
                        value.setFactory(factory);
                        break;
                    }
                }
            }
            return value;
        }

        /**
         * This method substitutes all the operations contained by an external {@link OperationCollection} inside the current
         * {@link OperationCollection}.
         * 
         * @param changedOps
         */
        void substituteOperations(OperationCollection changedOps) {
            // Selection of all the operations of the external OperationCollection
            Map<String, OperationItem> mapChanged = changedOps.map;
            Collection<OperationItem> items = mapChanged.values();
            // Iteration on all the new operations and registration of them
            for (OperationItem item : items) {
                substituteDescriptors(item);
                // Insert the new Item inside the map
                map.put(item.getName(), item);
            }
        }

        /**
         * This method returns a new {@link OperationCollection} containing
         * operations which are present in this collection but aren't
         * available in the external one nor in the operation groups
         * (Such as algebric group for add, subtract, divide, multiply,...)
         * 
         * @param external
         */
        OperationCollection getUniqueOperations(OperationCollection external) {
            Map<String, OperationItem> externalMap = external.map;
            // Iteration on all the new operations and registration of them
            Collection<OperationItem> externalItems = externalMap.values();
            OperationCollection uniqueOperations = new OperationCollection(registry);
            Set<String> groupingNames = JAIExt.NAME_MAPPING.keySet();
            for (OperationItem item : externalItems) {
                String name = item.getName();
                if (!map.containsKey(name) && !groupingNames.contains(name)) {
                    uniqueOperations.add(item);
                }
            }
            return uniqueOperations;
        }

        /**
         * This method substitute an old {@link OperationItem} object with a new one, if not already present.
         * 
         * @param changedOp
         */
        private void substituteDescriptors(OperationItem changedOp) {
            // Selection of the old OperationItem
            OperationItem operationItem = map.get(changedOp.getName());
            // Check if the item is present
            boolean present = operationItem != null
                    && !operationItem.getVendor().equalsIgnoreCase(changedOp.getVendor());
            // Selection of the new factory
            Object factory = changedOp.getCurrentFactory();
            // If the OperationItem is already present, then it is unregistered and the new item is registered.
            if (present) {
                // Unregistering Descriptor and Factory
                Object currentFactory = operationItem.getCurrentFactory();
                boolean registerFactory = factory == null
                        || (currentFactory != null && !currentFactory.getClass().isAssignableFrom(factory.getClass()));
                if (currentFactory != null && registerFactory) {
                    //registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, operationItem
                            //.getDescriptor().getName(), operationItem.getVendor(), currentFactory);
                }
                registry.unregisterDescriptor(operationItem.getDescriptor());
                // registering descriptor
                registry.registerDescriptor(changedOp.getDescriptor());
                // registering factory
                if (factory != null) {
                    registry.registerFactory(RenderedRegistryMode.MODE_NAME, changedOp
                            .getDescriptor().getName(), changedOp.getVendor(), changedOp
                            .getCurrentFactory());
                	//registry.setFactoryPreference(RenderedRegistryMode.MODE_NAME,
                			//changedOp.getDescriptor().getName(),
                			//changedOp.getVendor(),
                			//factory,
                			//currentFactory);
                }
            } else {
                // If the operationItem is null then it is registered
                if (operationItem == null) {
                    // registering descriptor
                    registry.registerDescriptor(changedOp.getDescriptor());
                    // registering factory
                    if (changedOp.getCurrentFactory() != null) {
                        registry.registerFactory(RenderedRegistryMode.MODE_NAME, changedOp
                                .getDescriptor().getName(), changedOp.getVendor(), changedOp
                                .getCurrentFactory());
                    }
                }
            }
        }

        /**
         * This method add a new {@link OperationItem} inside the map, replacing an old one if present.
         * 
         * @param changedOp
         */
        public void substituteSingleOp(OperationItem changedOp) {
            map.put(changedOp.getName(), changedOp);
        }

        /**
         * This method substitute the factory associated to the operation
         * 
         * @param factory
         * @param descriptorName
         * @param vendor
         */
        public void substituteFactory(Object factory, String descriptorName, String vendor) {
            OperationItem item = map.get(descriptorName);

            // Check if the Operation descriptor is present
            boolean present = item != null && item.getVendor().equalsIgnoreCase(vendor);
            // If present, the factory is set.
            if (present) {
                if (factory.getClass().getName().contains("Mlib")) {
                    item.setMlibFactory(factory);
                    item.setMlibPreference(true);
                } else {
                    item.setFactory(factory);
                    item.setMlibPreference(false);
                }
            } else {
                // Create a new Item and set it in the map
                RegistryElementDescriptor desc = registry.getDescriptor(
                        RenderedRegistryMode.MODE_NAME, descriptorName);
                // The new item is added
                if (desc != null && desc instanceof OperationDescriptor) {
                    try {
                        OperationItem itemNew = new OperationItem((OperationDescriptor) desc);
                        if (factory.getClass().getName().contains("Mlib")) {
                            itemNew.setMlibFactory(factory);
                            itemNew.setMlibPreference(true);
                        } else {
                            itemNew.setFactory(factory);
                            itemNew.setMlibPreference(false);
                        }
                        map.put(descriptorName, itemNew);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Unable to register the Factory for the following descriptor: "
                                        + descriptorName, e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unable to register the Factory for the following descriptor: "
                                    + descriptorName);
                }
            }
        }

        /**
         * Removes an {@link OperationItem} from the inner map
         * 
         * @param operationItem
         */
        public void removeSingleOp(OperationItem operationItem) {
            map.remove(operationItem.getName());
        }

        /**
         * Removes the factory from the map.
         * 
         * @param factory
         * @param descriptorName
         * @param productName
         */
        public void removeFactory(Object factory, String descriptorName, String productName) {
            OperationItem item = map.get(descriptorName);

            // Check if the Operation descriptor is present
            boolean present = item != null && item.getVendor().equalsIgnoreCase(productName);

            if (present) {
                item.setFactory(null);
                item.setMlibFactory(null);
                item.setMlibPreference(false);
            } else {
                // Create a new Item and set it in the map without defining the factory
                RegistryElementDescriptor desc = registry.getDescriptor(
                        RenderedRegistryMode.MODE_NAME, descriptorName);

                if (desc != null && desc instanceof OperationDescriptor) {
                    OperationItem itemNew = new OperationItem((OperationDescriptor) desc);
                    map.put(descriptorName, itemNew);
                } else {
                    throw new IllegalArgumentException(
                            "Unable to unregister the Factory for the following descriptor: "
                                    + descriptorName);
                }
            }
        }
    }
}
