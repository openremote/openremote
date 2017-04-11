//
//  Constants.swift
//  console
//
//  Created by William Balcaen on 28/03/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation

struct DefaultsKey {
    static let offlineToken = "offlineToken"
    static let refreshToken = "refreshToken"
    static let idToken = "idToken"
}

struct AppGroup {
    static let entitlement = "group.org.openremote.console"
}

enum Server {
    static let hostURL = "192.168.0.173"
    static let initialPath = String(format:"console/%@/index.html",Server.realm)
    static let realm = "blok61"
    static let apiTestResource = String(format:"https://%@/%@/notification/alert",Server.hostURL,Server.realm)
}

enum ActionType {
    static let ACTION_TYPE1 = "ACTION_TYPE1"
    static let ACTION_TYPE2 = "ACTION_TYPE2"
}
