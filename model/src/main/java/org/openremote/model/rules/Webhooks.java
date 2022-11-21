package org.openremote.model.rules;

import org.openremote.model.util.TsIgnore;
import org.openremote.model.webhook.Webhook;

@TsIgnore
public abstract class Webhooks {

    public abstract void send(Webhook webhook);
}
