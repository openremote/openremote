//
//  ViewControllerFactory.swift
//  ORLib
//
//  Created by Michael Rademaker on 18/02/2024.
//

import UIKit

// In your lib project
public class ViewControllerFactory {
    public static var offlineViewControllerClass: UIViewController.Type = OrOfflineViewController.self

    static func createOfflineViewController() -> UIViewController {
        return offlineViewControllerClass.init()
    }
}
