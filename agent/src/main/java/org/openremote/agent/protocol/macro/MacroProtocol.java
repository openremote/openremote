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
package org.openremote.agent.protocol.macro;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.macro.MacroConfiguration.getMacroActionIndex;
import static org.openremote.agent.protocol.macro.MacroConfiguration.isValidMacroConfiguration;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE;

/**
 * This protocol is responsible for executing macros.
 * <p>
 * It expects a {@link AttributeExecuteStatus} as the attribute event value on the {@link #processLinkedAttributeWrite}.
 * The protocol will then try to perform the request on the linked macro protocol configuration.
 * <p>
 * {@link AssetAttribute}s can also read/write the macro configuration's {@link MacroAction} values by using the
 * {@link #META_MACRO_ACTION_INDEX} Meta Item with the index of the {@link MacroAction} to link to.
 */
public class MacroProtocol extends AbstractProtocol {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MacroProtocol.class);

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":macro";
    public static final String PROTOCOL_DISPLAY_NAME = "Macro";
    public static final String META_MACRO_ACTION = PROTOCOL_NAME + ":action";
    public static final String META_MACRO_ACTION_INDEX = PROTOCOL_NAME + ":actionIndex";
    protected static final String VERSION = "1.0";
    protected static final MacroAction EMPTY_ACTION = new MacroAction(new AttributeState(new AttributeRef("ENTITY_ID", "ATTRIBUTE_NAME"), null));

    protected static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Collections.singletonList(
        new MetaItemDescriptorImpl(META_MACRO_ACTION, ValueType.OBJECT, true, null, null, null, EMPTY_ACTION.toObjectValue(), false, null, null, null)
    );

    protected static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Collections.singletonList(
        new MetaItemDescriptorImpl(META_MACRO_ACTION_INDEX, ValueType.NUMBER, false, REGEXP_PATTERN_INTEGER_POSITIVE, MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE.name(), null, null, false, null, null, null)
    );

    class MacroExecutionTask {

        AttributeRef attributeRef;
        List<MacroAction> actions;
        boolean repeat;
        boolean cancelled;
        ScheduledFuture scheduledFuture;
        int iteration = -1;

        public MacroExecutionTask(AttributeRef attributeRef, List<MacroAction> actions, boolean repeat) {
            this.attributeRef = attributeRef;
            this.actions = actions;
            this.repeat = repeat;
        }

        void start() {
            executions.put(attributeRef, this);
            updateLinkedAttribute(new AttributeState(attributeRef, AttributeExecuteStatus.RUNNING.asValue()));
            run();
        }

        void cancel() {
            LOG.fine("Macro Execution cancel");
            scheduledFuture.cancel(false);
            cancelled = true;
            executions.remove(attributeRef);
            updateLinkedAttribute(new AttributeState(attributeRef, AttributeExecuteStatus.CANCELLED.asValue()));
        }

        private void run() {
            if (cancelled) {
                return;
            }

            if (iteration >= 0) {
                // Process the execution of the next action
                MacroAction action = actions.get(iteration);
                AttributeState actionState = action.getAttributeState();

                // send attribute event
                sendAttributeEvent(actionState);
            }

            boolean isLast = iteration == actions.size() - 1;
            boolean restart = isLast && repeat;

            if (restart) {
                iteration = 0;
            } else {
                iteration++;
            }

            if ((isLast && !restart)) {
                executions.remove(attributeRef);
                // Update the command Status of this attribute
                updateLinkedAttribute(new AttributeState(attributeRef, AttributeExecuteStatus.COMPLETED.asValue()));
                return;
            }

            // Get next execution delay
            int delayMillis = actions.get(iteration).getDelayMilliseconds();

            // Schedule the next iteration
            scheduledFuture = executorService.schedule(this::run, delayMillis > 0 ? delayMillis : 0);
        }
    }

    protected final Map<AttributeRef, List<MacroAction>> macroMap = new ConcurrentHashMap<>();
    protected final Map<AttributeRef, MacroExecutionTask> executions = new ConcurrentHashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(new MetaItem(META_MACRO_ACTION, EMPTY_ACTION.toObjectValue()));
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            MacroConfiguration.validateMacroConfiguration(protocolConfiguration, result);
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
        // Protocol configuration is actually a Macro Configuration
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();

        // Check macro configuration is valid
        if (!isValidMacroConfiguration(protocolConfiguration)) {
            LOG.fine("Macro configuration is not valid: " + protocolConfiguration);
            updateStatus(macroRef, ConnectionStatus.ERROR);
            // Put an empty list of actions against this macro
            macroMap.put(macroRef, Collections.emptyList());
        } else {
            // Store the macro actions for later execution requests
            macroMap.put(macroRef, MacroConfiguration.getMacroActions(protocolConfiguration));
            updateStatus(macroRef, ConnectionStatus.CONNECTED);
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();
        macroMap.remove(macroRef);
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();

        // Check for executable meta item
        if (attribute.isExecutable()) {
            LOG.fine("Macro linked attribute is marked as executable so it will be linked to the firing of the macro");
            // Update the command Status of this attribute
            updateLinkedAttribute(
                new AttributeState(
                    attribute.getReferenceOrThrow(),
                    protocolConfiguration.isEnabled()
                        ? AttributeExecuteStatus.READY.asValue()
                        : AttributeExecuteStatus.DISABLED.asValue()
                )
            );
            return;
        }

        // Check for action index or default to index 0
        int actionIndex = getMacroActionIndex(attribute)
            .orElse(0);

        // Pull the macro action value out with the same type as the linked attribute
        // otherwise push a null value through to the attribute
        List<MacroAction> actions = getMacroActions(macroRef);
        Value actionValue = null;

        if (actions.isEmpty()) {
            LOG.fine("No actions are available for the linked macro, maybe it is disabled?: " + macroRef);
        } else {
            actionIndex = Math.min(actions.size(), Math.max(0, actionIndex));
            actionValue = actions.get(actionIndex).getAttributeState().getValue().orElse(null);
            LOG.fine("Attribute is linked to the value of macro action index: actionIndex");
        }

        if (actionValue != null) {
            // Verify the type of the attribute matches the action value
            if (attribute
                .getType()
                .map(AttributeValueDescriptor::getValueType)
                .orElse(null) != actionValue.getType()) {
                // Use a value of null so it is clear that the attribute isn't linked correctly
                actionValue = null;
            }
        }

        // Push the value of this macro action into the attribute
        updateLinkedAttribute(new AttributeState(attribute.getReferenceOrThrow(), actionValue));
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {

        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        if (attribute.isExecutable()) {
            // This is a macro execution related write operation
            AttributeExecuteStatus status = event.getValue()
                .flatMap(Values::getString)
                .flatMap(AttributeExecuteStatus::fromString)
                .orElse(null);
            AttributeRef attributeRef = event.getAttributeRef();

            // Check if it's a cancellation request
            if (status == AttributeExecuteStatus.REQUEST_CANCEL) {
                LOG.fine("Request received to cancel macro execution: " + event);
                executions.computeIfPresent(attributeRef,
                    (attributeRef1, macroExecutionTask) -> {
                        macroExecutionTask.cancel();
                        return macroExecutionTask;
                    }
                );
                return;
            }

            // If protocol configuration is disabled then nothing to do here
            if (!protocolConfiguration.isEnabled()) {
                LOG.fine("Protocol configuration is disabled so cannot be executed: " + protocolConfiguration.getReferenceOrThrow());
                return;
            }

            List<MacroAction> actions = getMacroActions(protocolConfiguration.getReferenceOrThrow());

            if (actions.isEmpty()) {
                LOG.fine("No actions to execute");
                return;
            }

            executeMacro(attributeRef, actions, status == AttributeExecuteStatus.REQUEST_REPEATING);
            return;
        }

        // Assume this is a write to a macro action value (default to index 0)
        int actionIndex = getMacroActionIndex(attribute).orElse(0);

        // Extract macro actions from protocol configuration rather than modify the in memory ones
        List<MacroAction> actions = MacroConfiguration.getMacroActions(protocolConfiguration);

        if (actions.isEmpty()) {
            LOG.fine("No actions are available for the linked macro, maybe it is disabled?: " + protocolConfiguration.getReferenceOrThrow());
        } else {
            actionIndex = Math.min(actions.size(), Math.max(0, actionIndex));
            LOG.fine("Updating macro action [" + actionIndex + "] value to: " + event.getValue().map(Value::toJson).orElse(""));
            MacroAction action = actions.get(actionIndex);
            action.setAttributeState(new AttributeState(action.getAttributeState().getAttributeRef(), event.getValue().orElse(null)));
            MetaItem[] actionMeta = actions
                .stream()
                .map(MacroAction::toMetaItem)
                .toArray(MetaItem[]::new);

            updateLinkedProtocolConfiguration(
                protocolConfiguration,
                protocolConfig -> MetaItem.replaceMetaByName(protocolConfig.getMeta(), META_MACRO_ACTION, Arrays.asList(actionMeta))
            );
        }
    }

    protected List<MacroAction> getMacroActions(AttributeRef protocolConfigurationRef) {
        List<MacroAction> macroActions = macroMap.get(protocolConfigurationRef);

        if (macroActions == null || macroActions.isEmpty()) {
            LOG.fine("No macro actions found for macro configuration: " + protocolConfigurationRef);
            return Collections.emptyList();
        }

        return macroActions;
    }

    protected void executeMacro(AttributeRef attributeRef, List<MacroAction> actions, boolean repeat) {
        MacroExecutionTask task = new MacroExecutionTask(attributeRef, actions, repeat);
        task.start();
    }
}
