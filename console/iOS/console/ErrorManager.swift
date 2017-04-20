//
//  ErrorManager.swift
//  console
//
//  Created by William Balcaen on 20/04/2017.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation
import UIKit

class ErrorManager : NSObject {
    
    static func showError(error : Error)  {
        DispatchQueue.main.async {
            NSLog("showing error %@",error as NSError)
            let topWindow = UIWindow(frame: UIScreen.main.bounds)
            topWindow.rootViewController = UIViewController()
            topWindow.windowLevel = UIWindowLevelAlert + 1
            let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertControllerStyle.alert)
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
