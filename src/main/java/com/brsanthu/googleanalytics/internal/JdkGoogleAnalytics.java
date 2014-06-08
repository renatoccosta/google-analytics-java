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
package com.brsanthu.googleanalytics.internal;

import com.brsanthu.googleanalytics.DefaultRequest;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
import com.brsanthu.googleanalytics.GoogleAnalyticsResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Implementation for using native JDK {@link HttpURLConnection}
 *
 * @author Renato
 */
public class JdkGoogleAnalytics extends GoogleAnalytics {

    public JdkGoogleAnalytics(String trackingId) {
        super(trackingId);
    }

    public JdkGoogleAnalytics(GoogleAnalyticsConfig config, 
            String trackingId) {
        super(config, trackingId);
    }

    public JdkGoogleAnalytics(String trackingId, String appName, 
            String appVersion) {
        super(trackingId, appName, appVersion);
    }

    public JdkGoogleAnalytics(GoogleAnalyticsConfig config, String trackingId, 
            String appName, String appVersion) {
        super(config, trackingId, appName, appVersion);
    }

    public JdkGoogleAnalytics(GoogleAnalyticsConfig config, 
            DefaultRequest defaultRequest) {
        super(config, defaultRequest);
    }

    @Override
    protected void sendRequest(GoogleAnalyticsResponse response, Map<String, String> postParms) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
