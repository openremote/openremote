//
//  ORConsoleConfig.swift
//  ORLib
//
//  Created by Eric Bariaux on 21/07/2022.
//

import Foundation

public struct ORConsoleConfig: Codable {
    
    public init() {
    }
    
    public var showAppTextInput = false
    public var showRealmTextInput = true
    public var app: String?
    public var allowedApps: [String]?
    public var apps: [String:ORAppInfo]?
}
