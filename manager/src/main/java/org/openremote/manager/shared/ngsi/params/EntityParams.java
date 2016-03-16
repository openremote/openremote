package org.openremote.manager.shared.ngsi.params;

import javax.ws.rs.QueryParam;

public class EntityParams {

    @QueryParam("type")
    public String type;

    @QueryParam("attrs")
    public String[] attributes;

    @QueryParam("options")
    public EntityRepresentation options;

    public EntityParams() {
    }

    public EntityParams type(String type) {
        this.type = type;
        return this;
    }

    public EntityParams attributes(String[] attributes) {
        this.attributes = attributes;
        return this;
    }

    public EntityParams options(EntityRepresentation options) {
        this.options = options;
        return this;
    }
}
