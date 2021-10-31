/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.provisioning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.Constants;
import org.openremote.model.security.ClientRole;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Date;

import static javax.persistence.DiscriminatorType.STRING;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "provisioning_config")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = STRING)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    @JsonSubTypes.Type(name = "x509", value = X509ProvisioningConfig.class)
)
public abstract class ProvisioningConfig<T> {

    @Id
    @Column(name = "ID")
    @Min(1)
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_MODIFIED", nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    protected Date lastModified;

    @NotNull
    @Column(name = "NAME", nullable = false)
    @Size(min = 3, max = 255, message = "{ProvisioningConfig.name.Size}")
    protected String name;

    @Column(name = "TYPE", nullable = false, updatable = false, insertable = false, length = 100, columnDefinition = "char(100)")
    @Size(min = 3, max = 100, message = "{ProvisioningConfig.type.Size}")
    protected String type;

    @Column(name = "REALM")
    protected String realm;

    @Column(name = "ASSET_TEMPLATE", columnDefinition = "text")
    protected String assetTemplate;

    @Column(name = "RESTRICTED_USER", nullable = false)
    protected boolean restrictedUser;

    @Column(name = "ROLES", columnDefinition = "client_role[]")
    @org.hibernate.annotations.Type(type = Constants.PERSISTENCE_STRING_ARRAY_TYPE)
    @Enumerated(EnumType.STRING)
    protected ClientRole[] userRoles;

    @Column(name = "DISABLED", nullable = false)
    protected boolean disabled = false;

    protected ProvisioningConfig() {}

    protected ProvisioningConfig(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getLastModified() {
        return lastModified;
    }

    @PreUpdate
    @PrePersist
    protected void updateLastModified() {
        lastModified = new Date();
    }

    public String getName() {
        return name;
    }

    public ProvisioningConfig<T> setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public String getAssetTemplate() {
        return assetTemplate;
    }

    public ProvisioningConfig<T> setAssetTemplate(String assetTemplate) {
        this.assetTemplate = assetTemplate;
        return this;
    }

    public boolean isRestrictedUser() {
        return restrictedUser;
    }

    public ProvisioningConfig<T> setRestrictedUser(boolean restrictedUser) {
        this.restrictedUser = restrictedUser;
        return this;
    }

    public ClientRole[] getUserRoles() {
        return userRoles;
    }

    public ProvisioningConfig<T> setUserRoles(ClientRole[] userRoles) {
        this.userRoles = userRoles;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public ProvisioningConfig<T> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /**
     * Implementors must annotate the data field with @Column as JPA (Hibernate) doesn't work with generic fields
     */
    public abstract T getData();
    public abstract ProvisioningConfig<T> setData(T data);

    public String getRealm() {
        return realm;
    }

    public ProvisioningConfig<T> setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", createdOn=" + createdOn +
            ", lastModified=" + lastModified +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", realm='" + realm + '\'' +
            ", assetTemplateSet='" + (assetTemplate != null) + '\'' +
            ", restrictedUser=" + restrictedUser +
            ", userRoles=" + Arrays.toString(userRoles) +
            ", disabled=" + disabled +
            ", data=" + getData() +
            '}';
    }
}
