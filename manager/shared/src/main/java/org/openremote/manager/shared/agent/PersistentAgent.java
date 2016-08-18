package org.openremote.manager.shared.agent;

import elemental.json.JsonObject;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import java.util.Date;

import static org.openremote.manager.shared.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "AGENT")
public class PersistentAgent {

    public static final String NO_CONNECTOR_ASSIGNED_TYPE = "urn:openremote:connector:none";
    public static final String NO_DESCRIPTION = "-";

    @Id
    @Column(name = "ID", length = 22)
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "OBJ_VERSION")
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false)
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn;

    @NotNull
    @Column(name = "NAME")
    protected String name;

    @NotNull
    @Column(name = "DESCRIPTION")
    protected String description = NO_DESCRIPTION;

    @NotNull
    @Column(name = "ENABLED")
    protected boolean enabled;

    @NotNull
    @Column(name = "CONNECTOR_TYPE")
    protected String connectorType = NO_CONNECTOR_ASSIGNED_TYPE;

    @Column(name = "CONNECTOR_SETTINGS", columnDefinition = "json")
    @org.hibernate.annotations.Type(type = "json")
    protected JsonObject connectorSettings;

    public PersistentAgent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public JsonObject getConnectorSettings() {
        return connectorSettings;
    }

    public void setConnectorSettings(JsonObject connectorSettings) {
        this.connectorSettings = connectorSettings;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", enabled=" + enabled +
            ", connectorType='" + connectorType + '\'' +
            '}';
    }
}
