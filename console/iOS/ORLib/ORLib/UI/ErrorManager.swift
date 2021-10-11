/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import Foundation
import UIKit

public class ErrorManager : NSObject {
    
    static func showError(error : Error)  {
        DispatchQueue.main.async {
            NSLog("showing error %@",error as NSError)
            let topWindow = UIWindow(frame: UIScreen.main.bounds)
            topWindow.rootViewController = UIViewController()
            topWindow.windowLevel = UIWindow.Level.alert + 1
            let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertController.Style.alert)
            let alertAction = UIAlertAction(title: "Done", style: .cancel) { (action) in
                topWindow.rootViewController?.presentedViewController?.dismiss(animated: true, completion: nil)
                topWindow.isHidden = true
            }
            
            alertVC.addAction(alertAction)
            DispatchQueue.main.async {
                topWindow.makeKeyAndVisible()
                topWindow.rootViewController?.present(alertVC, animated: true, completion: nil)
            }
        }
    }
}
