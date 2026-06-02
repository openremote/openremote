package org.openremote.manager.telematics;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.telematics.session.DeviceConnection;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.core.TelematicsMessagePublisher;
import org.openremote.model.telematics.core.TelematicsMessageHandler;
import org.openremote.model.telematics.core.TelematicsMessageEnvelope;
import org.openremote.model.telematics.protocol.MessageContext;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central telematics runtime registry and message processing service.
 */
public class TelematicsService extends RouteBuilder implements ContainerService, TelematicsMessagePublisher {

    private static final Logger LOG = Logger.getLogger(TelematicsService.class.getName());

    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;


    public static final String TELEMATICS_MESSAGE_QUEUE =
            "seda://telematics-device-messages?size=10000&concurrentConsumers=4&blockWhenFull=true";

    private final Map<String, TelematicsVendor> vendorMap = new ConcurrentHashMap<>();
    private final Map<String, DeviceConnection> deviceConnectionMap = new ConcurrentHashMap<>();
    private final Map<String, TelematicsMessageHandler> handlerMap = new ConcurrentHashMap<>();

    private MessageBrokerService messageBrokerService;

    @Override
    public int getPriority() {
        return ContainerService.LOW_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        messageBrokerService.getContext().addRoutes(this);

        this.assetProcessingService = container.getService(AssetProcessingService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);

        this.registerVendor(TeltonikaVendor.getInstance());

        LOG.info("TelematicsService initialised");
    }

    @Override
    public void start(Container container) {
        logContainerServices(container);
        logRegisteredVendors();
    }

    @Override
    public void stop(Container container) {
        LOG.info("Stopping TelematicsService: vendors=" + vendorMap.size() + ", devices=" + deviceConnectionMap.size());
        deviceConnectionMap.clear();
        vendorMap.clear();
    }

    @Override
    public void configure() {
        from(TELEMATICS_MESSAGE_QUEUE)
                .routeId("Telematics-ProcessDeviceMessage")
                .process(exchange -> {
                    Object body = exchange.getIn().getBody();
                    if (!(body instanceof TelematicsMessageEnvelope envelope)) {
                        throw new IllegalArgumentException("Unexpected telematics queue payload type: " +
                                (body != null ? body.getClass().getName() : "null"));
                    }
                    processEnvelope(envelope);
                })
                .onException(Exception.class)
                .logStackTrace(true)
                .handled(true)
                .process(this::logAndDropQueueFailure);
    }

    private void processEnvelope(TelematicsMessageEnvelope envelope) {
        String vendorId = envelope.getVendorId();
        TelematicsMessageHandler handler = handlerMap.get(vendorId);
        if (handler == null) {
            throw new IllegalStateException("No telematics message handler registered for vendor: " + vendorId);
        }

        DeviceConnection connection = deviceConnectionMap.get(toDeviceKey(vendorId, envelope.getDeviceId()));
        if (connection == null) {
            throw new IllegalStateException("No telematics connection state found for " + vendorId + ":" + envelope.getDeviceId());
        }

        handler.process(envelope, connection).ifPresent(connection::setAssetId);
    }

    private void logAndDropQueueFailure(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Object body = exchange.getIn().getBody();

        if (body instanceof TelematicsMessageEnvelope envelope) {
            String msg = "Failed telematics message processing for vendor=" + envelope.getVendorId() +
                    ", deviceId=" + envelope.getDeviceId() + ", dropping message";
            if (exception != null) {
                LOG.log(Level.SEVERE, msg, exception);
            } else {
                LOG.severe(msg + ": unknown error");
            }
            return;
        }

        String msg = "Failed telematics queue processing, dropping message";
        if (exception != null) {
            LOG.log(Level.SEVERE, msg, exception);
        } else {
            LOG.severe(msg + ": unknown error");
        }
    }

    public void registerVendor(TelematicsVendor vendor) {
        Objects.requireNonNull(vendor, "vendor cannot be null");

        TelematicsVendor existing = vendorMap.putIfAbsent(vendor.getVendorId(), vendor);
        if (existing != null) {
            throw new IllegalStateException("Vendor already registered: " + vendor.getVendorId());
        }

        LOG.info("Registered telematics vendor: id=" + vendor.getVendorId() + ", name=" + vendor.getVendorName() + ", transports=" + vendor.getTransports());
        handlerMap.putIfAbsent(vendor.getVendorId(), vendor.createMessageHandler(LOG, assetStorageService, assetProcessingService));
        logRegisteredVendors();
    }

    public Optional<TelematicsVendor> getVendor(String vendorId) {
        return Optional.ofNullable(vendorMap.get(vendorId));
    }

    public Map<String, TelematicsVendor> getRegisteredVendors() {
        return Collections.unmodifiableMap(vendorMap);
    }

    public DeviceConnection markTrackerConnected(TelematicsVendor vendor,
                                                 String deviceId,
                                                 String realm,
                                                 String protocolId,
                                                 String codecId,
                                                 MessageContext.Transport transport) {
        ensureVendorRegistered(vendor);
        String key = toDeviceKey(vendor.getVendorId(), deviceId);

        return deviceConnectionMap.compute(key, (ignored, existing) -> {
            DeviceConnection connection = existing != null ? existing : new DeviceConnection(vendor.getVendorId(), deviceId, realm);
            connection.setProtocolId(protocolId);
            connection.setCodecId(codecId);
            connection.setTransport(transport);
            connection.setConnected(true);
            connection.incrementConnectionCount();
            connection.touch();
            return connection;
        });
    }

    public Optional<DeviceConnection> markTrackerDisconnected(String vendorId, String deviceId) {
        return getTrackerState(vendorId, deviceId).map(connection -> {
            connection.setConnected(false);
            connection.touch();
            return connection;
        });
    }

    public Optional<DeviceConnection> updateTrackerAssetId(String vendorId, String deviceId, String assetId) {
        return getTrackerState(vendorId, deviceId).map(connection -> {
            connection.setAssetId(assetId);
            return connection;
        });
    }

    public void submitMessage(String vendorId,
                              String realm,
                              MessageContext.Transport transport,
                              String codecId,
                              DeviceMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        TelematicsVendor vendor = getVendor(vendorId)
                .orElseThrow(() -> new IllegalStateException("Vendor not registered in TelematicsService: " + vendorId));

        DeviceConnection connection = markTrackerConnected(
                vendor,
                message.getDeviceId(),
                realm,
                message.getProtocolName() != null ? message.getProtocolName() : vendor.getProtocol().getProtocolId(),
                codecId,
                transport
        );
        connection.incrementMessageCount();
        connection.touch();

        TelematicsMessageEnvelope envelope = new TelematicsMessageEnvelope(
                vendorId,
                message.getDeviceId(),
                realm,
                connection.getProtocolId().orElse(vendor.getProtocol().getProtocolId()),
                transport,
                Instant.now(),
                message
        );

        messageBrokerService.getProducerTemplate().sendBody(TELEMATICS_MESSAGE_QUEUE, envelope);
    }

    public Optional<DeviceConnection> getTrackerState(String vendorId, String deviceId) {
        return Optional.ofNullable(deviceConnectionMap.get(toDeviceKey(vendorId, deviceId)));
    }

    public List<DeviceConnection> getTrackerStates() {
        return List.copyOf(deviceConnectionMap.values());
    }

    public List<DeviceConnection> getTrackerStatesByVendor(String vendorId) {
        return deviceConnectionMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(vendorId + ":"))
                .map(Map.Entry::getValue)
                .toList();
    }

    public Optional<DeviceConnection> removeTrackerState(String vendorId, String deviceId) {
        return Optional.ofNullable(deviceConnectionMap.remove(toDeviceKey(vendorId, deviceId)));
    }

    public boolean isDeviceConnected(String vendorId, String deviceId) {
        return getTrackerState(vendorId, deviceId)
                .map(DeviceConnection::isConnected)
                .orElse(false);
    }

    public int getConnectedDeviceCount() {
        int count = 0;
        for (DeviceConnection connection : deviceConnectionMap.values()) {
            if (connection.isConnected()) {
                count++;
            }
        }
        return count;
    }

    private void ensureVendorRegistered(TelematicsVendor vendor) {
        if (!vendorMap.containsKey(vendor.getVendorId())) {
            throw new IllegalStateException("Vendor not registered in TelematicsService: " + vendor.getVendorName());
        }
    }

    private void logContainerServices(Container container) {
        StringBuilder sb = new StringBuilder("Container services registered: ");
        ContainerService[] services = container.getServices();
        for (int i = 0; i < services.length; i++) {
            sb.append(services[i].getClass().getSimpleName());
            if (i < services.length - 1) {
                sb.append(", ");
            }
        }
        LOG.info(sb.toString());
    }

    private void logRegisteredVendors() {
        if (vendorMap.isEmpty()) {
            LOG.warning("No telematics vendors registered. Register vendors via TelematicsService.registerVendor(...)");
            return;
        }

        StringBuilder sb = new StringBuilder("Telematics vendors registered: ");
        int i = 0;
        for (TelematicsVendor vendor : vendorMap.values()) {
            sb.append(vendor.getVendorId()).append("(").append(vendor.getVendorName()).append(")");
            if (i < vendorMap.size() - 1) {
                sb.append(", ");
            }
            i++;
        }
        LOG.info(sb.toString());
    }

    private String toDeviceKey(String vendorId, String deviceId) {
        return vendorId + ":" + deviceId;
    }
}
