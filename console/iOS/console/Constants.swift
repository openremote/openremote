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
    static let initialPath = "console/master/index.html"
    static let realm = "master"
    static let apiTestResource = "http://192.168.99.100:8080/master/asset"
}

struct Client {
    static let clientId = "openremote"
}
