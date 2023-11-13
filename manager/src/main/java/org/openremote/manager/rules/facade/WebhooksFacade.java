/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.rules.facade;

import org.openremote.manager.rules.RulesEngineId;
import org.openremote.manager.webhook.WebhookService;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.Webhooks;
import org.openremote.model.webhook.Webhook;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

public class WebhooksFacade<T extends Ruleset> extends Webhooks {

    protected final RulesEngineId<T> rulesEngineId;
    protected final WebhookService webhookService;

    public WebhooksFacade(RulesEngineId<T> rulesEngineId, WebhookService webhookService) {
        this.rulesEngineId = rulesEngineId;
        this.webhookService = webhookService;
    }

    @Override
    public boolean send(Webhook webhook, MediaType mediaType, WebTarget target) {
        return webhookService.sendHttpRequest(webhook, mediaType, target);
    }

    @Override
    public WebTarget buildTarget(Webhook webhook) {
        return webhookService.buildWebTarget(webhook);
    }
}
