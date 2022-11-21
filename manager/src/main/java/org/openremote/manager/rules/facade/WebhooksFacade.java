package org.openremote.manager.rules.facade;

import org.openremote.manager.rules.RulesEngineId;
import org.openremote.manager.webhook.WebhookService;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.Webhooks;
import org.openremote.model.webhook.Webhook;

public class WebhooksFacade<T extends Ruleset> extends Webhooks {

    protected final RulesEngineId<T> rulesEngineId;
    protected final WebhookService webhookService;

    public WebhooksFacade(RulesEngineId<T> rulesEngineId, WebhookService webhookService) {
        this.rulesEngineId = rulesEngineId;
        this.webhookService = webhookService;
    }

    @Override
    public void send(Webhook webhook) {
        webhookService.sendHttpRequest(webhook);
    }
}
