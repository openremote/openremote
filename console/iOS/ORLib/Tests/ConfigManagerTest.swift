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

    func test0() async throws {
        let state = try await configManager.setDomain(domain: "test0")
        XCTAssertEqual(state, ConfigManagerState.selectRealm("https://test0.openremote.app", "manager", nil))
    }

    func test1() async throws {
        let state = try await configManager.setDomain(domain: "test1")
        XCTAssertEqual(state, ConfigManagerState.complete("https://test1.openremote.app", "manager", nil))
    }

    func test2() async throws {
        let state = try await configManager.setDomain(domain: "test2")
        XCTAssertEqual(state, ConfigManagerState.selectRealm("https://test2.openremote.app", "Console 1", nil))
    }

    func test3() async throws {
        let state = try await configManager.setDomain(domain: "test3")
        XCTAssertEqual(state, ConfigManagerState.selectRealm("https://test3.openremote.app", "Console 1", nil))
    }

    func test4() async throws {
        let state = try await configManager.setDomain(domain: "test4")
        XCTAssertEqual(state, ConfigManagerState.selectApp("https://test4.openremote.app", ["Console 1", "Console 2"]))
    }

    func test5() async throws {
        let state = try await configManager.setDomain(domain: "test5")
        XCTAssertEqual(state, ConfigManagerState.selectApp("https://test5.openremote.app", nil))
    }

    func test6() async throws {
        let state = try await configManager.setDomain(domain: "test6")
        XCTAssertEqual(state, ConfigManagerState.selectApp("https://test6.openremote.app", ["Console 1", "Console 2"]))
    }
}
