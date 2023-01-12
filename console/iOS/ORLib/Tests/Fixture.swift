//
//  Fixture.swift
//  Tests
//
//  Created by Eric Bariaux on 17/09/2022.
//

import Foundation
@testable import ORLib

struct Fixture: Codable {
    var consoleConfigReturnCode: Int?
    var consoleConfig: ORConsoleConfig?
    var appsReturnCode: Int?
    var apps: [String]?
    var appsInfoReturnCode: Int?
    var appsInfo: [String: ORAppInfo]?
    
    init() {
        consoleConfigReturnCode = 404
        appsReturnCode = 404
        appsInfoReturnCode = 404
    }
}
