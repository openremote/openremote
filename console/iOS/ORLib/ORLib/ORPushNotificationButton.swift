//
//  ORPushNotificationButton.swift
//  ORLib
//
//  Created by Michael Rademaker on 07/08/2018.
//

import UIKit

public struct ORPushNotificationButton: Decodable {
    public let title: String
    public let action: ORPushNotificationAction?
}
