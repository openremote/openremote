package org.openremote.manager.energy.gopacs;

import org.lfenergy.shapeshifter.api.FlexRequest;
import org.lfenergy.shapeshifter.api.FlexRequestResponse;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;
import org.lfenergy.shapeshifter.core.model.UftpParticipant;
import org.lfenergy.shapeshifter.core.service.handler.UftpPayloadHandler;

import java.util.function.BiConsumer;

public class PayloadHandler implements UftpPayloadHandler {

    protected BiConsumer<UftpParticipant, FlexRequest> messageConsumer;
    protected BiConsumer<UftpParticipant, FlexRequestResponse> responseConsumer;

    public PayloadHandler(BiConsumer<UftpParticipant, FlexRequest> messageConsumer, BiConsumer<UftpParticipant, FlexRequestResponse> responseConsumer) {
        this.messageConsumer = messageConsumer;
        this.responseConsumer = responseConsumer;
    }

    @Override
    public void notifyNewIncomingMessage(IncomingUftpMessage<? extends PayloadMessageType> message) {
        var messageType = message.payloadMessage().getClass();
        if(!FlexRequest.class.isAssignableFrom(messageType)) {
            return;
        }

        var flexRequest = (FlexRequest) message.payloadMessage();

        messageConsumer.accept(message.sender(), flexRequest);
    }

    @Override
    public void notifyNewOutgoingMessage(OutgoingUftpMessage<? extends PayloadMessageType> message) {
        var messageType = message.payloadMessage().getClass();
        if(!FlexRequestResponse.class.isAssignableFrom(messageType)) {
            return;
        }

        var flexRequestResponse = (FlexRequestResponse) message.payloadMessage();
        responseConsumer.accept(message.sender(), flexRequestResponse);
    }
}
