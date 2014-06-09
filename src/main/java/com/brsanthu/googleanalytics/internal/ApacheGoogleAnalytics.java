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
import static com.brsanthu.googleanalytics.GaUtils.isNotEmpty;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
import com.brsanthu.googleanalytics.GoogleAnalyticsResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * Implementation for using Apache Http Client
 *
 * @author Renato
 */
public class ApacheGoogleAnalytics extends GoogleAnalytics {

    private CloseableHttpClient httpClient = null;

    public ApacheGoogleAnalytics(String trackingId) {
        super(trackingId);
    }

    public ApacheGoogleAnalytics(GoogleAnalyticsConfig config,
            String trackingId) {
        super(config, trackingId);
    }

    public ApacheGoogleAnalytics(String trackingId, String appName,
            String appVersion) {
        super(trackingId, appName, appVersion);
    }

    public ApacheGoogleAnalytics(GoogleAnalyticsConfig config,
            String trackingId, String appName, String appVersion) {
        super(config, trackingId, appName, appVersion);
    }

    public ApacheGoogleAnalytics(GoogleAnalyticsConfig config,
            DefaultRequest defaultRequest) {
        super(config, defaultRequest);
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void close() {
        super.close();

        try {
            httpClient.close();
        } catch (IOException e) {
            //ignore
        }
    }

    @Override
    protected void createClient() {
        PoolingHttpClientConnectionManager connManager
                = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(getDefaultMaxPerRoute(config));

        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(
                connManager);

        if (isNotEmpty(config.getUserAgent())) {
            builder.setUserAgent(config.getUserAgent());
        }

        if (isNotEmpty(config.getProxyHost())) {
            builder.setProxy(new HttpHost(config.getProxyHost(),
                    config.getProxyPort()));

            if (isNotEmpty(config.getProxyUserName())) {
                BasicCredentialsProvider credentialsProvider
                        = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(
                        config.getProxyHost(), config.getProxyPort()),
                        new UsernamePasswordCredentials(config.getProxyUserName(),
                                config.getProxyPassword()));
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        this.httpClient = builder.build();
    }

    @Override
    protected void sendRequest(GoogleAnalyticsResponse response,
            Map<String, String> postParms) throws IOException {
        CloseableHttpResponse httpResponse = null;
        try {
            HttpPost httpPost = new HttpPost(config.getUrl());
            httpPost.setEntity(new UrlEncodedFormEntity(
                    convertNameValuePair(postParms), UTF8));

            httpResponse = (CloseableHttpResponse) httpClient.execute(httpPost);
            response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            response.setPostedParms(postParms);

            EntityUtils.consumeQuietly(httpResponse.getEntity());
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }

    private List<NameValuePair> convertNameValuePair(Map<String, String> postParms) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
                postParms.size());

        for (Map.Entry<String, String> entry : postParms.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(
                    entry.getKey(), entry.getValue()));
        }

        return nameValuePairs;
    }

}
