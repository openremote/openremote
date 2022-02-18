//
//  ORAppConfig.swift
//  GenericApp
//
//  Created by Michael Rademaker on 21/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UIKit

public struct ORAppConfig: Codable {
    let realm: String
    let initialUrl: String?
    let url: String
    let menuEnabled: Bool
    let menuPosition: String?
    let primaryColor: String?
    let secondaryColor: String?
    let links: [ORLinkConfig]?
}
