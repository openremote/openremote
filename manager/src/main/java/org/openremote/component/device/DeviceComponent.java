package org.openremote.component.device;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.openremote.manager.server.device.DeviceService;

import java.util.Map;

public class DeviceComponent extends DefaultComponent {
    public enum Action {
        ADD,
        REMOVE,
        UPDATE
    }

    public static final String HEADER_DEVICE_ACTION = DeviceComponent.class.getCanonicalName() + ".DEVICE_ACTION";
    protected final DeviceService deviceService;

    public DeviceComponent(CamelContext context, DeviceService deviceService) {
        super(context);
        this.deviceService = deviceService;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint iotEndpoint = new DeviceEndpoint(uri, this, deviceService);
        return iotEndpoint;
    }
}
