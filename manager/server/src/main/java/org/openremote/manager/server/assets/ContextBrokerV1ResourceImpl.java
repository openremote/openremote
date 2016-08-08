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
import org.jboss.marshalling.Pair;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
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
    protected ContextRegistrationV1 providerRegistration = new ContextRegistrationV1(new ArrayList<>(), null);
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
    public ContextResponseWrapper queryContext(EntityAttributeQuery query) {
        List<ContextResponse> responses = query.getEntities()
                .stream()
                .map(e -> getContextResponse(e, query.getAttributes()))
                .filter(cr -> cr != null)
                .collect(Collectors.toList());

        if (responses.size() == 0) {
            return new ContextResponseWrapper(null, new StatusCode("404", "No context element found"));
        } else {
            return new ContextResponseWrapper(responses);
        }
    }

    @Override
    public ContextRegistrationV1Response registerContext(ContextRegistrationV1 registration) {
        return null;
    }

    @Override
    public String getContextProviderUri() {
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

    protected synchronized ContextResponse getContextResponse(ContextEntity entityQuery, List<String> attributeNames) {
        Map.Entry<Pair<String, String>, Map<Attribute, AssetProvider>> providerEntry = providers
                .entrySet()
                .stream()
                .filter(es -> es.getKey().getA().equalsIgnoreCase(entityQuery.getId()))
                .findFirst()
                .orElse(null);
        Map<Attribute, AssetProvider> providerInfo = providerEntry != null ? providerEntry.getValue() : null;
        Map<AssetProvider, List<String>> attributeProviders = null;

        if (providerInfo != null) {
            if (attributeNames == null || attributeNames.isEmpty()) {
                // Get all attribute providers
                attributeProviders = providerInfo
                        .entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(es -> es.getValue(), Collectors.mapping(es -> es.getKey().getName(), Collectors.toList())));
            } else {
                attributeProviders = attributeNames
                        .stream()
                        .map(attr -> {
                            Map.Entry<Attribute, AssetProvider> entry = providerInfo
                                    .entrySet()
                                    .stream()
                                    .filter(es -> es.getKey().getName().equals(attr))
                                    .findFirst()
                                    .orElse(null);
                            return new Pair<>(entry.getValue(), attr);
                        })
                        .filter(p -> p.getA() != null)
                        .collect(Collectors.groupingBy(Pair::getA, Collectors.mapping(Pair::getB, Collectors.toList())));
            }
        }

        if (attributeProviders == null || attributeProviders.isEmpty()) {
            LOG.info("No asset providers found");
            return null;
        }

        List<Attribute> attributes = attributeProviders.entrySet()
                .stream()
                .filter(es -> !es.getValue().isEmpty())
                .flatMap(es -> es.getKey().getAssetAttributeValues(entityQuery.getId(), es.getValue()).stream())
                .collect(Collectors.toList());

        if (attributes.isEmpty()) {
            LOG.info("Asset provider returned no attributes when asked for the value of one or more attributes");
            return null;
        }

        return new ContextResponse(
                new ContextElement(entityQuery.getType(),entityQuery.getId(), false, attributes),
                new StatusCode("200", "OK"));
    }

    @Override
    protected synchronized void updateRegistration(String assetType, String assetId, List<Attribute> attributes) {
        // Check if this asset ID is already registered
        EntityAttributeRegistrationV1 existingReg = providerRegistration.getRegistrations()
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
            EntityAttributeRegistrationV1 newReg = providerRegistration.getRegistrations()
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
                newReg = new EntityAttributeRegistrationV1(entities, attrs, getContextProviderUri());
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
        ContextRegistrationV1 reg = providerRegistration;

        if (reg.getDuration().equalsIgnoreCase("PT0S")) {
            reg = new ContextRegistrationV1(new ArrayList<>(), reg.getDuration(), reg.getRegistrationId());
            List<ContextEntity> dummyEntities = new ArrayList<>();
            dummyEntities.add(new ContextEntity("##############","##############", false));
            reg.getRegistrations().add(new EntityAttributeRegistrationV1(dummyEntities, null, getContextProviderUri()));
        }

        ContextRegistrationV1Response response = getContextBrokerV1().registerContext(reg);
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
