package org.openremote.model.telematics.teltonika;

import org.openremote.model.value.ValueConstraint;

public class TeltonikaResponseParameter extends TeltonikaValueDescriptor<String> {

    public static final TeltonikaResponseParameter Instance = new TeltonikaResponseParameter("response", String.class, null);

    public TeltonikaResponseParameter(String name, Class avlType, ValueConstraint... constraints) {
        super(name, avlType, constraints);
    }
}
