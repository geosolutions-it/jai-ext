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

import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationCollection;
import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    /** The reader/writer lock for this class. */
    private ReadWriteLock lock;

    /** The default instance of the {@link JAIExt} class */
    private static JAIExt jaiext;

    /** {@link OperationRegistry} used by the {@link JAIExt} class */
    private ConcurrentOperationRegistry registry;

    /** {@link Logger} used for Logging any excpetion or warning */
    private static final Logger LOGGER = Logger.getLogger(JAIExt.class.toString());

    private static final String JAI_EXT_VENDOR = "it.geosolutions.jaiext";

    /** Initialization of the {@link JAIExt} instance */
    public synchronized static void initJAIEXT() {
        if (jaiext == null) {
            jaiext = getJAIEXT();
        }
    }

    private synchronized static JAIExt getJAIEXT() {
        if (jaiext == null) {
            ConcurrentOperationRegistry initializeRegistry = (ConcurrentOperationRegistry) ConcurrentOperationRegistry.initializeRegistry();
            jaiext = new JAIExt(
                    initializeRegistry);
            JAI.getDefaultInstance().setOperationRegistry(initializeRegistry);
        }
        return jaiext;
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
     * Returns the current {@link ConcurrentOperationRegistry} used.
     * 
     * @return
     */
    public static ConcurrentOperationRegistry getRegistry() {
        return getJAIEXT().registry;
    }
    
    /**
     * Indicates if the operation is registered as JAI.
     * 
     * @param descriptorName
     * @return
     */
    public static boolean isJAIExtOperation(String descriptorName){
        return getJAIEXT().isJAIExtOp(descriptorName);
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
                            .equalsIgnoreCase(vendor)) {

                // Selection of the OperationItem associated to the old descriptor
                OperationCollection collection = registry.getOperationCollection();
                OperationItem oldItem = collection.get(descriptorName);

                // Getting the JAI/JAI-EXT associated OperationItem
                OperationItem newItem = registry.getOperationMap(!fromJAI).get(descriptorName);
                // Unregistering of the old operation and registering of the new one
                if (newItem != null) {
                    if (oldItem.getCurrentFactory() != null) {
                        registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                                vendor, oldItem.getCurrentFactory());
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
                    registry.registerDescriptor(newItem.getDescriptor());

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Registered " + outVendor
                                + " descriptor for the operation: " + descriptorName);
                    }

                    if (newItem.getCurrentFactory() != null) {
                        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                                newItem.getVendor(), newItem.getCurrentFactory());
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
            if (item.getVendor().equalsIgnoreCase(ConcurrentOperationRegistry.JAI_PRODUCT)
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
                registry.unregisterFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                        ConcurrentOperationRegistry.JAI_PRODUCT, oldFactory);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Unregistered" + acc1 + " Factory for the operation: "
                            + descriptorName);
                }
                registry.registerFactory(RenderedRegistryMode.MODE_NAME, descriptorName,
                        ConcurrentOperationRegistry.JAI_PRODUCT, newFactory);
                
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Registered" + acc2 + " Factory for the operation: "
                            + descriptorName);
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
        Collection<OperationItem> items = registry.getOperations();
        List<OperationItem> ops = new ArrayList<OperationItem>(items.size());
        for (OperationItem item : items) {
            ops.add(item);
        }
        return ops;
    }

    /**
     * Returns a List of the operations currently registered which are present in the JAI-EXT API:
     * 
     * @return
     */
    private List<OperationItem> getJAIEXTAvailableOps() {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            OperationCollection items = registry.getOperationCollection();
            Set<String> jaiextKeys = registry.getOperationMap(false).keySet();
            List<OperationItem> ops = new ArrayList<OperationItem>(jaiextKeys.size());
            for (String key : jaiextKeys) {
                ops.add(items.get(key));
            }
            return ops;
        } finally {
            writeLock.unlock();
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
            return operationItem.getVendor().equalsIgnoreCase(JAI_EXT_VENDOR);
        } finally {
            readLock.unlock();
        }
    }
}
