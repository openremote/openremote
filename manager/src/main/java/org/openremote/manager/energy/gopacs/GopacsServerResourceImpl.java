package org.openremote.manager.energy.gopacs;

import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;

import java.util.function.Consumer;

public class GopacsServerResourceImpl implements GopacsServerResource {

    protected Consumer<IncomingUftpMessage<? extends PayloadMessageType>> messageConsumer;


    public GopacsServerResourceImpl(Consumer<IncomingUftpMessage<? extends PayloadMessageType>> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void inMessage(IncomingUftpMessage<? extends PayloadMessageType> message) {
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
    }
}
