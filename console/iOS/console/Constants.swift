//
//  Constants.swift
//  console
//
//  Created by William Balcaen on 16/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation

struct Server {
    static let hostURL = "192.168.99.100"
    static let port = "8080"
    static let initialPath = String(format:"console/%@/index.html",Server.realm)
    static let realm = "blok61"
    static let apiTestResource = String(format:"http://%@:%@/%@/asset",Server.hostURL,Server.port,Server.realm)
}

struct Client {
    static let clientId = "openremote"
}

struct DefaultsKey {
    static let offlineToken = "offlineToken"
    static let refreshToken = "refreshToken"
    static let idToken = "idToken"
}

struct NotificationsNames {
    static let isAuthenticated = "isAuthenticated"
}

struct AppGroup {
    static let entitlement = "group.org.openremote.console"
}
