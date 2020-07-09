/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.zwave;

import org.openremote.protocol.zwave.model.Controller;
import org.openremote.protocol.zwave.model.ControllerListener;
import org.openremote.protocol.zwave.model.ZWaveNode;
import org.openremote.protocol.zwave.model.commandclasses.channel.Channel;
import org.openremote.protocol.zwave.model.commandclasses.channel.ChannelListener;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.Value;

import java.util.function.Consumer;

public class ChannelConsumerLink implements ChannelListener, ControllerListener {

    public static ChannelConsumerLink createLink(int nodeId, int endpoint, String channelName, Consumer<Value> consumer, Controller controller) {
        Channel channel = null;
        channel = controller.findChannel(nodeId, endpoint, channelName);
        ChannelConsumerLink link = new ChannelConsumerLink(nodeId, endpoint, channelName, channel, consumer, controller);
        if (channel != null) {
            channel.addValueListener(link);
        }
        controller.addListener(link);
        return link;
    }

    private final Consumer<Value> consumer;
    private Channel channel;
    private final Controller controller;
    private final int nodeId;
    private final int endpoint;
    private final String channelName;

    private ChannelConsumerLink(int nodeId, int endpoint, String channelName, Channel channel, Consumer<Value> consumer, Controller controller) {
        this.nodeId = nodeId;
        this.endpoint = endpoint;
        this.channelName = channelName;
        this.channel = channel;
        this.consumer = consumer;
        this.controller = controller;
    }


    // Implements ChannelListener -----------------------------------------------------------------

    @Override
    public synchronized void valueHasChanged(Channel channel, Value channelValue) {
        consumer.accept(channelValue);
    }


    // Implements ControllerListener --------------------------------------------------------------

    @Override
    public synchronized void onNodeAdded(ZWaveNode node) {
        if (channel == null && nodeId == node.getNodeID()) {
            channel = controller.findChannel(nodeId, endpoint, channelName);
            if (channel != null) {
                channel.addValueListener(this);
            }
        }
    }

    @Override
    public synchronized void onNodeRemoved(ZWaveNode node) {
        if (nodeId == node.getNodeID()) {
            if (channel != null) {
                channel.removeValueListener(this);
            }
            controller.removeListener(this);
        }
    }


    // Public Methods -----------------------------------------------------------------------------

    public synchronized Channel getChannel() {
        return channel;
    }

    public synchronized void unlink() {
        if (channel != null) {
            channel.removeValueListener(this);
        }
        controller.removeListener(this);
    }
}
