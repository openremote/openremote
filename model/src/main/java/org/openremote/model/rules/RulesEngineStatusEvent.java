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

import org.openremote.model.event.shared.SharedEvent;

/**
 * Published by the server when a rules engine changes its {@link RulesEngineStatus}.
 */
public class RulesEngineStatusEvent extends SharedEvent {

    protected String engineId;
    protected RulesEngineInfo engineInfo;

    protected RulesEngineStatusEvent() {
    }

    public RulesEngineStatusEvent(long timestamp, String engineId, RulesEngineInfo engineInfo) {
        super(timestamp);
        this.engineId = engineId;
        this.engineInfo = engineInfo;
    }

    public String getEngineId() {
        return engineId;
    }

    public RulesEngineInfo getEngineInfo() {
        return engineInfo;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "engineId='" + engineId + '\'' +
            ", engineInfo=" + engineInfo +
            '}';
    }
}
