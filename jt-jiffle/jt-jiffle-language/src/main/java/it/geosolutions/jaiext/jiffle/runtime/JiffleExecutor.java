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
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package it.geosolutions.jaiext.jiffle.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A multi-threaded, event-driven executor service for Jiffle scripts. Jiffle
 * objects can be submitted to this class to execute rather than the client 
 * working with a JiffleRuntime instance directly. This is preferable
 * for computationally demanding tasks because the scripts run in a separate
 * thread to the client. For multiple tasks, the executor can be set up to
 * run all tasks concurrently in separate threads (if system resources permit),
 * or run up to N tasks concurrently. If necessary, a task will be held in a 
 * queue while waiting for a thread. Setting N to 1 gives the option of serial
 * execution.
 * <p>
 * The client can optionally follow progress during execution with a 
 * {@link it.geosolutions.jaiext.jiffle.runtime.JiffleProgressListener}. When the task is
 * finished its status and results can be retrieved via {@link JiffleEventListener}.
 * <p>
 * Example of use:
 * 
 * <pre><code>
 * // assuming the executor is a class field in this example
 * executor = new JiffleExecutor();
 *
 * executor.addEventListener(new JiffleEventListener() {
 *
 *     public void onCompletionEvent(JiffleEvent ev) {
 *         myCompletionMethod(ev);
 *     }
 *
 *     public void onFailureEvent(JiffleEvent ev) {
 *         myFailureMethod(ev);
 *     }
 * });
 * </code></pre>
 * 
 * Now we can build Jiffle objects and submit them to the executor as shown here:
 * 
 * <pre><code>
 * String script = "dest = src > 10 ? src : null;" ;
 *
 * Map&lt;String, Jiffle.ImageRole&gt; imageParams = CollectionFactory.map();
 * imageParams.put("src", Jiffle.ImageRole.SOURCE);
 * imageParams.put("dest", Jiffle.ImageRole.DEST);
 *
 * Jiffle jiffle = new Jiffle(script, imageParams);
 *
 * // Map with the source and destination images
 * RenderedImage sourceImg = ...
 * WritableRenderedImage destImg = ...
 * Map&lt;String, RenderedImage&gt; images = CollectionFactory.map();
 * images.put("src", sourceImg);
 * images.put("dest", destImg);
 *
 * // Submit the task to the executor
 * executor.submit(jiffle, images, new MyProgressListener());
 * </code></pre>
 * 
 * When the script has completed the event listener will be notified and
 * the results can be retrieved:
 * 
 * <pre><code>
 * private void myCompletionMethod(JiffleEvent ev) {
 *     // Get and display the result image
 *     JiffleExecutorResult result = ev.getResult();
 *     RenderedImage img = result.getImages().get("dest");
 *     ...
 * }
 *
 * private void myFailureMethod(JiffleEvent ev) {
 *     System.out.println("Bummer...");
 * }
 * </code></pre>
 * 
 * Once the application has finished with th executor it should call one of
 * the shutdown methods which terminate the task and polling threads.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */public class JiffleExecutor {
    
    private static final Logger LOGGER = Logger.getLogger(JiffleExecutor.class.getName());

    // Templates for log INFO messages
    private static final String TASK_SUBMITTED_MSG = "Task {0} submitted";
    private static final String TASK_SUCCESS_MSG = "Task {0} completed";
    private static final String TASK_FAILURE_MSG = "Task {0} failed";
    
    
    /** 
     * The default interval for polling tasks to check for
     * completion (20 mS)
     */
    public static final long DEFAULT_POLLING_INTERVAL = 20L;

    private long pollingInterval = DEFAULT_POLLING_INTERVAL;
    
    /* Provides unique job ID values across all executor instances. */
    private static final AtomicInteger jobID = new AtomicInteger(0);

    private final Object _lock = new Object();
    
    private final ExecutorService taskService;
    private final ScheduledExecutorService pollingService;
    private final ScheduledExecutorService shutdownService;
    private final ExecutorCompletionService<JiffleExecutorResult> completionService;
    
    private final List<JiffleEventListener> listeners;
    
    private boolean isPolling;
    private int numTasksRunning;
    
    /* Used by constructors when setting up the task service. */
    private static enum ThreadPoolType {
        CACHED,
        FIXED;
    }

    
    /**
     * Creates an executor with default settings. There is no upper limit 
     * on the number of concurrent tasks. A cached thread pool will be used
     * which recycles existing threads where possible.
     */
    public JiffleExecutor() {
        this(ThreadPoolType.CACHED, -1);
    }
    
    
    /**
     * Creates an executor that can have, at most,{@code maxTasks} 
     * running concurrently, with further tasks being placed in a queue.
     * 
     * @param maxTasks the maximum number of concurrent tasks
     */
    public JiffleExecutor(int maxTasks) {
        this(ThreadPoolType.FIXED, maxTasks);
    }
    
    /**
     * Private constructor for common setup.
     * 
     * @param type type of thread pool to use
     * 
     * @param maxJobs maximum number of concurrent jobs (ignored if
     *        {@code type} is not {@code FIXED}
     */
    private JiffleExecutor(ThreadPoolType type, int maxJobs) {
        switch (type) {
            case CACHED:
                taskService = Executors.newCachedThreadPool();
                break;
                
            case FIXED:
                taskService = Executors.newFixedThreadPool(maxJobs);
                break;
                
            default:
                throw new IllegalArgumentException("Bad arg to private JiffleExecutor constructor");
        }
        
        completionService = new ExecutorCompletionService<JiffleExecutorResult>(taskService);
        
        pollingService = Executors.newSingleThreadScheduledExecutor(
                new DaemonThreadFactory(Thread.NORM_PRIORITY, "executor-poll"));
        
        shutdownService = Executors.newSingleThreadScheduledExecutor(
                new DaemonThreadFactory(Thread.NORM_PRIORITY, "executor-shutdown"));
        
        listeners = new ArrayList<JiffleEventListener>();
        
        isPolling = false;
    }
    
    /**
     * Sets the polling interval for task completion. JiffleExecutor uses a 
     * separate thread to poll tasks for completion (either success
     * or failure) at a fixed interval. The interval can only be changed 
     * prior to submitting the first task. After that, any calls to this
     * method will result in a warning message being logged and the new
     * value being ignored.
     * 
     * @param millis interval between task polling in milliseconds; values
     *        less than 1 are ignored
     * 
     * @see #DEFAULT_POLLING_INTERVAL
     */
    public void setPollingInterval(long millis) {
        synchronized (_lock) {
            if (isPolling) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING,
                            "Request to change polling interval ignored");
                }
            } else if (millis < 1) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Invalid polling interval ignored: {0}", millis);
                }
            } else {
                pollingInterval = millis;
            }
        }
    }
    
    /**
     * Gets the interval in milliseconds for polling task completion.
     * 
     * @return polling interval
     */
    public long getPollingInterval() {
        synchronized (_lock) {
            return pollingInterval;
        }
    }
            
    
    /**
     * Adds an event listener.
     * 
     * @param listener the listener
     * 
     * @see JiffleEvent
     */
    public void addEventListener(JiffleEventListener listener) {
        synchronized(_lock) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes an event listener.
     * 
     * @param listener the listener
     * 
     * @return {@code true} if the listener was removed;
     *         {@code false} if it was not registered with this executor
     */
    public boolean removeEventListener(JiffleEventListener listener) {
        synchronized(_lock) {
            return listeners.remove(listener);
        }
    }
    
    /**
     * Checks if a particular listener is registered with this executor.
     * 
     * @param listener the listener
     * 
     * @return {@code true} if the listener has already been added;
     *         {@code false} otherwise
     */
    public boolean isListening(JiffleEventListener listener) {
        synchronized(_lock) {
            return listeners.contains(listener);
        }
    }
    
    /**
     * Submits an {@code JiffleDirectRuntime} object for execution. Depending 
     * on existing tasks and the number of threads available to the executor 
     * there could be a delay before the task starts. Clients can receive 
     * notification via an optional progress listener.
     * <p>
     * 
     * @param runtime the run-time instance to execute
     * 
     * @param progressListener an optional progress listener (may be {@code null})
     * 
     * @return the job ID that can be used to query progress
     */
    public int submit(JiffleDirectRuntime runtime,
            JiffleProgressListener progressListener) {

        synchronized (_lock) {
            if (taskService.isShutdown()) {
                throw new IllegalStateException("Submitting task after executor shutdown");
            }

            startPolling();
            int id = jobID.getAndIncrement();

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, TASK_SUBMITTED_MSG, id);
            }

            numTasksRunning++ ;

            completionService.submit(new JiffleExecutorTask(
                    this,
                    id,
                    runtime,
                    progressListener));

            return id;
        }
    }
    
    /**
     * Requests that the executor shutdown after completing any tasks
     * already submitted. Control returns immediately to the client.
     */
    public void shutdown() {
        synchronized(_lock) {
            taskService.shutdown();
            stopPolling(false);
        }
    }
    
    /**
     * Requests that the executor shutdown after completing any tasks
     * already submitted. Control returns to the calling thread after
     * the executor has shutdown or the time out period has elapsed, 
     * whichever comes first.
     * 
     * @param timeOut time-out period
     * @param unit time unit
     * 
     * @return {@code true} if the executor has shutdown; {@code false} if
     *         the time-out period elapsed or the thread was interrupted
     */
    public boolean shutdownAndWait(long timeOut, TimeUnit unit) {
        synchronized (_lock) {
            boolean success = false;
            taskService.shutdown();
            stopPolling(false);
            
            try {
                success = taskService.awaitTermination(timeOut, unit);
                
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            return success;
        }
    }
    
    /**
     * Attempts to shutdown the executor immediately.
     */
    public void shutdownNow() {
        taskService.shutdownNow();
        stopPolling(true);
    }
        
    /**
     * Starts the polling service if it is not already running.
     */
    private void startPolling() {
        if (!isPolling) {
            pollingService.scheduleWithFixedDelay(new PollingTask(),
                    pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
            isPolling = true;
        }
    }
    
    /**
     * Stops the polling service.
     * 
     * @param immediate whether to stop the service immediately or wait
     *        for any running tasks to complete
     */
    private void stopPolling(boolean immediate) {
        if (immediate) {
            pollingService.shutdown();
            return;
        }

        shutdownService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (numTasksRunning == 0) {
                    pollingService.shutdown();
                    shutdownService.shutdown();
                }
            }
        }, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
    }
    
    private class PollingTask implements Runnable {
        public void run() {
            try {
                Future<JiffleExecutorResult> future = completionService.poll();
                if (future != null) {
                    JiffleExecutorResult result = future.get();
                    numTasksRunning-- ;
                    
                    if (result.isCompleted()) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, TASK_SUCCESS_MSG, result.getTaskID());
                        }
                        notifySuccess(result);
                        
                    } else {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, TASK_FAILURE_MSG, result.getTaskID());
                        }
                        notifyFailure(result);
                    }
                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private void notifySuccess(JiffleExecutorResult result) {
            for (JiffleEventListener listener : listeners) {
                listener.onCompletionEvent(new JiffleEvent(result));
            }
        }

        private void notifyFailure(JiffleExecutorResult result) {
            for (JiffleEventListener listener : listeners) {
                listener.onFailureEvent(new JiffleEvent(result));
            }
        }
    }
    
}
