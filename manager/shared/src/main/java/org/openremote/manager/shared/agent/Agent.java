/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.shared.agent;

import elemental.json.JsonObject;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

import static org.openremote.manager.shared.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "AGENT")
public class Agent {

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

    public Agent() {
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
            ", connectorSettings='" + (connectorSettings != null ? connectorSettings.toJson() : "null") + '\'' +
            '}';
    }
}
