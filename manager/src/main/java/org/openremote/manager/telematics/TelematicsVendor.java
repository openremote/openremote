package org.openremote.manager.telematics;

import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.telematics.asset.TelematicsAssetMapper;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.core.TelematicsMessageHandler;
import org.openremote.model.telematics.parameter.ParseableValueDescriptor;
import org.openremote.model.telematics.parameter.TelematicsParameterRegistry;
import org.openremote.model.telematics.protocol.DeviceCommand;
import org.openremote.model.telematics.protocol.DeviceProtocol;
import org.openremote.model.telematics.session.DeviceSessionManager;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public interface TelematicsVendor {

    String getVendorId();

    String getVendorName();

    Class<? extends DeviceCommand> getCommandClass();

    Class<? extends DeviceMessage> getMessageClass();

    DeviceProtocol getProtocol();

    TelematicsParameterRegistry<? extends ParseableValueDescriptor<?>> getParameterRegistry();

    TelematicsAssetMapper<? extends Asset<?>> getAssetMapper();

    DeviceSessionManager getSessionManager();

    CommandMapper<? extends DeviceCommand, ? extends DeviceMessage> getCommandMapper();

    Set<String> getTransports();

    TelematicsMessageHandler createMessageHandler(Logger logger,
                                                  AssetStorageService assetStorageService,
                                                  AssetProcessingService assetProcessingService);

    interface CommandMapper<C extends DeviceCommand, M extends DeviceMessage> {
        boolean supports(C command);

        Map<String, Object> toOutboundPayload(C command);

        Optional<C> fromInboundResponse(M message);
    }
}
