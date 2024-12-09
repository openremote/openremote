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
package org.openremote.manager.map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.container.web.WebResource;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.http.RequestParams;
import org.openremote.model.manager.MapConfig;
import org.openremote.model.map.MapResource;
import org.openremote.model.util.ValueUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;

public class MapResourceImpl extends WebResource implements MapResource {

    protected final MapService mapService;
    protected final ManagerIdentityService identityService;

    public MapResourceImpl(MapService mapService, ManagerIdentityService identityService) {
        this.mapService = mapService;
        this.identityService = identityService;
    }

    @Override
    public Object saveSettings(RequestParams requestParams, MapConfig mapConfig) {
        ObjectNode mapSourcesJson = ValueUtil.JSON.valueToTree(mapConfig.sources);
        if (mapSourcesJson.has("external")) {
            try {
                final String url = mapSourcesJson
                    .get("external")
                    .get("url")
                    .textValue();

                if (!url.contains("/{z}/{x}/{y}")) {
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
                for (String realm : mapConfig.options.keySet()) {
                    if (realm == "default") {
                        realm = "master";
                    }
                    String externalEndpoint = this.uriInfo.getBaseUri() + realm + "/map/external/tile";
                    if (url.contains(externalEndpoint)) {
                        throw new WebApplicationException(Response.Status.BAD_REQUEST);
                    }
                }
            } catch (NullPointerException exception) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            };
        }

        return mapService.saveMapConfig(mapConfig);
    }

    @Override
    public ObjectNode getSettings(RequestParams requestParams) {
        return mapService.getMapSettings(
            getRequestRealmName(),
            requestParams.getExternalSchemeHostAndPort()
        );
    }

    @Override
    public ObjectNode getSettingsJs(RequestParams requestParams) {
        return mapService.getMapSettingsJs(
            getAuthenticatedRealmName(),
            requestParams.getExternalSchemeHostAndPort()
        );
    }

    @Override
    public byte[] getTile(int zoom, int column, int row) {
        byte[] tile = mapService.getMapTile(zoom, column, row);
        if (tile != null) {
            return tile;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public void getExternalTile(@Context HttpServletRequest request, @Context HttpServletResponse response, int zoom, int column, int row) {
        URI uri = mapService.getExternalMapTileUri(zoom, column, row);

        if (uri == null) {
            throw new WebApplicationException(Response.Status.PROXY_AUTHENTICATION_REQUIRED);
        }
        
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                connection.setRequestProperty(headerName, request.getHeader(headerName));
            }

            int statusCode = connection.getResponseCode();
            response.setStatus(statusCode);

            String contentType = connection.getContentType();
            response.setContentType(contentType);

            String encoding = connection.getContentEncoding();
            InputStream inputStream = connection.getInputStream();

            try (OutputStream outputStream = response.getOutputStream()) {
                if ("gzip".equalsIgnoreCase(encoding)) {
                    response.setHeader("Content-Encoding", "gzip");
                }
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

        } catch (IOException error) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
