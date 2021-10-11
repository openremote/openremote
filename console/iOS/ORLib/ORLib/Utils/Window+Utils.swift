//
//  Window+Utils.swift
//  GenericApp
//
//  Created by Michael Rademaker on 02/11/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import Foundation
import UIKit

extension UIWindow {

    public var topController : UIViewController? {

        guard let rootViewController = self.rootViewController else {
            return nil
        }
        
        var topController = rootViewController

        while let newTopController = topController.presentedViewController {
            topController = newTopController
        }

        return topController
    }
}
