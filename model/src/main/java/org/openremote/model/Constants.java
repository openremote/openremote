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
package org.openremote.model;

public interface Constants {

    String KEYCLOAK_CLIENT_ID = "openremote";
    String MASTER_REALM = "master";
    String MASTER_REALM_ADMIN_USER = "admin";
    String REALM_ADMIN_ROLE = "admin";
    String AUTH_CONTEXT = "AUTH_CONTEXT";
    int ACCESS_TOKEN_LIFESPAN_SECONDS = 60; // 1 minute
    String PERSISTENCE_SEQUENCE_ID_GENERATOR = "SEQUENCE_ID_GENERATOR";
    String PERSISTENCE_UNIQUE_ID_GENERATOR = "UNIQUE_ID_GENERATOR";
    String PERSISTENCE_JSON_VALUE_TYPE = "json_value";
    String PERSISTENCE_JSON_OBJECT_TYPE = "json_object";
    String PERSISTENCE_JSON_ARRAY_TYPE = "json_array";
    String PERSISTENCE_STRING_ARRAY_TYPE = "string_array";
    String NAMESPACE = "urn:openremote";
    String PROTOCOL_NAMESPACE = NAMESPACE + ":protocol";
    String ASSET_NAMESPACE = NAMESPACE + ":asset";
    String ASSET_META_NAMESPACE = ASSET_NAMESPACE + ":meta";
    String DEFAULT_DATETIME_FORMAT ="dd. MMM yyyy HH:mm:ss zzz";
    String DEFAULT_DATETIME_FORMAT_MILLIS ="dd. MMM yyyy HH:mm:ss:SSS zzz";
    String DEFAULT_DATE_FORMAT ="dd. MMM yyyy";
    String DEFAULT_TIME_FORMAT ="HH:mm:ss";

    String SETUP_EMAIL_USER = "SETUP_EMAIL_USER";
    String SETUP_EMAIL_HOST = "SETUP_EMAIL_HOST";
    String SETUP_EMAIL_PASSWORD = "SETUP_EMAIL_PASSWORD";
    String SETUP_EMAIL_PORT = "SETUP_EMAIL_PORT";
    int SETUP_EMAIL_PORT_DEFAULT = 25;
    String SETUP_EMAIL_TLS = "SETUP_EMAIL_TLS";
    boolean SETUP_EMAIL_TLS_DEFAULT = true;
    String SETUP_EMAIL_FROM = "SETUP_EMAIL_FROM";
    String SETUP_EMAIL_FROM_DEFAULT = "admin@openremote.io";
    String REQUEST_HEADER_REALM = "Auth-Realm";
}
