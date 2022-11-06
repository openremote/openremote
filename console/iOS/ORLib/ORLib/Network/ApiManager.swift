//
//  ApiManager.swift
//  ORLib
//
//  Created by Eric Bariaux on 31/08/2022.
//

import Foundation

public typealias ResponseBlock<T: Codable> = (_ statusCode: Int, _ object: T?, _ error: Error?) -> ()

public enum ApiManagerError: Error {
    case notFound
    case communicationError(Int)
    case parsingError(Int)
}

public protocol ApiManager {
    
    func getConsoleConfig(callback: ResponseBlock<ORConsoleConfig>?)

    func getConsoleConfig() async throws -> ORConsoleConfig?
    
    func getApps(callback: ResponseBlock<[String]>?)
    
    func getApps() async throws -> [String]?
    
    func getAppInfo(appName: String) async throws -> ORAppInfo?

}
