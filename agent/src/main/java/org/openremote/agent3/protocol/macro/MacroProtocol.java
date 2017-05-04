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
package org.openremote.agent3.protocol.macro;

import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.macro.MacroConfiguration.getMacroActions;
import static org.openremote.agent3.protocol.macro.MacroConfiguration.isValidMacroConfiguration;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

/**
 * This protocol is responsible for executing macros.
 * <p>
 * It expects a {@link AttributeExecuteStatus} as the attribute event value on the
 * sendToActuator. The protocol will then try to perform the requested status on the
 * linked macro.
 */
public class MacroProtocol extends AbstractProtocol {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":macro";

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

            // Update the command Status of this attribute
            onSensorUpdate(new AttributeState(attributeRef, AttributeExecuteStatus.RUNNING.asValue()));
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
            onSensorUpdate(new AttributeState(attributeRef, AttributeExecuteStatus.CANCELLED.asValue()));
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
                onSensorUpdate(new AttributeState(attributeRef, AttributeExecuteStatus.COMPLETED.asValue()));
                return;
            }

            // Get next execution delay
            Integer delayMillis = actions.get(iteration).getDelayMilliseconds();

            // Schedule the next iteration
            scheduledFuture = executorService.schedule(this::run, delayMillis > 0 ? delayMillis: 0);
        }
    }

    private static final Logger LOG = Logger.getLogger(MacroProtocol.class.getName());
    protected final Map<AttributeRef, List<MacroAction>> macroMap = new HashMap<>();
    protected final Map<AttributeRef, AttributeRef> macroAttributeMap = new HashMap<>();
    protected final Map<AttributeRef, MacroExecutionTask> executions = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void sendToActuator(AttributeEvent event) {
        // Asset processing service has already done sanity checks
        // Can be valid to pass null to
        Optional<Value> value = event.getValue();
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

        AttributeRef macroRef;
        List<MacroAction> actions;

        synchronized (macroAttributeMap) {
            macroRef = macroAttributeMap.get(attributeRef);
        }

        if (macroRef == null) {
            LOG.fine("Attribute is not linked to a macro: " + attributeRef);
            return;
        }

        synchronized (macroMap) {
            actions = macroMap.get(macroRef);
        }

        if (actions == null || actions.size() == 0) {
            LOG.fine("No macro actions found for attribute event: " + event);
            return;
        }

        executeMacro(attributeRef, actions, status == AttributeExecuteStatus.REQUEST_REPEATING);
    }

    @Override
    protected void onAttributeAdded(AssetAttribute attribute, AssetAttribute macroConfiguration) {
        // Protocol configuration is actually a Macro Configuration
        AttributeRef macroRef = macroConfiguration.getReferenceOrThrow();

        // Only process the macro configuration the first time it is encountered
        synchronized (macroMap) {
            if (!macroMap.containsKey(macroRef)) {
                // Check macro configuration is valid
                if (!isValidMacroConfiguration(macroConfiguration)) {
                    LOG.fine("Macro configuration is not valid: " + macroConfiguration);

                    // Put an empty list of actions against this macro
                    macroMap.put(macroRef, Collections.emptyList());
                } else {
                    // Store the macro actions for later execution requests
                    macroMap.put(macroRef, getMacroActions(macroConfiguration));
                }
            }
        }

        // Store link between attribute and configuration
        synchronized (macroAttributeMap) {
            macroAttributeMap.put(attribute.getReferenceOrThrow(), macroRef);
        }
    }

    @Override
    protected void onAttributeUpdated(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        onAttributeAdded(attribute, protocolConfiguration);
    }

    @Override
    protected void onAttributeRemoved(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // Store the macro actions for later execution requests
        AttributeRef reference = attribute.getReferenceOrThrow();

        synchronized (macroAttributeMap) {
            macroAttributeMap.remove(attribute.getReference());
        }
    }

    protected void executeMacro(AttributeRef attributeRef, List<MacroAction> actions, boolean repeat) {
        MacroExecutionTask task = new MacroExecutionTask(attributeRef, actions, repeat);
        task.start();
    }
}
