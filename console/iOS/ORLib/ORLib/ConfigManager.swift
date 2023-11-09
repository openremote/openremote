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
    
    public private(set) var globalAppInfos : [String:ORAppInfo] = [:] // app infos from the top level consoleConfig information
    public private(set) var appInfos : [String:ORAppInfo] = [:] // app infos from each specific app info.json

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
                        globalAppInfos = apps
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
                    
                    // TODO: we should potentially set the realms, either from console config or from specific app config
                    
                    
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
                        let filteredApps = await filterPotentialApps(apiManager: api, potentialApps: apps)
                        if let fa = filteredApps, fa.count == 1, let appName = fa.first {
                            state = .selectRealm(baseUrl, appName, nil)
                        } else {
                            state = .selectApp(baseUrl, filteredApps)
                        }
                        
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
                    let filteredApps = await filterPotentialApps(apiManager: api, potentialApps: cc.allowedApps)
                    if let fa = filteredApps, fa.count == 1, let appName = fa.first {
                        state = .selectRealm(baseUrl, appName, nil)
                    } else {
                        state = .selectApp(baseUrl, filteredApps)
                    }
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
    
    private func filterPotentialApps(apiManager: ApiManager, potentialApps: [String]?) async -> [String]? {
        var filteredApps : [String]?
        if let appNames = potentialApps {
            filteredApps = []
            for appName in appNames {
                if let appInfo = globalAppInfos[appName] {
                    if !appInfo.consoleAppIncompatible {
                        filteredApps!.append(appName)
                    }
                } else {
                    do {
                        if let appInfo = try await apiManager.getAppInfo(appName: appName) {
                            if !appInfo.consoleAppIncompatible {
                                appInfos[appName] = appInfo
                                filteredApps!.append(appName)
                            }
                        } else {
                            filteredApps!.append(appName)
                        }
                    } catch {
                        // We couldn't fetch app info, just include app in list
                        filteredApps!.append(appName)
                    }
                }
            }
        }
        return filteredApps
    }
    
    public func setApp(app: String) throws -> ConfigManagerState {
        switch state {
        case .selectDomain,
                .selectRealm,
                .complete:
            throw ConfigManagerError.invalidState
        case .selectApp(let baseURL, _):
            if let appInfo = globalAppInfos[app] {
                self.state = .selectRealm(baseURL, app, appInfo.realms)
            } else if let appInfo = appInfos[app] {
                self.state = .selectRealm(baseURL, app, appInfo.realms)
            } else if let appInfo = globalAppInfos["default"] {
                self.state = .selectRealm(baseURL, app, appInfo.realms)
            } else {
                self.state = .selectRealm(baseURL, app, nil)
            }
            return state
        }
    }
    
    public func setRealm(realm: String?) throws -> ConfigManagerState {
        switch state {
        case .selectDomain,
                .selectApp:
            throw ConfigManagerError.invalidState
        case .complete(let project):
            
            // TODO: should set the providers on the project config
            
            self.state = .complete(ProjectConfig(domain: project.baseURL, app: project.app, realm: realm))
        case .selectRealm(let baseURL, let app, _):
            
            // TODO: should set the providers on the project config
            
            
            self.state = .complete(ProjectConfig(domain: baseURL, app: app, realm: realm))
        }
        return state
    }
    
}
