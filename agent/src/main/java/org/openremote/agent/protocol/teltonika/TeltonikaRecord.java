package org.openremote.agent.protocol.teltonika;

import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.geo.GeoJSONPoint;

/**
 * Represents a single decoded Teltonika AVL record.
 *
 * Each record contains:
 * - IMEI (device identifier)
 * - Timestamp
 * - GPS location
 * - Validity flag
 * - IO elements (parsed as OpenRemote attributes)
 */
public class TeltonikaRecord {

    private String imei;
    private long timestamp;
    private GeoJSONPoint location;
    private boolean valid;
    private AttributeMap attributes;
    private int globalMask; // For GH3000 codec

    public TeltonikaRecord() {
        this.attributes = new AttributeMap();
        this.valid = true;
    }

    // Getters and setters

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public GeoJSONPoint getLocation() {
        return location;
    }

    public void setLocation(GeoJSONPoint location) {
        this.location = location;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public AttributeMap getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributeMap attributes) {
        this.attributes = attributes;
    }

    public int getGlobalMask() {
        return globalMask;
    }

    public void setGlobalMask(int globalMask) {
        this.globalMask = globalMask;
    }

    @Override
    public String toString() {
        return "TeltonikaRecord{" +
                "imei='" + imei + '\'' +
                ", timestamp=" + timestamp +
                ", location=" + location +
                ", valid=" + valid +
                ", attributes=" + attributes.size() + " attrs" +
                '}';
    }
}

