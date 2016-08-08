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
package org.openremote.manager.server.assets;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.assets.AssetsResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class AssetsResourceImpl extends WebResource implements AssetsResource {

    private static final Logger LOG = Logger.getLogger(AssetsResourceImpl.class.getName());

    protected final AssetsService assetsService;

    public AssetsResourceImpl(AssetsService assetsService) {
        this.assetsService = assetsService;
    }

    @Override
    public Entity[] getEntities(RequestParams requestParams, EntityListParams entityListParams) {
        return assetsService.getContextBroker().getEntities(entityListParams);
    }

    @Override
    public void postEntity(RequestParams requestParams, Entity entity) {
        checkSuccessResponse(assetsService.getContextBroker().postEntity(entity));
    }

    @Override
    public Entity getEntity(RequestParams requestParams, String entityId, EntityParams entityParams) {
        return assetsService.getContextBroker().getEntity(entityId, entityParams);
    }

    @Override
    public void deleteEntity(RequestParams requestParams, String entityId) {
        checkSuccessResponse(assetsService.getContextBroker().deleteEntity(entityId));
    }

    @Override
    public void putEntityAttributes(RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        checkSuccessResponse(assetsService.getContextBroker().putEntityAttributes(entityId, entity));
    }

    @Override
    public void patchEntityAttributes(RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        checkSuccessResponse(assetsService.getContextBroker().patchEntityAttributes(entityId, entity));
    }

    protected Response checkSuccessResponse(Response response) {
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
            return response;
        throw new WebApplicationException(
            "Failure response from context broker: " + response.getStatusInfo(), response.getStatus()
        );
    }

    /**
     * You must strip out id and type attributes of an entity if it's a PATCH or POST.
     * Why doesn't the NGSI server just ignore those fields? FU, that's why.
     */
    protected JsonObject fixForUpdate(JsonObject original) {
        JsonObject copy = Json.parse(original.toJson());
        if (copy.hasKey("id"))
            copy.remove("id");
        if (copy.hasKey("type"))
            copy.remove("type");
        return copy;
    }
}
