/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.agent.protocol.macro;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import javax.validation.constraints.Min;
import java.util.Optional;

@Entity
public class MacroAgent extends Agent<MacroAgent, MacroProtocol, MacroAgent.MacroAgentLink> {

    public static final class MacroAgentLink extends AgentLink<MacroAgentLink> {

        @Min(0)
        protected Integer actionIndex;

        // For Hydrators
        protected MacroAgentLink() {}

        public MacroAgentLink(String id) {
            super(id);
        }

        public Optional<Integer> getActionIndex() {
            return Optional.ofNullable(actionIndex);
        }

        public MacroAgentLink setActionIndex(Integer actionIndex) {
            this.actionIndex = actionIndex;
            return this;
        }
    }

    public static final ValueDescriptor<MacroAction> MACRO_ACTION_VALUE = new ValueDescriptor<>("Macro action", MacroAction.class);

    public static final AttributeDescriptor<MacroAction[]> MACRO_ACTIONS = new AttributeDescriptor<>("macroActions", MACRO_ACTION_VALUE.asArray());

    public static final AttributeDescriptor<Boolean> MACRO_DISABLED = new AttributeDescriptor<>("macroDisabled", ValueType.BOOLEAN);

    public static final AttributeDescriptor<AttributeExecuteStatus> MACRO_STATUS = new AttributeDescriptor<>("macroStatus", ValueType.EXECUTION_STATUS);

    public static final AgentDescriptor<MacroAgent, MacroProtocol, MacroAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        MacroAgent.class, MacroProtocol.class, MacroAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected MacroAgent() {
    }

    public MacroAgent(String name) {
        super(name);
    }

    @Override
    public MacroProtocol getProtocolInstance() {
        return new MacroProtocol(this);
    }

    public Optional<MacroAction[]> getMacroActions() {
        return getAttributes().getValue(MACRO_ACTIONS);
    }

    public MacroAgent setMacroActions(MacroAction[] actions) {
        getAttributes().getOrCreate(MACRO_ACTIONS).setValue(actions);
        return this;
    }

    public Optional<Boolean> isMacroDisabled() {
        return getAttributes().getValue(MACRO_DISABLED);
    }

    public MacroAgent setMacroDisabled(boolean macroDisabled) {
        getAttributes().getOrCreate(MACRO_DISABLED).setValue(macroDisabled);
        return this;
    }

    public Optional<AttributeExecuteStatus> getMacroStatus() {
        return getAttributes().getValue(MACRO_STATUS);
    }

    @Override
    public boolean isConfigurationAttribute(String attributeName) {
        return !attributeName.equals(MACRO_STATUS.getName()) && super.isConfigurationAttribute(attributeName);
    }
}
