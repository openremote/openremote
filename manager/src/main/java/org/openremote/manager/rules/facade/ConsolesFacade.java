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

import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Ruleset;

public class ConsolesFacade<T extends Ruleset> {

    protected final AssetsFacade<T> assetsFacade;
    protected final NotificationService notificationService;
    protected final ManagerIdentityService identityService;

    public ConsolesFacade(AssetsFacade<T> assetsFacade, NotificationService notificationService, ManagerIdentityService identityService) {
        this.assetsFacade = assetsFacade;
        this.notificationService = notificationService;
        this.identityService = identityService;
    }

    public void notify(String consoleId, AlertNotification alert) {

        // Check if the console asset can be found with the default query of this facade
        BaseAssetQuery<Assets.RestrictedQuery> checkQuery = assetsFacade.query();
        checkQuery.id = consoleId; // Set directly on field, as modifying query restrictions is not allowed
        Asset console = assetsFacade.assetStorageService.find(checkQuery);
        if (console == null || console.getWellKnownType() != AssetType.CONSOLE) {
            throw new IllegalArgumentException(
                "Access to console not allowed for this rule engine scope: " + consoleId
            );
        }

        notificationService.notifyConsole(consoleId, alert);
    }
}
