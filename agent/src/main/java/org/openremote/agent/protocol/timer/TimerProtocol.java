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
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import org.quartz.CronExpression;

import javax.ws.rs.NotSupportedException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static java.util.logging.Level.FINER;
import static org.openremote.agent.protocol.timer.TimerConfiguration.*;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemFixedBoolean;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This protocol can be used to trigger an {@link AttributeEvent} using a cron based timer.
 * <p>
 * A timer is defined by creating a {@link TimerConfiguration} attribute on an agent. A timer configuration consists of
 * the following Meta Items:
 * <ul>
 * <li>{@link #META_TIMER_CRON_EXPRESSION}: The cron expression of the timer</li>
 * <li>{@link #META_TIMER_ACTION}: The {@link AttributeState} that should be sent when the timer is triggered</li>
 * </ul>
 * <p>
 * {@link AssetAttribute}s can be linked to time triggers to read/write the trigger time and/or enable/disable the
 * trigger. A linked attribute must have a valid {@link MetaItemType#AGENT_LINK} Meta Item and
 * also a {@link #META_TIMER_VALUE_LINK} Meta Item to indicate what value of timer to link to {@link TimerValue}
 */
public class TimerProtocol extends AbstractProtocol {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TimerProtocol.class);

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":timer";
    public static final String PROTOCOL_DISPLAY_NAME = "Timer";
    public static final String META_TIMER_CRON_EXPRESSION = PROTOCOL_NAME + ":cronExpression";
    public static final String META_TIMER_ACTION = PROTOCOL_NAME + ":action";
    public static final String META_TIMER_VALUE_LINK = PROTOCOL_NAME + ":link";
    public static final String META_TIMER_DISABLED = PROTOCOL_NAME + ":disabled";
    protected static final String VERSION = "1.0";

    public static final MetaItemDescriptor META_PROTOCOL_TIMER_ACTION = new MetaItemDescriptorImpl(
        META_TIMER_ACTION,
        ValueType.OBJECT,
            true,
            null,
            null,
            null,
            null,
            false, null, null, null, false);

    public static final MetaItemDescriptor META_PROTOCOL_TIMER_CRON_EXPRESSION = new MetaItemDescriptorImpl(
        META_TIMER_CRON_EXPRESSION,
        ValueType.STRING,
            true,
            null, // TODO Should use TextUtil.REGEXP_PATTERN_CRON_EXPRESSION
        MetaItemDescriptor.PatternFailure.CRON_EXPRESSION.name(),
            null,
                null,
                false, null, null, null);

    public static final MetaItemDescriptor META_PROTOCOL_TIMER_DISABLED = metaItemFixedBoolean(
        META_TIMER_DISABLED,
        ACCESS_PRIVATE,
        false);

    public static final MetaItemDescriptor META_ATTRIBUTE_TIMER_VALUE_LINK = new MetaItemDescriptorImpl(
        META_TIMER_VALUE_LINK,
        ValueType.STRING,
        true,
        "^(" +
            TimerValue.ENABLED + "|" +
            TimerValue.CRON_EXPRESSION + "|" +
            TimerValue.TIME +
            ")$",
        TimerValue.ENABLED + "|" +
            TimerValue.CRON_EXPRESSION + "|" +
            TimerValue.TIME,
        null,
        null,
        false, null, null, null);

    protected static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_TIMER_ACTION,
        META_PROTOCOL_TIMER_CRON_EXPRESSION,
        META_PROTOCOL_TIMER_DISABLED
    );

    protected static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Collections.singletonList(
        META_ATTRIBUTE_TIMER_VALUE_LINK
    );

    protected final Map<AttributeRef, CronExpressionParser> cronExpressionMap = new HashMap<>();
    protected CronScheduler cronScheduler;

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(
                new MetaItem(META_TIMER_CRON_EXPRESSION, null),
                new MetaItem(META_TIMER_ACTION, null)
            );
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            TimerConfiguration.validateTimerConfiguration(protocolConfiguration, result);
        }
        return result;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return new ArrayList<>(ATTRIBUTE_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        // Verify that this is a valid Timer Configuration
        if (!isValidTimerConfiguration(protocolConfiguration)) {
            LOG.warning("Timer Configuration is not valid so it will be ignored: " + protocolRef);
            updateStatus(protocolRef, ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        // Validate the cron expression
        CronExpressionParser expressionParser = getCronExpression(protocolConfiguration)
            .map(CronExpressionParser::new)
            .orElse(null);

        if (expressionParser == null || !expressionParser.isValid()) {
            LOG.warning("Timer cron expression is missing or invalid");
            updateStatus(protocolRef, ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        CronExpression cronExpression = createCronExpression(expressionParser.buildCronExpression());

        if (cronExpression == null) {
            LOG.warning("Timer cron expression is missing or invalid");
            updateStatus(protocolRef, ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        cronExpressionMap.put(protocolRef, expressionParser);

        if (!isTimerDisabled(protocolConfiguration)) {
            getCronScheduler()
                .addOrReplaceJob(
                    getTimerId(protocolRef),
                    cronExpression,
                    () -> doTriggerAction(protocolConfiguration)
                );
            updateStatus(protocolRef, ConnectionStatus.CONNECTED);
        } else {
            updateStatus(protocolRef, ConnectionStatus.DISABLED);
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        AttributeRef protocolConfigRef = protocolConfiguration.getReferenceOrThrow();

        if (cronExpressionMap.remove(protocolConfigRef) != null) {
            getCronScheduler().removeJob(getTimerId(protocolConfigRef));
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        TimerValue timerValue = TimerConfiguration.getValue(attribute).orElse(null);

        if (timerValue == null) {
            LOG.warning("Attribute doesn't have a valid timer value so ignoring write request: " + attribute.getReferenceOrThrow());
            return;
        }

        // Don't remove or alter any running timer just push update back through the system and wait for link/unlink
        // protocol configuration method call
        Optional<String> writeValue = event.
            getValue()
            .flatMap(Values::getString)
            .flatMap(TextUtil::asNonNullAndNonEmpty);

        switch (timerValue) {
            case ENABLED:
                // check event value is a boolean
                boolean enabled = Values.getBoolean(event.getValue()
                    .orElse(null))
                    .orElseThrow(() -> new IllegalStateException("Writing to protocol configuration CONNECTED property requires a boolean value"));

                if (enabled != isTimerDisabled(protocolConfiguration)) {
                    LOG.finer("Timer enabled status is already: " + enabled);
                } else {
                    LOG.fine("Updating timer enabled status: " + enabled);
                    updateLinkedProtocolConfiguration(
                        protocolConfiguration,
                        protocolConfig -> {
                            if (!enabled) {
                                protocolConfig.addMeta(META_PROTOCOL_TIMER_DISABLED);
                            } else {
                                protocolConfig.removeMeta(META_PROTOCOL_TIMER_DISABLED);
                            }
                        }
                    );
                }
                break;
            case CRON_EXPRESSION:
                // Allow writing invalid cron expressions; mean that the trigger will stop working
                // but that is handled gracefully
                if (!writeValue.isPresent()) {
                    LOG.warning("Send to actuator value for time trigger must be a non empty string");
                    return;
                }
                updateTimerValue(new AttributeState(protocolConfiguration.getReferenceOrThrow(), Values.create(writeValue.get().trim())));
                break;
            case TIME:
                if (!writeValue.isPresent()) {
                    LOG.warning("Send to actuator value for time trigger must be a non empty string");
                    return;
                }
                CronExpressionParser parser = cronExpressionMap.get(protocolConfiguration.getReferenceOrThrow());
                if (parser == null) {
                    LOG.info("Ignoring trigger update because current cron expression is invalid");
                    return;
                }

                String[] writeTimeValues = writeValue.get().trim().split(":");
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

                parser.setTime(hours, minutes, seconds);
                updateTimerValue(new AttributeState(protocolConfiguration.getReferenceOrThrow(), Values.create(parser.buildCronExpression())));
                break;
            default:
                throw new NotSupportedException("Unsupported timer value: " + timerValue);
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        TimerValue timerValue = TimerConfiguration.getValue(attribute).orElse(null);

        if (timerValue == null) {
            LOG.warning("Attribute doesn't have a valid timer value: " + attribute.getReferenceOrThrow());
            return;
        }

        LOG.fine("Attribute is linked to timer value: " + timerValue);

        switch (timerValue) {
            case ENABLED:
                updateLinkedAttribute(new AttributeState(attribute.getReferenceOrThrow(), Values.create(!isTimerDisabled(protocolConfiguration))));
                break;
            case CRON_EXPRESSION:
            case TIME:
                CronExpressionParser parser = cronExpressionMap.get(protocolConfiguration.getReferenceOrThrow());
                if (parser == null) {
                    LOG.info("Attribute is linked to an invalid timer so it will be ignored");
                    return;
                }

                Value value = timerValue == TimerValue.CRON_EXPRESSION
                    ? Values.create(parser.buildCronExpression())
                    : Values.create(parser.getFormattedTime());

                    updateLinkedAttribute(new AttributeState(attribute.getReferenceOrThrow(), value));
                break;
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // Nothing to do here
    }

    /**
     * Sends the trigger's attribute state into the processing chain
     */
    protected void doTriggerAction(AssetAttribute triggerConfiguration) {
        if (triggerConfiguration == null) {
            LOG.fine("Cannot execute timer action as timer configuration cannot be found");
            return;
        }

        LOG.info("Executing timer action: " + triggerConfiguration.getReferenceOrThrow());
        getAction(triggerConfiguration)
            .ifPresent(this::sendAttributeEvent);
    }

    /**
     * Update the trigger's {@link #META_TIMER_CRON_EXPRESSION}
     */
    protected void updateTimerValue(AttributeState state) {
        LOG.fine("Updating the timer value: " + state);

        updateLinkedProtocolConfiguration(
            getLinkedProtocolConfiguration(state.getAttributeRef()),
            protocolConfig -> {
                MetaItem.replaceMetaByName(
                    protocolConfig.getMeta(),
                    META_TIMER_CRON_EXPRESSION,
                    state.getValue().orElse(null)
                );
            }
        );
    }

    protected CronScheduler getCronScheduler() {
        if (cronScheduler == null) {
            LOG.fine("Create cron scheduler");
            cronScheduler = new CronScheduler();
        }

        return cronScheduler;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    protected static String getTimerId(AttributeRef timerRef) {
        return timerRef.getEntityId() + ":" + timerRef.getAttributeName();
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

    protected static boolean isTimerDisabled(Attribute protocolConfiguration) {
        return protocolConfiguration.getMetaItem(META_TIMER_DISABLED).isPresent();
    }
}
