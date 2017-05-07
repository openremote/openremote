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
package org.openremote.agent3.protocol.trigger.time;

import org.openremote.agent3.protocol.trigger.AbstractTriggerHandler;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;
import org.quartz.CronExpression;

import javax.ws.rs.NotSupportedException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.logging.Level.FINER;
import static org.openremote.agent3.protocol.trigger.time.CronExpressionParser.parseNumberExpression;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class TimeTriggerHandler extends AbstractTriggerHandler {

    private static final Logger LOG = Logger.getLogger(TimeTriggerHandler.class.getName());
    public static final String TIME_TRIGGER_HANDLER_NAME = "Time Trigger Handler";
    protected final Map<AttributeRef, CronExpressionParser> cronExpressionMap = new HashMap<>();
    protected CronScheduler cronScheduler;

    @Override
    protected String getName() {
        return TIME_TRIGGER_HANDLER_NAME;
    }

    @Override
    protected boolean isValidValue(Value triggerValue) {
        return Values.getString(triggerValue).filter(TimeTriggerHandler::isValidCronExpression).isPresent();
    }

    @Override
    protected void registerTrigger(AttributeRef triggerRef, Value value, boolean isEnabled) {
        Optional<String> expression = Values.getString(value).flatMap(TextUtil::asNonNullAndNonEmpty);
        CronExpressionParser cronExpressionParser = null;

        if (expression.isPresent()) {
            cronExpressionParser = new CronExpressionParser(expression.get());
        } else {
            LOG.warning("Time trigger value is not a cron expression string");
        }

        synchronized (cronExpressionMap) {
            cronExpressionMap.put(triggerRef, cronExpressionParser);
        }

        if (isEnabled && cronExpressionParser != null && cronExpressionParser.isValid()) {
            @SuppressWarnings("ConstantConditions")
            CronExpression cronExpression = createCronExpression(cronExpressionParser.buildCronExpression());
            if (cronExpression != null) {
                getCronScheduler().addOrReplaceJob(getTriggerId(triggerRef), cronExpression, () -> {
                    LOG.fine("Quartz job has triggered");

                    // This is executed when the job triggers
                    executeTrigger(triggerRef);
                });
            }
        }
    }

    @Override
    protected void unregisterTrigger(AttributeRef triggerRef) {
        getCronScheduler().removeJob(getTriggerId(triggerRef));
    }

    @Override
    protected void registerAttribute(AttributeRef attributeRef, AttributeRef triggerRef, String propertyName) {
        TimeTriggerProperty property = TimeTriggerProperty.fromString(propertyName);
        if (property == null) {
            LOG.warning("Property name is not valid for time trigger handler: " + propertyName);
            return;
        }

        CronExpressionParser parser = getCronExpressionParser(triggerRef);
        if (parser == null) {
            LOG.info("Attribute is linked to an invalid trigger so it will be ignored");
            return;
        }

        switch (property) {
            case CRON_EXPRESSION:
                // Pass the entire cron expression through to the attribute
                updateAttributeValue(new AttributeState(attributeRef, Values.create(parser.buildCronExpression())));
                break;
            case TIME:
                // Pass the 24h formatted time
                updateAttributeValue(new AttributeState(attributeRef, Values.create(parser.getFormattedTime())));
                break;
        }
    }

    @Override
    protected void unregisterAttribute(AttributeRef attributeRef, AttributeRef triggerRef) {

    }

    @Override
    protected void processAttributeWrite(AssetAttribute attribute, AssetAttribute protocolConfiguration, String propertyName, AttributeEvent event) {
        TimeTriggerProperty triggerProperty = TimeTriggerProperty.fromString(propertyName);
        if (triggerProperty == null) {
            LOG.warning("Attribute is using an invalid trigger property name '" + propertyName + "': " + attribute.getReferenceOrThrow());
            return;
        }

        // Don't remove or alter any running trigger just push update back through the system
        // and wait for add, remove or update trigger call
        Optional<String> writeValue = event.getValue().flatMap(Values::getString).flatMap(TextUtil::asNonNullAndNonEmpty);

        if (!writeValue.isPresent()) {
            LOG.warning("Send to actuator value for time trigger must be a non empty string");
            return;
        }

        String value = writeValue.get().trim();

        switch (triggerProperty) {
            case CRON_EXPRESSION:
                // Allow writing invalid cron expressions; mean that the trigger will stop working
                // but that is handled gracefully
                updateTriggerValue(new AttributeState(protocolConfiguration.getReferenceOrThrow(), Values.create(value)));
                break;
            case TIME:
                CronExpressionParser parser = getCronExpressionParser(protocolConfiguration.getReferenceOrThrow());
                if (parser == null) {
                    LOG.info("Ignoring trigger update because current cron expression is invalid");
                    return;
                }

                String[] writeTimeValues = value.split(":");
                Integer hours;
                Integer minutes;
                Integer seconds;

                if (writeTimeValues.length != 3
                    || (hours = parseNumberExpression(writeTimeValues[0])) == null
                    || (minutes = parseNumberExpression(writeTimeValues[1])) == null
                    || (seconds = parseNumberExpression(writeTimeValues[2])) == null) {
                    LOG.info("Expected value to be in format HH:MM:SS, actual: " + writeValue);
                    return;
                }

                parser.setTime(hours, minutes, seconds);
                updateTriggerValue(new AttributeState(protocolConfiguration.getReferenceOrThrow(), Values.create(parser.buildCronExpression())));
                break;
            default:
                throw new NotSupportedException("Unsupported trigger property");
        }

    }

    protected CronScheduler getCronScheduler() {
        if (cronScheduler == null) {
            LOG.fine("Create cron scheduler");
            cronScheduler = new CronScheduler();
        }

        return cronScheduler;
    }

    protected CronExpressionParser getCronExpressionParser(AttributeRef triggerRef) {
        synchronized (cronExpressionMap) {
            return cronExpressionMap.get(triggerRef);
        }
    }

    public static CronExpression createCronExpression(String expression) {
        CronExpression cronExpression = null;
        try {
            cronExpression = new CronExpression(expression);
        } catch (ParseException e) {
            LOG.log(FINER, "Failed to create cron expression from string: " + expression, e);
        }

        return cronExpression;
    }

    public static boolean isValidCronExpression(String expression) {
        return !isNullOrEmpty(expression) && createCronExpression(expression) != null;
    }

    protected static String getTriggerId(AttributeRef triggerRef) {
        return triggerRef.getEntityId() + ":" + triggerRef.getAttributeName();
    }
}
