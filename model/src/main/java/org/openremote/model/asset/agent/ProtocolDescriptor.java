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
package org.openremote.model.asset.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;

import java.util.List;

public class ProtocolDescriptor {

    protected String name;
    protected String displayName;
    protected String version;
    protected boolean configurationDiscovery;
    protected boolean configurationImport;
    protected boolean deviceDiscovery;
    protected boolean deviceImport;
    protected AssetAttribute configurationTemplate;
    protected List<MetaItemDescriptor> protocolConfigurationMetaItems;
    protected List<MetaItemDescriptor> linkedAttributeMetaItems;

    @JsonCreator
    public ProtocolDescriptor(@JsonProperty("name") String name,
                              @JsonProperty("displayName") String displayName,
                              @JsonProperty("version") String version,
                              @JsonProperty("configurationDiscovery") boolean configurationDiscovery,
                              @JsonProperty("configurationImport") boolean configurationImport,
                              @JsonProperty("deviceDiscovery") boolean deviceDiscovery,
                              @JsonProperty("deviceImport") boolean deviceImport,
                              @JsonProperty("configurationTemplate") AssetAttribute configurationTemplate,
                              @JsonProperty("protocolConfigurationMetaItems") List<MetaItemDescriptor> protocolConfigurationMetaItems,
                              @JsonProperty("linkedAttributeMetaItems") List<MetaItemDescriptor> linkedAttributeMetaItems) {
        this.name = name;
        this.displayName = displayName;
        this.version = version;
        this.configurationDiscovery = configurationDiscovery;
        this.configurationImport = configurationImport;
        this.deviceDiscovery = deviceDiscovery;
        this.deviceImport = deviceImport;
        this.configurationTemplate = configurationTemplate;
        this.protocolConfigurationMetaItems = protocolConfigurationMetaItems;
        this.linkedAttributeMetaItems = linkedAttributeMetaItems;
    }

    /**
     * Get the URN protocol name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Indicates if the protocol supports {@link ProtocolConfiguration} discovery
     */
    public boolean isConfigurationDiscovery() {
        return configurationDiscovery;
    }

    /**
     * Indicates if the protocol supports {@link ProtocolConfiguration} import by uploading protocol
     * specific file(s)
     */
    public boolean isConfigurationImport() {
        return configurationImport;
    }

    /**
     * Indicates if the protocol supports device {@link org.openremote.model.asset.Asset} discovery
     */
    public boolean isDeviceDiscovery() {
        return deviceDiscovery;
    }

    public boolean isDeviceImport() {
        return deviceImport;
    }

    public AssetAttribute getConfigurationTemplate() {
        return configurationTemplate;
    }

    /**
     * Get the {@link MetaItemDescriptor}s that describe {@link MetaItem}s that can be used on {@link ProtocolConfiguration}s
     * of this protocol (for UI purposes)
     */
    public List<MetaItemDescriptor> getProtocolConfigurationMetaItems() {
        return protocolConfigurationMetaItems;
    }

    /**
     * Get the {@link MetaItemDescriptor}s that describe {@link MetaItem}s that can be used on {@link AssetAttribute}s
     * linked to this protocol (for UI purposes)
     */
    public List<MetaItemDescriptor> getLinkedAttributeMetaItems() {
        return linkedAttributeMetaItems;
    }
}
