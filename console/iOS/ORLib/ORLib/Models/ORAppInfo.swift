//
//  ORAppInfo.swift
//  ORLib
//
//  Created by Eric Bariaux on 21/07/2022.
//

import Foundation

public struct ORAppInfo: Codable {
    public var consoleAppIncompatible: Bool
    public var realms: [String]?
    public var providers: [String]?
    public var description: String?
}
