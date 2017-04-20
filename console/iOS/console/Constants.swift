//
//  Constants.swift
//  console
//
//  Created by William Balcaen on 16/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation

enum AccesTokenResult<String>
{
    case Success(String?)
    case Failure(NSError?)
}

enum Server {
    static let hostURL = "192.168.0.177"
    static let initialPath = String(format:"console/%@/index.html",Server.realm)
    static let realm = "blok61"
    static let apiTestResource = String(format:"https://%@/%@/notification/alert",Server.hostURL,Server.realm)
    static let registerDeviceResource = String(format:"https://%@/%@/notification/token",Server.hostURL,Server.realm)
}

enum Client {
    static let clientId = "openremote"
}

enum FCM {
    static let serverKey = "Dummy"
}
enum DefaultsKey {
    static let offlineToken = "offlineToken"
    static let refreshToken = "refreshToken"
    static let idToken = "idToken"
    static let deviceId = "deviceToken"
}

enum NotificationsNames {
    static let isAuthenticated = "isAuthenticated"
    static let isdeviceIdSent = "isdeviceIdSent"
}

enum AppGroup {
    static let entitlement = "group.org.openremote.console"
}

enum Notifications {
    static let needsReachabilityCheck = Notification.Name("NeedsReachabilityCheck")
    static let fullScreenShown = Notification.Name("fullScreenShown")
    static let fullScreenHidden = Notification.Name("fullScreenHidden")
    static let idleTimerFired = Notification.Name("idleTimerFired")
}

enum ActionType {
    static let ACTION_DEEP_LINK = "ACTION_DEEP_LINK"
    static let ACTION_ACTUATOR = "ACTION_ACTUATOR"
}
