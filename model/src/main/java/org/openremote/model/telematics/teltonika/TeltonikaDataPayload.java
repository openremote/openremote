package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.AbstractPayload;

import java.util.Map;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class TeltonikaDataPayload extends AbstractPayload {

    private static final System.Logger LOG = System.getLogger(TeltonikaDataPayload.class.getName() + "." + PROTOCOL.name());

    public TeltonikaDataPayload(Map<String, Object> payloadMap) {
        super();

        for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
            try {
                this.put(TeltonikaValueDescriptors.getByName(entry.getKey()).orElseThrow(), entry.getValue());
            }catch (Exception e) {
                LOG.log(System.Logger.Level.ERROR, "Failed to map Teltonika payload key: " + entry.getKey() + " with value: " + entry.getValue());
                if(entry.getKey().equals("ts")) {
                    LOG.log(System.Logger.Level.INFO, "Timestamp value type: " + entry.getValue().getClass().getName());
                }
                this.put(new TeltonikaValueDescriptor<>(entry.getKey(), String.class, ignored -> entry.getValue().toString()), entry.getValue());
            }
        }
    }

    @Override
    public Long getTimestamp() {
        return (Long) this.get(this.getValueDescriptorById(TeltonikaValueDescriptors.timestamp.getName()));
    }
}
