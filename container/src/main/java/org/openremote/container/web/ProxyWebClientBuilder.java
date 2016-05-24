/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.web;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * This client will always set the configured Host header on all outgoing requests.
 * We use this to emulate a reverse proxy that "preserves" the Host header. Using a
 * request interceptor is the only reliable method for setting the Host header
 * in Apache HttpClient.
 */
public class ProxyWebClientBuilder extends ResteasyClientBuilder {

    final protected String proxyHost;
    final protected Integer proxyPort;

    public ProxyWebClientBuilder(String proxyHost, Integer proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    @Override
    protected ClientHttpEngine initDefaultEngine() {
        ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) super.initDefaultEngine();
        DefaultHttpClient httpClient = (DefaultHttpClient) engine.getHttpClient();
        httpClient.addRequestInterceptor((request, context) ->
            request.setHeader(HTTP.TARGET_HOST, new HttpHost(proxyHost, proxyPort).toHostString())
        );
        return engine;
    }
}
