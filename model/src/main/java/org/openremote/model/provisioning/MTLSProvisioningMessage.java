package org.openremote.model.provisioning;

public class MTLSProvisioningMessage extends ProvisioningMessage {
    protected String req;

    public MTLSProvisioningMessage(String req) {
        this.req = req;
    }

    public String getRequest() {
        return req;
    }
}
