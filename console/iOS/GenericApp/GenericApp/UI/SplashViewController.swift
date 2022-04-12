//
//  SplashController.swift
//  GenericApp
//
//  Created by Michael Rademaker on 21/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UIKit
import ORLib

class SplashViewController: UIViewController {

    var appconfig: ORAppConfig?
    var host: String?

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let savedHost = userDefaults.string(forKey: DefaultsKey.hostKey),
           let realm = userDefaults.string(forKey: DefaultsKey.realmKey) {
            host = savedHost
            let url = host!.appending("/api/\(realm)")

            let apiManager = ApiManager(baseUrl: url)
            apiManager.getAppConfig(realm: realm, callback: { statusCode, orAppConfig, error in
                DispatchQueue.main.async {
                    if statusCode == 200 && error == nil {
                        self.appconfig = orAppConfig

                        self.performSegue(withIdentifier: "goToWebView", sender: self)
                    } else {
                        self.performSegue(withIdentifier: "goToProjectView", sender: self)
                    }
                }
            })
        } else {
            self.performSegue(withIdentifier: "goToProjectView", sender: self)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "goToWebView" {
            let orViewController = segue.destination as! ORViewcontroller
            orViewController.appConfig = self.appconfig
            orViewController.baseUrl = host
        }
    }
}
