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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.protocol.ProtocolAssetImport;
import org.openremote.model.protocol.ProtocolInstanceDiscovery;

/**
 * Special type of {@link AssetDescriptor} that describes an agent {@link Asset}.
 */
@JsonTypeName("agent")
public class AgentDescriptor<T extends Agent<T, U, V>, U extends Protocol<T>, V extends AgentLink<?>> extends AssetDescriptor<T> {

    public static class DiscoveryBooleanConverter extends StdConverter<Class<?>, Boolean> {

        @Override
        public Boolean convert(Class<?> value) {
            return value != null;
        }
    }

    public static final String ICON = "cogs";
    public static final String ICON_COLOUR = "000000";
    @JsonIgnore
    protected Class<U> protocolClass;
    @JsonIgnore
    protected Class<V> agentLinkClass;
    @JsonSerialize(converter = DiscoveryBooleanConverter.class)
    protected Class<? extends ProtocolInstanceDiscovery> instanceDiscoveryProvider;

    public AgentDescriptor(Class<T> agentClass, Class<U> protocolClass, Class<V> agentLinkClass) {
        this(agentClass, protocolClass, agentLinkClass, null);
    }

    public AgentDescriptor(Class<T> agentClass, Class<U> protocolClass, Class<V> agentLinkClass, Class<? extends ProtocolInstanceDiscovery> instanceDiscoveryProvider) {
        super(ICON, ICON_COLOUR, agentClass);
        this.protocolClass = protocolClass;
        this.agentLinkClass = agentLinkClass;
        this.instanceDiscoveryProvider = instanceDiscoveryProvider;
    }

    public Class<? extends ProtocolInstanceDiscovery> getInstanceDiscoveryProvider() {
        return instanceDiscoveryProvider;
    }

    public boolean isInstanceDiscovery() {
        return instanceDiscoveryProvider != null;
    }

    @JsonProperty
    public boolean isAssetDiscovery() {
        return ProtocolAssetDiscovery.class.isAssignableFrom(protocolClass);
    }

    @JsonProperty
    public boolean isAssetImport() {
        return ProtocolAssetImport.class.isAssignableFrom(protocolClass);
    }

    @JsonProperty
    public String getAgentLinkType() {
        return agentLinkClass.getSimpleName();
    }

    public Class<U> getProtocolClass() {
        return protocolClass;
    }

    public Class<V> getAgentLinkClass() {
        return agentLinkClass;
    }

    public Class<? extends ProtocolInstanceDiscovery> getInstanceDiscoveryProviderClass() {
        return instanceDiscoveryProvider;
    }
}
