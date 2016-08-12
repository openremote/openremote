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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.container.Container;
import org.openremote.container.web.WebResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.assets.AssetsResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.params.*;
import org.openremote.manager.shared.ngsi.SubscribeRequestV2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.manager.shared.Constants.MASTER_REALM;

public class AssetsResourceImpl extends WebResource implements AssetsResource, SubscriptionProvider {
    public static final String SUBSCRIBER_ENDPOINT_PATH = "assets/subscriber";
    private static final Logger LOG = Logger.getLogger(AssetsResourceImpl.class.getName());
    public static final int SUBSCRIPTION_REFRESH_INTERVAL = 180;
    protected URI hostUri;
    protected ObjectMapper mapper;
    protected Map<Consumer<Entity[]>, SubscribeRequestV2> subscribers = new HashMap<>();
    protected Calendar calendar = Calendar.getInstance();

    protected final AssetsService assetsService;

    public AssetsResourceImpl(AssetsService assetsService) {
        this.assetsService = assetsService;
    }

    public void configure(Container container) {
        hostUri = container.getService(WebService.class).getHostUri();
        mapper = container.JSON;
    }

    @Override
    public void stop() {

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

    @Override
    public void subscriberCallback(Entity[] entities) {
        // We get here when Orion detects a change to a currently subscribed entity
        // TODO: Notify the listeners interested in this asset

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

    @Override
    public boolean register(Consumer<Entity[]> listener, SubscriptionParams subscription) {
        // Find existing subscription for this listener (if it exists)
        SubscribeRequestV2 storedSubscription = subscribers.get(listener);

        if (storedSubscription != null) {
            String id = storedSubscription.getId();
            storedSubscription.setSubject(subscription);
            storedSubscription.setExpires(createNewExpiryDate());
            Response response = assetsService.getContextBroker().updateSubscription(id, storedSubscription);
            // TODO: Handle failed subscription update
            return response.getStatus() == 204;
        }

        // Create a new subscription
        storedSubscription = new SubscribeRequestV2();
        storedSubscription.setNotification(createNotificationParams());
        storedSubscription.setSubject(subscription);
        storedSubscription.setExpires(createNewExpiryDate());

        try {
            String str = mapper.writeValueAsString(storedSubscription);
            System.out.print(str);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Response response = assetsService.getContextBroker().createSubscription(storedSubscription);

        if (response.getStatus() == 204) {
            String locationHeader = response.getHeaderString("Location");
            String id = locationHeader.split("/")[3];
            storedSubscription.setId(id);
            subscribers.put(listener, storedSubscription);
            return true;
        }

        return false;
    }

    @Override
    public SubscriptionParams getSubscription(Consumer<Entity[]> listener) {
        SubscribeRequestV2 subscriber = subscribers.get(listener);
        return subscriber != null ? subscriber.getSubject() : null;
    }

    @Override
    public synchronized void unregister(Consumer<Entity[]> listener) {
        SubscribeRequestV2 subscriber = subscribers.get(listener);

        if (subscriber != null) {
            assetsService.getContextBroker().deleteSubscription(subscriber.getId());
            subscribers.remove(subscriber);
        }
    }

    @Override
    public String getSubscriptionCallbackUri() {
        return UriBuilder.fromUri(hostUri)
                .path(MASTER_REALM)
                .path(SUBSCRIBER_ENDPOINT_PATH)
                .build()
                .toString();
    }

    protected synchronized void notifySubscribers(Entity[] modifiedEntities) {

    }

    protected Date createNewExpiryDate() {
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, 3);
        return calendar.getTime();
    }

    protected NotificationParams createNotificationParams() {
        NotificationParams params = new NotificationParams();
        params.setHttp(new HttpParams(getSubscriptionCallbackUri()));
        return params;
    }
}
