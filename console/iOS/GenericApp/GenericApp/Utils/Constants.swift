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

import Foundation
import UIKit

public enum DefaultsKey {
    static let groupEntitlement = "group.io.openremote.app"
    static let projectKey = "PROJECT"
    static let realmKey = "REALM"
    static let refreshTokenKey = "REFRESH_TOKEN"
    static let fcmTokenKey = "FCM_TOKEN_KEY"
    static let fcmDeviceIdKey = "FCM_DEVICE_ID_KEY"
    static let actionKey = "action"
    static let buttonsKey = "buttons"
    static let providerKey = "provider"
    static let versionKey = "version"
    static let requiresPermissionKey = "requiresPermission"
    static let hasPermissionKey = "hasPermission"
    static let successKey = "success"
    static let dataKey = "data"
    static let enabledKey = "enabled"
    static let disabledKey = "disabled"
}

public enum Actions {
    static let providerInit = "PROVIDER_INIT"
    static let providerEnable = "PROVIDER_ENABLE"
    static let providerDisable = "PROVIDER_DISABLE"
    static let geofenceRefresh = "GEOFENCE_REFRESH"
    static let store = "STORE"
    static let retrieve = "RETRIEVE"
    static let getLocation = "GET_LOCATION"
}

public enum Providers {
    static let push = "push"
    static let geofence = "geofence"
    static let storage = "storage"
}

public enum NotificationsNames {
    static let isAuthenticated = "isAuthenticated"
    static let isdeviceIdSent = "isdeviceIdSent"
}

public enum Notifications {
    static let needsReachabilityCheck = Notification.Name("NeedsReachabilityCheck")
    static let fullScreenShown = Notification.Name("fullScreenShown")
    static let fullScreenHidden = Notification.Name("fullScreenHidden")
    static let idleTimerFired = Notification.Name("idleTimerFired")
}

public enum ActionType {
    static let actions = "actions"
    static let aps = "aps"
    static let alert = "alert"
    static let title = "title"
    static let type = "type"
    static let appUrl = "appUrl"
    static let silent = "silent"
    static let httpMethod = "httpMethod"
    static let openInBrowser = "openInBrowser"
    static let notificationId = "notification-id"
    static let ACTION_DEEP_LINK = "LINK"
    static let ACTION_ACTUATOR = "ACTUATOR"
}
