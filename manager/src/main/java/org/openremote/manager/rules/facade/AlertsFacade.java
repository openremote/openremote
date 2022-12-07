/*
 * Copyright 2017, OpenRemote Inc.
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

import org.openremote.manager.alert.AlertService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.alert.Alert;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Alerts;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;
public class AlertsFacade<T extends Ruleset> extends Alert {

    protected final RulesEngineId<T> rulesEngineId;
    protected final AlertService alertService;

    public AlertsFacade(RulesEngineId<T> rulesEngineId, AlertService alertService) {
        this.rulesEngineId = rulesEngineId;
        this.alertService = alertService;
    }

    public void send(Alert alert) {
        String trigger = alert.getTrigger();
        alertService.sendAlert(alert, trigger);
    }
}
