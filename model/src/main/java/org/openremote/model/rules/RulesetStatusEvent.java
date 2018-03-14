/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.rules;

import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.event.shared.TenantScopedEvent;

/**
 * Published by the server when a a protocol configuration of an
 * agent changes its {@link ConnectionStatus}.
 *
 * TODO Unused
 */
public class RulesetStatusEvent extends TenantScopedEvent {

    protected Ruleset ruleset;
    protected String error;

    protected RulesetStatusEvent() {
    }

    public RulesetStatusEvent(long timestamp, String realmId, Ruleset ruleset, String error) {
        super(timestamp, realmId);
        this.ruleset = ruleset;
        this.error = error;
    }

    public Ruleset getRuleset() {
        return ruleset;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ruleset=" + ruleset +
            ", error='" + error + '\'' +
            '}';
    }
}
