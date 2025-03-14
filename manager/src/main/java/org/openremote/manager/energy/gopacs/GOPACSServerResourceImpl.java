package org.openremote.manager.energy.gopacs;

import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.api.SignedMessage;
import org.lfenergy.shapeshifter.core.common.xml.XmlSerializer;
import org.lfenergy.shapeshifter.core.common.xsd.XsdFactory;
import org.lfenergy.shapeshifter.core.common.xsd.XsdSchemaFactoryPool;
import org.lfenergy.shapeshifter.core.common.xsd.XsdSchemaProvider;
import org.lfenergy.shapeshifter.core.common.xsd.XsdValidator;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;
import org.lfenergy.shapeshifter.core.model.UftpParticipant;
import org.lfenergy.shapeshifter.core.service.crypto.UftpCryptoService;
import org.lfenergy.shapeshifter.core.service.serialization.UftpSerializer;
import org.lfenergy.shapeshifter.core.tools.UftpKeyPairTool;

import java.util.function.Consumer;

public class GOPACSServerResourceImpl implements GOPACSServerResource {

    protected Consumer<String> messageConsumer;


    public GOPACSServerResourceImpl(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void inMessage(String transportXml) {
        if (messageConsumer != null) {
            messageConsumer.accept(transportXml);
        }
    }
}
