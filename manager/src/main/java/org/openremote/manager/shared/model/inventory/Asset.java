package org.openremote.manager.shared.model.inventory;

public class Asset extends InventoryObject {

    protected Device[] devices;

    protected Asset[] children;

    protected Asset() {
    }

    public Asset(String label, String id, String type, Asset... children) {
        this(label, id, type, new Device[0], children);
    }

    public Asset(String label, String id, String type, Device[] devices, Asset... children) {
        super(label, id, type);
        this.children = children;
        this.devices = devices;
    }

    public Device[] getDevices() {
        return devices;
    }

    public Asset[] getChildren() {
        return children;
    }
}
