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
import com.vladmihalcea.hibernate.type.array.EnumArrayType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.openremote.model.security.ClientRole;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;

import static javax.persistence.DiscriminatorType.STRING;
import static org.openremote.model.Constants.*;

@TypeDefs({
    @TypeDef(
        typeClass = EnumArrayType.class,
        defaultForType = ClientRole[].class,
        parameters = {
            @Parameter(
                name = EnumArrayType.SQL_ARRAY_TYPE,
                value = "client_role"
            )
        }
    )
})
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
    @Column(name = "NAME", nullable = false, columnDefinition = "text")
    protected String name;

    @Column(name = "TYPE", columnDefinition = "text")
    protected String type;

    @Column(name = "REALM", columnDefinition = "text")
    protected String realm;

    @Column(name = "ASSET_TEMPLATE", columnDefinition = "text")
    protected String assetTemplate;

    @Column(name = "RESTRICTED_USER", nullable = false)
    protected boolean restrictedUser;

    @Column(name = "ROLES", columnDefinition = "client_role[]")
    protected ClientRole[] userRoles;

    @Column(name = "DISABLED", nullable = false)
    protected boolean disabled;

    /**
     * JSON serialisable data for a specific implementation
     */
    @Column(name = "DATA", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected T data;

    public Long getId() {
        return id;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getLastModified() {
        return lastModified;
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

    public T getData() {
        return data;
    }

    public ProvisioningConfig<T> setData(T data) {
        this.data = data;
        return this;
    }

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
            ", assetTemplate='" + assetTemplate + '\'' +
            ", restrictedUser=" + restrictedUser +
            ", userRoles=" + Arrays.toString(userRoles) +
            ", disabled=" + disabled +
            ", data=" + data +
            '}';
    }
}
