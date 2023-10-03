/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.agent.protocol.mail;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public abstract class AbstractMailAgent<T extends AbstractMailAgent<T, U, V>, U extends AbstractMailProtocol<T, U, V>, V extends AgentLink<V>> extends Agent<T, U, V> {

    @NotNull
    public static final AttributeDescriptor<String> PROTOCOL = new AttributeDescriptor<>("protocol", ValueType.TEXT);
    @NotNull
    public static final AttributeDescriptor<UsernamePassword> USERNAME_AND_PASSWORD = Agent.USERNAME_AND_PASSWORD.withOptional(false);
    @NotNull
    public static final AttributeDescriptor<String> HOST = Agent.HOST.withOptional(false);
    @NotNull
    public static final AttributeDescriptor<Integer> PORT = Agent.PORT.withOptional(false);
    public static final AttributeDescriptor<Integer> CHECK_INTERVAL_SECONDS = new AttributeDescriptor<>("checkIntervalSeconds", ValueType.POSITIVE_INTEGER).withOptional(true);
    public static final AttributeDescriptor<Boolean> DELETE_PROCESSED_MAIL = new AttributeDescriptor<>("deleteProcessedMail", ValueType.BOOLEAN).withOptional(true);
    public static final AttributeDescriptor<Boolean> PREFER_HTML = new AttributeDescriptor<>("preferHTML", ValueType.BOOLEAN).withOptional(true);
    public static final AttributeDescriptor<String> MAIL_FOLDER_NAME = new AttributeDescriptor<>("mailFolderName", ValueType.TEXT).withOptional(true);

    protected AbstractMailAgent() {
    }

    public AbstractMailAgent(String name) {
        super(name);
    }

    public Optional<String> getProtocol() {
        return getAttributes().getValue(PROTOCOL);
    }

    @SuppressWarnings("unchecked")
    public T setProtocol(String value) {
        getAttributes().getOrCreate(PROTOCOL).setValue(value);
        return (T) this;
    }

    public Optional<Integer> getCheckIntervalSeconds() {
        return getAttributes().getValue(CHECK_INTERVAL_SECONDS);
    }

    @SuppressWarnings("unchecked")
    public T setCheckIntervalSeconds(Integer value) {
        getAttributes().getOrCreate(CHECK_INTERVAL_SECONDS).setValue(value);
        return (T) this;
    }

    public Optional<Boolean> getDeleteProcessedMail() {
        return getAttributes().getValue(DELETE_PROCESSED_MAIL);
    }

    @SuppressWarnings("unchecked")
    public T setDeleteProcessedMail(Boolean value) {
        getAttributes().getOrCreate(DELETE_PROCESSED_MAIL).setValue(value);
        return (T) this;
    }

    public Optional<Boolean> getPreferHTML() {
        return getAttributes().getValue(PREFER_HTML);
    }

    @SuppressWarnings("unchecked")
    public T setPreferHTML(Boolean value) {
        getAttributes().getOrCreate(PREFER_HTML).setValue(value);
        return (T) this;
    }

    public Optional<String> getMailFolderName() {
        return getAttributes().getValue(MAIL_FOLDER_NAME);
    }

    @SuppressWarnings("unchecked")
    public T setMailFolderName(String value) {
        getAttributes().getOrCreate(MAIL_FOLDER_NAME).setValue(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUsernamePassword(UsernamePassword value) {
        super.setUsernamePassword(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setHost(String value) {
        super.setHost(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPort(Integer value) {
        super.setPort(value);
        return (T) this;
    }
}
