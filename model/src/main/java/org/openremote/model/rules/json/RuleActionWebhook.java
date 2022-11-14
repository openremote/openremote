package org.openremote.model.rules.json;

public class RuleActionWebhook extends RuleAction {

    public String url;
    public RuleActionWebhookHeader[] headers;
    public String method;
    public String payload;
}
