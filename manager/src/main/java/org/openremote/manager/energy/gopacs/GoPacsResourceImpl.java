package org.openremote.manager.energy.gopacs;

import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;

import java.util.function.Consumer;

public class GoPacsResourceImpl implements GoPacsResource {

    protected Consumer<IncomingUftpMessage<? extends PayloadMessageType>> messageConsumer;


    public GoPacsResourceImpl(Consumer<IncomingUftpMessage<? extends PayloadMessageType>> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void message(IncomingUftpMessage<? extends PayloadMessageType> message) {
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
    }
}
