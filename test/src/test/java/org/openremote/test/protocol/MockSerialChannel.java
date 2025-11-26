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
package org.openremote.test.protocol;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.oio.OioByteStreamChannel;
import org.openremote.agent.protocol.serial.JSerialCommDeviceAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock Serial Channel for testing serial-based protocols without actual hardware.
 * Extends OioByteStreamChannel just like JSerialCommChannel.
 * This channel simulates a serial port by using in-memory queues for data transfer.
 * Tests can configure a {@link DataHandler} to process outgoing data and generate responses.
 */
@SuppressWarnings("deprecation")
public class MockSerialChannel extends OioByteStreamChannel {

    private static final JSerialCommDeviceAddress LOCAL_ADDRESS = new JSerialCommDeviceAddress("localhost");

    /**
     * Handler for processing data written to the serial port and generating responses.
     */
    public interface DataHandler {
        /**
         * Called when data is written to the serial port.
         * @param data the data that was written
         * @param responseCallback callback to send response data back to the reader
         */
        void onDataWritten(byte[] data, ResponseCallback responseCallback);
    }

    /**
     * Callback interface for sending response data.
     */
    public interface ResponseCallback {
        /**
         * Queue response bytes to be read by the channel.
         * @param response the response bytes to queue
         */
        void sendResponse(byte[] response);
    }

    // Static handler for processing data - set by tests
    private static volatile DataHandler dataHandler = null;

    // Static reference for the last data written (useful for test assertions)
    private static final AtomicReference<byte[]> lastWrittenData = new AtomicReference<>(null);

    private final ChannelConfig config;
    private boolean open = true;
    private JSerialCommDeviceAddress deviceAddress;

    // Instance-specific response queue - each channel has its own queue
    private final BlockingQueue<Byte> responseQueue = new LinkedBlockingQueue<>();

    // Instance-specific streams
    private MockInputStream mockInputStream;
    private MockOutputStream mockOutputStream;

    public MockSerialChannel() {
        super(null);
        config = new DefaultChannelConfig(this);
    }

    /**
     * Set the data handler for processing writes and generating responses.
     * @param handler the handler, or null to disable
     */
    public static void setDataHandler(DataHandler handler) {
        dataHandler = handler;
    }

    /**
     * Get the last data that was written to the channel.
     * @return the last written data, or null if nothing has been written
     */
    public static byte[] getLastWrittenData() {
        return lastWrittenData.get();
    }

    /**
     * Reset mock state before each test.
     */
    public static void resetState() {
        lastWrittenData.set(null);
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new MockUnsafe();
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        deviceAddress = (JSerialCommDeviceAddress) remoteAddress;
    }

    protected void doInit() throws Exception {
        mockInputStream = new MockInputStream();
        mockOutputStream = new MockOutputStream();
        activate(mockInputStream, mockOutputStream);
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
            responseQueue.clear();
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

    /**
     * Queue response bytes to be read by the channel.
     * This is called by the DataHandler via the ResponseCallback.
     */
    private void queueResponse(byte[] response) {
        if (response != null) {
            for (byte b : response) {
                responseQueue.offer(b);
            }
        }
    }

    /**
     * Mock InputStream that reads from the response queue.
     * Uses short timeouts to allow the OIO thread to process pending writes.
     */
    private class MockInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            if (!open) return -1;

            try {
                Byte b = responseQueue.poll(50, TimeUnit.MILLISECONDS);
                if (b != null) {
                    return b & 0xFF;
                }
                return -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!open) return -1;
            if (len == 0) return 0;

            try {
                Byte first = responseQueue.poll(50, TimeUnit.MILLISECONDS);
                if (first == null) {
                    // No data available - return 0 to indicate timeout without EOF
                    return 0;
                }

                b[off] = first;
                int bytesRead = 1;

                // Read any additional available bytes without blocking
                while (bytesRead < len) {
                    Byte next = responseQueue.poll();
                    if (next == null) break;
                    b[off + bytesRead] = next;
                    bytesRead++;
                }

                return bytesRead;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }

        @Override
        public int available() throws IOException {
            return responseQueue.size();
        }
    }

    /**
     * Mock OutputStream that captures writes and invokes the data handler.
     */
    private class MockOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] data = new byte[len];
            System.arraycopy(b, off, data, 0, len);

            lastWrittenData.set(data);

            DataHandler handler = dataHandler;
            if (handler != null) {
                handler.onDataWritten(data, MockSerialChannel.this::queueResponse);
            }
        }

        @Override
        public void flush() throws IOException {
            // No-op for mock
        }
    }

    private final class MockUnsafe extends AbstractUnsafe {
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
    }
}
