/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brsanthu.googleanalytics;

import static com.brsanthu.googleanalytics.GaUtils.isEmpty;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class of this library that accepts the requests from clients
 * and sends the events to Google Analytics (GA).
 *
 * Clients needs to instantiate this object with {@link GoogleAnalyticsConfig}
 * and {@link DefaultRequest}. Configuration contains sensible defaults so one
 * could just initialize using one of the convenience constructors.
 *
 * This object is ThreadSafe and it is intended that clients create one instance
 * of this for each GA Tracker Id and reuse each time an event needs to be
 * posted.
 *
 * This object contains resources which needs to be shutdown/disposed. So
 * {@link #close()} method is called to release all resources. Once close method
 * is called, this instance cannot be reused so create new instance if required.
 */
public abstract class GoogleAnalytics {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAnalytics.class);

    protected static final Charset UTF8 = Charset.forName("UTF-8");

    protected GoogleAnalyticsConfig config = null;
    protected DefaultRequest defaultRequest = null;
    protected ThreadPoolExecutor executor = null;
    protected GoogleAnalyticsStats stats = new GoogleAnalyticsStats();

    public GoogleAnalytics(String trackingId) {
        this(new GoogleAnalyticsConfig(), new DefaultRequest().trackingId(trackingId));
    }

    public GoogleAnalytics(GoogleAnalyticsConfig config, String trackingId) {
        this(config, new DefaultRequest().trackingId(trackingId));
    }

    public GoogleAnalytics(String trackingId, String appName, String appVersion) {
        this(new GoogleAnalyticsConfig(), trackingId, appName, appVersion);
    }

    public GoogleAnalytics(GoogleAnalyticsConfig config, String trackingId, String appName, String appVersion) {
        this(config, new DefaultRequest().trackingId(trackingId).applicationName(appName).applicationVersion(appVersion));
    }

    public GoogleAnalytics(GoogleAnalyticsConfig config, DefaultRequest defaultRequest) {
        if (config.isDiscoverRequestParameters() && config.getRequestParameterDiscoverer() != null) {
            config.getRequestParameterDiscoverer().discoverParameters(config, defaultRequest);
        }

        logger.info("Initializing Google Analytics with config=" + config + " and defaultRequest=" + defaultRequest);

        this.config = config;
        this.defaultRequest = defaultRequest;
        createClient();
    }

    public GoogleAnalyticsConfig getConfig() {
        return config;
    }

    public DefaultRequest getDefaultRequest() {
        return defaultRequest;
    }

    public void setDefaultRequest(DefaultRequest request) {
        this.defaultRequest = request;
    }

    @SuppressWarnings({"rawtypes"})
    public GoogleAnalyticsResponse post(GoogleAnalyticsRequest request) {
        GoogleAnalyticsResponse response = new GoogleAnalyticsResponse();
        if (!config.isEnabled()) {
            return response;
        }

        try {
            Map<String, String> postParms = new HashMap<String, String>();

            logger.debug("Processing " + request);

            //Process the parameters
            processParameters(request, postParms);

            //Process custom dimensions
            processCustomDimentionParameters(request, postParms);

            //Process custom metrics
            processCustomMetricParameters(request, postParms);

            logger.debug("Processed all parameters and sending the request " + postParms);

            sendRequest(response, postParms);
            
            if (config.isGatherStats()) {
                gatherStats(request);
            }

        } catch (Exception e) {
            if (e instanceof UnknownHostException) {
                logger.warn("Coudln't connect to Google Analytics. Internet may not be available. " + e.toString());
            } else {
                logger.warn("Exception while sending the Google Analytics tracker request " + request, e);
            }
        }

        return response;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processParameters(GoogleAnalyticsRequest request, 
            Map<String, String> postParms) {
        Map<GoogleAnalyticsParameter, String> requestParms = request.getParameters();
        Map<GoogleAnalyticsParameter, String> defaultParms = defaultRequest.getParameters();
        
        for (GoogleAnalyticsParameter parm : defaultParms.keySet()) {
            String value = requestParms.get(parm);
            String defaultValue = defaultParms.get(parm);
            if (isEmpty(value) && !isEmpty(defaultValue)) {
                requestParms.put(parm, defaultValue);
            }
        }
        
        for (GoogleAnalyticsParameter key : requestParms.keySet()) {
            postParms.put(key.getParameterName(), requestParms.get(key));
        }
    }

    /**
     * Processes the custom dimentions and adds the values to list of
     * parameters, which would be posted to GA.
     *
     * @param request
     * @param postParms
     */
    private void processCustomDimentionParameters(
            @SuppressWarnings("rawtypes") GoogleAnalyticsRequest request, 
            Map<String, String> postParms) {
        Map<String, String> customDimParms = new HashMap<String, String>();
        for (String defaultCustomDimKey : defaultRequest.customDimentions().keySet()) {
            customDimParms.put(defaultCustomDimKey, defaultRequest.customDimentions().get(defaultCustomDimKey));
        }

        @SuppressWarnings("unchecked")
        Map<String, String> requestCustomDims = request.customDimentions();
        for (String requestCustomDimKey : requestCustomDims.keySet()) {
            customDimParms.put(requestCustomDimKey, requestCustomDims.get(requestCustomDimKey));
        }

        for (String key : customDimParms.keySet()) {
            postParms.put(key, customDimParms.get(key));
        }
    }

    /**
     * Processes the custom metrics and adds the values to list of parameters,
     * which would be posted to GA.
     *
     * @param request
     * @param postParms
     */
    private void processCustomMetricParameters(
            @SuppressWarnings("rawtypes") GoogleAnalyticsRequest request, 
            Map<String, String> postParms) {
        Map<String, String> customMetricParms = new HashMap<String, String>();
        for (String defaultCustomMetricKey : defaultRequest.custommMetrics().keySet()) {
            customMetricParms.put(defaultCustomMetricKey, defaultRequest.custommMetrics().get(defaultCustomMetricKey));
        }

        @SuppressWarnings("unchecked")
        Map<String, String> requestCustomMetrics = request.custommMetrics();
        for (String requestCustomDimKey : requestCustomMetrics.keySet()) {
            customMetricParms.put(requestCustomDimKey, requestCustomMetrics.get(requestCustomDimKey));
        }

        for (String key : customMetricParms.keySet()) {
            postParms.put(key, customMetricParms.get(key));
        }
    }

    private void gatherStats(@SuppressWarnings("rawtypes") GoogleAnalyticsRequest request) {
        String hitType = request.hitType();

        if ("pageview".equalsIgnoreCase(hitType)) {
            stats.pageViewHit();

        } else if ("appview".equalsIgnoreCase(hitType)) {
            stats.appViewHit();

        } else if ("event".equalsIgnoreCase(hitType)) {
            stats.eventHit();

        } else if ("item".equalsIgnoreCase(hitType)) {
            stats.itemHit();

        } else if ("transaction".equalsIgnoreCase(hitType)) {
            stats.transactionHit();

        } else if ("social".equalsIgnoreCase(hitType)) {
            stats.socialHit();

        } else if ("timing".equalsIgnoreCase(hitType)) {
            stats.timingHit();
        }
    }

    public Future<GoogleAnalyticsResponse> postAsync(final RequestProvider requestProvider) {
        if (!config.isEnabled()) {
            return null;
        }

        Future<GoogleAnalyticsResponse> future = getExecutor().submit(new Callable<GoogleAnalyticsResponse>() {
            public GoogleAnalyticsResponse call() throws Exception {
                try {
                    @SuppressWarnings("rawtypes")
                    GoogleAnalyticsRequest request = requestProvider.getRequest();
                    if (request != null) {
                        return post(request);
                    }
                } catch (Exception e) {
                    logger.warn("Request Provider (" + requestProvider + ") thrown exception " + e.toString() + " and hence nothing is posted to GA.");
                }

                return null;
            }
        });
        return future;
    }

    @SuppressWarnings("rawtypes")
    public Future<GoogleAnalyticsResponse> postAsync(final GoogleAnalyticsRequest request) {
        if (!config.isEnabled()) {
            return null;
        }

        Future<GoogleAnalyticsResponse> future = getExecutor().submit(new Callable<GoogleAnalyticsResponse>() {
            public GoogleAnalyticsResponse call() throws Exception {
                return post(request);
            }
        });
        return future;
    }

    public void close() {
        try {
            executor.shutdown();
        } catch (Exception e) {
            //ignore
        }
    }

    protected int getDefaultMaxPerRoute(GoogleAnalyticsConfig config) {
        return Math.max(config.getMaxThreads(), 1);
    }

    protected ThreadPoolExecutor getExecutor() {
        if (executor == null) {
            executor = createExecutor(config);
        }
        return executor;
    }

    protected synchronized ThreadPoolExecutor createExecutor(
            GoogleAnalyticsConfig config) {
        return new ThreadPoolExecutor(0, config.getMaxThreads(), 5, 
                TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>(), 
                createThreadFactory());
    }

    protected ThreadFactory createThreadFactory() {
        return new GoogleAnalyticsThreadFactory(config.getThreadNameFormat());
    }

    public GoogleAnalyticsStats getStats() {
        return stats;
    }

    public void resetStats() {
        stats = new GoogleAnalyticsStats();
    }

    protected abstract void createClient();
    
    protected abstract void sendRequest(GoogleAnalyticsResponse response, 
            Map<String, String> postParms) throws IOException;
    
}

class GoogleAnalyticsThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private String threadNameFormat = null;

    public GoogleAnalyticsThreadFactory(String threadNameFormat) {
        this.threadNameFormat = threadNameFormat;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(Thread.currentThread().getThreadGroup(), r, 
                MessageFormat.format(threadNameFormat, threadNumber.getAndIncrement()), 0);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    }
}
