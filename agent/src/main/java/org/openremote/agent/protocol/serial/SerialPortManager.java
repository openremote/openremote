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
package org.openremote.agent.protocol.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.openremote.model.syslog.SyslogCategory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages shared access to serial ports across multiple protocol instances.
 *
 * In protocols like Modbus RTU, multiple devices (with different unit IDs) can share
 * the same physical RS485 bus. This manager allows multiple protocol instances
 * to share a single serial port connection while ensuring thread-safe access
 * through synchronization.
 *
 * This is an opt-in mechanism - protocols must explicitly use this manager.
 * Existing protocols that directly use jSerialComm remain unaffected.
 */
public class SerialPortManager {

    private static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, SerialPortManager.class);
    private static final int PORT_CLEANUP_TIMEOUT_MS = 5000; // 5 seconds timeout for port cleanup

    // Singleton instance
    private static final SerialPortManager INSTANCE = new SerialPortManager();

    // Map of port descriptors to shared port instances
    private final Map<String, SharedSerialPort> sharedPorts = new ConcurrentHashMap<>();

    // Map of ports being cleaned up (prevents immediate re-acquisition)
    private final Map<String, Future<?>> portCleanupTasks = new ConcurrentHashMap<>();

    // Executor for background port cleanup operations
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SerialPortCleanup");
        t.setDaemon(true); // Don't prevent JVM shutdown
        return t;
    });

    // For testing: inject a mock serial port wrapper factory
    private static SerialPortWrapperFactory mockFactoryForTesting = null;

    private SerialPortManager() {
        // Private constructor for singleton
    }

    public static SerialPortManager getInstance() {
        return INSTANCE;
    }

    /**
     * Set a mock factory for testing. This allows tests to inject mock serial ports.
     * @param factory Mock factory, or null to use real serial ports
     */
    public static void setMockFactoryForTesting(SerialPortWrapperFactory factory) {
        mockFactoryForTesting = factory;
    }

    /**
     * Factory interface for creating SerialPortWrapper instances (for testing)
     */
    public interface SerialPortWrapperFactory {
        SerialPortWrapper createWrapper(String portDescriptor, int baudRate, int dataBits, int stopBits, int parity);
    }

    /**
     * Acquire access to a serial port. If the port is already open, returns the existing
     * shared instance. Otherwise, opens the port and creates a new shared instance.
     *
     * @param portDescriptor Port name (e.g., "/dev/ttyUSB0" or "COM3")
     * @param baudRate Baud rate
     * @param dataBits Data bits
     * @param stopBits Stop bits
     * @param parity Parity mode
     * @return SharedSerialPort instance, or null if port cannot be opened
     */
    public synchronized SerialPortWrapper acquirePort(
            String portDescriptor,
            int baudRate,
            int dataBits,
            int stopBits,
            int parity) {

        String portKey = createPortKey(portDescriptor, baudRate, dataBits, stopBits, parity);

        // Wait for any pending cleanup to complete before acquiring
        Future<?> pendingCleanup = portCleanupTasks.get(portKey);
        if (pendingCleanup != null) {
            LOG.info("Port " + portDescriptor + " is being cleaned up, waiting for completion...");
            try {
                pendingCleanup.get(PORT_CLEANUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                LOG.info("Port cleanup completed for " + portDescriptor);
            } catch (TimeoutException e) {
                LOG.warning("Port cleanup timed out after " + PORT_CLEANUP_TIMEOUT_MS + "ms for " + portDescriptor + ", proceeding anyway");
                portCleanupTasks.remove(portKey); // Remove stale cleanup task
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warning("Interrupted while waiting for port cleanup: " + portDescriptor);
                return null;
            } catch (ExecutionException e) {
                LOG.log(Level.WARNING, "Port cleanup failed for " + portDescriptor + ": " + e.getMessage(), e);
                portCleanupTasks.remove(portKey); // Remove failed cleanup task
            }
        }

        SharedSerialPort sharedPort = sharedPorts.get(portKey);

        if (sharedPort == null) {
            // Port not yet opened, create new shared instance
            LOG.info("Opening new shared serial port: " + portDescriptor);

            SerialPortWrapper wrapper;

            // Check if mock factory is set for testing
            if (mockFactoryForTesting != null) {
                wrapper = mockFactoryForTesting.createWrapper(portDescriptor, baudRate, dataBits, stopBits, parity);
                if (!wrapper.openPort()) {
                    LOG.severe("Failed to open mock serial port: " + portDescriptor);
                    return null;
                }
                LOG.info("Using mock serial port for testing");
            } else {
                SerialPort sp = SerialPort.getCommPort(portDescriptor);
                sp.setBaudRate(baudRate);
                sp.setNumDataBits(dataBits);
                sp.setNumStopBits(stopBits);
                sp.setParity(parity);
                sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 0);

                wrapper = new RealSerialPortWrapper(sp);

                if (!wrapper.openPort()) {
                    LOG.severe("Failed to open serial port: " + portDescriptor);
                    return null;
                }
            }

            sharedPort = new SharedSerialPort(portKey, wrapper);
            sharedPorts.put(portKey, sharedPort);
        }

        // Increment reference count
        sharedPort.incrementRefCount();
        LOG.info("Acquired shared serial port: " + portDescriptor + " (refCount=" + sharedPort.getRefCount() + ")");

        return sharedPort;
    }

    /**
     * Release access to a serial port. Decrements the reference count, and closes
     * the port asynchronously if this was the last agent using it.
     *
     * This method returns immediately. The actual port closure happens in the background
     * to avoid blocking the caller while waiting for OS-level resource cleanup.
     *
     * @param portDescriptor Port name
     * @param baudRate Baud rate
     * @param dataBits Data bits
     * @param stopBits Stop bits
     * @param parity Parity mode
     */
    public synchronized void releasePort(
            String portDescriptor,
            int baudRate,
            int dataBits,
            int stopBits,
            int parity) {

        String portKey = createPortKey(portDescriptor, baudRate, dataBits, stopBits, parity);

        SharedSerialPort sharedPort = sharedPorts.get(portKey);

        if (sharedPort == null) {
            LOG.warning("Attempted to release port that was not acquired: " + portDescriptor);
            return;
        }

        int refCount = sharedPort.decrementRefCount();
        LOG.info("Released shared serial port: " + portDescriptor + " (refCount=" + refCount + ")");

        if (refCount == 0) {
            // Last agent released the port - schedule asynchronous cleanup
            LOG.info("Scheduling asynchronous cleanup for shared serial port (no more references): " + portDescriptor);

            // Remove from active ports immediately to prevent new acquisitions during cleanup
            sharedPorts.remove(portKey);

            // Schedule async cleanup task
            Future<?> cleanupTask = cleanupExecutor.submit(() -> {
                try {
                    LOG.info("Starting port cleanup for: " + portDescriptor);
                    boolean closed = sharedPort.actualPort.closePort();
                    if (closed) {
                        LOG.info("Successfully closed serial port: " + portDescriptor);
                    } else {
                        LOG.warning("Failed to close serial port (closePort returned false): " + portDescriptor);
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Exception during serial port cleanup for " + portDescriptor + ": " + e.getMessage(), e);
                } finally {
                    // Remove cleanup task from tracking map
                    portCleanupTasks.remove(portKey);
                    LOG.info("Port cleanup task completed for: " + portDescriptor);
                }
            });

            portCleanupTasks.put(portKey, cleanupTask);
        }
    }

    private String createPortKey(String portDescriptor, int baudRate, int dataBits, int stopBits, int parity) {
        return portDescriptor + ":" + baudRate + ":" + dataBits + ":" + stopBits + ":" + parity;
    }

    /**
     * Wait for all pending port cleanup tasks to complete.
     * This is useful in tests or during controlled shutdown sequences to ensure
     * all asynchronous cleanup operations have finished before proceeding.
     *
     * @param timeoutMs Maximum time to wait in milliseconds
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if cleanup tasks don't complete within timeout
     */
    public synchronized void awaitPendingCleanups(long timeoutMs) throws InterruptedException, TimeoutException {
        if (portCleanupTasks.isEmpty()) {
            LOG.fine("No pending cleanup tasks to wait for");
            return;
        }

        LOG.info("Waiting for " + portCleanupTasks.size() + " pending port cleanup task(s) to complete...");
        long deadline = System.currentTimeMillis() + timeoutMs;

        // Create a snapshot to avoid ConcurrentModificationException
        List<Map.Entry<String, Future<?>>> tasks = new ArrayList<>(portCleanupTasks.entrySet());

        for (Map.Entry<String, Future<?>> entry : tasks) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new TimeoutException("Timeout waiting for cleanup tasks after " + timeoutMs + "ms");
            }

            try {
                entry.getValue().get(remaining, TimeUnit.MILLISECONDS);
                LOG.fine("Cleanup task completed for: " + entry.getKey());
            } catch (ExecutionException e) {
                LOG.log(Level.WARNING, "Cleanup task failed for " + entry.getKey() + ": " + e.getMessage(), e);
                // Continue waiting for other tasks even if one fails
            }
        }

        LOG.info("All pending cleanup tasks completed");
    }

    /**
     * Shutdown the SerialPortManager, closing all open ports and stopping background cleanup tasks.
     * This should be called during application shutdown to ensure clean resource release.
     */
    public synchronized void shutdown() {
        LOG.info("Shutting down SerialPortManager...");

        // Close all currently open ports
        for (Map.Entry<String, SharedSerialPort> entry : sharedPorts.entrySet()) {
            String portKey = entry.getKey();
            SharedSerialPort sharedPort = entry.getValue();
            LOG.info("Closing serial port during shutdown: " + portKey + " (refCount=" + sharedPort.getRefCount() + ")");
            try {
                sharedPort.actualPort.closePort();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error closing port during shutdown: " + portKey, e);
            }
        }
        sharedPorts.clear();

        // Wait for pending cleanup tasks to complete
        if (!portCleanupTasks.isEmpty()) {
            LOG.info("Waiting for " + portCleanupTasks.size() + " pending port cleanup task(s) to complete...");
            for (Map.Entry<String, Future<?>> entry : portCleanupTasks.entrySet()) {
                try {
                    entry.getValue().get(2000, TimeUnit.MILLISECONDS); // 2 second timeout per task
                } catch (TimeoutException e) {
                    LOG.warning("Cleanup task timed out during shutdown: " + entry.getKey());
                    entry.getValue().cancel(true);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error waiting for cleanup task during shutdown: " + entry.getKey(), e);
                }
            }
        }
        portCleanupTasks.clear();

        // Shutdown the cleanup executor
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warning("Cleanup executor did not terminate within 5 seconds, forcing shutdown");
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }

        LOG.info("SerialPortManager shutdown complete");
    }

    /**
     * Real SerialPort wrapper implementation
     */
    static class RealSerialPortWrapper implements SerialPortWrapper {
        private final SerialPort port;
        private final Object lock = new Object();

        RealSerialPortWrapper(SerialPort port) {
            this.port = port;
        }

        @Override
        public boolean openPort() {
            return port.openPort();
        }

        @Override
        public boolean closePort() {
            return port.closePort();
        }

        @Override
        public boolean isOpen() {
            return port.isOpen();
        }

        @Override
        public int writeBytes(byte[] buffer, long bytesToWrite) {
            return port.writeBytes(buffer, (int) bytesToWrite);
        }

        @Override
        public int readBytes(byte[] buffer, long bytesToRead, long offset) {
            return port.readBytes(buffer, (int) bytesToRead, (int) offset);
        }

        @Override
        public int bytesAvailable() {
            return port.bytesAvailable();
        }

        @Override
        public Object getSynchronizationLock() {
            return lock;
        }
    }

    /**
     * Wrapper around a serial port that tracks reference count and synchronizes access
     */
    private static class SharedSerialPort implements SerialPortWrapper {
        private final String portKey;
        private final SerialPortWrapper actualPort;
        private final ReentrantLock accessLock = new ReentrantLock();
        private int refCount = 0;

        public SharedSerialPort(String portKey, SerialPortWrapper actualPort) {
            this.portKey = portKey;
            this.actualPort = actualPort;
        }

        public synchronized void incrementRefCount() {
            refCount++;
        }

        public synchronized int decrementRefCount() {
            refCount--;
            return refCount;
        }

        public synchronized int getRefCount() {
            return refCount;
        }

        @Override
        public boolean openPort() {
            // Port is already open (managed by SerialPortManager)
            return actualPort.isOpen();
        }

        @Override
        public boolean closePort() {
            // Don't actually close - managed by SerialPortManager
            // This will be called when refCount reaches 0
            return true;
        }

        @Override
        public boolean isOpen() {
            return actualPort.isOpen();
        }

        @Override
        public int writeBytes(byte[] buffer, long bytesToWrite) {
            // Synchronize access to prevent multiple agents writing simultaneously
            accessLock.lock();
            try {
                return actualPort.writeBytes(buffer, bytesToWrite);
            } finally {
                accessLock.unlock();
            }
        }

        @Override
        public int readBytes(byte[] buffer, long bytesToRead, long offset) {
            // Reading is part of the synchronized write-read cycle
            // The lock is already held from writeBytes
            if (!accessLock.isHeldByCurrentThread()) {
                accessLock.lock();
                try {
                    return actualPort.readBytes(buffer, bytesToRead, offset);
                } finally {
                    accessLock.unlock();
                }
            } else {
                // Already locked by writeBytes, just delegate
                return actualPort.readBytes(buffer, bytesToRead, offset);
            }
        }

        @Override
        public int bytesAvailable() {
            return actualPort.bytesAvailable();
        }

        @Override
        public Object getSynchronizationLock() {
            // Return the accessLock itself for synchronization
            return accessLock;
        }
    }
}
