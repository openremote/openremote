/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.http;

/**
 * Compile-time constants shared by OpenAPI annotations.
 */
public final class OpenApiDescriptions {

    public static final String REALM = "OpenRemote realm name.";
    public static final String ASSET_ID = "Globally unique 22-character asset identifier.";
    public static final String ATTRIBUTE_NAME = "Attribute name as defined by the asset model.";
    public static final String USER_ID = "Identity-provider user identifier.";
    public static final String CLIENT_ID = "Identity-provider client identifier.";
    public static final String TIMESTAMP = "Unix timestamp in milliseconds.";

    public static final String EXAMPLE_REALM = "building";
    public static final String EXAMPLE_ASSET_ID = "7A6p4AnLTkKxJUCQAAABAA";
    public static final String EXAMPLE_ATTRIBUTE_NAME = "temperature";
    public static final String EXAMPLE_USER_ID = "2f1c17e5-72b8-4dbe-9f8d-c49e66f82e10";
    public static final String EXAMPLE_CLIENT_ID = "openremote";
    public static final String EXAMPLE_TIMESTAMP = "1767225600000";

    private OpenApiDescriptions() {
    }
}
