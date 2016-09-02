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
package org.openremote.manager.shared.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.agent.AgentStatus;
import org.openremote.manager.shared.device.Device;

public interface ConnectorComponent {
    public static final String HEADER_DISCOVERY_START = ConnectorComponent.class.getCanonicalName() + ".HEADER_DISCOVERY_START";
    public static final String HEADER_DISCOVERY_STOP = ConnectorComponent.class.getCanonicalName() + ".HEADER_DISCOVERY_STOP";
    public static final String HEADER_INVENTORY_ACTION = ConnectorComponent.class.getCanonicalName() + ".HEADER_INVENTORY_ACTION";
    public static final String HEADER_DEVICE_ACTION = ConnectorComponent.class.getCanonicalName() + ".HEADER_DEVICE_ACTION";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_READ = "READ";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_SUBSCRIBE = "SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String ACTION_WRITE = "WRITE";

    /**
     * Get the unique type descriptor for this connector component
     */
    String getType();

    /**
     * Get the friendly display name for this connector component
     */
    String getDisplayName();

    /**
     * Get CRUD support for child assets of the supplied parent asset. A null
     * response indicates that this asset doesn't support child assets (this can
     * also be indicated with the appropriate values in the returned {@link ChildAssetSupport}.
     *
     * If parentAsset is null then response indicates child CRUD support at the
     * root (in connector terms this usually means agent asset CRUD).
     */
    ChildAssetSupport getChildSupport(Asset parentAsset);

    /**
     * Indicates whether or not this connector component supports child discovery
     * of the supplied parent asset.
     *
     * If parentAsset is null then response indicates whether child discovery is
     * supported at the root (in connector terms this usually means is agent asset
     * discovery supported).
     */
    boolean supportsChildDiscovery(Asset parentAsset);

    /**
     * Get the settings mask for discovering child assets of the supplied
     * parent asset. This is used by clients for user entry of discovery settings.
     *
     * If parentAsset is null then child discovery is being requested at the root
     * (in connector terms this usually means discovering agent assets).
     */
    Attributes getChildDiscoverySettings(Asset parentAsset);

    /**
     * Get the settings mask for creating/updating a child asset of the supplied
     * parent asset. This is used by clients for user entry of child assets.
     *
     * If parentAsset is null then child is being provisioned at the root
     * (in connector terms this usually means creating agent asset).
     */
    Attributes getConnectorSettings();

    /**
     * Endpoint URI for performing child asset discovery. If implemented
     * when child discovery is required then a route will be created requiring
     * a consumer from this endpoint which should generate messages for any
     * child assets that the connector discovers based on the supplied parent asset
     * and discovery settings.
     *
     * Messages output by the consumer should contain either a single {@link Asset} object or
     * an array of {@link Asset} objects. The outputted assets should have IDs and be linked
     * to their parent(s) thus allowing child discovery to return a hierarchy of assets.
     *
     * Discovery should run as long as this route is running.
     */
    String getChildDiscoveryUri(Asset parentAsset, Attributes discoverySettings);

    /**
     * Indicates whether or not this connector component supports monitoring of the supplied
     * asset.
     *
     * If asset monitoring is supported then {@link #getAssetMonitorUri(Asset)} must
     * return the Endpoint URI for the asset monitor.
     */
    boolean supportsMonitoring(Asset asset);

    /**
     * Endpoint URI for performing asset CRUD. The capabilities supported by a particular
     * connector for this asset are determined by the response from {@link #getChildSupport(Asset)}.
     *
     * Support for reading devices from the inventory is mandatory, all other capabilities are
     * optional and are agent/connector dependent.
     *
     * The endpoint should support InOut (Request Reply) producers. The action to perform is set
     * by the {@link #HEADER_INVENTORY_ACTION} message header.
     *
     * {@Link #ACTION_CREATE}, {@Link #ACTION_UPDATE}, {@Link #ACTION_DELETE} operations
     * should consume a {@link Asset}[]. In the case of {@link #ACTION_DELETE} then only the
     * asset ID should be required. The reply message body should contain an integer indicating
     * the status of the request (conforming to the HTTP Status code specification).
     *
     * {@link #ACTION_READ} operations should consume a {@link Asset}[] where only the ID should
     * be required, if no assets are supplied then all child assets should be returned.
     * The reply message body should contain a {@link Asset}[] of the requested devices.
     *
     */
    String getChildInventoryUri(Asset asset);

    /**
     * Endpoint URI for: -
     *      Reading/Writing from/to assets
     *      Subscribing/Un-subscribing to asset changes
     *
     * This endpoint is expected to support the InOut MEP with the
     * {@link #HEADER_DEVICE_ACTION} message header indicating the action to
     * perform/data to return on messages to be consumed by this endpoint.
     *
     * {@link #ACTION_SUBSCRIBE}, {@link #ACTION_UNSUBSCRIBE} operations should consume a
     * {@link Asset}[] where only the ID(s) should be required. The reply message body
     * should contain an integer indicating the status of the request (conforming to the HTTP
     * Status code specification). Connectors that support asset subscriptions must return
     * a valid endpoint URI from {@link #getAssetMonitorUri(Asset)}.
     *
     * Assets of a connector can share an endpoint which is indicated by returning the same
     * endpoint URI.
     */
    String getAssetUri(Asset asset);

    /**
     * Endpoint URI for consuming asset changes as reported by the connector.
     *
     * A subscribed asset should be monitored by the connector for any resource value
     * changes; these changes should then be output in a message body as {@link Asset}[]
     * containing the modified attributes.
     *
     * If not supported then just return null.
     */
    String getAssetMonitorUri(Asset asset);

    /**
     * Generate the asset represented by the supplied asset settings, the connector is responsible
     * for setting the asset ID and asset type as well as any attributes and metadata that are required.
     */
    Asset createAsset(Asset parent, Attributes assetSettings);
}
