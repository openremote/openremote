package org.openremote.model.rules.json;

// Still not very happy with an enum like this, describing auth options
public enum RuleActionWebhookAuth {
    HTTP_BASIC, API_KEY, OAUTH2
}
