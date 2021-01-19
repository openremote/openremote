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
package org.openremote.agent.protocol.timer;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;
import org.quartz.CronExpression;

import javax.ws.rs.NotSupportedException;
import java.text.ParseException;
import java.util.logging.Logger;

import static java.util.logging.Level.FINER;
import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This protocol can be used to trigger an {@link AttributeEvent} using a cron based timer.
 * <p>
 * It is also possible to link {@link Attribute}s to this {@link Protocol} to allow reading information about the
 * timer instance and also to allow altering the timer instance (e.g. altering what time a daily timer triggers).
 */
public class TimerProtocol extends AbstractProtocol<TimerAgent, TimerAgent.TimerAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TimerProtocol.class);

    public static final String PROTOCOL_DISPLAY_NAME = "Timer";

    protected boolean active;
    protected CronExpressionParser expressionParser;
    protected CronScheduler cronScheduler;

    public TimerProtocol(TimerAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return expressionParser != null ? expressionParser.buildCronExpression() : "";
    }

    @Override
    protected void doStart(Container container) throws Exception {

        boolean isActive = agent.isTimerActive().orElse(true);
        expressionParser = agent.getTimerCronExpression().orElse(null);

        if (expressionParser == null || !expressionParser.isValid()) {
            LOG.warning("Timer cron expression is missing or invalid");
            throw new IllegalArgumentException("Timer cron expression is missing or invalid");
        }

        updateActive(isActive);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (expressionParser != null) {
            expressionParser = null;
            getCronScheduler().removeJob(getTimerId(agent));
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, TimerAgent.TimerAgentLink agentLink) throws RuntimeException {
        TimerValue timerValue = getOrThrowAgentLinkProperty(agentLink.getTimerValue(), "timer value");

        LOG.fine("Attribute is linked to timer value: " + timerValue);
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        updateLinkedAttributeTimerValue(timerValue, attributeRef);
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, TimerAgent.TimerAgentLink agentLink) {
        // Nothing to do here
    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, TimerAgent.TimerAgentLink agentLink, AttributeEvent event, Object processedValue) {

        // Should never be null as attribute would've only been linked if it has a valid timer value
        TimerValue timerValue = getOrThrowAgentLinkProperty(agentLink.getTimerValue(), "timer value");

        switch (timerValue) {
            case ACTIVE:
                // check event value is a boolean
                boolean active = Values.getBoolean(processedValue)
                    .orElseThrow(() -> new IllegalStateException("Writing to agent active attribute requires a boolean value"));

                if (this.active == active) {
                    return;
                } else {
                    LOG.fine("Updating timer enabled status: " + active);
                    updateActive(active);
                    updateAgentAttribute(new AttributeState(agent.getId(), TimerAgent.TIMER_ACTIVE.getName(), active));
                    updateLinkedAttributesTimerValues();
                }
                break;
            case CRON_EXPRESSION:
                this.expressionParser = Values.getValueCoerced(processedValue, CronExpressionParser.class)
                    .orElseThrow(() -> new IllegalStateException("Writing to agent cron expression attribute requires a string value"));
                updateActive(agent.isTimerActive().orElse(true));
                updateAgentAttribute(new AttributeState(agent.getId(), TimerAgent.TIMER_CRON_EXPRESSION.getName(), this.expressionParser));
                updateLinkedAttributesTimerValues();
                break;
            case TIME:
                String writeValue = Values.getString(processedValue).orElse(null);
                if (TextUtil.isNullOrEmpty(writeValue)) {
                    LOG.warning("Send to actuator value for time trigger must be a non empty string");
                    return;
                }

                String[] writeTimeValues = writeValue.trim().split(":");
                Integer hours;
                Integer minutes;
                Integer seconds;

                if (writeTimeValues.length != 3
                    || (hours = TextUtil.asInteger(writeTimeValues[0]).orElse(null)) == null
                    || (minutes = TextUtil.asInteger(writeTimeValues[1]).orElse(null)) == null
                    || (seconds = TextUtil.asInteger(writeTimeValues[2]).orElse(null)) == null) {
                    LOG.info("Expected value to be in format HH:MM:SS, actual: " + writeValue);
                    return;
                }

                expressionParser.setTime(hours, minutes, seconds);
                updateActive(agent.isTimerActive().orElse(true));
                updateAgentAttribute(new AttributeState(agent.getId(), TimerAgent.TIMER_CRON_EXPRESSION.getName(), this.expressionParser));
                updateLinkedAttributesTimerValues();
                break;
            default:
                throw new NotSupportedException("Unsupported timer value: " + timerValue);
        }
    }

    protected void updateActive(boolean active) {

        CronExpression cronExpression = createCronExpression(expressionParser.buildCronExpression());
        this.active = active && cronExpression != null;

        if (cronExpression == null) {
            LOG.warning("Timer cron expression is missing or invalid so timer will be inactive");
        }

        if (this.active) {
            getCronScheduler()
                .addOrReplaceJob(
                    getTimerId(agent),
                    cronExpression,
                    this::doTriggerAction
                );
            setConnectionStatus(ConnectionStatus.CONNECTED);
        } else {
            getCronScheduler().removeJob(getTimerId(agent));
            setConnectionStatus(ConnectionStatus.STOPPED);
        }
    }

    protected void updateLinkedAttributesTimerValues() {
        linkedAttributes.entrySet()
            .forEach(es -> {
                TimerAgent.TimerAgentLink agentLink = getAgent().getAgentLink(es.getValue());
                if (agentLink != null && agentLink.getId().equals(agent.getId()) && agentLink.getTimerValue().isPresent()) {
                    updateLinkedAttributeTimerValue(agentLink.getTimerValue().get(), es.getKey());
                }
            });
    }

    protected void updateLinkedAttributeTimerValue(TimerValue timerValue, AttributeRef attributeRef) {
        if (timerValue == null) {
            return;
        }

        // Push current value through to the linked attribute
        switch (timerValue) {
            case ACTIVE:
                updateLinkedAttribute(new AttributeState(attributeRef, active));
                break;
            case CRON_EXPRESSION:
            case TIME:
                if (expressionParser == null || !expressionParser.valid) {
                    LOG.info("Attribute is linked to an invalid timer so cannot extract requested timer values");
                    updateLinkedAttribute(new AttributeState(attributeRef));
                    return;
                }

                String value = timerValue == TimerValue.CRON_EXPRESSION
                    ? expressionParser.buildCronExpression()
                    : expressionParser.getFormattedTime();

                updateLinkedAttribute(new AttributeState(attributeRef, value));
                break;
        }
    }

    /**
     * Sends the trigger's attribute state into the processing chain
     */
    protected void doTriggerAction() {
        agent.getTimerAction()
            .map(action -> {
                LOG.info("Executing timer action for protocol: " + this);
                sendAttributeEvent(action);
                return action;
            }).orElseGet(() -> {
                LOG.warning("Timer action is not set or is invalid so cannot execute: " + this);
                return null;
        });
    }

    protected CronScheduler getCronScheduler() {
        if (cronScheduler == null) {
            LOG.fine("Create cron scheduler");
            cronScheduler = new CronScheduler();
        }

        return cronScheduler;
    }

    protected static String getTimerId(TimerAgent agent) {
        return "TimerProtocol-" + agent.getId();
    }

    protected static CronExpression createCronExpression(String expression) {
        CronExpression cronExpression = null;
        try {
            cronExpression = new CronExpression(expression);
        } catch (ParseException e) {
            LOG.log(FINER, "Failed to create cron expression from string: " + expression, e);
        }

        return cronExpression;
    }
}
