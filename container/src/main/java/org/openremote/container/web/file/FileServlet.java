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
package org.openremote.container.web.file;

import io.undertow.server.handlers.resource.ResourceManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.Constants.REALM_PARAM_NAME;

public class FileServlet extends AbstractFileServlet {

    private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());

    public static final long DEFAULT_EXPIRE_SECONDS = 600; // 10 minutes
    public static final long EXPIRES_SECONDS_CACHE_JS = 60 * 60 * 24 * 14; // 14 days
    public static final Map<String, String> MIME_TYPES = new HashMap<>() {
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

    final protected boolean devMode;
    final protected ResourceManager resourceManager;
    final protected String[] requiredRoles;
    final protected Map<String, String> mimeTypes;
    final protected Map<String, Integer> mimeTypesExpireSeconds;
    final protected String[] alreadyZippedExtensions;

    public FileServlet(boolean devMode,
                              ResourceManager resourceManager,
                              String[] requiredRoles) {
        this(devMode, resourceManager, requiredRoles, MIME_TYPES, MIME_TYPES_EXPIRE_SECONDS, FILE_EXTENSIONS_ALREADY_ZIPPED);
    }

    public FileServlet(boolean devMode, ResourceManager resourceManager, String[] requiredRoles, Map<String, String> mimeTypes, Map<String, Integer> mimeTypesExpireSeconds, String[] alreadyZippedExtensions) {
        this.devMode = devMode;
        this.resourceManager = resourceManager;
        this.requiredRoles = requiredRoles;
        this.mimeTypes = mimeTypes;
        this.mimeTypesExpireSeconds = mimeTypesExpireSeconds;
        this.alreadyZippedExtensions = alreadyZippedExtensions;
    }

    public boolean isSecured() {
        return requiredRoles != null && requiredRoles.length > 0;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (isSecured()) {
            if (request.authenticate(response)) {
                boolean userHasAllRoles = true;
                for (String requiredRole : requiredRoles) {
                    if (!request.isUserInRole(requiredRole))
                        userHasAllRoles = false;
                }
                if (userHasAllRoles) {
                    LOG.fine("User has all roles to access: " + request.getPathInfo());
                    super.service(request, response);
                } else {
                    LOG.fine("User doesn't have the required roles '" + Arrays.toString(requiredRoles) + "' to access: " + request.getPathInfo());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            super.service(request, response);
        }
    }

    @Override
    protected Resource getResource(HttpServletRequest request) throws RedirectException {
        String relativePath = request.getPathInfo();
        if (relativePath == null || relativePath.isEmpty()) {
            relativePath = "";
        }
        while (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);

        String actualBase = "";

        // If secured, serve files from a sub-directory that represents the authenticated realm
        if (isSecured()) {

            String realm = request.getHeader(REALM_PARAM_NAME);

            // If we are missing the auth realm header, ignore...
            if (realm == null || realm.isEmpty()) {
                LOG.fine("Ignoring request, secured service needs request header: " + REALM_PARAM_NAME);
                return null;
            }

            actualBase = realm;

            Resource resource = getResource(actualBase);
            if (resource.getURL() == null) {
                LOG.fine("Ignoring request, missing realm content directory: " + actualBase);
                return null;
            }
        }

        Resource resource = getResource(actualBase, relativePath);

        // Handle index.html and redirect /directory to /directory/
        if (resource.getURL() == null) {
            if (request.getPathInfo() == null || !request.getPathInfo().endsWith("/")) {
                throw new RedirectException(request.getRequestURI() + "/");
            }

            resource = getResource(actualBase, relativePath + "index.html");
        }

        LOG.fine(resource.getURL() == null ? "Not serving file" : "Serving: " + resource.getURL());
        return resource;
    }

    private Resource getResource(String... path) {
        String resourcePath = String.join("/", path);
        io.undertow.server.handlers.resource.Resource resource = null;
        try {
            resource = resourceManager.getResource(resourcePath);
        } catch (IOException e) {
            LOG.log(Level.FINEST, "Failed to get resource: " + resourcePath, e);
        }
        return new ResourceImpl(resource);
    }

    @Override
    protected long getExpireTime(HttpServletRequest request, String fileName) {
        long expireTime = DEFAULT_EXPIRE_SECONDS;

        String contentType = getContentType(request, fileName);
        if (mimeTypesExpireSeconds.containsKey(contentType))
            expireTime = mimeTypesExpireSeconds.get(contentType);

        // Don't cache at all in dev mode
        expireTime = devMode ? 0 : expireTime;

        return expireTime;
    }

    @Override
    protected String getContentType(HttpServletRequest request, String fileName) {
        return coalesce(coalesce(request.getServletContext().getMimeType(fileName),
            mimeTypes.get(getExtension(fileName))), "application/octet-stream");
    }

    @Override
    protected String setContentHeaders(HttpServletRequest request, HttpServletResponse response, Resource resource, List<Range> ranges) {
        String result = super.setContentHeaders(request, response, resource, ranges);

        // If a file is already zipped, we need to set the header (yes, it's stupid, but
        // that's what happens when font experts mangle HTTP for their PBF format...)
        for (String alreadyZippedExtension : alreadyZippedExtensions) {
            if (request.getPathInfo().endsWith(alreadyZippedExtension)) {
                response.addHeader("Content-Encoding", "gzip");
                break;
            }
        }
        return result;
    }

    protected String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(i + 1) : "";
    }
}
