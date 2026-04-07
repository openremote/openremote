/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.dispersion;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.query.filter.ValuePredicate;

import java.util.Optional;

public class PollutantDispersionAgentLink extends AgentLink<PollutantDispersionAgentLink> {

    @NotNull
    @JsonPropertyDescription("Defines how this linked attribute is used by the dispersion protocol")
    protected PollutantDispersionLinkRole role = PollutantDispersionLinkRole.OUTPUT_CONCENTRATION;

    @JsonPropertyDescription("Predicate evaluated against source values; propagation triggers when this transitions from false to true")
    protected ValuePredicate triggerPredicate;

    @JsonPropertyDescription("Emission rate in grams per second used when this link is SOURCE_TRIGGER")
    protected Double emissionRateGramsPerSecond;

    // For Hydrators
    protected PollutantDispersionAgentLink() {
    }

    public PollutantDispersionAgentLink(String id) {
        super(id);
    }

    public PollutantDispersionLinkRole getRole() {
        return role;
    }

    public PollutantDispersionAgentLink setRole(PollutantDispersionLinkRole role) {
        this.role = role;
        return this;
    }

    public Optional<ValuePredicate> getTriggerPredicate() {
        return Optional.ofNullable(triggerPredicate);
    }

    public PollutantDispersionAgentLink setTriggerPredicate(ValuePredicate triggerPredicate) {
        this.triggerPredicate = triggerPredicate;
        return this;
    }

    public Optional<Double> getEmissionRateGramsPerSecond() {
        return Optional.ofNullable(emissionRateGramsPerSecond);
    }

    public PollutantDispersionAgentLink setEmissionRateGramsPerSecond(Double emissionRateGramsPerSecond) {
        this.emissionRateGramsPerSecond = emissionRateGramsPerSecond;
        return this;
    }
}
