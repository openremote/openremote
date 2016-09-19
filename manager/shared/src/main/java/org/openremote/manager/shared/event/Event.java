/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.event;

import com.fasterxml.jackson.annotation.*;
import org.openremote.manager.shared.agent.RefreshInventoryEvent;
import org.openremote.manager.shared.asset.AssetModifiedEvent;
import org.openremote.manager.shared.asset.SubscribeAssetModified;
import org.openremote.manager.shared.asset.UnsubscribeAssetModified;
import org.openremote.manager.shared.util.Util;

@JsonSubTypes({
    // Events used on client and server (serialized on client/server bus)
    @JsonSubTypes.Type(value = Message.class, name = "MESSAGE"),
    @JsonSubTypes.Type(value = SubscribeAssetModified.class, name = "SUBSCRIBE_ASSET_CHANGES"),
    @JsonSubTypes.Type(value = UnsubscribeAssetModified.class, name = "UNSUBSCRIBE_ASSET_CHANGES"),
    @JsonSubTypes.Type(value = AssetModifiedEvent.class, name = "ASSET_MODIFIED"),
    @JsonSubTypes.Type(value = RefreshInventoryEvent.class, name = "REFRESH_INVENTORY"),
    /*
    @JsonSubTypes.Type(value = FlowDeployEvent.class, name = "FLOW_DEPLOY"),
    @JsonSubTypes.Type(value = FlowDeploymentFailureEvent.class, name = "FLOW_DEPLOYMENT_FAILURE"),
    @JsonSubTypes.Type(value = FlowRuntimeFailureEvent.class, name = "FLOW_RUNTIME_FAILURE"),
    @JsonSubTypes.Type(value = FlowRequestStatusEvent.class, name = "FLOW_REQUEST_STATUS"),
    @JsonSubTypes.Type(value = FlowStatusEvent.class, name = "FLOW_STATUS"),
    @JsonSubTypes.Type(value = FlowStopEvent.class, name = "FLOW_STOP"),
    @JsonSubTypes.Type(value = InventoryDevicesUpdatedEvent.class, name = "INVENTORY_DEVICES_UPDATED"),
    // Events used only on client (serialized on console/native shell bus)
    @JsonSubTypes.Type(value = ConsoleEditModeEvent.class, name = "CONSOLE_EDIT_MODE"),
    @JsonSubTypes.Type(value = ConsoleLoopDetectedEvent.class, name = "CONSOLE_LOOP_DETECTED"),
    @JsonSubTypes.Type(value = ConsoleRefreshedEvent.class, name = "CONSOLE_REFRESHED"),
    @JsonSubTypes.Type(value = ConsoleRefreshEvent.class, name = "CONSOLE_REFRESH"),
    @JsonSubTypes.Type(value = ConsoleWidgetModifiedEvent.class, name = "CONSOLE_WIDGET_MODIFIED"),
    @JsonSubTypes.Type(value = ConsoleZoomEvent.class, name = "CONSOLE_ZOOM"),
    @JsonSubTypes.Type(value = MessageReceivedEvent.class, name = "MESSAGE_RECEIVED"),
    @JsonSubTypes.Type(value = MessageSendEvent.class, name = "MESSAGE_SEND"),
    @JsonSubTypes.Type(value = NodeCreateEvent.class, name = "NODE_CREATE"),
    @JsonSubTypes.Type(value = NodeSelectedEvent.class, name = "NODE_SELECTED"),
    @JsonSubTypes.Type(value = ShellCloseEvent.class, name = "SHELL_CLOSE"),
    @JsonSubTypes.Type(value = ShellOpenEvent.class, name = "SHELL_OPEN"),
    @JsonSubTypes.Type(value = ShellReadyEvent.class, name = "SHELL_READY"),
    @JsonSubTypes.Type(value = ShowFailureEvent.class, name = "SHOW_FAILURE"),
    @JsonSubTypes.Type(value = ShowInfoEvent.class, name = "SHOW_INFO"),
    @JsonSubTypes.Type(value = SubflowNodeCreateEvent.class, name = "SUBFLOW_NODE_CREATE")
    */
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event"
)
// TODO The GWT jackson integration sometimes tries to pick up ALL subclasses when serialization generators are produced, use @JsonIgnoreType on them
public abstract class Event {

    // This is compatible with the Polymer event naming, so events can be used in JS/Polymer components
    public static String getType(String simpleClassName) {
        String type = Util.toLowerCaseDash(simpleClassName);
        if (type.length() > 6 && type.substring(type.length() - 6).equals("-event"))
            type = type.substring(0, type.length() - 6);
        return type;
    }

    public static String getType(Class<? extends Event> actionClass) {
        return getType(actionClass.getSimpleName());
    }

    public String getType() {
        return getType(getClass());
    }


    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
