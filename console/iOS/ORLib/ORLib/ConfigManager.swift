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
    case selectRealm(String, String, [String]?) // baseURL, app, list of realms to choose from
    case complete(ProjectConfig)
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
    
    public private(set) var appInfos : [String:ORAppInfo] = [:]

    public private(set) var state = ConfigManagerState.selectDomain
    
    public init(apiManagerFactory: @escaping ApiManagerFactory) {
        self.apiManagerFactory = apiManagerFactory
    }

    public func setDomain(domain: String) async throws -> ConfigManagerState  {
        switch state {
        case .selectDomain:
            let baseUrl = domain.isValidURL ? domain : "https://\(domain).openremote.app"
            let url = baseUrl.appending("/api/master")

            apiManager = apiManagerFactory(url)
            
            guard let api = apiManager else {
                throw ConfigManagerError.communicationError
            }
            
            do {
                let cc: ORConsoleConfig
                do {
                    cc = try await api.getConsoleConfig() ?? ORConsoleConfig()
                    if let apps = cc.apps {
                        appInfos = apps
                    }
                } catch ApiManagerError.communicationError(let httpStatusCode) {
                    if httpStatusCode == 404 || httpStatusCode == 403 {
                        // 403 is for backwards compatibility of older manager
                        cc = ORConsoleConfig()
                    } else {
                        throw ApiManagerError.communicationError(httpStatusCode)
                    }
                }
                
                if let selectedApp = cc.app {
                    state = .selectRealm(baseUrl, selectedApp, nil)
                    return state
                }
                
                if cc.showAppTextInput {
                    state = .selectApp(baseUrl, nil)
                    return state
                }
                
                // allowedApps == nil -> get list of apps
                if cc.allowedApps == nil || cc.allowedApps!.isEmpty {
                    do {
                        let apps = try await api.getApps()
                        state = .selectApp(baseUrl, apps)
                    } catch ApiManagerError.communicationError(let httpStatusCode) {
                        if httpStatusCode == 404 || httpStatusCode == 403 {
                            if cc.showRealmTextInput {
                                state = .selectRealm(baseUrl, "manager", nil)
                            } else {
                                state = .complete(ProjectConfig(domain: baseUrl, app: "manager", realm: nil))
                            }
                        }
                    }
                } else {
                    self.state = .selectApp(baseUrl, cc.allowedApps)
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
    
    public func setApp(app: String) throws -> ConfigManagerState {
        switch state {
        case .selectDomain,
                .selectRealm,
                .complete:
            throw ConfigManagerError.invalidState
        case .selectApp(let baseURL, _):
            // TODO: must fill realm values when possible
            self.state = .selectRealm(baseURL, app, nil)
            return state
        }
    }
    
    public func setRealm(realm: String?) throws -> ConfigManagerState {
        switch state {
        case .selectDomain,
                .selectApp,
                .complete:
            throw ConfigManagerError.invalidState
        case .selectRealm(let baseURL, let app, _):
            
            // TODO: should set the providers on the project config
            
            
            self.state = .complete(ProjectConfig(domain: baseURL, app: app, realm: realm))
            return state
        }
    }
    
}
