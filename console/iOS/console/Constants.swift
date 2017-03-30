//
//  Constants.swift
//  console
//
//  Created by William Balcaen on 16/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation

enum Server {
    static let hostURL = "192.168.0.138"
    static let port = "8080"
    static let initialPath = String(format:"console/%@/index.html",Server.realm)
    static let realm = "blok61"
    static let apiTestResource = String(format:"http://%@:%@/%@/asset",Server.hostURL,Server.port,Server.realm)
}

enum Client {
    static let clientId = "openremote"
}

enum DefaultsKey {
    static let offlineToken = "offlineToken"
    static let refreshToken = "refreshToken"
    static let idToken = "idToken"
}

enum NotificationsNames {
    static let isAuthenticated = "isAuthenticated"
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
