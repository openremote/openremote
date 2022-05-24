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
import org.openremote.container.util.CodecUtil;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentResource;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.ws.rs.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Redirect gateway agent requests to the gateway
// TODO: Allow user to select which assets/attributes are actually added to the DB
public class AgentResourceImpl extends ManagerWebResource implements AgentResource {

    private static final Logger LOG = Logger.getLogger(AgentResourceImpl.class.getName());
    protected final AgentService agentService;
    protected final AssetStorageService assetStorageService;
    protected final ScheduledExecutorService executorService;

    public AgentResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             AssetStorageService assetStorageService,
                             AgentService agentService,
                             ScheduledExecutorService executorService) {
        super(timerService, identityService);
        this.agentService = agentService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
    }

    @Override
    public Agent<?, ?, ?>[] doProtocolInstanceDiscovery(RequestParams requestParams, String parentId, String agentType, String realm) {

        if (!isSuperUser()) {
            realm = getAuthenticatedRealmName();
        }

        if (parentId != null) {
            // Check parent is in the correct realm
            Asset<?> asset = assetStorageService.find(parentId, false);
            if (asset == null) {
                throw new NotFoundException("Parent asset does not exist");
            }
            if (realm != null && !asset.getRealm().equals(realm)) {
                throw new NotAuthorizedException("Parent asset not in the correct realm: agent ID =" + parentId);
            }
        }

        Optional<AgentDescriptor<?, ?, ?>> agentDescriptor = ValueUtil.getAgentDescriptor(agentType);

        if (!agentDescriptor.isPresent()) {
            throw new IllegalArgumentException("Agent descriptor not found: agent type =" + agentType);
        }

        if (!agentDescriptor.map(AgentDescriptor::isInstanceDiscovery).orElse(false)) {
            throw new NotSupportedException("Agent protocol doesn't support instance discovery");
        }

        List<Agent<?, ?, ?>> foundAgents = new ArrayList<>();
        agentService.doProtocolInstanceDiscovery(parentId, agentDescriptor.get().getInstanceDiscoveryProviderClass(), agents -> {
            if (agents != null) {
                foundAgents.addAll(Arrays.asList(agents));
            }
        });

        return foundAgents.toArray(new Agent[0]);
    }

    @Override
    public AssetTreeNode[] doProtocolAssetDiscovery(RequestParams requestParams, String agentId, String realm) {

        if (!isSuperUser()) {
            realm = getAuthenticatedRealmName();
        }

        Agent<?, ?, ?> agent = agentService.getAgent(agentId);

        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: agent ID =" + agentId);
        }

        if (realm != null && !realm.equals(agent.getRealm())) {
            throw new NotAuthorizedException("Agent not in the correct realm: agent ID =" + agentId);
        }

        List<AssetTreeNode> foundAssets = new ArrayList<>();
        String finalRealm = realm;

        Future<Void> result = agentService.doProtocolAssetDiscovery(agent, assets -> {
            if (assets != null) {
                // Persist the assets in a separate thread
                executorService.submit(() -> persistAssets(assets, agent, finalRealm));
                foundAssets.addAll(Arrays.asList(assets));
            }
        });

        try {
            result.get(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.fine("Protocol discovery stopped as timeout reached");
        } catch (Exception e) {
            LOG.log(Level.INFO, "Protocol discovery threw an exception: " + agent, e);
            throw new BadRequestException("Protocol discovery threw an exception: " + agent, e);
        }

        return foundAssets.toArray(new AssetTreeNode[0]);
    }

    @Override
    public AssetTreeNode[] doProtocolAssetImport(RequestParams requestParams, String agentId, String realm, FileInfo fileInfo) {

        if (!isSuperUser()) {
            realm = getAuthenticatedRealmName();
        }

        Agent<?, ?, ?> agent = agentService.getAgent(agentId);

        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: agent ID =" + agentId);
        }

        if (realm != null && !realm.equals(agent.getRealm())) {
            throw new NotAuthorizedException("Agent not in the correct realm: agent ID =" + agentId);
        }

        List<AssetTreeNode> foundAssets = new ArrayList<>();

        byte[] fileData;

        try {
            fileData = fileInfo.isBinary() ? CodecUtil.decodeBase64(fileInfo.getContents()) : fileInfo.getContents().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            String msg = "Failed to decode file info: name = " + fileInfo.getName();
            LOG.log(Level.WARNING, msg, e);
            throw new BadRequestException(msg);
        }

        try {
            Future<Void> future = agentService.doProtocolAssetImport(agent, fileData, assets -> {
                if (assets != null) {
                    foundAssets.addAll(Arrays.asList(assets));
                }
            });

            future.get();
        } catch (UnsupportedOperationException e) {
            throw new NotAllowedException(e);
        } catch (InterruptedException | ExecutionException e) {
            throw new ProcessingException(e);
        }

        AssetTreeNode[] foundAssetsArr = foundAssets.toArray(new AssetTreeNode[0]);
        persistAssets(foundAssetsArr, agent, realm);
        return foundAssetsArr;
    }

    // TODO: Allow user to select which assets/attributes are actually added to the DB
    protected void persistAssets(AssetTreeNode[] assets, Asset<?> parentAsset, String realm) {
        try {

            if (assets == null || assets.length == 0) {
                LOG.info("No assets to import");
                return;
            }

            for (AssetTreeNode assetNode : assets) {
                Asset<?> asset = assetNode.asset;

                if (asset == null) {
                    LOG.info("Skipping node as asset not set");
                    continue;
                }

                asset.setId(null);
                asset.setRealm(realm);
                asset.setParent(parentAsset);
                assetNode.asset = assetStorageService.merge(asset);

                if (assetNode.children != null) {
                    persistAssets(assetNode.children, assetNode.asset, realm);
                }
            }


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

    protected Asset<?> getParent(String parentId, String realm) throws WebApplicationException {
        if (!isSuperUser() && !realm.equals(getAuthenticatedRealmName())) {
            throw new ForbiddenException();
        }

        if (TextUtil.isNullOrEmpty(parentId)) {
            return null;
        }

        // Assets must be added in the same realm as the user (unless super user)
        Asset<?> parentAsset = assetStorageService.find(parentId);

        if (parentAsset == null || (!TextUtil.isNullOrEmpty(realm) && parentAsset.getRealm().equals(realm))) {
            throw new NotFoundException("Parent asset doesn't exist in the requested realm '" + realm + "'");
        }

        return parentAsset;
    }
}
