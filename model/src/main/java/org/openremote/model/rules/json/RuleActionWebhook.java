package org.openremote.model.rules.json;

import org.openremote.model.webhook.Webhook;

import javax.ws.rs.client.WebTarget;

public class RuleActionWebhook extends RuleAction {
    public Webhook webhook;
    public WebTarget target;
}
