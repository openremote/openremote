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
package org.openremote.agent.protocol;

import org.openremote.model.AssetModelProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Enables scanning of descriptors in the agent module
 */
public class AgentModelProvider implements AssetModelProvider {
    @Override
    public boolean useAutoScan() {
        return true;
    }

    @Override
    public AssetDescriptor<?>[] getAssetDescriptors() {
        return new AssetDescriptor<?>[0];
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<AttributeDescriptor<?>>> getAttributeDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<MetaItemDescriptor<?>>> getMetaItemDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<ValueDescriptor<?>>> getValueDescriptors() {
        return null;
    }

    @Override
    public void onAssetModelFinished() {
    }
}
