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
package org.openremote.manager.client.admin.agent;

import elemental.json.JsonArray;
import elemental.json.impl.JsonUtil;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.http.EntityReader;

import java.util.ArrayList;
import java.util.List;

public class AgentArrayMapper implements EntityReader<Agent[]> {

    @Override
    public Agent[] read(String value) {
        JsonArray jsonArray = JsonUtil.parse(value);
        List<Agent> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Agent agent = new Agent(jsonArray.getObject(i));
            // TODO: URGENT FIX NEEDED! ENTITY TYPE QUERIES ARE NOT SUPPORTED SO HERE WE GET _ALL_ ENTITIES!
            if (agent.getType().equals(Agent.TYPE)) {
                list.add(agent);
            }
        }
        return list.toArray(new Agent[list.size()]);
    }

}
