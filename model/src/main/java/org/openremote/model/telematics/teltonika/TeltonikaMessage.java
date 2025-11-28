package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.Message;
import org.openremote.model.telematics.Payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeltonikaMessage implements Message {

    List<Payload> payloads = new ArrayList<>(1);

    @Override
    public Payload getPayloadByIndex(int index) {
        return payloads.get(index);
    }
}
