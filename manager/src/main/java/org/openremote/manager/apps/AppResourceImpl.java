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
package org.openremote.manager.apps;

import org.openremote.container.web.WebResource;
import org.openremote.model.apps.AppResource;
import org.openremote.model.apps.ConsoleAppConfig;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.ValueUtil;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

public class AppResourceImpl extends WebResource implements AppResource {

    final protected ConsoleAppService consoleAppService;
    protected Map<String, Object> consoleAppInfoMap;
    protected Object consoleConfig;

    public AppResourceImpl(ConsoleAppService consoleAppService) {
        this.consoleAppService = consoleAppService;
    }

    @Override
    public String[] getApps(RequestParams requestParams) {
        try {
            return consoleAppService.getInstalled();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Response getAppInfos(RequestParams requestParams) {
        if (!Files.isDirectory(consoleAppService.consoleAppDocRoot)) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format("%s is not a directory", consoleAppService.consoleAppDocRoot))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        if (consoleAppInfoMap == null) {
            try {
                consoleAppInfoMap = Files.find(consoleAppService.consoleAppDocRoot,
                                2,
                                (filePath, fileAttr) -> filePath.getFileName().toString().endsWith("info.json") && fileAttr.isRegularFile())
                        .collect(Collectors.toMap(dir -> dir.getName(dir.getNameCount() - 2).toString(), dir -> {
                            try {
                                return ValueUtil.JSON.readValue(dir.toFile(), Object.class);
                            } catch (IOException e) {
                                throw new WebApplicationException(e);
                            }
                        }));
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }

        return Response.ok(consoleAppInfoMap, MediaType.APPLICATION_JSON).build();
    }

    @Override
    public Response getConsoleConfig(RequestParams requestParams) {
        if (!Files.isDirectory(consoleAppService.consoleAppDocRoot)) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format("%s is not a directory", consoleAppService.consoleAppDocRoot))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        if (!Files.exists(consoleAppService.consoleAppDocRoot.resolve("console_config.json"))) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }

        if (consoleConfig == null) {
            try {
                consoleConfig = ValueUtil.JSON.readValue(new File(consoleAppService.consoleAppDocRoot.resolve("console_config.json").toString()), Object.class);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }
        return Response.ok(consoleConfig, MediaType.APPLICATION_JSON).build();
    }

    @Deprecated
    protected ConsoleAppConfig getAppConfig(String realm) {
        try {
            if (!Files.isDirectory(consoleAppService.consoleAppDocRoot)) {
                return null;
            }

            return Files.list(consoleAppService.consoleAppDocRoot)
                    .filter(dir -> dir.getFileName().toString().startsWith(realm))
                    .map(dir -> {
                        try {
                            return ValueUtil.JSON.readValue(dir.toFile(), ConsoleAppConfig.class);
                        } catch (IOException e) {
                            throw new WebApplicationException(e);
                        }
                    })
                    .findFirst().orElseThrow(NotFoundException::new);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }
}
