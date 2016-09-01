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
package org.openremote.manager.client.event;

import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonSerializationContext;
import com.github.nmorel.gwtjackson.client.exception.JsonDeserializationException;
import com.github.nmorel.gwtjackson.client.exception.JsonSerializationException;
import org.openremote.manager.shared.event.Event;

/* TODO This generator fails because Event is an abstract class, so serializers/deserializers are generated for ALL subclasses which won't compile
@JsonMixIns({@JsonMixIns.JsonMixIn(target = Event.class, mixIn = DefaultJsonMixin.class)})
public interface EventMapper extends ObjectMapper<Event> {
}
*/
public class EventMapper {

    public Event read(String input) throws JsonDeserializationException {
        throw new UnsupportedOperationException("TODO: NOT IMPLEMENTED");
    }

    public Event read(String input, JsonDeserializationContext ctx) throws JsonDeserializationException {
        throw new UnsupportedOperationException("TODO: NOT IMPLEMENTED");
    }

    public String write(Event value) throws JsonSerializationException {
        throw new UnsupportedOperationException("TODO: NOT IMPLEMENTED");
    }

    public String write(Event value, JsonSerializationContext ctx) throws JsonSerializationException {
        throw new UnsupportedOperationException("TODO: NOT IMPLEMENTED");
    }
}