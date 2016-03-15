package org.openremote.manager.server.contextbroker;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.jboss.resteasy.annotations.Form;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.contextbroker.ContextBrokerResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.ngsi.AbstractEntity;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.EntryPoint;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class ContextBrokerResourceImpl extends WebResource implements ContextBrokerResource {

    private static final Logger LOG = Logger.getLogger(ContextBrokerResourceImpl.class.getName());

    protected final ContextBrokerService contextBrokerService;

    public ContextBrokerResourceImpl(ContextBrokerService contextBrokerService) {
        this.contextBrokerService = contextBrokerService;
    }

    @Override
    public EntryPoint getEntryPoint(@Form RequestParams requestParams) {
        return contextBrokerService.getContextBroker().getEntryPoint();
    }

    @Override
    public Entity[] getEntities(@Form RequestParams requestParams) {
        return contextBrokerService.getContextBroker().getEntities();
    }

    @Override
    public Response postEntity(@Form RequestParams requestParams, Entity entity) {
        return checkSuccessResponse(contextBrokerService.getContextBroker().postEntity(entity));
    }

    @Override
    public Entity getEntity(@Form RequestParams requestParams, String entityId) {
        return contextBrokerService.getContextBroker().getEntity(entityId);
    }

    @Override
    public Response deleteEntity(@Form RequestParams requestParams, String entityId) {
        return checkSuccessResponse(contextBrokerService.getContextBroker().deleteEntity(entityId));
    }

    @Override
    public Response putEntity(@Form RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        return checkSuccessResponse(contextBrokerService.getContextBroker().putEntity(entityId, entity));
    }

    @Override
    public Response patchEntity(@Form RequestParams requestParams, String entityId, Entity entity) {
        entity = new Entity(fixForUpdate(entity.getJsonObject()));
        return checkSuccessResponse(contextBrokerService.getContextBroker().patchEntity(entityId, entity));
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
