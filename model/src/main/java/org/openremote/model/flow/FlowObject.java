//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.openremote.model.flow;

import org.openremote.model.IdentifiableEntity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;


@MappedSuperclass
public class FlowObject implements IdentifiableEntity<FlowObject> {

    @Id
    @Column(name = "ID", length = 43) // TODO Is this ideal long-term for reads when keys must be distributed on cluster?
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    public String id;

    @NotNull
    @Size(min = 3, max = 255)
    @Column(name = "MODEL_TYPE")
    public String type;

    @Column(name = "LABEL")
    public String label;


    protected FlowObject() {
    }


    public FlowObject(String id, String type, String label) {
        this.id = id;
        this.type = type;
        this.label = label;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public FlowObject setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public boolean isOfType(String type) {
        return this.getType().equals(type);
    }

    public String toTypeIdString() {
        return this.type + ':' + this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isLabelEmpty() {
        return this.getLabel() == null || this.getLabel().length() == 0;
    }

    public String getDefaultedLabel() {
        return this.isLabelEmpty() ? "Unnamed " + this.getClass().getSimpleName() : this.getLabel();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            FlowObject that = (FlowObject) o;
            return this.getId().equals(that.getId());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", label='" + label + '\'' +
            '}';
    }
}
