package org.openremote.model.telematics.core;

import org.openremote.model.telematics.session.DeviceConnection;

import java.util.Optional;

/**
 * Vendor-specific message processor for create/update asset operations.
 */
public interface TelematicsMessageHandler {

    String getVendorId();

    Optional<String> process(TelematicsMessageEnvelope envelope, DeviceConnection connection);
}
