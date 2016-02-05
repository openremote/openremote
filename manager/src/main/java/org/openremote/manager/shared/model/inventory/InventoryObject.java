package org.openremote.manager.shared.model.inventory;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.model.Identifiable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsType
@MappedSuperclass
public class InventoryObject extends Identifiable {

    @NotNull
    @Size(min = 3, max = 1023)
    @Column(name = "LABEL")
    public String label;

    @Column(name = "ITEM_PROPERTIES", nullable = true, length = 1048576) // TODO 1MB?
    public String properties;

    @JsIgnore
    protected InventoryObject() {
    }

    @JsIgnore
    public InventoryObject(String label, String id, String type) {
        super(id, type);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "InventoryObject{" +
            "label='" + getLabel() + '\'' +
            ", id=" + getId() +
            ", properties=" + getProperties() +
            '}';
    }
}
