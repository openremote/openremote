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
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

// TODO: Remove this protocol once attribute linking and flow integration is done
/**
 * This protocol is responsible for executing macros.
 * <p>
 * It expects a {@link AttributeExecuteStatus} as the attribute event value on the {@link #doLinkedAttributeWrite}.
 * The protocol will then try to perform the request on the linked macro protocol instance.
 * <p>
 * {@link Attribute}s can also read/write the macro configuration's {@link MacroAction} values by using the
 * {@link MacroAgent.MacroAgentLink#getActionIndex} with the index of the {@link MacroAction} to link to.
 */
public class MacroProtocol extends AbstractProtocol<MacroAgent, MacroAgent.MacroAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MacroProtocol.class);

    public static final String PROTOCOL_DISPLAY_NAME = "Macro";

    class MacroExecutionTask {

        List<MacroAction> actions;
        boolean repeat;
        boolean cancelled;
        ScheduledFuture<?> scheduledFuture;
        int step = -1;

        public MacroExecutionTask(List<MacroAction> actions, boolean repeat) {
            this.actions = actions;
            this.repeat = repeat;
        }

        void start() {
            updateExecuteStatus(AttributeExecuteStatus.RUNNING);
            run();
        }

        void cancel() {
            LOG.fine("Macro Execution cancel");
            scheduledFuture.cancel(false);
            cancelled = true;
            execution = null;
            updateExecuteStatus(AttributeExecuteStatus.CANCELLED);
        }

        private void run() {
            if (cancelled) {
                return;
            }

            boolean finished = false;

            try {
                if (step >= 0) {
                    // Process the execution of the next action
                    MacroAction action = actions.get(step);
                    AttributeState actionState = action.getAttributeState();

                    // send attribute event
                    sendAttributeEvent(actionState);
                }

                boolean isLast = step == actions.size() - 1;
                boolean restart = isLast && repeat;

                if (restart) {
                    step = 0;
                } else {
                    step++;
                }

                finished = isLast && !restart;
            } finally {
                if (finished) {
                    execution = null;
                    // Update the command Status of this attribute
                    updateExecuteStatus(AttributeExecuteStatus.COMPLETED);
                } else {

                    // Get next execution delay
                    int delayMillis = actions.get(step).getDelayMilliseconds();

                    // Schedule the next iteration
                    scheduledFuture = executorService.schedule(this::run, Math.max(delayMillis, 0), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    protected final List<MacroAction> actions = new ArrayList<>();
    protected MacroExecutionTask execution;

    public MacroProtocol(MacroAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "macro://" + agent.getId();
    }

    @Override
    protected void doStart(Container container) throws Exception {

        actions.addAll(Arrays.asList(agent.getMacroActions().orElseThrow(() -> {
            String msg = "Macro actions attribute missing or invalid: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        })));

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (execution != null) {
            execution.cancel();
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, MacroAgent.MacroAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // Check for executable meta item
        if (attribute.getType().getType() == AttributeExecuteStatus.class) {
            LOG.finer("Macro linked attribute is marked as executable so it will be linked to the firing of the macro");
            // Update the command Status of this attribute
            updateLinkedAttribute(
                new AttributeState(
                    attributeRef,
                    agent.isMacroDisabled().orElse(true)
                        ? AttributeExecuteStatus.READY
                        : AttributeExecuteStatus.DISABLED
                )
            );
            return;
        }

        // Check for action index or default to index 0
        int actionIndex = agentLink.getActionIndex().orElse(0);

        // Pull the macro action value out with the same type as the linked attribute
        // otherwise push a null value through to the attribute
        Object actionValue = null;

        if (actions.isEmpty()) {
            LOG.finer("No actions are available for the linked macro, maybe it is disabled?: " + this);
        } else {
            actionIndex = Math.min(actions.size(), Math.max(0, actionIndex));
            actionValue = actions.get(actionIndex).getAttributeState().getValue().orElse(null);
            LOG.finer("Attribute is linked to the value of macro action index: actionIndex");
        }

        // Push the value of this macro action into the attribute
        updateLinkedAttribute(new AttributeState(attributeRef, actionValue));
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, MacroAgent.MacroAgentLink agentLink) {
    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, MacroAgent.MacroAgentLink agentLink, AttributeEvent event, Object processedValue) {

        if (attribute.getType().getType() == AttributeExecuteStatus.class) {
            // This is a macro execution related write operation
            AttributeExecuteStatus status = Values.getValueCoerced(event.getValue(), AttributeExecuteStatus.class)
                .orElse(null);

            if (status == null || !status.isWrite()) {
                LOG.info("Linked attribute write value is either null or not a valid execution status");
                return;
            }

            // Check if it's a cancellation request
            if (status == AttributeExecuteStatus.REQUEST_CANCEL) {
                if (execution == null) {
                    return;
                }

                LOG.fine("Request received to cancel macro execution: " + event);
                execution.cancel();
                return;
            }

            if (actions.isEmpty()) {
                LOG.fine("No actions to execute");
                return;
            }

            executeMacro(status == AttributeExecuteStatus.REQUEST_REPEATING);
            return;
        }

        // Assume this is a write to a macro action value (default to index 0)
        int actionIndex = agentLink.getActionIndex().orElse(0);

        if (actions.isEmpty()) {
            LOG.fine("No actions are available for the linked macro, maybe it is disabled?: " + this);
        } else {
            actionIndex = Math.min(actions.size(), Math.max(0, actionIndex));
            MacroAction action = actions.get(actionIndex);

            if (action == null) {
                return;
            }

            Object newActionValue = event.getValue().orElse(null);
            action.setAttributeState(new AttributeState(action.getAttributeState().getRef(), newActionValue));
            updateAgentAttribute(new AttributeState(agent.getId(), MacroAgent.MACRO_ACTIONS.getName(), actions));
            updateLinkedAttribute(new AttributeState(event.getAttributeRef(), newActionValue));
        }
    }

    protected void executeMacro(boolean repeat) {
        MacroExecutionTask task = new MacroExecutionTask(actions, repeat);
        task.start();
    }

    protected void updateExecuteStatus(AttributeExecuteStatus executeStatus) {
        updateAgentAttribute(new AttributeState(agent.getId(), MacroAgent.MACRO_STATUS.getName(), executeStatus));

        // Update linked attribute of type AttributeExecuteStatus
        linkedAttributes.entrySet().stream()
            .filter(assetIdAndAttribute ->
                assetIdAndAttribute.getValue().getType().equals(ValueType.EXECUTION_STATUS))
            .forEach(assetIdAndAttribute ->
                updateLinkedAttribute(new AttributeState(assetIdAndAttribute.getKey(), executeStatus)));
    }
}
