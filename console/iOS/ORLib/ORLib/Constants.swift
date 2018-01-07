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
    static let token = "token"
    static let refreshToken = "refreshToken"
    static let idToken = "idToken"
    static let deviceId = "deviceToken"
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
    static let ACTION_DEEP_LINK = "LINK"
    static let ACTION_ACTUATOR = "ACTUATOR"
}
