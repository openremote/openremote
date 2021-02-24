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
package org.openremote.agent.protocol.timer;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class TimerAgent extends Agent<TimerAgent, TimerProtocol, TimerAgent.TimerAgentLink> {

    public static class TimerAgentLink extends AgentLink<TimerAgentLink> {

        protected TimerValue timerValue;

        // For Hydrators
        protected TimerAgentLink() {}

        public TimerAgentLink(String id) {
            super(id);
        }

        public Optional<TimerValue> getTimerValue() {
            return Optional.ofNullable(timerValue);
        }

        public TimerAgentLink setTimerValue(TimerValue timerValue) {
            this.timerValue = timerValue;
            return this;
        }
    }

    public static final ValueDescriptor<CronExpressionParser> TIMER_CRON_EXPRESSION_DESCRIPTOR = new ValueDescriptor<>("Timer cron expression", CronExpressionParser.class);

    public static final AttributeDescriptor<AttributeState> TIMER_ACTION = new AttributeDescriptor<>("timerAction", ValueType.ATTRIBUTE_STATE);

    public static final AttributeDescriptor<CronExpressionParser> TIMER_CRON_EXPRESSION = new AttributeDescriptor<>("timerCronExpression", TIMER_CRON_EXPRESSION_DESCRIPTOR);

    public static final AttributeDescriptor<Boolean> TIMER_ACTIVE = new AttributeDescriptor<>("timerActive", ValueType.BOOLEAN);

    public static final AgentDescriptor<TimerAgent, TimerProtocol, TimerAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        TimerAgent.class, TimerProtocol.class, TimerAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected TimerAgent() {
    }

    public TimerAgent(String name) {
        super(name);
    }

    @Override
    public TimerProtocol getProtocolInstance() {
        return new TimerProtocol(this);
    }

    public Optional<AttributeState> getTimerAction() {
        return getAttributes().getValue(TIMER_ACTION);
    }

    public TimerAgent setTimerAction(AttributeState timerAction) {
        getAttributes().getOrCreate(TIMER_ACTION).setValue(timerAction);
        return this;
    }

    public Optional<CronExpressionParser> getTimerCronExpression() {
        return getAttributes().getValue(TIMER_CRON_EXPRESSION);
    }

    public TimerAgent setTimerCronExpression(CronExpressionParser cronExpression) {
        getAttributes().getOrCreate(TIMER_CRON_EXPRESSION).setValue(cronExpression);
        return this;
    }

    public Optional<Boolean> isTimerActive() {
        return getAttributes().getValue(TIMER_ACTIVE);
    }

    public TimerAgent setTimerActive(boolean timerActive) {
        getAttributes().getOrCreate(TIMER_ACTIVE).setValue(timerActive);
        return this;
    }
}
