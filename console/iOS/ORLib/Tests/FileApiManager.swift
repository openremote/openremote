//
//  FileApiManager.swift
//  Tests
//
//  Created by Eric Bariaux on 31/08/2022.
//

import Foundation
@testable import ORLib

class FileApiManager: ApiManager {

    let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
//        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()


    private var consoleConfig : ORConsoleConfig?
    private var apps: [String]?
    
    
    // TODO: need a way to return errors, how to define that in test files ?
    
    public init(baseUrl: String) {
        if let consoleConfigFile = Bundle(for: FileApiManager.self).url(forResource: "\(baseUrl)-ConsoleConfig", withExtension: "json") {
            if let consoleConfigData = try? Data(contentsOf: consoleConfigFile as URL) {
                do {
                    consoleConfig = try self.decoder.decode(ORConsoleConfig.self, from: consoleConfigData)
                } catch {
                    print(error)
                }
            }
        }
        if let appsFile = Bundle(for: FileApiManager.self).url(forResource: "\(baseUrl)-Apps", withExtension: "txt") {
            if let appsContent = try? Data(contentsOf: appsFile as URL) {
                do {
                    apps = try self.decoder.decode([String].self, from: appsContent)
                } catch {
                    print(error)
                }
            }
        }
    }

    
    func getConsoleConfig(callback: ResponseBlock<ORConsoleConfig>?) {
    }

    public func getConsoleConfig() async throws -> ORConsoleConfig? {
        return consoleConfig
    }

    public func getApps(callback: ResponseBlock<[String]>?) {
    }
    
    public func getApps() async throws -> [String]? {
        return apps
    }

}
