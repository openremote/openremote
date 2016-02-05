package org.openremote.manager.shared.model;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsType
@MappedSuperclass
public abstract class Identifiable {

    @Id
    @Column(name = "ID")
    public String id;

    @NotNull
    @Size(min = 3, max = 255)
    @Column(name = "MODEL_TYPE")
    public String type; // URI

    @JsIgnore
    protected Identifiable() {
    }

    @JsIgnore
    public Identifiable(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public boolean isOfType(String type) {
        return getType().equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identifiable that = (Identifiable) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toTypeIdString() {
        return type + ':' + id;
    }

}
