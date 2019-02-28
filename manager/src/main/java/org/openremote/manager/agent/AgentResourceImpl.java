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
package org.openremote.manager.agent;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentResource;
import org.openremote.model.asset.agent.AgentStatusEvent;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgentResourceImpl extends ManagerWebResource implements AgentResource {

    private static final Logger LOG = Logger.getLogger(AgentResourceImpl.class.getName());
    protected final AgentService agentService;
    protected final AssetStorageService assetStorageService;

    public AgentResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             AssetStorageService assetStorageService,
                             AgentService agentService) {
        super(timerService, identityService);
        this.agentService = agentService;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public ProtocolDescriptor[] getSupportedProtocols(RequestParams requestParams, String agentId) {
        return withAgentConnector(agentId, agentConnector -> {
            LOG.finer("Asking connector '" + agentConnector.value.getClass().getSimpleName() + "' for protocol configurations");
            return agentConnector.value.getProtocolDescriptors(agentConnector.key);
        });
    }

    @Override
    public List<AgentStatusEvent> getAgentStatus(RequestParams requestParams, String agentId) {

        if (!identityService.getIdentityProvider().canSubscribeWith(
            getAuthContext(),
            new TenantFilter<AgentStatusEvent>() {
                @Override
                public boolean apply(AgentStatusEvent event) {
                    return event.getRealm().equals(getAuthenticatedTenant().getId());
                }
            }
        )) {
            LOG.fine("Forbidden access for user '" + getUsername() + "', can't get agent status of: " + agentId);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        List<AgentStatusEvent> result = withAgentConnector(agentId, agentConnector -> {
            LOG.finer("Asking connector '" + agentConnector.value.getClass().getSimpleName() + "' for connection status");
            return agentConnector.value.getConnectionStatus(agentConnector.key);
        });

        // Compress response (the request attribute enables the interceptor)
        request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

        return result;
    }

    @Override
    public Map<String, ProtocolDescriptor[]> getAllSupportedProtocols(RequestParams requestParams) {
        Map<String, ProtocolDescriptor[]> agentDescriptorMap = new HashMap<>(agentService.getAgents().size());
        agentService.getAgents().forEach((id, agent) ->
            agentDescriptorMap.put(
                id,
                agentService.getAgentConnector(agent)
                    .map(agentConnector -> {
                        LOG.finer("Asking connector '" + agentConnector.getClass().getSimpleName() + "' for protocol descriptors");
                        return agentConnector.getProtocolDescriptors(agent);
                    })
                    .orElseThrow(() -> {
                        LOG.warning("Agent connector not found for agent ID: " + id);
                        return new IllegalStateException("Agent connector not found or returned invalid response");
                    })
            ));

        return agentDescriptorMap;
    }

    @Override
    public AssetAttribute[] getDiscoveredProtocolConfigurations(RequestParams requestParams, String agentId, String protocolName) {
        return new AssetAttribute[0];
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(RequestParams requestParams, String agentId, AssetAttribute protocolConfiguration) {
        return withAgentConnector(agentId, agentConnector -> agentConnector.value.validateProtocolConfiguration(protocolConfiguration));
    }

    @Override
    public Asset[] searchForLinkedAttributes(RequestParams requestParams, String agentId, String protocolConfigurationName, String parentId, String realm) {
        AttributeRef protocolConfigRef = new AttributeRef(agentId, protocolConfigurationName);

        if (!isSuperUser() && TextUtil.isNullOrEmpty(realm)) {
            realm = getAuthenticatedRealm();
        }

        Asset parentAsset = getParent(parentId, realm);

        // TODO: Allow user to select which assets/attributes are actually added to the DB
        Asset[] assets = withAgentConnector(
            agentId,
            agentConnector -> {
                LOG.finer(
                    "Asking connector '" + agentConnector.value.getClass().getSimpleName()
                        + "' to do linked attribute discovery for protocol configuration: " + protocolConfigRef);
                return agentConnector.value.getDiscoveredLinkedAttributes(protocolConfigRef);
            }
        );

        try {
            persistAssets(assets, parentAsset, realm);
            return assets;
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new NotFoundException(e.getMessage());
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new NotSupportedException(e.getMessage());
        }
    }

    @Override
    public Asset[] importLinkedAttributes(RequestParams requestParams, String agentId, String protocolConfigurationName, String parentId, String realm, FileInfo fileInfo) {
        AttributeRef protocolConfigRef = new AttributeRef(agentId, protocolConfigurationName);

        if (TextUtil.isNullOrEmpty(parentId) && TextUtil.isNullOrEmpty(realm)) {
            realm = getAuthenticatedRealm();
        }

        Asset parentAsset = getParent(parentId, realm);

        if (fileInfo == null || fileInfo.getContents() == null) {
            throw new BadRequestException("A file must be provided for import");
        }

        Asset[] assets = withAgentConnector(
            agentId,
            agentConnector -> {
                LOG.finer(
                    "Asking connector '" + agentConnector.value.getClass().getSimpleName()
                        + "' to do linked attribute discovery using uploaded file for protocol configuration: " + protocolConfigRef
                );
                return agentConnector.value.getDiscoveredLinkedAttributes(protocolConfigRef, fileInfo);
            }
        );

        try {
            // TODO: Allow user to select which assets/attributes are actually added to the DB
            persistAssets(assets, parentAsset, realm);
            return assets;
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new NotFoundException(e.getMessage());
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new NotSupportedException(e.getMessage());
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected <T> T withAgentConnector(String agentId, Function<Pair<Asset, AgentConnector>, T> function) {
        return Optional.ofNullable(agentService.getAgents().get(agentId))
            .filter(asset -> asset.getWellKnownType() == AssetType.AGENT)
            .map(agent -> new Pair<>(agent, agentService.getAgentConnector(agent).orElseThrow(() -> {
                LOG.warning("Failed to find agent connector for: " + agent);
                return new IllegalStateException("Agent connector not found or returned invalid response");
            })))
            .map(function)
            .orElseThrow(() -> {
                LOG.warning("Failed to find agent with ID: " + agentId);
                return new IllegalArgumentException("Agent not found");
            });
    }

    protected void persistAssets(Asset[] assets, Asset parentAsset, String realm) {
        if (assets == null || assets.length == 0) {
            LOG.info("No assets to import");
            return;
        }

        for (int i = 0; i < assets.length; i++) {
            Asset asset = assets[i];
            asset.setId(null);
            asset.setRealm(realm);
            asset.setParent(parentAsset);
            Asset newAsset = Asset.map(asset, new Asset());
            assets[i] = assetStorageService.merge(newAsset);
        }
    }

    protected Asset getParent(String parentId, String realm) throws WebApplicationException {
        if (!isSuperUser() && !realm.equals(getAuthenticatedRealm())) {
            throw new ForbiddenException();
        }

        if (TextUtil.isNullOrEmpty(parentId)) {
            return null;
        }

        // Assets must be added in the same realm as the user (unless super user)
        Asset parentAsset = assetStorageService.find(parentId);

        if (parentAsset == null || (!TextUtil.isNullOrEmpty(realm) && parentAsset.getRealm().equals(realm))) {
            throw new NotFoundException("Parent asset doesn't exist in the requested realm '" + realm + "'");
        }

        return parentAsset;
    }
}
