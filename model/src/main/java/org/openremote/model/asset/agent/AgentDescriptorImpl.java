/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.asset.agent;

import org.openremote.model.asset.AssetDescriptorImpl;
import org.openremote.model.attribute.AttributeDescriptor;

public class AgentDescriptorImpl extends AssetDescriptorImpl implements AgentDescriptor {

    protected boolean instanceDiscovery;
    protected boolean instanceImport;
    protected boolean assetDiscovery;
    protected boolean assetImport;

    public AgentDescriptorImpl(String name, String type, String icon, String color, AttributeDescriptor[] attributeDescriptors) {
        super(name, type, icon, color, attributeDescriptors);
    }

    @Override
    public boolean hasInstanceDiscovery() {
        return instanceDiscovery;
    }

    public AgentDescriptorImpl setInstanceDiscovery(boolean instanceDiscovery) {
        this.instanceDiscovery = instanceDiscovery;
        return this;
    }

    @Override
    public boolean hasInstanceImport() {
        return false;
    }

    public AgentDescriptorImpl setInstanceImport(boolean instanceImport) {
        this.instanceImport = instanceImport;
        return this;
    }

    @Override
    public boolean hasAssetDiscovery() {
        return false;
    }

    public AgentDescriptorImpl setAssetDiscovery(boolean assetDiscovery) {
        this.assetDiscovery = assetDiscovery;
        return this;
    }

    @Override
    public boolean hasAssetImport() {
        return false;
    }

    public AgentDescriptorImpl setAssetImport(boolean assetImport) {
        this.assetImport = assetImport;
        return this;
    }
}
