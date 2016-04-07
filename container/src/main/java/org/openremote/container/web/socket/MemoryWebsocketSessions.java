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
package org.openremote.container.web.socket;

import javax.websocket.Session;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryWebsocketSessions extends ConcurrentHashMap<String, Session> implements WebsocketSessions {

    @Override
    public void add(Session websocketSession) {
        super.put(websocketSession.getId(), websocketSession);
    }

    @Override
    public void remove(Session websocketSession) {
        super.remove(websocketSession.getId());
    }

    @Override
    public Session get(String sessionId) {
        return super.get(sessionId);
    }

    @Override
    public Collection<Session> getAll() {
        return super.values();
    }

    @Override
    public void start() throws Exception {
        // noop
    }

    @Override
    public void stop() throws Exception {
        clear();
    }
}
