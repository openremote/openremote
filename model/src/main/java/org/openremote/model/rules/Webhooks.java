package org.openremote.model.rules;

import org.openremote.model.util.TsIgnore;
import org.openremote.model.webhook.Webhook;

import javax.ws.rs.client.WebTarget;

@TsIgnore
public abstract class Webhooks {

    public abstract void send(Webhook webhook, WebTarget target);
    public abstract WebTarget buildTarget(Webhook webhook);
}
