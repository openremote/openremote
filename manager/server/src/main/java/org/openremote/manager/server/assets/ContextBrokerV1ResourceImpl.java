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
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jgroups.util.Tuple;
import org.openremote.container.Container;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.ngsi.*;
import org.openremote.manager.shared.ngsi.ContextEntity;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.manager.server.assets.ContextBrokerV1ResourceImpl.CONTEXT_PROVIDER_V1_ENDPOINT_PATH;
import static org.openremote.manager.shared.Constants.MASTER_REALM;

/**
 * This implementation only needs to support queryContext for resolving context provider requests
 */
@Path(CONTEXT_PROVIDER_V1_ENDPOINT_PATH)
public class ContextBrokerV1ResourceImpl extends AbstractContextBrokerResourceImpl implements ContextBrokerV1Resource {
    public static final String CONTEXT_PROVIDER_V1_ENDPOINT_PATH = "ngsi/v1";
    protected static final Logger LOG = Logger.getLogger(ContextBrokerV1ResourceImpl.class.getName());
    protected UriBuilder contextBrokerHostUri;
    protected URI hostUri;
    protected ObjectMapper mapper;
    protected Client httpClient;
    protected RegistrationRequestV1 providerRegistration = new RegistrationRequestV1(new ArrayList<>(), null);
    protected Timer updateTimer;

    ContextBrokerV1ResourceImpl(Client httpClient, UriBuilder contextBrokerHostUri) {
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
    public QueryResponseWrapper queryContext(EntityAttributeQuery query) {
        List<QueryResponse> responses = query.getEntities()
                .stream()
                .map(e -> getContextResponse(e, query.getAttributes().stream().map(attr -> new Tuple<String, String>(attr, null)).collect(Collectors.toList()), false))
                .filter(cr -> cr != null)
                .collect(Collectors.toList());

        if (responses.size() == 0) {
            return new QueryResponseWrapper(null, new StatusCode("404", "No context element found"));
        } else {
            return new QueryResponseWrapper(responses);
        }
    }

    @Override
    public QueryResponseWrapper updateContext(UpdateRequestV1 updateRequest) {
        return null;
    }

    @Override
    public RegistrationResponseV1 registerContext(RegistrationRequestV1 registration) {
        return null;
    }

    @Override
    public String getRegistrationCallbackUri() {
        return UriBuilder.fromUri(hostUri)
                .path(MASTER_REALM)
                .path(CONTEXT_PROVIDER_V1_ENDPOINT_PATH)
                .build()
                .toString();
    }

    public ContextBrokerV1Resource getContextBrokerV1() {
        return getContextBrokerV1(getTarget(httpClient, contextBrokerHostUri.build()));
    }

    public ContextBrokerV1Resource getContextBrokerV1(ResteasyWebTarget target) {
        return target.proxy(ContextBrokerV1Resource.class);
    }

    protected synchronized QueryResponse getContextResponse(ContextEntity entityQuery, List<Tuple<String,String>> attributesAndValues, boolean isUpdate) {
        Map.Entry<Tuple<String, String>, Map<Attribute, AssetProvider>> providerEntry = providers
                .entrySet()
                .stream()
                .filter(es -> es.getKey().getVal1().equalsIgnoreCase(entityQuery.getId()))
                .findFirst()
                .orElse(null);
        Map<Attribute, AssetProvider> providerInfo = providerEntry != null ? providerEntry.getValue() : null;
        Map<AssetProvider, List<Attribute>> attributeProviders = null;

        if (providerInfo != null) {
            if (attributesAndValues == null || attributesAndValues.isEmpty()) {
                // Get all attribute providers for these attributes
                attributeProviders = providerInfo
                        .entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(es -> es.getValue(), Collectors.mapping(es -> es.getKey(), Collectors.toList())));
            } else {
                attributeProviders = attributesAndValues
                        .stream()
                        .map(attr -> {
                            Map.Entry<Attribute, AssetProvider> entry = providerInfo
                                    .entrySet()
                                    .stream()
                                    .filter(es -> es.getKey().getName().equals(attr.getVal1()))
                                    .findFirst()
                                    .orElse(null);
                            return new Tuple<>(entry.getValue(), entry.getKey());
                        })
                        .filter(p -> p.getVal1() != null)
                        .collect(Collectors.groupingBy(Tuple::getVal1, Collectors.mapping(Tuple::getVal2, Collectors.toList())));
            }
        }

        if (attributeProviders == null || attributeProviders.isEmpty()) {
            LOG.info("No asset providers found");
            return null;
        }

        List<Attribute> attributes;

        if (isUpdate) {
            attributes = attributeProviders.entrySet()
                    .stream()
                    .filter(es -> !es.getValue().isEmpty())
                    .flatMap(es -> {
                        List<Attribute> attrs = attributesAndValues
                                .stream()
                                .filter(attrVal ->
                                        es.getValue()
                                                .stream()
                                                .anyMatch(attr -> attr.getName().equalsIgnoreCase(attrVal.getVal1())))
                                .map(attrValue -> new Attribute(attrValue.getVal1(), null, Json.create(attrValue.getVal2())))
                                .collect(Collectors.toList());
                        // Call set attributes on provider

                        es.getKey().setAssetAttributeValues(entityQuery.getId(), attrs);
                        return attrs.stream();
                    })
                    .collect(Collectors.toList());
        } else {
            attributes = attributeProviders.entrySet()
                    .stream()
                    .filter(es -> !es.getValue().isEmpty())
                    .flatMap(es -> es.getKey().getAssetAttributeValues(entityQuery.getId(), es.getValue().stream().map(Attribute::getName).collect(Collectors.toList())).stream())
                    .collect(Collectors.toList());
        }

        if (attributes.isEmpty()) {
            LOG.info("Asset provider returned no attributes when asked for the value of one or more attributes");
            return null;
        }

        return new QueryResponse(
                new ContextElement(entityQuery.getType(),entityQuery.getId(), false, attributes),
                new StatusCode("200", "OK"));
    }

    @Override
    protected synchronized void updateRegistration(String assetType, String assetId, List<Attribute> attributes) {
        // Check if this asset ID is already registered
        EntityAttributeListV1 existingReg = providerRegistration.getRegistrations()
                .stream()
                .filter(reg ->
                        reg.getEntities()
                                .stream()
                                .anyMatch(e -> e.getId().equalsIgnoreCase(assetId)))
                .findFirst()
                .orElse(null);

        if (existingReg != null) {
            ContextEntity entity = existingReg.getEntities()
                    .stream()
                    .filter(e -> e.getId().equalsIgnoreCase(assetId))
                    .findFirst()
                    .orElse(null);

            if (attributes.isEmpty()) {
                // Just remove this entity from the registration
                existingReg.getEntities().remove(entity);
            } else {
                // Check if attributes all match if so there is nothing to do otherwise
                // We have to remove from this registration and either add to another
                // or create a new one to match these attributes
                boolean allMatch = attributes
                        .stream()
                        .allMatch(attr -> existingReg.getAttributes()
                                .stream()
                                .anyMatch(regAttr ->
                                        regAttr.getName().equalsIgnoreCase(attr.getName()) &&
                                        regAttr.getType().equalsIgnoreCase(attr.getType().getName())));

                if (allMatch) {
                    // Nothing to do here
                    return;
                }

                existingReg.getEntities().remove(entity);
            }

            // Check there is at least one entity in the reg otherwise remove it
            if (existingReg.getEntities().isEmpty()) {
                providerRegistration.getRegistrations().remove(existingReg);
            }
        }

        if (!attributes.isEmpty()) {
            EntityAttributeListV1 newReg = providerRegistration.getRegistrations()
                    .stream()
                    .filter(reg ->
                            attributes
                                    .stream()
                                    .allMatch(attr -> reg.getAttributes()
                                            .stream()
                                            .anyMatch(regAttr ->
                                                    regAttr.getName().equalsIgnoreCase(attr.getName()) &&
                                                            regAttr.getType().equalsIgnoreCase(attr.getType().getName()))))
                    .findFirst()
                    .orElse(null);

            ContextEntity entity = new ContextEntity(assetType, assetId, false);

            if (newReg != null) {
                newReg.getEntities().add(entity);
            } else {
                List<ContextEntity> entities = new ArrayList<>();
                List<ContextAttribute> attrs = new ArrayList<>();
                entities.add(entity);
                attributes.forEach(attr -> attrs.add(new ContextAttribute(attr.getName(), attr.getType().getName(), false)));
                newReg = new EntityAttributeListV1(entities, attrs, getRegistrationCallbackUri());
                providerRegistration.getRegistrations().add(newReg);
            }
        }

        // NOTE: There is no documented way to remove a registration just have to set it to expire now
        // but it lingers in the DB but it does mean we can reuse it at a later stage
        String duration = providerRegistration.getRegistrations().isEmpty() ? "PT0S" : "PT" + getRefreshInterval() + "S";
        providerRegistration.setDuration(duration);

        try {
            String str = mapper.writeValueAsString(providerRegistration);
            System.out.print(str);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        refreshRegistration();
    }

    private synchronized void refreshRegistration() {
        // If registration is set to 0s then we need to provide a dummy entity - WTF?
        RegistrationRequestV1 reg = providerRegistration;

        if (reg.getDuration().equalsIgnoreCase("PT0S")) {
            reg = new RegistrationRequestV1(new ArrayList<>(), reg.getDuration(), reg.getRegistrationId());
            List<ContextEntity> dummyEntities = new ArrayList<>();
            dummyEntities.add(new ContextEntity("##############","##############", false));
            reg.getRegistrations().add(new EntityAttributeListV1(dummyEntities, null, getRegistrationCallbackUri()));
        }

        RegistrationResponseV1 response = getContextBrokerV1().registerContext(reg);
        boolean success = false;

        if (response == null) {
            LOG.severe("Context Broker context registration returned null");
        } else if (response.getErrorCode() != null) {
            LOG.severe("Context registration error: " + response.getErrorCode().getCode() + " (" + response.getErrorCode().getMessage() + ")");
        } else {
            // Store the reg ID for later reuse
            providerRegistration.setRegistrationId(response.getRegistrationId());
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
