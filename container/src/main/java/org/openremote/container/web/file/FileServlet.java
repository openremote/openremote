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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FileServlet extends AbstractFileServlet {

    private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());

    public static final long DEFAULT_EXPIRE_SECONDS = 600; // 10 minutes

    final protected boolean devMode;
    final protected File base;
    final protected String[] requiredRoles;
    final protected Map<String, String> mimeTypes;
    final protected Map<String, Integer> mimeTypesExpireSeconds;
    final protected String[] alreadyZippedExtensions;

    public FileServlet(boolean devMode, File base, String[] requiredRoles, Map<String, String> mimeTypes, Map<String, Integer> mimeTypesExpireSeconds, String[] alreadyZippedExtensions) {
        this.devMode = devMode;
        this.base = base;
        this.requiredRoles = requiredRoles;
        this.mimeTypes = mimeTypes;
        this.mimeTypesExpireSeconds = mimeTypesExpireSeconds;
        this.alreadyZippedExtensions = alreadyZippedExtensions;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (requiredRoles != null && requiredRoles.length > 0) {
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
                    LOG.fine("User doesn't have the roles to access: " + request.getPathInfo());
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
    protected File getFile(HttpServletRequest request) {
        String relativePath = request.getPathInfo();
        if (relativePath == null || relativePath.isEmpty() || "/".equals(relativePath)) {
            throw new IllegalArgumentException();
        }
        while (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);
        return new File(base, relativePath);
    }

    @Override
    protected long getExpireTime(HttpServletRequest request, File file) {
        long expireTime = DEFAULT_EXPIRE_SECONDS;
        String contentType = getContentType(request, file);
        if (mimeTypesExpireSeconds.containsKey(contentType))
            expireTime = mimeTypesExpireSeconds.get(contentType);
        expireTime = devMode ? 0 : expireTime; // Don't cache at all in dev mode
        // If the String "nocache" is in the file name, don't cache (e.g. GWT JS file)
        if (file.getName().contains("nocache"))
            expireTime = 0;
        LOG.fine("Expiring in " + expireTime + " seconds: " + file);
        return expireTime;
    }

    @Override
    protected String getContentType(HttpServletRequest request, File file) {
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }
        return coalesce(coalesce(request.getServletContext().getMimeType(file.getName()),
            mimeTypes.get(extension)), "application/octet-stream");
    }

    @Override
    protected String setContentHeaders(HttpServletRequest request, HttpServletResponse response, AbstractFileServlet.Resource resource, List<Range> ranges) {
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
}
