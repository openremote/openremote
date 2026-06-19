/*
 * Copyright 2026 OpenRemote Inc.
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
package org.openremote.manager.asset;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.CustomAssetTypeDefinition;
import org.openremote.model.asset.CustomAssetTypeResource;
import org.openremote.model.http.RequestParams;

public class CustomAssetTypeResourceImpl extends ManagerWebResource implements CustomAssetTypeResource {

    protected final CustomAssetTypeStorageService storageService;

    public CustomAssetTypeResourceImpl(
        TimerService timerService,
        ManagerIdentityService identityService,
        CustomAssetTypeStorageService storageService
    ) {
        super(timerService, identityService);
        this.storageService = storageService;
    }

    @Override
    public CustomAssetTypeDefinition[] getAll(RequestParams requestParams) {
        requireSuperAdmin("list custom asset type definitions");
        return storageService.findAll().toArray(new CustomAssetTypeDefinition[0]);
    }

    @Override
    public CustomAssetTypeDefinition get(RequestParams requestParams, String name) {
        requireSuperAdmin("read custom asset type definitions");
        CustomAssetTypeDefinition definition = storageService.find(name);
        if (definition == null) {
            throw new NotFoundException("Custom asset type not found: " + name);
        }
        return definition;
    }

    @Override
    public CustomAssetTypeDefinition create(RequestParams requestParams, CustomAssetTypeDefinition definition) {
        requireSuperAdmin("create custom asset type definitions");
        try {
            return storageService.persist(definition);
        } catch (IllegalArgumentException ex) {
            throw isDuplicate(ex) ? conflict(ex) : badRequest(ex);
        }
    }

    @Override
    public CustomAssetTypeDefinition update(
        RequestParams requestParams,
        String name,
        CustomAssetTypeDefinition definition
    ) {
        requireSuperAdmin("update custom asset type definitions");
        requireMatchingName(name, definition);
        if (storageService.find(name) == null) {
            throw new NotFoundException("Custom asset type not found: " + name);
        }
        try {
            return storageService.merge(definition);
        } catch (IllegalArgumentException ex) {
            throw isCompatibilityConflict(ex) ? conflict(ex) : badRequest(ex);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String name) {
        requireSuperAdmin("delete custom asset type definitions");
        try {
            storageService.delete(name);
        } catch (IllegalStateException ex) {
            throw conflict(ex);
        }
    }

    @Override
    public void validate(RequestParams requestParams, String name, CustomAssetTypeDefinition definition) {
        requireSuperAdmin("validate custom asset type definitions");
        requireMatchingName(name, definition);
        try {
            storageService.validate(definition);
        } catch (IllegalArgumentException ex) {
            throw badRequest(ex);
        }
    }

    @Override
    public long getUsage(RequestParams requestParams, String name) {
        requireSuperAdmin("read custom asset type usage");
        return storageService.getUsageCount(name);
    }

    protected void requireSuperAdmin(String action) {
        if (!isSuperUser()) {
            throw new ForbiddenException("Only super admin can " + action);
        }
    }

    protected void requireMatchingName(String name, CustomAssetTypeDefinition definition) {
        if (definition == null || definition.getName() == null || !definition.getName().equals(name)) {
            throw new BadRequestException("Custom asset type path and payload names must match");
        }
    }

    protected boolean isDuplicate(IllegalArgumentException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("already exists");
    }

    protected boolean isCompatibilityConflict(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return message != null
            && (
                message.startsWith("Cannot ")
                    || message.startsWith("New attributes on in-use custom asset types")
            );
    }

    protected WebApplicationException badRequest(RuntimeException ex) {
        return new WebApplicationException(ex.getMessage(), ex, Response.Status.BAD_REQUEST);
    }

    protected WebApplicationException conflict(RuntimeException ex) {
        return new WebApplicationException(ex.getMessage(), ex, Response.Status.CONFLICT);
    }
}
