//
//  String+Utils.swift
//  GenericApp
//
//  Created by Michael Rademaker on 26/01/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import Foundation
import UIKit

extension String {
    func isUrl () -> Bool {
        if let url = NSURL(string: self) {
            return UIApplication.shared.canOpenURL(url as URL)
        }
        return false
    }
}
