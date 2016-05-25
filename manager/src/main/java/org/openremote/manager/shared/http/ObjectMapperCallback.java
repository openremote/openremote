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
package org.openremote.manager.shared.http;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import elemental.json.JsonValue;
import org.openremote.manager.shared.Consumer;

public class ObjectMapperCallback<T> extends AbstractCallback<T> {

    final protected ObjectMapper<T> objectMapper;

    public ObjectMapperCallback(ObjectMapper<T> objectMapper, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        super(onSuccess, onFailure);
        this.objectMapper = objectMapper;
    }

    public ObjectMapperCallback(ObjectMapper<T> objectMapper, int expectedStatusCode, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        super(expectedStatusCode, onSuccess, onFailure);
        this.objectMapper = objectMapper;
    }

    @Override
    protected T readMessageBody(int responseCode, Object entity) {
        return objectMapper.read(((JsonValue) entity).toJson());
    }
}