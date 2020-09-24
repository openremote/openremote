/*
 * Copyright 2019, OpenRemote Inc.
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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetModelProvider;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.attribute.AttributeValueDescriptor;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.util.AssetModelUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * This service populates the descriptors in {@link org.openremote.model.util.AssetModelUtil} by looking for
 * {@link AssetModelProvider}s through {@link ServiceLoader}.
 */
public class AssetModelService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetModelService.class.getName());
    protected final List<AssetModelProvider> assetModelProviders = new ArrayList<>();

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE + 10;
    }

    @Override
    public void init(Container container) throws Exception {
        ServiceLoader.load(AssetModelProvider.class).forEach(assetModelProviders::add);
    }

    @Override
    public void start(Container container) throws Exception {

        List<AgentDescriptor> agentDescriptors = new ArrayList<>();
        List<AssetDescriptor> assetDescriptors = new ArrayList<>();
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<>();
        List<AttributeValueDescriptor> attributeValueDescriptors = new ArrayList<>();
        List<MetaItemDescriptor> metaItemDescriptors = new ArrayList<>();

        assetModelProviders.forEach(assetModelProvider -> {
            LOG.fine("Adding asset model descriptors of provider: " + assetModelProvider.getClass().getName());

            agentDescriptors.addAll(Arrays.asList(assetModelProvider.getAgentDescriptors()));
            assetDescriptors.addAll(Arrays.asList(assetModelProvider.getAssetDescriptors()));
            attributeDescriptors.addAll(Arrays.asList(assetModelProvider.getAttributeDescriptors()));
            attributeValueDescriptors.addAll(Arrays.asList(assetModelProvider.getAttributeValueDescriptors()));
            metaItemDescriptors.addAll(Arrays.asList(assetModelProvider.getMetaItemDescriptors()));
        });

        AssetModelUtil.setAgentDescriptors(agentDescriptors.toArray(new AgentDescriptor[0]));
        AssetModelUtil.setAssetDescriptors(assetDescriptors.toArray(new AssetDescriptor[0]));
        AssetModelUtil.setAttributeDescriptors(attributeDescriptors.toArray(new AttributeDescriptor[0]));
        AssetModelUtil.setAttributeValueDescriptors(attributeValueDescriptors.toArray(new AttributeValueDescriptor[0]));
        AssetModelUtil.setMetaItemDescriptors(metaItemDescriptors.toArray(new MetaItemDescriptor[0]));
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public void addAssetModelProvider(AssetModelProvider assetModelProvider) {
        // Model providers should be singletons so make sure no duplicate instances of the same class
        if (assetModelProviders.stream().noneMatch(asm -> asm.getClass() == assetModelProvider.getClass())) {
            this.assetModelProviders.add(assetModelProvider);
        }
    }
}
