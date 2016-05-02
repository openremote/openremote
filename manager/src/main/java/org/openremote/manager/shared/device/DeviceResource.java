package org.openremote.manager.shared.device;

public class DeviceResource extends Resource {
    protected String uri;
    protected boolean passive;
    protected boolean constant;

    public DeviceResource() {
    }

    public DeviceResource(String name, String description, Type type, Access access, boolean collection, String units) {
        super(name, description, type, access, collection, units);
    }

    public String getUri() {
        return uri;
    }

    public boolean isPassive() {
        return passive;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }
}
