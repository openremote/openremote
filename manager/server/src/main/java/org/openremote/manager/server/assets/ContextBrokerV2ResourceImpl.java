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
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jgroups.util.Tuple;
import org.openremote.container.Container;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.ngsi.*;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;
import org.openremote.manager.shared.ngsi.params.SubscriptionParams;

import javax.ws.rs.BeanParam;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.manager.server.assets.ContextBrokerV2ResourceImpl.CONTEXT_PROVIDER_V2_ENDPOINT_PATH;
import static org.openremote.manager.shared.Constants.MASTER_REALM;

/**
 * This implementation only needs to support queryContext for resolving context provider requests
 */
@Path(CONTEXT_PROVIDER_V2_ENDPOINT_PATH)
public class ContextBrokerV2ResourceImpl extends AbstractContextBrokerResourceImpl implements ContextBrokerV2Resource {
    public static final String CONTEXT_PROVIDER_V2_ENDPOINT_PATH = "ngsi/v2";
    protected static final Logger LOG = Logger.getLogger(ContextBrokerV2ResourceImpl.class.getName());
    protected UriBuilder contextBrokerHostUri;
    protected URI hostUri;
    protected ObjectMapper mapper;
    protected Client httpClient;
    protected Timer updateTimer;
    protected BatchRegistrationRequestV2 providerRegistration;

    ContextBrokerV2ResourceImpl(Client httpClient, UriBuilder contextBrokerHostUri) {
        this.httpClient = httpClient;
        this.contextBrokerHostUri = contextBrokerHostUri;
    }

    @Override
    public void configure(Container container) throws Exception {
        hostUri = container.getService(WebService.class).getHostUri();
        mapper = container.JSON;
        container.getService(WebService.class).getApiSingletons().add(this);
    }

    @Override
    public void stop() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
    }

    @Override
    public Entity[] getEntities(@BeanParam EntityListParams entityListParams) {
        String[] matchedIds = resolveIds(entityListParams);
        EntityParams params = new EntityParams();
        params.attributes(entityListParams.attributes);
        return Arrays
                .stream(matchedIds)
                .map(id -> getEntity(id, params))
                .filter(e -> e != null)
                .toArray(size -> new Entity[size]);
    }

    @Override
    public Entity getEntity(String entityId, @BeanParam EntityParams entityParams) {
        return getEntityResponse(entityId, Arrays.asList(entityParams.attributes));
    }

    /*
    *
    * BELOW METHODS ARE NOT IMPLEMENTED AS THEY SHOULD NEVER BE NEEDED FOR CONTEXT PROVIDER
    *
    */

    @Override
    public EntryPoint getEntryPoint() {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response postEntity(Entity entity) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response deleteEntity(String entityId) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response putEntityAttributes(String entityId, Entity entity) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response patchEntityAttributes(String entityId, Entity entity) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public SubscribeRequestV2[] getSubscriptions() {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response createSubscription(SubscribeRequestV2 subscription) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response updateSubscription(String subscriptionId, SubscribeRequestV2 subscription) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response deleteSubscription(String subscriptionId) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response createRegistration(RegistrationRequestV2 registration) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public SubscribeRequestV2 getSubscription(String subscriptionId) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response updateRegistration(String registrationId, Duration duration) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response deleteRegistration(String registrationId) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public Response batchRegistration(BatchRegistrationRequestV2 registrations) {
        throw new NotSupportedException("NGSI v2 Provider doesn't support this method");
    }

    @Override
    public String getRegistrationCallbackUri() {
        return UriBuilder.fromUri(hostUri)
                .path(MASTER_REALM)
                .path(CONTEXT_PROVIDER_V2_ENDPOINT_PATH)
                .build()
                .toString();
    }

    public ContextBrokerV2Resource getContextBroker() {
        return getContextBroker(getTarget(httpClient, contextBrokerHostUri.build()));
    }

    public ContextBrokerV2Resource getContextBroker(ResteasyWebTarget target) {
        return target.proxy(ContextBrokerV2Resource.class);
    }

    protected synchronized String[] resolveIds(EntityListParams entityListParams) {
        // TODO: Add support for non entity ID/type based queries
        Stream<Map.Entry<Tuple<String, String>, Map<Attribute, AssetProvider>>> filteredProviders = providers
                .entrySet()
                .stream();

        if (entityListParams.id != null && entityListParams.id.length > 0) {
            // Filter by ID (exact)
            filteredProviders
                    .filter(
                            es -> Arrays
                                .stream(entityListParams.id)
                                .anyMatch(id -> id.equalsIgnoreCase(es.getKey().getVal1())));
        } else if (entityListParams.idPattern != null) {
            // Filter by ID (regex)
            Pattern idRegex = Pattern.compile(entityListParams.idPattern);
            filteredProviders = filteredProviders
                    .filter(
                        es -> idRegex.matcher(es.getKey().getVal1()).matches()
                    );
        }

        if (entityListParams.type != null && entityListParams.type.length > 0) {
            // Further filter by type (exact)
            filteredProviders = filteredProviders
                    .filter(
                            es -> Arrays
                                .stream(entityListParams.type)
                                .anyMatch(type -> type.equalsIgnoreCase(es.getKey().getVal2())));
        } else if (entityListParams.typePattern != null) {
            // Further filter by type (regex)
            Pattern typeRegex = Pattern.compile(entityListParams.typePattern);
            filteredProviders = filteredProviders.filter(
                    es -> typeRegex.matcher(es.getKey().getVal2()).matches()
            );
        }

        return filteredProviders.map(es -> es.getKey().getVal1()).toArray(size -> new String[size]);
    }

    protected synchronized Entity getEntityResponse(String entityId, List<String> attributeNames) {
        Map<AssetProvider, List<String>> attributeProviders = null;

        Map.Entry<Tuple<String, String>, Map<Attribute, AssetProvider>> providerEntry = providers
                .entrySet()
                .stream()
                .filter(es -> es.getKey().getVal1().equalsIgnoreCase(entityId))
                .findFirst()
                .orElse(null);

        if (providerEntry != null) {
            if (attributeNames == null || attributeNames.isEmpty()) {
                // Get all attribute providers
                attributeProviders = providerEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(es -> es.getValue(), Collectors.mapping(es -> es.getKey().getName(), Collectors.toList())));
            } else {
                attributeProviders = attributeNames
                        .stream()
                        .map(attr -> {
                            Map.Entry<Attribute, AssetProvider> entry = providerEntry
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    .filter(es -> es.getKey().getName().equals(attr))
                                    .findFirst()
                                    .orElse(null);
                            return new Tuple<>(entry.getValue(), attr);
                        })
                        .filter(p -> p.getVal1() != null)
                        .collect(Collectors.groupingBy(Tuple::getVal1, Collectors.mapping(Tuple::getVal2, Collectors.toList())));
            }
        }

        if (attributeProviders == null || attributeProviders.isEmpty()) {
            LOG.info("No asset providers found");
            return null;
        }

        List<Attribute> attributes = attributeProviders.entrySet()
                .stream()
                .filter(es -> !es.getValue().isEmpty())
                .flatMap(es -> es.getKey().getAssetAttributeValues(entityId, es.getValue()).stream())
                .collect(Collectors.toList());

        if (attributes.isEmpty()) {
            LOG.info("Asset provider returned no attributes when asked for the value of one or more attributes");
            return null;
        }

        // Construct the entity object
        Entity entity = new Entity();
        entity.setType(providerEntry.getKey().getVal2());
        entity.setId(providerEntry.getKey().getVal1());
        attributes.forEach(entity::addAttribute);

        return entity;
    }

    @Override
    // TODO: Update registration code once NGSI9 v2 is finalised
    protected synchronized void updateRegistration(String assetType, String assetId, List<Attribute> attributes) {
        // Check if this asset ID is already registered
        RegistrationRequestV2 existingReg = providerRegistration.getRegistrations()
                .stream()
                .filter(reg ->
                        reg.getSubject().getEntities()
                                .stream()
                                .anyMatch(e -> e.getId().equalsIgnoreCase(assetId)))
                .findFirst()
                .orElse(null);

        if (existingReg != null) {
            ContextEntity entity = existingReg.getSubject().getEntities()
                    .stream()
                    .filter(e -> e.getId().equalsIgnoreCase(assetId))
                    .findFirst()
                    .orElse(null);

            if (attributes.isEmpty()) {
                // Just remove this entity from the registration
                existingReg.getSubject().getEntities().remove(entity);
            } else {
                // Check if attributes all match if so there is nothing to do otherwise
                // We have to remove from this registration and either add to another
                // or create a new one to match these attributes
                boolean allMatch = attributes
                        .stream()
                        .allMatch(attr -> existingReg.getSubject().getAttributes()
                                .stream()
                                .anyMatch(regAttr ->
                                        regAttr.getName().equalsIgnoreCase(attr.getName())));

                if (allMatch) {
                    // Nothing to do here
                    return;
                }

                existingReg.getSubject().getEntities().remove(entity);
            }

            // Check there is at least one entity in the reg otherwise remove it
            if (existingReg.getSubject().getEntities().isEmpty()) {
                providerRegistration.getRegistrations().remove(existingReg);
            }
        }

        RegistrationRequestV2 newReg = providerRegistration.getRegistrations()
                .stream()
                .filter(reg ->
                        attributes
                                .stream()
                                .allMatch(attr -> reg.getSubject().getAttributes()
                                        .stream()
                                        .anyMatch(regAttr ->
                                                regAttr.getName().equalsIgnoreCase(attr.getName()))))
                .findFirst()
                .orElse(null);

        ContextEntity entity = new ContextEntity(assetType, assetId, false);

        if (newReg != null) {
            newReg.getSubject().getEntities().add(entity);
        } else {
            List<ContextEntity> entities = new ArrayList<>();
            List<ContextAttribute> attrs = new ArrayList<>();
            entities.add(entity);
            attributes.forEach(attr -> attrs.add(new ContextAttribute(attr.getName())));
            EntityAttributeListV2 subject = new EntityAttributeListV2(entities,attrs);
            newReg = new RegistrationRequestV2(subject, getRegistrationCallbackUri(), null, null);
            newReg.setDuration("PT" + getRefreshInterval() + "S");
            providerRegistration.getRegistrations().add(newReg);
        }

        if (providerRegistration.getRegistrations().isEmpty()) {
            // Remove the entire registration
            providerRegistration.setActionType(ActionType.DELETE);
        } else {
            // Is this an update or create - docs not clear at this time
            providerRegistration.setActionType(ActionType.UPDATE);
        }

        try {
            String str = mapper.writeValueAsString(providerRegistration);
            System.out.print(str);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        refreshRegistration();
    }

    // TODO: Update registration code once NGSI9 v2 is finalised
    private synchronized void refreshRegistration() {
        Response response = getContextBroker().batchRegistration(providerRegistration);
        boolean success = false;

        if (response == null) {
            LOG.severe("Context Broker context registration returned null");
        } else if (response.getStatus() != 200) {
            LOG.severe("Context registration error: " + response.getStatus() + " (" + response.getEntity() + ")");
        } else {
            // Store the reg IDs for later reuse
            String[] ids = (String[])response.getEntity();
            IntStream.range(0, ids.length)
                    .forEach(index -> providerRegistration.getRegistrations().get(index).setRegistrationId(ids[index]));
            success = true;
        }

        if (updateTimer != null) {
            updateTimer.cancel();
        }

        if (!providerRegistration.getRegistrations().isEmpty()) {
            updateTimer = new Timer();
            // TODO: Handle the reg update failure more gracefully
            int period = success ? (getRefreshInterval()-10)*1000 : 20000;
            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    refreshRegistration();
                }
            }, period, period);
        }
    }
}
