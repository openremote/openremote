//
//  ORPushNotificationAction.swift
//  ORLib
//
//  Created by Michael Rademaker on 01/08/2018.
//

import UIKit

public struct ORPushNotificationAction: Codable {
    public let url: String
    public let httpMethod: String?
    public let data: String?
    public let silent: Bool
    public let openInBrowser: Bool
}
