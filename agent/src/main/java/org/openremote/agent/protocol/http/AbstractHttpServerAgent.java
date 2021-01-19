/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.agent.protocol.http;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

import static org.openremote.agent.protocol.http.HttpClientAgent.VALUE_HTTP_METHOD;

public abstract class AbstractHttpServerAgent<T extends AbstractHttpServerAgent<T, U, V>, U extends AbstractHttpServerProtocol<U, T, V>, V extends AgentLink<?>> extends Agent<T, U, V> {

    public static final AttributeDescriptor<String> DEPLOYMENT_PATH = new AttributeDescriptor<>("deploymentPath", ValueType.TEXT);

    public static final AttributeDescriptor<HttpMethod[]> ALLOWED_HTTP_METHODS = new AttributeDescriptor<>("allowedHTTPMethods", VALUE_HTTP_METHOD.asArray());
    public static final AttributeDescriptor<String[]> ALLOWED_ORIGINS = new AttributeDescriptor<>("allowedOrigins", ValueType.TEXT.asArray());
    public static final AttributeDescriptor<Boolean> ROLE_BASED_SECURITY = new AttributeDescriptor<>("roleBasedSecurity", ValueType.BOOLEAN);

    protected AbstractHttpServerAgent() {}

    protected AbstractHttpServerAgent(String name) {
        super(name);
    }

    public Optional<String> getDeploymentPath() {
        return getAttributes().getValue(DEPLOYMENT_PATH);
    }

    @SuppressWarnings("unchecked")
    public T setDeploymentPath(String value) {
        getAttributes().getOrCreate(DEPLOYMENT_PATH).setValue(value);
        return (T)this;
    }

    public Optional<HttpMethod[]> getAllowedHTTPMethods() {
        return getAttributes().getValue(ALLOWED_HTTP_METHODS);
    }

    @SuppressWarnings("unchecked")
    public T setAllowedHTTPMethods(HttpMethod[] value) {
        getAttributes().getOrCreate(ALLOWED_HTTP_METHODS).setValue(value);
        return (T)this;
    }

    public Optional<String[]> getAllowedOrigins() {
        return getAttributes().getValue(ALLOWED_ORIGINS);
    }

    @SuppressWarnings("unchecked")
    public T setAllowedOrigins(String[] value) {
        getAttributes().getOrCreate(ALLOWED_ORIGINS).setValue(value);
        return (T)this;
    }

    public Optional<Boolean> isRoleBasedSecurity() {
        return getAttributes().getValue(ROLE_BASED_SECURITY);
    }

    @SuppressWarnings("unchecked")
    public T setRoleBasedSecurity(Boolean value) {
        getAttributes().getOrCreate(ROLE_BASED_SECURITY).setValue(value);
        return (T)this;
    }
}
