/*
 * Copyright 2021, OpenRemote Inc.
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

import com.fazecast.jSerialComm.SerialPort;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;
import static org.openremote.agent.protocol.serial.JSerialCommChannelOption.*;

/**
 * A channel to a serial device using the jSerialComm library.
 */
@SuppressWarnings("deprecation")
public class JSerialCommChannel extends io.netty.channel.oio.OioByteStreamChannel {

    private static final JSerialCommDeviceAddress LOCAL_ADDRESS = new JSerialCommDeviceAddress("localhost");

    private final JSerialCommChannelConfig config;

    private boolean open = true;
    private JSerialCommDeviceAddress deviceAddress;
    private SerialPort serialPort;

    public JSerialCommChannel() {
        super(null);

        config = new DefaultJSerialCommChannelConfig(this);
    }

    @Override
    public JSerialCommChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new JSerialCommChannel.JSCUnsafe();
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        JSerialCommDeviceAddress remote = (JSerialCommDeviceAddress) remoteAddress;
        SerialPort commPort = SerialPort.getCommPort(remote.value());
        if (!commPort.openPort()) {
            throw new IOException("Could not open port: " + remote.value());
        }
        deviceAddress = remote;
        serialPort = commPort;
    }

    protected void doInit() throws Exception {
        serialPort.setComPortParameters(
            config().getOption(BAUD_RATE),
            config().getOption(DATA_BITS),
            config().getOption(STOP_BITS).value(),
            config().getOption(PARITY_BIT).value()
        );
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING, config().getOption(READ_TIMEOUT), 0);
        activate(serialPort.getInputStreamWithSuppressedTimeoutExceptions(), serialPort.getOutputStream());
    }

    @Override
    public JSerialCommDeviceAddress localAddress() {
        return (JSerialCommDeviceAddress) super.localAddress();
    }

    @Override
    public JSerialCommDeviceAddress remoteAddress() {
        return (JSerialCommDeviceAddress) super.remoteAddress();
    }

    @Override
    protected JSerialCommDeviceAddress localAddress0() {
        return LOCAL_ADDRESS;
    }

    @Override
    protected JSerialCommDeviceAddress remoteAddress0() {
        return deviceAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        open = false;
        try {
            super.doClose();
        } finally {
            if (serialPort != null) {
                serialPort.closePort();
                serialPort = null;
            }
        }
    }

    @Override
    protected boolean isInputShutdown() {
        return !open;
    }

    @Override
    protected ChannelFuture shutdownInput() {
        return newFailedFuture(new UnsupportedOperationException("shutdownInput"));
    }


    private final class JSCUnsafe extends AbstractUnsafe {
        @Override
        public void connect(
            final SocketAddress remoteAddress,
            final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !isOpen()) {
                return;
            }

            try {
                final boolean wasActive = isActive();
                doConnect(remoteAddress, localAddress);

                int waitTime = config().getOption(WAIT_TIME);
                if (waitTime > 0) {
                    eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doInit();
                                safeSetSuccess(promise);
                                if (!wasActive && isActive()) {
                                    pipeline().fireChannelActive();
                                }
                            } catch (Throwable t) {
                                safeSetFailure(promise, t);
                                closeIfClosed();
                            }
                        }
                    }, waitTime, TimeUnit.MILLISECONDS);
                } else {
                    doInit();
                    safeSetSuccess(promise);
                    if (!wasActive && isActive()) {
                        pipeline().fireChannelActive();
                    }
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
            }
        }
    }
}
