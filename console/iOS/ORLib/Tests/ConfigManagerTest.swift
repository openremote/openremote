//
//  ConfigManagerTest.swift
//  Tests
//
//  Created by Eric Bariaux on 31/08/2022.
//

import XCTest

@testable import ORLib

class ConfigManagerTest: XCTestCase {

    let configManager = ConfigManager(apiManagerFactory: { url in
        FileApiManager(baseUrl: url)
    })

    func test1() async throws {
        let state = try await configManager.setDomain(domain: "test1")
        XCTAssertEqual(state, ConfigManagerState.selectApp("test1", nil))
    }

    func test2() async throws {
        let state = try await configManager.setDomain(domain: "test2")
        XCTAssertEqual(state, ConfigManagerState.selectRealm("test2", "Console 1"))
    }

    func test3() async throws {
        let state = try await configManager.setDomain(domain: "test3")
        XCTAssertEqual(state, ConfigManagerState.selectRealm("test3", "Console 1"))
    }

    func test4() async throws {
        let state = try await configManager.setDomain(domain: "test4")
        XCTAssertEqual(state, ConfigManagerState.selectApp("test4", ["Console 1", "Console 2"]))
    }

    func test5() async throws {
        let state = try await configManager.setDomain(domain: "test5")
        XCTAssertEqual(state, ConfigManagerState.selectApp("test5", nil))
    }

    func test6() async throws {
        let state = try await configManager.setDomain(domain: "test6")
        XCTAssertEqual(state, ConfigManagerState.selectApp("test6", ["Console 1", "Console 2"]))
    }
}
