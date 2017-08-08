/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.openremote.container.web.jsapi;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class JSAPIServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(JSAPIServlet.class.getName());

    //corresponding to RFC 4329 this is the right MEDIA_TYPE
    private static final String JS_MEDIA_TYPE = "application/javascript";

    private static final long serialVersionUID = -1985015444704126795L;

    private Map<String, ServiceRegistry> services;

    private JSAPIWriter apiWriter = new JSAPIWriter();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOG.fine("Loading JS API servlet");
        try {
            scanResources();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        LOG.fine("JS API servlet loaded");

        // make it possible to get to us for rescanning
        ServletContext servletContext = config.getServletContext();
        servletContext.setAttribute(getClass().getName(), this);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String uri = req.getRequestURL().toString();
        uri = uri.substring(0, uri.length() - req.getServletPath().length());
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Serving JS API on: " + pathInfo);
        }
        if (this.services == null) try {
            scanResources();
        } catch (Exception e) {
            resp.sendError(503, "No Resteasy deployments found"); // FIXME should return internal error
        }

        if (this.services == null) {
            resp.sendError(503, "No Resteasy deployments found");
        }
        resp.setContentType(JS_MEDIA_TYPE);
        this.apiWriter.writeJavaScript(uri, req, resp, services);

    }

    public void scanResources() throws Exception {

        ServletConfig config = getServletConfig();
        ServletContext servletContext = config.getServletContext();
        Map<String, ResteasyDeployment> deployments = (Map<String, ResteasyDeployment>) servletContext.getAttribute(ResteasyContextParameters.RESTEASY_DEPLOYMENTS);

        if (deployments == null) return;
        synchronized (this) {
            services = new HashMap<String, ServiceRegistry>();
            for (Map.Entry<String, ResteasyDeployment> entry : deployments.entrySet()) {
                ResourceMethodRegistry registry = (ResourceMethodRegistry) entry.getValue().getRegistry();
                ResteasyProviderFactory providerFactory =
                    entry.getValue().getProviderFactory();
                ServiceRegistry service = new ServiceRegistry(null, registry, providerFactory, null);
                services.put(entry.getKey(), service);
            }
        }
    }
}
