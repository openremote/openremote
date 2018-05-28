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

public enum AccesTokenResult<String>
{
    case Success(String?)
    case Failure(NSError?)
}

public enum DefaultsKey {
    public static let token = "token"
    public static let refreshToken = "refreshToken"
    public static let idToken = "idToken"
    public static let deviceId = "deviceToken"
    public static let actionKey = "action"
    public static let providerKey = "provider"
    public static let versionKey = "version"
    public static let requiresPermissionKey = "requiresPermission"
    public static let hasPermissionKey = "hasPermission"
    public static let successKey = "success"
    public static let dataKey = "data"
}

public enum Actions {
    public static let providerInit = "PROVIDER_INIT"
    public static let providerEnable = "PROVIDER_ENABLE"
    public static let providerDisable = "PROVIDER_DISABLE"
    public static let geofenceRefresh = "GEOFENCE_REFRESH"
}

public enum Providers {
    public static let push = "push"
    public static let geofence = "geofence"
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
    public static let actions = "actions"
    public static let aps = "aps"
    public static let alert = "alert"
    public static let title = "title"
    public static let type = "type"
    public static let appUrl = "appUrl"
    public static let ACTION_DEEP_LINK = "LINK"
    public static let ACTION_ACTUATOR = "ACTUATOR"
}
