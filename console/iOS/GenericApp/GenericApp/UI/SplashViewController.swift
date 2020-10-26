//
//  SplashController.swift
//  GenericApp
//
//  Created by Michael Rademaker on 21/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UIKit

class SplashViewController: UIViewController {

    var appconfig: ORAppConfig?

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let project = userDefaults.string(forKey: DefaultsKey.projectKey),
           let realm = userDefaults.string(forKey: DefaultsKey.realmKey) {
            let apiManager = ApiManager(baseUrl: "https://\(project).openremote.io/api/\(realm)")
            apiManager.getAppConfig(callback: { statusCode, orAppConfig, error in
                DispatchQueue.main.async {
                    if statusCode == 200 && error == nil {
                        let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
                        userDefaults?.set(project, forKey: DefaultsKey.projectKey)
                        userDefaults?.set(realm, forKey: DefaultsKey.realmKey)
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
        }
    }
}
