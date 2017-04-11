/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.shared.rules;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

/**
 * Rules can be defined in three scopes: global, for a realm, for an asset sub-tree.
 */
@MappedSuperclass
public abstract class Ruleset {

    public enum DeploymentStatus {
        /**
         * Ruleset compiled successfully but is not running, due to failure of other rulesets in same scope.
         */
        READY,

        /**
         * Ruleset has been compiled and is running.
         */
        DEPLOYED,

        /**
         * Ruleset did not compile successfully.
         */
        FAILED
    }

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_MODIFIED", nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    protected Date lastModified;

    @NotNull(message = "{Ruleset.name.NotNull}")
    @Column(name = "NAME", nullable = false)
    @Size(min = 3, max = 255, message = "{Ruleset.name.Size}")
    protected String name;

    @Column(name = "ENABLED", nullable = false)
    protected boolean enabled = true;

    @Lob
    @Column(name = "RULES", nullable = false)
    protected String rules;

    @Transient
    protected DeploymentStatus deploymentStatus;

    public Ruleset() {
    }

    public Ruleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled) {
        this(id, version, createdOn, lastModified, name, enabled, null);
    }

    public Ruleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String rules) {
        this.id = id;
        this.version = version;
        this.createdOn = createdOn;
        this.lastModified = lastModified;
        this.name = name;
        this.enabled = enabled;
        this.rules = rules;
    }

    public Ruleset(String name, String rules) {
        this.name = name;
        this.rules = rules;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    @PreUpdate
    @PrePersist
    public void updateLastModified() { // Don't call this setLastModified(), it confuses gwt-jackson
        setLastModified(new Date());
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public DeploymentStatus getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(DeploymentStatus deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", enabled='" + enabled + '\'' +
                '}';
    }
}
