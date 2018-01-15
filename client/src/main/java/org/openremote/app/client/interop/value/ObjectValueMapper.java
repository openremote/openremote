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
package org.openremote.app.client.interop.value;

import org.openremote.app.client.rest.EntityReader;
import org.openremote.app.client.rest.EntityWriter;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

public class ObjectValueMapper implements EntityReader<ObjectValue>, EntityWriter<ObjectValue> {

    @Override
    public ObjectValue read(String value) {
        return Values.<ObjectValue>parse(value).orElseThrow(() -> new RuntimeException("Empty JSON data"));
    }

    @Override
    public String write(ObjectValue value) {
        return value.toJson();
    }
}
