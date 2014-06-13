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
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Implementation for using JAX-RS specification. It still need an
 * implementation runtime binding.
 *
 * @author Renato
 */
public class JaxRsGoogleAnalytics extends GoogleAnalytics {

    private Client client;

    public JaxRsGoogleAnalytics(String trackingId) {
        super(trackingId);
    }

    public JaxRsGoogleAnalytics(GoogleAnalyticsConfig config,
            String trackingId) {
        super(config, trackingId);
    }

    public JaxRsGoogleAnalytics(String trackingId, String appName,
            String appVersion) {
        super(trackingId, appName, appVersion);
    }

    public JaxRsGoogleAnalytics(GoogleAnalyticsConfig config, String trackingId,
            String appName, String appVersion) {
        super(config, trackingId, appName, appVersion);
    }

    public JaxRsGoogleAnalytics(GoogleAnalyticsConfig config,
            DefaultRequest defaultRequest) {
        super(config, defaultRequest);
    }

    @Override
    protected void createClient() {
        this.client = ClientBuilder.newClient();
        this.client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext)
                    throws IOException {
                requestContext.getHeaders().add(HttpHeaders.USER_AGENT,
                         config.getUserAgent());
            }
        });
    }

    @Override
    protected void sendRequest(GoogleAnalyticsResponse response,
            Map<String, String> postParms) throws IOException {
        Response postResponse = null;
        try {
            WebTarget target = this.client.target(config.getUrl());
            postResponse = target.request().post(Entity.form(
                    convertMultiValueMap(postParms)));

            response.setStatusCode(postResponse.getStatus());
            response.setPostedParms(postParms);

            postResponse.bufferEntity();
        } finally {
            if (postResponse != null) {
                postResponse.close();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        this.client.close();
    }

    private MultivaluedMap<String, String> convertMultiValueMap(
            Map<String, String> postParms) {
        MultivaluedMap<String, String> multivaluedMap
                = new MultivaluedHashMap<String, String>(postParms.size());

        for (Map.Entry<String, String> entry : postParms.entrySet()) {
            multivaluedMap.putSingle(entry.getKey(), entry.getValue());
        }

        return multivaluedMap;
    }

}
