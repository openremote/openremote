package org.openremote.agent.protocol.homeassistant;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.homeassistant.entities.HomeAssistantEntityState;
import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class HomeAssistantWebSocketClient extends WebsocketIOClient<String> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, HomeAssistantHttpClient.class);
    private final HomeAssistantProtocol protocol;

    public HomeAssistantWebSocketClient(HomeAssistantProtocol protocol, URI homeAssistantWebSocketUrl) {
        super(homeAssistantWebSocketUrl, null, null);
        this.protocol = protocol;

        setEncoderDecoderProvider(() ->
            new ChannelHandler[] {new MessageToMessageDecoder<>(String.class, this)}
        );

        addMessageConsumer(this::onExternalMessageReceived);
        connect();
    }


    private void onExternalMessageReceived(String message) {
        tryHandleEntityStateChange(message);
    }


    @Override
    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        super.onConnectionStatusChanged(connectionStatus);
        LOG.info("Connection status changed to: " + connectionStatus);

        if (connectionStatus == ConnectionStatus.CONNECTED) {
            var authMessage = ValueUtil.asJSON(Map.of("type", "auth", "access_token", this.protocol.getAgent().getAccessToken().orElse("")));
            if(authMessage.isPresent())
            {
                LOG.info("Sending auth message to Home Assistant WebSocket Endpoint: " + authMessage.get());
                sendMessage(authMessage.get());
                subscribeToEntityStateChanges();
            }
        }
    }


    private void tryHandleEntityStateChange(String message) {
        try {
            LOG.info("Received entity state change message: " + message);
            var event = ValueUtil.parse(message, HomeAssistantEntityState.class);
            event.ifPresent(homeAssistantEntityState -> protocol.entityProcessor.handleEntityStateEvent(homeAssistantEntityState.getEvent()));
        } catch (Exception e) {
            LOG.warning("Failed to parse entity state change message: " + message);
        }
    }

    // Subscribe to state changes for all entities within Home Assistant
    private void subscribeToEntityStateChanges() {
        var subscribeMessage = ValueUtil.asJSON(Map.of("id", 1, "type", "subscribe_events", "event_type", "state_changed"));
        if(subscribeMessage.isPresent())
        {
            LOG.info("Sending subscribe message to Home Assistant WebSocket Endpoint: " + subscribeMessage.get());
            sendMessage(subscribeMessage.get());
        }
    }
}
