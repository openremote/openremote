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
package org.openremote.agent.protocol.modbus;

import org.openremote.agent.protocol.modbus.util.ModbusTcpFrame;
import org.openremote.agent.protocol.tcp.TCPIOClient;
import org.openremote.model.syslog.SyslogCategory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ModbusTcpIOClient extends TCPIOClient<ModbusTcpFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusTcpIOClient.class);

    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);

    public ModbusTcpIOClient(String host, int port) {
        super(host, port);
    }

    public int getNextTransactionId() {
        return transactionIdCounter.updateAndGet(current -> (current + 1) % 65536);
    }
}
