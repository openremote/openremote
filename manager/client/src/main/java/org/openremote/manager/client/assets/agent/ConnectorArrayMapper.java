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
package org.openremote.manager.client.assets.agent;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.github.nmorel.gwtjackson.client.annotation.JsonMixIns;
import org.openremote.manager.client.interop.jackson.DefaultJsonMixin;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;

@JsonMixIns({@JsonMixIns.JsonMixIn(target = Connector.class, mixIn = DefaultJsonMixin.class)})
public interface ConnectorArrayMapper
    extends ObjectMapper<Connector[]>,
    EntityReader<Connector[]>,
    EntityWriter<Connector[]> {
}
