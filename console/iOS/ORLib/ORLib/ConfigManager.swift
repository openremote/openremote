//
//  ConfigManager.swift
//  ORLib
//
//  Created by Eric Bariaux on 31/08/2022.
//

import Foundation

public enum ConfigManagerState: Equatable {
    case selectDomain
    case selectApp(String, [String]?) // baseURL, list of apps to choose from
    case selectRealm(String, String) // baseURL, app
    case complete(String, String, String) // baseURL, app, realm
}

public enum ConfigManagerError: Error {
    case invalidState
    case communicationError
    case couldNotLoadAppConfig
}

public typealias ApiManagerFactory = (String) -> ApiManager


public class ConfigManager {
    
    private var apiManagerFactory: ((String) -> ApiManager)
    private var apiManager: ApiManager?

    private var state = ConfigManagerState.selectDomain
    
    public init(apiManagerFactory: @escaping ApiManagerFactory) {
        self.apiManagerFactory = apiManagerFactory
    }

    public func setDomain(domain: String) async throws -> ConfigManagerState  {
        switch state {
        case .selectDomain:
            let url = domain.isValidURL ? domain.appending("/api/master") : "https://\(domain).openremote.app/api/master"

            apiManager = apiManagerFactory(url)
            
            guard let api = apiManager else {
                throw ConfigManagerError.communicationError
            }
            
            do {
                let cc = try await api.getConsoleConfig() ?? ORConsoleConfig()
                
                
                // TODO: this throws if can't get the config, is this what we want ???
                
                
                

                // TODO: app != nil -> don't ask user for app
                if let selectedApp = cc.app {
                    state = .selectRealm(domain, selectedApp)
                    return state
                }
                
                if cc.showAppTextInput {
                    state = .selectApp(domain, nil)
                    return state
                }
                
                // allowedApps == nil -> get list of apps
                if cc.allowedApps == nil || cc.allowedApps!.isEmpty {
                    let apps = try await api.getApps()
                    state = .selectApp(domain, apps)
                } else {
                    self.state = .selectApp(domain, cc.allowedApps)
                }
                return state
            } catch {
                print("SetDomain -> error: \(error)")
                throw ConfigManagerError.couldNotLoadAppConfig
            }
        case .selectApp,
                .selectRealm,
                .complete:
            throw ConfigManagerError.invalidState
        }
        
    }
    
    func setApp(app: String) throws {
        switch state {
        case .selectDomain,
                .selectRealm,
                .complete:
            throw ConfigManagerError.invalidState
        case .selectApp:
            print()
        }
    }
    
    func setRealm(realm: String) throws {
        switch state {
        case .selectDomain,
                .selectApp,
                .complete:
            throw ConfigManagerError.invalidState
        case .selectRealm:
            print()
        }
    }
    
}
