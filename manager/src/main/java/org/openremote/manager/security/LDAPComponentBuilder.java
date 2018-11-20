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
package org.openremote.manager.security;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;

public class LDAPComponentBuilder {

    private String name;
    private String providerId;
    private String realmId;
    private String providerType;
    private MultivaluedHashMap<String, String> ldapConfig;

    public LDAPComponentBuilder() {
        providerType = "org.keycloak.storage.UserStorageProvider";
        ldapConfig = new MultivaluedHashMap<>();
    }

    public LDAPComponentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public LDAPComponentBuilder setProviderId(String providerId) {
        this.providerId = providerId;
        return this;
    }

    public LDAPComponentBuilder setRealmId(String realmId) {
        this.realmId = realmId;
        return this;
    }

    public LDAPComponentBuilder setVendor(Vendor vendor) {
        ldapConfig.add(LDAPConstants.VENDOR, vendor.toString());
        return this;
    }

    public LDAPComponentBuilder setEditMode(EditMode editMode) {
        ldapConfig.add(LDAPConstants.EDIT_MODE, editMode.toString());
        return this;
    }

    public LDAPComponentBuilder setUserNameLDAPAttribute(String usernameLDAPAttribute) {
        ldapConfig.add(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, usernameLDAPAttribute);
        return this;
    }

    public LDAPComponentBuilder setRDNLDAPAttribute(String rdnLDAPAttribute) {
        ldapConfig.add(LDAPConstants.RDN_LDAP_ATTRIBUTE, rdnLDAPAttribute);
        return this;
    }

    public LDAPComponentBuilder setUUIDLDAPAttribute(String uuidLDAPAttribute) {
        ldapConfig.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, uuidLDAPAttribute);
        return this;
    }

    public LDAPComponentBuilder setUserObjectClasses(String userObjectClasses) {
        ldapConfig.add(LDAPConstants.USER_OBJECT_CLASSES, userObjectClasses);
        return this;
    }

    public LDAPComponentBuilder setConnectionUrl(String connectionUrl) {
        ldapConfig.add(LDAPConstants.CONNECTION_URL, connectionUrl);
        return this;
    }

    public LDAPComponentBuilder setUsersDn(String usersDn) {
        ldapConfig.add(LDAPConstants.USERS_DN, usersDn);
        return this;
    }

    public LDAPComponentBuilder setAuthType(AuthType authType) {
        ldapConfig.add(LDAPConstants.AUTH_TYPE, authType.toString());
        return this;
    }

    public LDAPComponentBuilder setBindDn(String bindDn) {
        ldapConfig.add(LDAPConstants.BIND_DN, bindDn);
        return this;
    }

    public LDAPComponentBuilder setBindCredential(String bindCredential) {
        ldapConfig.add(LDAPConstants.BIND_CREDENTIAL, bindCredential);
        return this;
    }

    public LDAPComponentBuilder setCustomUserSearchFilter(String customUserSearchFilter) {
        ldapConfig.add(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, customUserSearchFilter);
        return this;
    }

    public LDAPComponentBuilder setSearchScope(int searchScope) {
        ldapConfig.add(LDAPConstants.SEARCH_SCOPE, String.valueOf(searchScope));
        return this;
    }

    public LDAPComponentBuilder setUseTrustStoreSPI(UseTrustStoreSpi useTruststoreSpi) {
        ldapConfig.add(LDAPConstants.USE_TRUSTSTORE_SPI, useTruststoreSpi.toString());
        return this;
    }

    public LDAPComponentBuilder setConnectionPooling(boolean connectionPooling) {
        ldapConfig.add(LDAPConstants.CONNECTION_POOLING, String.valueOf(connectionPooling));
        return this;
    }

    public LDAPComponentBuilder setPagination(boolean pagination) {
        ldapConfig.add(LDAPConstants.PAGINATION, String.valueOf(pagination));
        return this;
    }

    public LDAPComponentBuilder setBatchSizeForSync(int batchSizeForSync) {
        ldapConfig.add(LDAPConstants.BATCH_SIZE_FOR_SYNC, String.valueOf(batchSizeForSync));
        return this;
    }

    /**
     * Period to fully sync the users, including users that didn't changed
     *
     * @param periodInSeconds set to -1 to disable
     * @return
     */
    public LDAPComponentBuilder setFullSyncPeriod(int periodInSeconds) {
        ldapConfig.add("fullSyncPeriod", String.valueOf(periodInSeconds));
        return this;
    }

    /**
     * Period to sync changed users, excluding users that didn't changed
     *
     * @param periodInSeconds set to -1 to disable
     * @return
     */
    public LDAPComponentBuilder setChangedSyncPeriod(int periodInSeconds) {
        ldapConfig.add("changedSyncPeriod", String.valueOf(periodInSeconds));
        return this;
    }

    public LDAPComponentBuilder setKerberosAuthentication(boolean allowKerberosAuthentication) {
        ldapConfig.add("allowKerberosAuthentication", String.valueOf(allowKerberosAuthentication));
        return this;
    }

    public LDAPComponentBuilder setDebug(boolean debug) {
        ldapConfig.add("debug", String.valueOf(debug));
        return this;
    }

    public LDAPComponentBuilder setPriority(int priority) {
        ldapConfig.add("priority", String.valueOf(priority));
        return this;
    }

    public ComponentRepresentation build() {
        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setName(name);
        componentRepresentation.setParentId(realmId);
        componentRepresentation.setProviderType(providerType);
        componentRepresentation.setProviderId(providerId);
        componentRepresentation.setConfig(ldapConfig);

        return componentRepresentation;
    }

    public class LDAPConstants {
        public static final String LDAP_PROVIDER = "ldap";

        public static final String VENDOR = "vendor";

        public static final String USERNAME_LDAP_ATTRIBUTE = "usernameLDAPAttribute";
        public static final String RDN_LDAP_ATTRIBUTE = "rdnLDAPAttribute";
        public static final String UUID_LDAP_ATTRIBUTE = "uuidLDAPAttribute";
        public static final String USER_OBJECT_CLASSES = "userObjectClasses";

        public static final String UID = "uid";


        public static final String CONNECTION_URL = "connectionUrl";
        public static final String USERS_DN = "usersDn";
        public static final String BIND_DN = "bindDn";
        public static final String BIND_CREDENTIAL = "bindCredential";

        public static final String AUTH_TYPE = "authType";

        public static final String USE_TRUSTSTORE_SPI = "useTruststoreSpi";

        public static final String SEARCH_SCOPE = "searchScope";
        public static final String CONNECTION_POOLING = "connectionPooling";
        public static final String PAGINATION = "pagination";

        public static final String EDIT_MODE = "editMode";

        // Count of users processed per single transaction during sync process
        public static final String BATCH_SIZE_FOR_SYNC = "batchSizeForSync";


        // Custom user search filter
        public static final String CUSTOM_USER_SEARCH_FILTER = "customUserSearchFilter";

    }

    public enum Vendor {
        AD("ad"),
        RHDS("rhds"),
        TIVOLI("tivoli"),
        NOVELL_EDIRECTORY("edirectory"),
        OTHER("other");

        private String value;

        Vendor(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum EditMode {
        READ_ONLY,
        WRITEABLE,
        UNSYNCED
    }

    public enum AuthType {
        NONE("none"),
        SIMPLE("simple");

        private String value;

        AuthType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum UseTrustStoreSpi {
        ALWAYS("always"),
        NEVER("never"),
        LDAPS_ONLY("ldapsOnly");

        private String value;

        UseTrustStoreSpi(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
