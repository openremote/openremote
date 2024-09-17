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
package org.openremote.agent.protocol.snmp;

import org.openremote.model.asset.agent.AgentLink;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public class SNMPAgentLink extends AgentLink<SNMPAgentLink> {

    @NotNull
    protected String oid;

    // For Hydrators
    protected SNMPAgentLink() {
    }

    public SNMPAgentLink(String id, String oid) {
        super(id);

        this.oid = oid;
    }

    public Optional<String> getOID() {
        return Optional.ofNullable(oid);
    }

    public SNMPAgentLink setOID(String oid) {
        this.oid = oid;
        return this;
    }
}
