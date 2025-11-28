package org.openremote.model.telematics;


import java.util.List;
import java.util.Optional;

public interface Message {
    Payload getPayloadByIndex(int index);
}
