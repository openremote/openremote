/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan;

public final class LoRaWANConstants {
    public static final String AGENT_LINK_CONFIG_UPLINK_PORT = "uplinkPort";
    public static final String AGENT_LINK_CONFIG_DOWNLINK_PORT = "downlinkPort";
    public static final String AGENT_LINK_CONFIG_VALUE_FILTER_JSON_PATH = "valueFilterJsonPath";
    public static final String AGENT_LINK_CONFIG_VALUE_FILTER_REGEX = "valueFilterRegex";
    public static final String AGENT_LINK_CONFIG_VALUE_CONVERTER = "valueConverter";
    public static final String AGENT_LINK_CONFIG_WRITE_VALUE_CONVERTER = "writeValueConverter";
    public static final String AGENT_LINK_CONFIG_WRITE_OBJECT_VALUE_TEMPLATE = "writeObjectValueTemplate";

    public static final String ATTRIBUTE_NAME_DEV_EUI = "devEUI";
    public static final String ATTRIBUTE_NAME_VENDOR_ID = "vendorId";
    public static final String ATTRIBUTE_NAME_MODEL_ID = "modelId";
    public static final String ATTRIBUTE_NAME_FIRMWARE_VERSION= "firmwareVersion";

    public static final String ASSET_TYPE_TAG = "openremote-asset-type";
}
