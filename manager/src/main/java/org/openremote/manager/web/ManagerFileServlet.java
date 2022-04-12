/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.web;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.openremote.model.Container;
import org.openremote.container.web.file.FileServlet;
import org.openremote.container.web.file.GzipResponseFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagerFileServlet extends FileServlet {

    private static final Logger LOG = Logger.getLogger(ManagerFileServlet.class.getName());

    public static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {
        {
            put("pbf", "application/x-protobuf");
            put("woff2", "font/woff2");
            put("wsdl", "application/xml");
            put("xsl", "text/xsl");
        }
    };

    public static final String[] FILE_EXTENSIONS_ALREADY_ZIPPED = {
        ".pbf"
    };

    public static final Map<String, Integer> MIME_TYPES_EXPIRE_SECONDS = new HashMap<String, Integer>() {
        {
            put("image/png", 60 * 60 * 12); // 12 hours
            put("image/jpg", 60 * 60 * 12); // 12 hours
            put("text/html", 0); // No cache
            put("text/xml", 1800);
            put("text/css", 1800);
            put("text/javascript", 60 * 60 * 12); // 12 hours
            put("application/javascript", 60 * 60 * 12); // 12 hours
            put("application/json", 1800);
            put("application/font-woff2", 60 * 60 * 12); // 12 hours
            put("application/x-protobuf", 60 * 60 * 12); // 12 hours
        }
    };

    public static final String[] MIME_TYPES_TO_ZIP = {
        "text/plain",
        "text/html",
        "text/xml",
        "text/css",
        "text/javascript",
        "text/csv",
        "text/rtf",
        "application/xml",
        "application/xhtml+xml",
        "application/javascript",
        "application/json",
        "image/svg+xml"
    };

    public ManagerFileServlet(boolean devMode,
                              File base,
                              String[] requiredRoles) {
        super(devMode, base, requiredRoles, MIME_TYPES, MIME_TYPES_EXPIRE_SECONDS, FILE_EXTENSIONS_ALREADY_ZIPPED);
    }

    public static DeploymentInfo createDeploymentInfo(boolean devMode, String contextPath, Path docRoot, String[] requiredRoles) {
        if (!Files.isDirectory(docRoot)) {
            throw new IllegalArgumentException("Document root does not exist: " + docRoot.toAbsolutePath());
        }

        ManagerFileServlet fileServlet = new ManagerFileServlet(devMode, docRoot.toFile(), requiredRoles);
        ServletInfo servletInfo = Servlets.servlet("Manager File Servlet", FileServlet.class, () -> new ImmediateInstanceHandle<>(fileServlet));
        servletInfo.addMapping("/*");

        Filter gzipFilter = new GzipResponseFilter(MIME_TYPES_TO_ZIP);
        FilterInfo gzipFilterInfo = Servlets.filter("Gzip Filter", GzipResponseFilter.class, () -> new ImmediateInstanceHandle<>(gzipFilter))
                .setAsyncSupported(true);

        return new DeploymentInfo()
            .setDeploymentName(contextPath + " File Servlet Deployment")
            .setContextPath(contextPath)
            .addServlet(servletInfo)
            .addFilter(gzipFilterInfo)
            .addFilterUrlMapping(gzipFilterInfo.getName(), "/*", DispatcherType.REQUEST)
            .setClassLoader(Container.class.getClassLoader());
    }

    /**
     * This handler corrects the request path, using the matching group 1 of the pattern as the request path.
     */
    public static HttpHandler wrapHandler(HttpHandler wrapped, Pattern requestPattern) {
        return exchange -> {
            String requestPath = exchange.getRequestPath();
            Matcher staticMatcher = requestPattern.matcher(requestPath);
            if (staticMatcher.matches()) {
                LOG.finer("Serving static resource: " + requestPath);
                String remaining = staticMatcher.group(1);
                String relativePath = remaining == null || remaining.length() == 0 ? "/" : remaining;
                exchange.setRelativePath(relativePath);
                wrapped.handleRequest(exchange);
            }
        };
    }
}
