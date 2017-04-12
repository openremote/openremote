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

import elemental.json.JsonValue;
import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.container.Container;
import org.openremote.model.*;
import org.openremote.model.asset.macro.MacroAction;
import org.openremote.model.asset.macro.MacroAttribute;
import org.openremote.model.asset.macro.MacroAttributeCommand;
import org.openremote.model.asset.thing.ThingAttribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This protocol is responsible for executing macros.
 *
 * It expects a {@link MacroAttributeCommand} or the super type{@link AttributeCommand}
 * as the attribute event value on the sendToActuator. The protocol will then execute
 * the command and store the command status in the attribute value.
 */
public class MacroProtocol extends AbstractProtocol {
    class MacroExecutionTask {
        AttributeRef attributeRef;
        List<MacroAction> actions;
        List<MacroAttributeCommand.Execution> schedule;
        boolean repeat;
        boolean cancelled;
        ScheduledFuture scheduledFuture;
        int iteration = -1;

        public MacroExecutionTask(AttributeRef attributeRef, List<MacroAction> actions, List<MacroAttributeCommand.Execution> schedule, boolean repeat) {
            this.attributeRef = attributeRef;
            this.actions = actions;
            this.schedule = schedule;
            this.repeat = repeat;
        }

        void start() {
            synchronized (scheduleMap) {
                scheduleMap.put(attributeRef, this);
            }

            // Update the command Status of this attribute
            onSensorUpdate(new AttributeState(attributeRef, CommandStatus.ACTIVE.asJsonValue()));
            run();
        }

        void cancel() {
            LOG.fine("Macro Execution cancel");
            scheduledFuture.cancel(false);
            cancelled = true;
            synchronized (scheduleMap) {
                scheduleMap.remove(attributeRef);
            }

            // Update the command Status of this attribute
            onSensorUpdate(new AttributeState(attributeRef, CommandStatus.CANCELLED.asJsonValue()));
        }

        private void run() {
            if (cancelled) {
                return;
            }

            if (iteration >= 0) {
                // Process the execution
                MacroAttributeCommand.Execution execution = schedule.get(iteration);
                actions.forEach(action -> {
                    // Look for override value in execution
                    AttributeState overrideState = execution.getActionOverrides() == null ? null : execution.getActionOverrides()
                            .stream()
                            .filter(actionOverride -> actionOverride.getAttributeRef().equals(action.getAttributeState().getAttributeRef()))
                            .findFirst()
                            .orElse(null);

                    AttributeState actionState = overrideState != null ? overrideState : action.getAttributeState();
                    if (action.getDelay() > 0) {
                        // TODO: Better handling of macro action delays - this is ok for short delays
                        try {
                            Thread.sleep(action.getDelay());
                        } catch (InterruptedException e) {
                            LOG.log(Level.WARNING, "Macro action was cancelled assume the thread is being terminated", e);
                            return;
                        }
                    }

                    // send attribute event
                    sendAttributeEvent(actionState);
                });
            }

            boolean isLast = iteration == schedule.size() - 1;
            boolean restart = isLast && repeat;

            if (restart) {
                iteration = 0;
            } else {
                iteration++;
            }

            if ((isLast && !restart)) {
                synchronized (scheduleMap) {
                    scheduleMap.remove(attributeRef);
                }

                // Update the command Status of this attribute
                onSensorUpdate(new AttributeState(attributeRef, CommandStatus.COMPLETED.asJsonValue()));
                return;
            }

            // Get next execution delay
            Integer delay = schedule.get(iteration).getDelay();

            // Schedule the next iteration
            scheduledFuture = executor.schedule(this::run, delay != null && delay >= 0 ? delay : 0, TimeUnit.MILLISECONDS);
        }
    }

    private static final Logger LOG = Logger.getLogger(MacroProtocol.class.getName());
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected final Map<AttributeRef, List<MacroAction>> actionMap = new HashMap<>();
    protected final Map<AttributeRef, MacroExecutionTask> scheduleMap = new HashMap<>();

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
    }

    @Override
    public String getProtocolName() {
        return MacroAttribute.PROTOCOL_NAME;
    }

    @Override
    protected void sendToActuator(AttributeEvent event) {
        AttributeRef attributeRef = event.getAttributeRef();

        // Check that incoming value is of type Attribute Command
        JsonValue value = event.getValue();
        MacroAttributeCommand macroCommand = MacroAttributeCommand.fromJsonValue(value);

        // Check if it's a cancellation request
        if (macroCommand.isCancel()) {
            LOG.fine("Request received to cancel macro execution: " + event);
            synchronized (scheduleMap) {
                MacroExecutionTask executionTask = scheduleMap.get(attributeRef);
                if (executionTask != null) {
                    LOG.fine("Cancelling macro execution");
                    executionTask.cancel();
                }
            }

            return;
        }

        List<MacroAction> actions;

        synchronized (actionMap) {
            actions = actionMap.get(attributeRef);
        }

        if (actions == null || actions.size() == 0) {
            LOG.fine("No macro actions found for attribute event: " + event);
            return;
        }

        // If the executing command has provided schedule information then include
        // this in the execution of the macro actions
        List<MacroAttributeCommand.Execution> schedule = macroCommand.getSchedule();

        if (schedule == null) {
            // Push in an empty execution
            schedule = Collections.singletonList(new MacroAttributeCommand.Execution(0));
        }

        executeMacro(attributeRef, actions, schedule, macroCommand.isRepeat());
    }

    @Override
    protected void onAttributeAdded(ThingAttribute attribute) {
        // Protocol configuration is actually a MacroAttribute
        MacroAttribute macroAttribute = new MacroAttribute(attribute.getProtocolConfiguration());

        // Store the macro actions for later execution requests
        AttributeRef reference = attribute.getReference();
        List<MacroAction> actions = macroAttribute.getActions();

        synchronized (actionMap) {
            actionMap.put(reference, actions);
        }
    }

    @Override
    protected void onAttributeUpdated(ThingAttribute attribute) {
        onAttributeAdded(attribute);
    }

    @Override
    protected void onAttributeRemoved(ThingAttribute attribute) {
        // Store the macro actions for later execution requests
        AttributeRef reference = attribute.getReference();

        synchronized (actionMap) {
            actionMap.remove(reference);
        }
    }

    protected void executeMacro(AttributeRef attributeRef, List<MacroAction> actions, List<MacroAttributeCommand.Execution> schedule, boolean repeat) {
        MacroExecutionTask task = new MacroExecutionTask(attributeRef, actions, schedule, repeat);
        task.start();
    }
}
