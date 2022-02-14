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
package org.openremote.test.protocol;

import org.openremote.model.asset.agent.AgentLink;

import java.util.Optional;

public class MockAgentLink extends AgentLink<MockAgentLink> {

    protected String requiredValue;

    // For Hydrators
    protected MockAgentLink() {
    }

    protected MockAgentLink(String id) {
        super(id);
    }

    public Optional<String> getRequiredValue() {
        return Optional.ofNullable(requiredValue);
    }

    public MockAgentLink setRequiredValue(String requiredValue) {
        this.requiredValue = requiredValue;
        return this;
    }
}
