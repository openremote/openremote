package org.openremote.manager.server.assets;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.jboss.resteasy.annotations.Form;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.assets.AssetsResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
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
    public JsonArray getEntities(@Form RequestParams requestParams, @Form EntityListParams entityListParams) {
        return assetsService.getContextBroker().getEntities(entityListParams);
    }

    @Override
    public void postEntity(@Form RequestParams requestParams, Entity entity) {
        checkSuccessResponse(assetsService.getContextBroker().postEntity(entity));
    }

    @Override
    public JsonObject getEntity(@Form RequestParams requestParams, String entityId, @Form EntityParams entityParams) {
        return assetsService.getContextBroker().getEntity(entityId, entityParams);
    }

    @Override
    public void deleteEntity(@Form RequestParams requestParams, String entityId) {
        checkSuccessResponse(assetsService.getContextBroker().deleteEntity(entityId));
    }

    @Override
    public void putEntity(@Form RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        checkSuccessResponse(assetsService.getContextBroker().putEntity(entityId, entity));
    }

    @Override
    public void patchEntity(@Form RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        checkSuccessResponse(assetsService.getContextBroker().patchEntity(entityId, entity));
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
