//
//  ProjectConfig.swift
//  ORLib
//
//  Created by Eric Bariaux on 16/09/2022.
//

import Foundation

public struct ProjectConfig: Codable, Equatable {
    public var id: String
    var projectName = "TODO"

    public var domain: String
    public var app: String
    public var realm: String?
    
    public var providers: [String]?
    
    public var baseURL: String {
        return domain
    }
    
    public var targetUrl: String {
        let consoleProviders = self.providers?.joined(separator: " ") ?? "geofence push storage"
        if let realm = realm {
            return "\(baseURL)/\(app)/?realm=\(realm)&consoleProviders=\(consoleProviders)&consoleAutoEnable=true#!geofences"
        } else {
            return "\(baseURL)/\(app)/?consoleProviders=\(consoleProviders)&consoleAutoEnable=true#!geofences"
        }
        
        // TODO: what's that &consoleAutoEnable=true#!geofences part of the URL for ?
    }
    
    
    public init() {
        id = UUID().uuidString
        domain = "demo" // TODO: How to manage this ? Should we have a default init ?
        app = "manager"
    }
    
    public init(domain: String, app: String, realm: String?) {
        id = UUID().uuidString
        self.domain = domain
        self.app = app
        self.realm = realm
    }
    
    public static func ==(lhs: ProjectConfig, rhs: ProjectConfig) -> Bool {
        return lhs.domain == rhs.domain && lhs.app == rhs.app && lhs.realm == rhs.realm && lhs.providers == rhs.providers
    }

}
