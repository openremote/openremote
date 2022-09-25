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

    var domain: String
    var app: String
    var realm: String?
    
    var baseURL: String {
        return domain
    }
    
    public var targetUrl: String {
        if let realm = realm {
            return "\(baseURL)/\(app)/?realm=\(realm)&consoleProviders=geofence push storage&consoleAutoEnable=true#!geofences"
        } else {
            return "\(baseURL)/\(app)/?consoleProviders=geofence push storage&consoleAutoEnable=true#!geofences"
        }
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
        return lhs.domain == rhs.domain && lhs.app == rhs.app && lhs.realm == rhs.realm
    }

}
