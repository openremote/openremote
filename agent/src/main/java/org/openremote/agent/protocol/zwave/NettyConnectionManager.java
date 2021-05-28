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
package org.openremote.agent.protocol.zwave;

import org.openremote.protocol.zwave.DefaultZWConnectionManager;
import org.openremote.protocol.zwave.port.TransportLayer;
import org.openremote.protocol.zwave.port.ZWavePortConfiguration;

public class NettyConnectionManager extends DefaultZWConnectionManager {

    public static NettyConnectionManager create(ZWavePortConfiguration configuration, ZWaveSerialIOClient messageProcessor) {
        NettyConnectionManager mgr = new NettyConnectionManager(configuration, messageProcessor);
        mgr.createControllerAPI();
        mgr.addShutdownHook();
        return mgr;
    }

    private final ZWaveSerialIOClient ioClient;

    protected NettyConnectionManager(ZWavePortConfiguration configuration, ZWaveSerialIOClient ioClient) {
        super(configuration);
        this.ioClient = ioClient;
    }

    @Override
    protected TransportLayer createTransportLayer(ZWavePortConfiguration configuration) {
        return ioClient;
    }
}
