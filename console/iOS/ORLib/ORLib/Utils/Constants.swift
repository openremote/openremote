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

public enum Segues {
    public static var goToSettingsView = "goToSettingsView"
    public static var goToWizardDomainView = "goToWizardDomainView"
    public static var goToWizardAppView = "goToWizardAppView"
    public static var goToWizardRealmView = "goToWizardRealmView"
    public static var goToWebView = "goToWebView"
    public static var addProject = "addProject"
}

public enum DefaultsKey {
    public static var groupEntitlement = "group.io.openremote.app"
    public static let selectedProjectKey = "SELECTED_PROJECT"
    public static let projectsConfigurationKey = "PROJECTS_CONFIGURATION"
    public static let projectKey = "PROJECT"
    public static let hostKey = "HOST"
    public static let realmKey = "REALM"
    public static let refreshTokenKey = "REFRESH_TOKEN"
    public static let fcmTokenKey = "FCM_TOKEN_KEY"
    public static let fcmDeviceIdKey = "FCM_DEVICE_ID_KEY"
    public static let actionKey = "action"
    public static let buttonsKey = "buttons"
    public static let providerKey = "provider"
    public static let versionKey = "version"
    public static let requiresPermissionKey = "requiresPermission"
    public static let hasPermissionKey = "hasPermission"
    public static let successKey = "success"
    public static let dataKey = "data"
    public static let enabledKey = "enabled"
    public static let disabledKey = "disabled"
}

public enum Actions {
    public static let providerInit = "PROVIDER_INIT"
    public static let providerEnable = "PROVIDER_ENABLE"
    public static let providerDisable = "PROVIDER_DISABLE"
    public static let geofenceRefresh = "GEOFENCE_REFRESH"
    public static let store = "STORE"
    public static let retrieve = "RETRIEVE"
    public static let scanQr = "SCAN_QR"
    public static let scanResult = "SCAN_RESULT"
    public static let scanBleDevices = "SCAN_BLE_DEVICES"
    public static let connectToBleDevice = "CONNECT_TO_DEVICE"
    public static let sendToBleDevice = "SEND_TO_DEVICE"
}

public enum Providers {
    public static let push = "push"
    public static let geofence = "geofence"
    public static let storage = "storage"
    public static let qr = "qr"
    public static let ble = "ble"
}

public enum NotificationsNames {
    public static let isAuthenticated = "isAuthenticated"
    public static let isdeviceIdSent = "isdeviceIdSent"
}

public enum Notifications {
    public static let needsReachabilityCheck = Notification.Name("NeedsReachabilityCheck")
    public static let fullScreenShown = Notification.Name("fullScreenShown")
    public static let fullScreenHidden = Notification.Name("fullScreenHidden")
    public static let idleTimerFired = Notification.Name("idleTimerFired")
}

public enum ActionType {
    public static let actions = "actions"
    public static let aps = "aps"
    public static let alert = "alert"
    public static let title = "title"
    public static let type = "type"
    public static let appUrl = "appUrl"
    public static let silent = "silent"
    public static let httpMethod = "httpMethod"
    public static let openInBrowser = "openInBrowser"
    public static let notificationId = "notification-id"
    public static let ACTION_DEEP_LINK = "LINK"
    public static let ACTION_ACTUATOR = "ACTUATOR"
}
