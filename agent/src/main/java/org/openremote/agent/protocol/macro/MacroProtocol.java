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
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.*;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.macro.MacroConfiguration.getMacroActionIndex;
import static org.openremote.agent.protocol.macro.MacroConfiguration.isValidMacroConfiguration;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

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

    private static final Logger LOG = Logger.getLogger(MacroProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":macro";
    public static final String META_MACRO_ACTION = PROTOCOL_NAME + ":action";
    public static final String META_MACRO_ACTION_INDEX = PROTOCOL_NAME + ":actionIndex";

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
            synchronized (executions) {
                executions.put(attributeRef, this);
            }

            // Update the command Status of this attribute - We use a timestamp slightly in the past otherwise
            // it is possible for COMPLETED status update below to have the same timestamp and to then be rejected
            // by the asset processing service
            updateLinkedAttribute(new AttributeState(
                attributeRef,
                AttributeExecuteStatus.RUNNING.asValue()),
                timerService.getCurrentTimeMillis()-10
            );
            run();
        }

        void cancel() {
            LOG.fine("Macro Execution cancel");
            scheduledFuture.cancel(false);
            cancelled = true;
            synchronized (executions) {
                executions.remove(attributeRef);
            }

            // Update the command Status of this attribute
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
                synchronized (executions) {
                    executions.remove(attributeRef);
                }

                // Update the command Status of this attribute
                updateLinkedAttribute(new AttributeState(attributeRef, AttributeExecuteStatus.COMPLETED.asValue()));
                return;
            }

            // Get next execution delay
            Integer delayMillis = actions.get(iteration).getDelayMilliseconds();

            // Schedule the next iteration
            scheduledFuture = executorService.schedule(this::run, delayMillis > 0 ? delayMillis: 0);
        }
    }

    protected final Map<AttributeRef, Pair<Boolean, List<MacroAction>>> macroMap = new HashMap<>();
    protected final Map<AttributeRef, AttributeRef> macroAttributeMap = new HashMap<>();
    protected final Map<AttributeRef, MacroExecutionTask> executions = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        // Protocol configuration is actually a Macro Configuration
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();
        boolean isEnabled = protocolConfiguration.isEnabled();

        synchronized (macroMap) {
            // Check macro configuration is valid
            if (!isValidMacroConfiguration(protocolConfiguration)) {
                LOG.fine("Macro configuration is not valid: " + protocolConfiguration);
                updateDeploymentStatus(macroRef, DeploymentStatus.ERROR);
                // Put an empty list of actions against this macro
                macroMap.put(macroRef, new Pair<>(isEnabled, Collections.emptyList()));
            } else {
                // Store the macro actions for later execution requests
                macroMap.put(macroRef, new Pair<>(isEnabled, MacroConfiguration.getMacroActions(protocolConfiguration)));
                updateDeploymentStatus(macroRef, isEnabled ? DeploymentStatus.LINKED_ENABLED : DeploymentStatus.LINKED_DISABLED);
            }
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();
        synchronized (macroMap) {
            macroMap.remove(macroRef);
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef macroRef = protocolConfiguration.getReferenceOrThrow();

        // Store link between attribute and configuration
        synchronized (macroAttributeMap) {
            macroAttributeMap.put(attribute.getReferenceOrThrow(), macroRef);
        }

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
            actionValue = actions.get(actionIndex).getAttributeState().getCurrentValue().orElse(null);
            LOG.fine("Attribute is linked to the value of macro action index: actionIndex");
        }

        if (actionValue != null) {
            // Verify the type of the attribute matches the action value
            if (attribute
                .getType()
                .map(AttributeType::getValueType)
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
        // Store the macro actions for later execution requests
        AttributeRef reference = attribute.getReferenceOrThrow();

        synchronized (macroAttributeMap) {
            macroAttributeMap.remove(reference);
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {

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
                synchronized (executions) {
                    executions.computeIfPresent(attributeRef,
                        (attributeRef1, macroExecutionTask) -> {
                            macroExecutionTask.cancel();
                            return macroExecutionTask;
                        }
                    );
                }

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

            updateLinkedProtocolConfiguration(protocolConfiguration, META_MACRO_ACTION, actionMeta);
        }
    }

    protected List<MacroAction> getMacroActions(AttributeRef protocolConfigurationRef) {
        synchronized (macroMap) {
            Pair<Boolean, List<MacroAction>> actionsEnabledInfo;
            actionsEnabledInfo = macroMap.get(protocolConfigurationRef);

            if (actionsEnabledInfo == null || actionsEnabledInfo.value.size() == 0) {
                LOG.fine("No macro actions found for macro configuration: " + protocolConfigurationRef);
                return Collections.emptyList();
            }

            if (!actionsEnabledInfo.key) {
                LOG.fine("Macro configuration is disabled");
                return Collections.emptyList();
            }

            return actionsEnabledInfo.value;
        }
    }

    protected void executeMacro(AttributeRef attributeRef, List<MacroAction> actions, boolean repeat) {
        MacroExecutionTask task = new MacroExecutionTask(attributeRef, actions, repeat);
        task.start();
    }
}
