package org.openremote.model.telematics.core;

import org.openremote.model.ContainerService;
import org.openremote.model.telematics.protocol.MessageContext;

/**
 * Service interface for submitting decoded telematics messages into the central processing pipeline.
 */
public interface TelematicsMessagePublisher extends ContainerService {

    void submitMessage(String vendorId,
                       String realm,
                       MessageContext.Transport transport,
                       String codecId,
                       DeviceMessage message);
}
