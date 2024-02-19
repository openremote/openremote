/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.homeassistant;

import org.openremote.model.asset.agent.AgentLink;

import java.util.Optional;

public class HomeAssistantAgentLink extends AgentLink<HomeAssistantAgentLink> {

    protected String domainId;
    protected String entityId;

    protected HomeAssistantAgentLink() {
    }

    public HomeAssistantAgentLink(String id, String domainId, String entityId) {
        super(id);
        this.domainId = domainId;
        this.entityId = entityId;
    }

    public Optional<String> getDomainId() {
        return Optional.ofNullable(domainId);
    }

    public Optional<String> getEntityId() {
        return Optional.ofNullable(entityId);
    }
}
