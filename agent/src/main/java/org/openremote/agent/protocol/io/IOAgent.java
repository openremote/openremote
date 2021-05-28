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
package org.openremote.agent.protocol.io;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;

import java.util.Optional;

@SuppressWarnings("unchecked")
public abstract class IOAgent<T extends IOAgent<T, U, V>, U extends AbstractIOClientProtocol<U, T, ?, ?, V>, V extends AgentLink<?>> extends Agent<T, U, V> {

    protected IOAgent() {}

    protected IOAgent(String name) {
        super(name);
    }

    public Optional<Boolean> getMessageConvertHex() {
        return getAttributes().getValue(MESSAGE_CONVERT_HEX);
    }

    @SuppressWarnings("unchecked")
    public T setMessageConvertHex(Boolean value) {
        getAttributes().getOrCreate(MESSAGE_CONVERT_HEX).setValue(value);
        return (T)this;
    }

    public Optional<Boolean> getMessageConvertBinary() {
        return getAttributes().getValue(MESSAGE_CONVERT_BINARY);
    }

    @SuppressWarnings("unchecked")
    public T setMessageConvertBinary(Boolean value) {
        getAttributes().getOrCreate(MESSAGE_CONVERT_BINARY).setValue(value);
        return (T)this;
    }

    public Optional<String> getMessageCharset() {
        return getAttributes().getValue(MESSAGE_CHARSET);
    }

    @SuppressWarnings("unchecked")
    public T setMessageCharset(String value) {
        getAttributes().getOrCreate(MESSAGE_CHARSET).setValue(value);
        return (T)this;
    }

    public Optional<Integer> getMessageMaxLength() {
        return getAttributes().getValue(MESSAGE_MAX_LENGTH);
    }

    @SuppressWarnings("unchecked")
    public T setMessageMaxLength(Integer value) {
        getAttributes().getOrCreate(MESSAGE_MAX_LENGTH).setValue(value);
        return (T)this;
    }

    public Optional<String[]> getMessageDelimiters() {
        return getAttributes().getValue(MESSAGE_DELIMITERS);
    }

    @SuppressWarnings("unchecked")
    public T setMessageDelimiters(String[] value) {
        getAttributes().getOrCreate(MESSAGE_DELIMITERS).setValue(value);
        return (T)this;
    }

    public Optional<Boolean> getMessageStripDelimiter() {
        return getAttributes().getValue(MESSAGE_STRIP_DELIMITER);
    }

    @SuppressWarnings("unchecked")
    public T setMessageStripDelimiter(Boolean value) {
        getAttributes().getOrCreate(MESSAGE_STRIP_DELIMITER).setValue(value);
        return (T)this;
    }
}
