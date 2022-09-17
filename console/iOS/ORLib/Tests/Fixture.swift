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
    
    init() {
        consoleConfigReturnCode = 404
        appsReturnCode = 404
    }
}
