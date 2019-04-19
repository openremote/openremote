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
package org.openremote.agent.protocol.velbus;

import org.openremote.agent.protocol.MessageProcessor;
import org.openremote.agent.protocol.ProtocolConfigurationDiscovery;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.attribute.MetaItemDescriptorImpl;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;

import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO;

public class VelbusSerialProtocol extends AbstractVelbusProtocol implements ProtocolConfigurationDiscovery {

    public static final String PROTOCOL_NAME = PROTOCOL_BASE_NAME + "Serial";
    public static final String PROTOCOL_DISPLAY_NAME = "VELBUS Serial";
    public static final String META_VELBUS_SERIAL_PORT = PROTOCOL_NAME + ":port";
    public static final String META_VELBUS_SERIAL_BAUDRATE = PROTOCOL_NAME + ":baudRate";
    public static final int DEFAULT_BAUDRATE = 38400;
    public static final List<MetaItemDescriptorImpl> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl(
            META_VELBUS_SERIAL_PORT,
            ValueType.STRING,
            true,
            null,
            null,
            1,
            null,
            false
        ),
        new MetaItemDescriptorImpl(
            META_VELBUS_SERIAL_BAUDRATE,
            ValueType.NUMBER,
            false,
            REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO,
            MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            1,
            Values.create(DEFAULT_BAUDRATE),
            false
        )
    );

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = super.getProtocolConfigurationMetaItemDescriptors();
        descriptors.addAll(PROTOCOL_META_ITEM_DESCRIPTORS);
        return descriptors;
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            VelbusConfiguration.validateSerialConfiguration(protocolConfiguration, result);
        }
        return result;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(new MetaItem(META_VELBUS_SERIAL_PORT, null));
    }

    @Override
    protected MessageProcessor<VelbusPacket> createMessageProcessor(AssetAttribute protocolConfiguration) throws RuntimeException {

        // Extract port and baud rate
        String port = protocolConfiguration.getMetaItem(META_VELBUS_SERIAL_PORT).flatMap(AbstractValueHolder::getValueAsString).orElse(null);
        Integer baudRate = protocolConfiguration.getMetaItem(META_VELBUS_SERIAL_BAUDRATE).flatMap(AbstractValueHolder::getValueAsInteger).orElse(DEFAULT_BAUDRATE);

        TextUtil.requireNonNullAndNonEmpty(port, "Port cannot be null or empty");

        return new VelbusSerialMessageProcessor(port, baudRate, executorService);
    }

    @Override
    protected String getUniqueNetworkIdentifier(AssetAttribute protocolConfiguration) {
        return protocolConfiguration
            .getMetaItem(META_VELBUS_SERIAL_PORT)
            .flatMap(AbstractValueHolder::getValueAsString)
            .orElse("");
    }

    @Override
    public AssetAttribute[] discoverProtocolConfigurations() {
        // TODO: Search for VELBUS USB devices
        return new AssetAttribute[]{
            initProtocolConfiguration(new AssetAttribute(), PROTOCOL_NAME)
                .addMeta(
                new MetaItem(META_VELBUS_SERIAL_PORT, Values.create("COM6"))
            )
        };
    }
}
