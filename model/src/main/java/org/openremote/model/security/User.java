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
package org.openremote.model.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * This can be used (among other things) to query the USER_ENTITY table in JPA queries.
 */
@Entity
@Subselect("select * from PUBLIC.USER_ENTITY") // Map this immutable to an SQL view, don't use/create table
public class User {
    public static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

    @Formula("(select r.NAME from PUBLIC.REALM r where r.ID = REALM_ID)")
    protected String realm;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Id
    protected String id;

    @JsonIgnore
    @Column(name = "USERNAME")
    protected String username;

    @Column(name = "FIRST_NAME")
    protected String firstName;

    @Column(name = "LAST_NAME")
    protected String lastName;

    @Column(name = "EMAIL")
    protected String email;

    @Column(name = "ENABLED")
    protected Boolean enabled;

    @Transient
    protected boolean serviceAccount;

    public User() {
    }

    public String getRealm() {
        return realm;
    }

    public User setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public String getRealmId() {
        return realmId;
    }

    public User setRealmId(String realmId) {
        this.realmId = realmId;
        return this;
    }

    public String getId() {
        return id;
    }

    public User setId(String id) {
        this.id = id;
        return this;
    }

    @NotNull(message = "{User.username.NotNull}")
    @Size(min = 3, max = 255, message = "{User.username.Size}")
    @Pattern(regexp = "[a-zA-Z0-9-_]+", message = "{User.username.Pattern}")
    @JsonProperty
    public String getUsername() {
        return serviceAccount ? username.replace(SERVICE_ACCOUNT_PREFIX, "") : username;
    }

    @JsonSetter("username")
    public User setUsername(String username) {
        if (this.serviceAccount && !username.startsWith(SERVICE_ACCOUNT_PREFIX)) {
            username = SERVICE_ACCOUNT_PREFIX + username;
        }
        this.username = username;
        return this;
    }

    public boolean isServiceAccount() {
        return serviceAccount;
    }

    public User setServiceAccount(boolean serviceAccount) {
        if (serviceAccount == this.serviceAccount) {
            return this;
        }

        this.serviceAccount = serviceAccount;

        if (username != null) {
            username = serviceAccount ? SERVICE_ACCOUNT_PREFIX + username.replace(SERVICE_ACCOUNT_PREFIX, "") : username.replace(SERVICE_ACCOUNT_PREFIX, "");
        }
        return this;
    }

    @Size(min = 0, max = 127, message = "{User.firstName.Size}")
    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Size(min = 0, max = 127, message = "{User.lastName.Size}")
    public String getLastName() {
        return lastName;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    @Email(message = "{User.email.Email}")
    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public User setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getFullName() {
        return getUsername() + " (" + getFirstName() + " " + getLastName() + ")";
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
            "realm='" + realm + '\'' +
            ", id='" + id + '\'' +
            ", username='" + username + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", enabled=" + enabled +
            '}';
    }
}
