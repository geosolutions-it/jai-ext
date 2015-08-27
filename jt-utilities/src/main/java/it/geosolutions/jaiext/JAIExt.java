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

import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationCollection;
import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * Utility class used for registering and unregistering JAI and JAI-EXT operations.
 * 
 * @author Nicola Lagomarsini - GeoSolutions
 * 
 */
public class JAIExt {

    private static final JAI DEFAULT_INSTANCE = JAI.getDefaultInstance();

    public static final String OPERATION_CONST_NAME = "operationConst";

    public static final String ALGEBRIC_NAME = "algebric";

    public static final String STATS_NAME = "Stats";

    /** The reader/writer lock for this class. */
    private ReadWriteLock lock;

    /** The default instance of the {@link JAIExt} class */
    private static JAIExt jaiext;

    /** {@link OperationRegistry} used by the {@link JAIExt} class */
    private ConcurrentOperationRegistry registry;

    /** {@link Logger} used for Logging any excpetion or warning */
    private static final Logger LOGGER = Logger.getLogger(JAIExt.class.toString());

    /** Set containing the mapping from/to jai/jaiext operation names*/
    static final HashMap<String, Set<String>> NAME_MAPPING;

    static{
        // Instantiating the map
        NAME_MAPPING = new HashMap<String, Set<String>>();
        // Add statistics names
        Set<String> stats = new TreeSet<String>();
        stats.add("Extrema");
        stats.add("Histogram");
        stats.add("Mean");
        NAME_MAPPING.put(STATS_NAME, stats);
        // Add operation Consts names
        Set<String> opConst = new TreeSet<String>();
        opConst.add("AddConst");
        opConst.add("DivideByConst");
        opConst.add("MultiplyConst");
        opConst.add("SubtractConst");
        opConst.add("SubtractFromConst");
        opConst.add("AndConst");
        opConst.add("OrConst");
        opConst.add("XorConst");
        NAME_MAPPING.put(OPERATION_CONST_NAME, opConst);
        // Add Algebric names
        Set<String> algebric = new TreeSet<String>();
        algebric.add("Add");
        algebric.add("Divide");
        algebric.add("Multiply");
        algebric.add("Subtract");
        algebric.add("Absolute");
        algebric.add("And");
        algebric.add("Or");
        algebric.add("Xor");
        algebric.add("Exp");
        algebric.add("Log");
        algebric.add("Invert");
        algebric.add("SubtractFrom");
        algebric.add("DivideInto");
        NAME_MAPPING.put(ALGEBRIC_NAME, algebric);
    }
    
    /**
     * {@code true} if JAI media lib is available.
     */
    private static final boolean mediaLibAvailable;
    static {

        // do we wrappers at hand?
        boolean mediaLib = false;
        Class<?> mediaLibImage = null;
        try {
            mediaLibImage = Class.forName("com.sun.medialib.mlib.Image");
        } catch (ClassNotFoundException e) {
        }
        mediaLib = (mediaLibImage != null);

        // npw check if we either wanted to disable explicitly and if we installed the native libs
        if (mediaLib) {
            try {
                // explicit disable
                mediaLib = !Boolean.getBoolean("com.sun.media.jai.disableMediaLib");
                // native libs installed
                if (mediaLib) {
                    final Class<?> mImage = mediaLibImage;
                    mediaLib = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            try {
                                // get the method
                                final Class<?> params[] = {};
                                Method method = mImage.getDeclaredMethod("isAvailable", params);

                                // invoke
                                final Object paramsObj[] = {};

                                final Object o = mImage.newInstance();
                                return (Boolean) method.invoke(o, paramsObj);
                            } catch (Throwable e) {
                                return false;
                            }
                        }
                    });
                }
            } catch (Throwable e) {
                // Because the property com.sun.media.jai.disableMediaLib isn't
                // defined as public, the users shouldn't know it. In most of
                // the cases, it isn't defined, and thus no access permission
                // is granted to it in the policy file. When JAI is utilized in
                // a security environment, AccessControlException will be thrown.
                // In this case, we suppose that the users would like to use
                // medialib accelaration. So, the medialib won't be disabled.

                // The fix of 4531501

                mediaLib = false;
            }
        }
        mediaLibAvailable = mediaLib;
    }
 
    /** 
     * Initialization of the {@link JAIExt} instance.
     * Default behavior is using JAIExt operations
     */
    public synchronized static void initJAIEXT() {
        initJAIEXT(true);
    }

    /** Initialization of the {@link JAIExt} instance */
    public synchronized static void initJAIEXT(boolean useJaiExtOps) {
        initJAIEXT(useJaiExtOps, false);
    }

    /** Initialization of the {@link JAIExt} instance */
    public synchronized static void initJAIEXT(boolean useJaiExtOps, boolean forceReInit) {
        if (jaiext == null || forceReInit) {
            jaiext = getJAIEXT(useJaiExtOps, forceReInit);
        }
    }

    private synchronized static JAIExt getJAIEXT() {
        return getJAIEXT(true, false);
    }

    private synchronized static JAIExt getJAIEXT(boolean useJaiExtOps, boolean forceReInit) {
        if (jaiext == null || forceReInit) {
            ConcurrentOperationRegistry initializeRegistry = (ConcurrentOperationRegistry) ConcurrentOperationRegistry.initializeRegistry(useJaiExtOps);
            jaiext = new JAIExt(initializeRegistry);
            DEFAULT_INSTANCE.setOperationRegistry(initializeRegistry);
        }
        return jaiext;
    }
    
    public static void registerAllOperations(boolean jaiextOperations) {
        JAIExt je = getJAIEXT();

        if(jaiextOperations){
            List<OperationItem> jaiextOps = getJAIEXTOperations();
            for(OperationItem item : jaiextOps){
                String itemName = item.getName();
                String jaiExtName = getOperationName(itemName);
                if(itemName != jaiExtName){
                    OperationItem itemJE = getRegistry().getOperationCollection().get(jaiExtName);
                    if(itemJE == null){
                        itemJE = searchForOperation(jaiExtName, true);
                        je.registerOperation(itemJE);
                    }
                }else{
                    registerJAIEXTDescriptor(itemName);
                }
            }
        } else{
            List<OperationItem> jaiOps = getJAIOperations();
            for(OperationItem item : jaiOps){
                String itemName = item.getName();
                String jaiExtName = getJAIExtName(itemName);
                if(itemName != jaiExtName){
                    OperationItem itemJE = getRegistry().getOperationCollection().get(jaiExtName);
                    if(itemJE != null){
                        je.unregisterOperation(itemJE);
                    }
                }else{
                    registerJAIDescriptor(itemName);
                }
            }
        }
    }
    
    private static OperationItem searchForOperation(String jaiExtName, boolean jaiext) {
        JAIExt je = getJAIEXT();
        List<OperationItem> operations = jaiext ? getJAIEXTOperations() : je.getJAIAvailableOps();
        for(OperationItem it : operations){
            if(it.getName().equalsIgnoreCase(jaiExtName)){
                return it;
            }
        }
        return null;
    }

    public static void registerOperations(Set<String> operations, boolean jaiext) {
        JAIExt je = getJAIEXT();
        for (String opName : operations) {
            String jaiextName = getJAIExtName(opName);
            if (jaiext) {
                if (!isJAIExtOperation(jaiextName)) {
                    registerJAIEXTDescriptor(jaiextName);
                }
            } else if (jaiextName.equalsIgnoreCase(opName) && isJAIExtOperation(opName) && !NAME_MAPPING.containsKey(jaiextName)) {
                registerJAIDescriptor(opName);
            } else if(!jaiextName.equalsIgnoreCase(opName) || NAME_MAPPING.containsKey(jaiextName)){
                OperationItem it = searchForOperation(jaiextName, true);
                je.unregisterOperation(it);
            }
        }
    }
    
    /**
     * This method unregister a JAI operation and register the related JAI-EXT one
     * 
     * @param descriptorName
     */
    public static void registerJAIEXTDescriptor(String descriptorName) {
        getJAIEXT().registerOp(descriptorName, true);
    }

	/**
     * This method unregister a JAI-EXT operation and register the related JAI one
     * 
     * @param descriptorName
     */
    public static void registerJAIDescriptor(String descriptorName) {
    	getJAIEXT().registerOp(descriptorName, false);
    }

    /**
     * This method sets/unsets the netive acceleration for a JAI operation
     * 
     * @param descriptorName
     * @param accelerated
     */
    public static void setJAIAcceleration(String descriptorName, boolean accelerated) {
    	getJAIEXT().setAcceleration(descriptorName, accelerated);
    }

    /**
     * Gets a List of all the operations currently registered.
     * 
     * @return
     */
    public static List<OperationItem> getOperations() {
        return getJAIEXT().getItems();
    }

    /**
     * Get a List of the available JAI-EXT operations
     * 
     * @return
     */
    public static List<OperationItem> getJAIEXTOperations() {
        return getJAIEXT().getJAIEXTAvailableOps();
    }

    /**
     * Get a List of the available JAI operations
     * 
     * @return
     */
    public static List<OperationItem> getJAIOperations() {
        return getJAIEXT().getJAIAvailableOps();
    }
    
    /**
     * Returns the current {@link ConcurrentOperationRegistry} used.
     * 
     * @return
     */
    public static ConcurrentOperationRegistry getRegistry() {
        return getJAIEXT().registry;
    }
    
    /**
     * Utility method for substituting the JAI operation names with the JAI-EXT ones, if they are present
     * 
     * @param name
     * @return
     */
    public static String getOperationName(String name) {
        if (isJAIExtOperation(STATS_NAME)
                && NAME_MAPPING.get(STATS_NAME).contains(name)) {
            return STATS_NAME;
        } else if (isJAIExtOperation(ALGEBRIC_NAME)
                && NAME_MAPPING.get(ALGEBRIC_NAME).contains(name)) {
            return ALGEBRIC_NAME;
        } else if (isJAIExtOperation(OPERATION_CONST_NAME)
                && NAME_MAPPING.get(OPERATION_CONST_NAME).contains(name)) {
            return OPERATION_CONST_NAME;
        }

        return name;
    }
    
    /**
     * Utility method for substituting the JAI operation names with the JAI-EXT ones
     * 
     * @param name
     * @return
     */
    public static String getJAIExtName(String name) {
        if (NAME_MAPPING.get(STATS_NAME).contains(name)) {
            return STATS_NAME;
        } else if (NAME_MAPPING.get(ALGEBRIC_NAME).contains(name)) {
            return ALGEBRIC_NAME;
        } else if (NAME_MAPPING.get(OPERATION_CONST_NAME).contains(name)) {
            return OPERATION_CONST_NAME;
        }

        return name;
    }
    
    /**
     * Utility method for substituting the JAIExt operation name with the related JAI one/ones
     * 
     * @param name
     * @return
     */
    public static List<String> getJAINames(String name) {
        List<String> names = new ArrayList<String>();
        if(NAME_MAPPING.containsKey(name)){
            names.addAll(NAME_MAPPING.get(name));
        }else {
            names.add(name);
        }
        return names;
    }
    
    /**
     * Indicates if the operation is registered as JAI-EXT.
     * 
     * @param descriptorName
     * @return
     */
    public static boolean isJAIExtOperation(String descriptorName){
        return getJAIEXT().isJAIExtOp(descriptorName);
    }
    
    /**
     * Indicates if the operation is registered as JAI.
     * 
     * @param descriptorName
     * @return
     */
    public static boolean isJAIAPI(String descriptorName){
        return getJAIEXT().isJAIAvailableOperation(descriptorName);
    }

    private JAIExt(ConcurrentOperationRegistry registry) {
        this.registry = registry;
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Registers the operation defined by the descriptor name. The boolean indicates if the initial operation was a JAI or a JAI-EXT one.
     * 
     * @param descriptorName
     * @param fromJAI
     */
    private void registerOp(String descriptorName, boolean fromJAI) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            // Selection of the input vendor name
            String vendor = fromJAI ? (descriptorName.equalsIgnoreCase("Null") ? ConcurrentOperationRegistry.JAI_PRODUCT_NULL
                    : ConcurrentOperationRegistry.JAI_PRODUCT)
                    : ConcurrentOperationRegistry.JAIEXT_PRODUCT;
            // Check if the old descriptor is present
            OperationDescriptor op = (OperationDescriptor) registry.getDescriptor(
                    RenderedRegistryMode.MODE_NAME, descriptorName);

            boolean registered = false;

            String inVendor = fromJAI ? "JAI" : "JAI-EXT";
            String outVendor = fromJAI ? "JAI-EXT" : "JAI";
            // Ensure that the descriptor has the same vendor from that indicated by the vendor, string.
            // Otherwise it is already registered or it is not a JAI/JAI-EXT operation.
            if (op != null
                    && op.getResourceBundle(null)
                            .getString(ConcurrentOperationRegistry.VENDOR_NAME)
                            .equalsIgnoreCase(vendor) || op == null) {

                // Selection of the OperationItem associated to the old descriptor
                OperationCollection collection = registry.getOperationCollection();
                OperationItem oldItem = collection.get(descriptorName);

                // Getting the JAI/JAI-EXT associated OperationItem
                OperationItem newItem = registry.getOperationMap(!fromJAI).get(descriptorName);
                // Unregistering of the old operation and registering of the new one
                if (newItem != null) {
                    if(op != null){
                        if (oldItem != null && oldItem.getCurrentFactory() != null) {
                            //registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                                    //vendor, oldItem.getCurrentFactory());
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Unregistered Factory for the operation: "
                                        + descriptorName);
                            }
                        }
                        registry.unregisterDescriptor(op);
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "Unregistered " + inVendor
                                    + " descriptor for the operation: " + descriptorName);
                        }
                    }


                    registry.registerDescriptor(newItem.getDescriptor());

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Registered " + outVendor
                                + " descriptor for the operation: " + descriptorName);
                    }

                    Object newFactory = newItem.getCurrentFactory();
					if (newFactory != null && registry.getFactory(RenderedRegistryMode.MODE_NAME, newItem.getDescriptor().getName()) == null) {
                        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                                newItem.getVendor(), newFactory);
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "Registered Factory for the operation: "
                                    + descriptorName);
                        }
                    }
                    registered = true;
                }
            }
            // Log the operation
            if (!registered) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to register the descriptor related "
                            + "to the following operation: " + descriptorName);
                }
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Registered operation: " + descriptorName);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * This method sets or unsets the acceleration for the input operation if it is a JAI one.
     * 
     * @param descriptorName
     * @param accelerated
     */
    private void setAcceleration(String descriptorName, boolean accelerated) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            // Selection of the OperationItem associated to the descriptor
            OperationCollection collection = registry.getOperationCollection();
            OperationItem item = collection.get(descriptorName);
            // Selection of the old JAI OperationItem.
            OperationItem jaiItem = registry.getOperationMap(true).get(descriptorName);
            if (item != null && item.getVendor().equalsIgnoreCase(ConcurrentOperationRegistry.JAI_PRODUCT)
                    && jaiItem.getFactory() != null && jaiItem.getMlibFactory() != null) {
                // Definition of the old and the new factories
                Object oldFactory = null;
                Object newFactory = null;
                // Acceleration String
                String acc1 = null;
                String acc2 = null;
                // Setting/ Unsetting of the acceleration
                if (accelerated) {
                    oldFactory = jaiItem.getFactory();
                    newFactory = jaiItem.getMlibFactory();
                    acc1 = " Accelerated";
                    acc2 = "";
                } else {
                    newFactory = jaiItem.getFactory();
                    oldFactory = jaiItem.getMlibFactory();
                    acc1 = "";
                    acc2 = " Accelerated";
                }
                // Registration step
                List orderedFactoryList = registry.getOrderedFactoryList(
                        RenderedRegistryMode.MODE_NAME, descriptorName,
                        ConcurrentOperationRegistry.JAI_PRODUCT);
                if (orderedFactoryList != null && orderedFactoryList.contains(oldFactory)) {
                    // registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                    // ConcurrentOperationRegistry.JAI_PRODUCT, oldFactory);
                    registry.unregisterDescriptor(jaiItem.getDescriptor());
                    registry.registerDescriptor(jaiItem.getDescriptor());
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Unregistered" + acc1
                                + " Factory for the operation: " + descriptorName);
                    }
                }
                orderedFactoryList = registry.getOrderedFactoryList(RenderedRegistryMode.MODE_NAME,
                        descriptorName, ConcurrentOperationRegistry.JAI_PRODUCT);
                if (orderedFactoryList == null || !orderedFactoryList.contains(newFactory)) {
                    registry.registerFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                            ConcurrentOperationRegistry.JAI_PRODUCT, newFactory);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Registered" + acc2
                                + " Factory for the operation: " + descriptorName);
                    }
                }
            }else{
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to set acceleration for following operation: " + descriptorName);
                }
            }
        } finally {
            writeLock.unlock();
        }

    }

    /**
     * Returns a List of the operations currently registered
     * 
     * @return
     */
    private List<OperationItem> getItems() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            Collection<OperationItem> items = registry.getOperations();
            List<OperationItem> ops = new ArrayList<OperationItem>(items.size());
            ops.addAll(items);
            return ops;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns a List of the operations currently registered which are present in the JAI-EXT API:
     * 
     * @return
     */
    private List<OperationItem> getJAIEXTAvailableOps() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            //OperationCollection items = registry.getOperationCollection();
            Map<String, OperationItem> operationMap = registry.getOperationMap(false);
            List<OperationItem> ops = new ArrayList<OperationItem>(operationMap.size());
            ops.addAll(operationMap.values());
            //for (String key : jaiextKeys) {
                //ops.add(items.get(key));
            //}
            return ops;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Returns a List of the operations currently registered which are present in the JAI-EXT API:
     * 
     * @return
     */
    private List<OperationItem> getJAIAvailableOps() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            //OperationCollection items = registry.getOperationCollection();
            Map<String, OperationItem> operationMap = registry.getOperationMap(true);
            List<OperationItem> ops = new ArrayList<OperationItem>(operationMap.size());
            ops.addAll(operationMap.values());
            //for (String key : jaiKeys) {
                //ops.add(items.get(key));
            //}
            return ops;
        } finally {
            readLock.unlock();
        }
    }
    
    private boolean isJAIExtOp(String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            OperationCollection items = registry.getOperationCollection();
            OperationItem operationItem = items.get(descriptorName);
            if(operationItem == null){
                return false;
            }
            return operationItem.getVendor().equalsIgnoreCase(ConcurrentOperationRegistry.JAIEXT_PRODUCT);
        } finally {
            readLock.unlock();
        }
    }

    private boolean isJAIAvailableOperation(String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            String jaiextName = getJAIExtName(descriptorName);
            if (!jaiextName.equalsIgnoreCase(descriptorName)) {
                return true;
            }
            List<OperationItem> ops = getJAIAvailableOps();
            for (OperationItem item : ops) {
                String vendor = item.getVendor();
                if ((vendor.equalsIgnoreCase(ConcurrentOperationRegistry.JAI_PRODUCT) || vendor
                        .equalsIgnoreCase(ConcurrentOperationRegistry.JAI_PRODUCT_NULL))
                        && item.getName().equalsIgnoreCase(descriptorName)) {
                    return true;
                }
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    private void unregisterOperation(OperationItem op) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            if (op == null) {
                return;
            }
            //Object factory = op.getFactory();
            OperationDescriptor descriptor = op.getDescriptor();
            //registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, op.getName(),
                    //op.getVendor(), factory);
            registry.unregisterDescriptor(descriptor);
        } finally {
            writeLock.unlock();
        }
    }

    private void registerOperation(OperationItem op) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            if (op == null) {
                return;
            }
            Object factory = op.getFactory();
            OperationDescriptor descriptor = op.getDescriptor();
            registry.registerDescriptor(descriptor);
            registry.registerFactory(RenderedRegistryMode.MODE_NAME, op.getName(), op.getVendor(),
                    factory);
        } finally {
            writeLock.unlock();
        }
    }

    public static boolean isMedialibavailable() {
        return mediaLibAvailable;
    }
}
