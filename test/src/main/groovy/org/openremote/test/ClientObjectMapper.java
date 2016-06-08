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
package org.openremote.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonSerializationContext;
import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.github.nmorel.gwtjackson.client.exception.JsonDeserializationException;
import com.github.nmorel.gwtjackson.client.exception.JsonSerializationException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Simulate GWT Jackson by using regular Jackson mapper.
 */
public class ClientObjectMapper<T> implements ObjectMapper<T> {

    private static final Logger LOG = Logger.getLogger(ClientObjectMapper.class.getName());

    final protected com.fasterxml.jackson.databind.ObjectMapper jsonMapper;
    final Class<T> type;

    public ClientObjectMapper(com.fasterxml.jackson.databind.ObjectMapper jsonMapper, Class<T> type) {
        this.jsonMapper = jsonMapper;
        this.type = type;
    }

    @Override
    public T read(String input) throws JsonDeserializationException {
        try {
            LOG.info("Reading JSON string as '" + type + "': " + input);
            return jsonMapper.readValue(input, type);
        } catch (IOException ex) {
            throw new JsonDeserializationException(ex);
        }
    }

    @Override
    public T read(String input, JsonDeserializationContext ctx) throws JsonDeserializationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String write(T value) throws JsonSerializationException {
        try {
            LOG.info("Writing JSON string of: " + value);
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new JsonSerializationException(ex);
        }
    }

    @Override
    public String write(T value, JsonSerializationContext ctx) throws JsonSerializationException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
