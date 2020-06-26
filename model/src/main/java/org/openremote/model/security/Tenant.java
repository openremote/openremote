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
package org.openremote.model.security;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * This can be used (among other things) to query the REALM table in JPA queries.
 */
@JsType
@Entity
@Subselect("select * from PUBLIC.REALM") // Map this immutable to an SQL view, don't use/create table
public class Tenant {

    @Id
    protected String id;

    @Column(name = "NAME")
    protected String realm;

    @Formula("(select ra.VALUE from PUBLIC.REALM_ATTRIBUTE ra where ra.REALM_ID = ID and ra.name = 'displayName')")
    protected String displayName;

    @Column(name = "ENABLED")
    protected Boolean enabled;

    @Column(name = "NOT_BEFORE")
    protected Double notBefore; // This will explode in 2038

    // We allow password reset by default
    @Transient
    protected Boolean resetPasswordAllowed = true;

    @Transient
    protected Boolean duplicateEmailsAllowed;

    @Transient
    protected Boolean rememberMe;

    @Transient
    protected String loginTheme;

    @Transient
    protected String accountTheme;

    @Transient
    protected String adminTheme;

    @Transient
    protected String emailTheme;

    @Transient
    protected Integer accessTokenLifespan;

    @JsIgnore
    public Tenant() {
        this(null, null, null, null);
    }

    public Tenant(String id, String realm, String displayName, Boolean enabled) {
        this.id = id;
        this.realm = realm;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    @JsProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsProperty
    @NotNull(message = "{Tenant.realm.NotNull}")
    @Size(min = 3, max = 255, message = "{Tenant.realm.Size}")
    @Pattern(regexp = "[a-zA-Z0-9\\-_]+", message = "{Tenant.realm.Pattern}")
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @JsProperty
    @NotNull(message = "{Tenant.displayName.NotNull}")
    @Size(min = 3, max = 255, message = "{Tenant.displayName.Size}")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsProperty
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Double notBefore) {
        this.notBefore = notBefore;
    }

    public Boolean getResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public Boolean getDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public boolean isActive(double currentTimeMillis) {
        return enabled != null && enabled
            && (notBefore == null || notBefore == 0 || notBefore <= (currentTimeMillis/1000));
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.loginTheme = loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.accountTheme = accountTheme;
    }

    public String getAdminTheme() {
        return adminTheme;
    }

    public void setAdminTheme(String adminTheme) {
        this.adminTheme = adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public void setEmailTheme(String emailTheme) {
        this.emailTheme = emailTheme;
    }



    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", realm='" + realm + '\'' +
            ", displayName='" + displayName + '\'' +
            ", enabled=" + enabled +
            ", notBefore=" + notBefore +
            '}';
    }
}
