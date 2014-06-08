/*
 * Copyright 2014 brsanthu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import com.brsanthu.googleanalytics.internal.ApacheGoogleAnalytics;
import com.brsanthu.googleanalytics.internal.JaxRsGoogleAnalytics;
import com.brsanthu.googleanalytics.internal.JdkGoogleAnalytics;
import java.net.HttpURLConnection;

/**
 * Factory to instanciate a GoogleAnalytics implementation based on the
 * available connection library. Currently using 3 libraries:
 * <ul>
 * <li>Apache HTTP Client</li>
 * <li>JAX-RS Client Implementation (defaults to Jersey 2)</li>
 * <li>Native {@link HttpURLConnection}</li>
 * </ul>
 *
 * @author Renato
 */
public class GoogleAnalyticsFactory {

    private enum ConnectionImplType {

        APACHE, JAX_RS, JDK
    }

    private static ConnectionImplType defaultImpl;

    public static GoogleAnalytics createInstance(String trackingId) {
        switch (chooseImpl()) {
            case APACHE:
                return new ApacheGoogleAnalytics(trackingId);
            case JAX_RS:
                return new JaxRsGoogleAnalytics(trackingId);
            default: //JDK
                return new JdkGoogleAnalytics(trackingId);
        }
    }

    public static GoogleAnalytics createInstance(GoogleAnalyticsConfig config,
            String trackingId) {
        switch (chooseImpl()) {
            case APACHE:
                return new ApacheGoogleAnalytics(config, trackingId);
            case JAX_RS:
                return new JaxRsGoogleAnalytics(config, trackingId);
            default: //JDK
                return new JdkGoogleAnalytics(config, trackingId);
        }
    }

    public static GoogleAnalytics createInstance(String trackingId,
            String appName, String appVersion) {
        switch (chooseImpl()) {
            case APACHE:
                return new ApacheGoogleAnalytics(trackingId, appName, appVersion);
            case JAX_RS:
                return new JaxRsGoogleAnalytics(trackingId, appName, appVersion);
            default: //JDK
                return new JdkGoogleAnalytics(trackingId, appName, appVersion);
        }
    }

    public static GoogleAnalytics createInstance(GoogleAnalyticsConfig config,
            String trackingId, String appName, String appVersion) {
        switch (chooseImpl()) {
            case APACHE:
                return new ApacheGoogleAnalytics(config, trackingId, appName, appVersion);
            case JAX_RS:
                return new JaxRsGoogleAnalytics(config, trackingId, appName, appVersion);
            default: //JDK
                return new JdkGoogleAnalytics(config, trackingId, appName, appVersion);
        }
    }

    public static GoogleAnalytics createInstance(GoogleAnalyticsConfig config,
            DefaultRequest defaultRequest) {
        switch (chooseImpl()) {
            case APACHE:
                return new ApacheGoogleAnalytics(config, defaultRequest);
            case JAX_RS:
                return new JaxRsGoogleAnalytics(config, defaultRequest);
            default: //JDK
                return new JdkGoogleAnalytics(config, defaultRequest);
        }
    }

    private static ConnectionImplType chooseImpl() {
        if (defaultImpl == null) {
            try {
                Class.forName("org.apache.http.client.HttpClient");
                return defaultImpl = ConnectionImplType.APACHE;
            } catch (ClassNotFoundException ex) {
            }

            try {
                Class.forName("javax.ws.rs.client.Client");
                return defaultImpl = ConnectionImplType.JAX_RS;
            } catch (ClassNotFoundException ex) {
            }

            defaultImpl = ConnectionImplType.JDK;
        }

        return defaultImpl;
    }

}
