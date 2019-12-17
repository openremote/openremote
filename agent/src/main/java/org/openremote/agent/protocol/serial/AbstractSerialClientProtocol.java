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
package org.openremote.agent.protocol.serial;

import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract TCP client protocol for communicating with TCP servers; concrete implementations must provide
 * an {@link IoClient<T> for handling over the wire communication}.
 */
public abstract class AbstractSerialClientProtocol<T> extends AbstractIoClientProtocol<T, SerialIoClient<T>> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractSerialClientProtocol.class);
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":serialClient";

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_SERIAL_PORT,
        META_PROTOCOL_SERIAL_BAUDRATE
    );

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(
                new MetaItem(META_PROTOCOL_SERIAL_PORT, null)
            );
    }

    @Override
    protected SerialIoClient<T> createIoClient(AssetAttribute protocolConfiguration) throws Exception {

        String port = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_SERIAL_PORT,
            true,
            true
        ).flatMap(Values::getString).orElse(null);

        Integer baudrate = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_SERIAL_BAUDRATE,
            false,
            true
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        if (baudrate != null && baudrate < 1) {
            throw new IllegalArgumentException("Baudrate must be in greater than 0");
        }

        if (TextUtil.isNullOrEmpty(port)) {
            throw new IllegalArgumentException("Serial port cannot be empty");
        }

        return new SerialIoClient<>(port, baudrate, executorService);
    }
}
