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

//    var appconfig: ORAppConfig?
    var host: String?
    
    var project: ProjectConfig?
   
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let projectsData = userDefaults.data(forKey: DefaultsKey.projectsConfigurationKey),
           let selectedProject = userDefaults.string(forKey: DefaultsKey.projectKey) {
            
            let projects = try? JSONDecoder().decode([ProjectConfig].self, from: projectsData)
            
            if let projects = projects {
                print(projects)
                print(selectedProject)
                
                // TODO: proper lookup per key
                project = projects[0]
                
                
                // TODO: validate project "correct" before navigating
                self.performSegue(withIdentifier: Segues.goToWebView, sender: self)
            }
               /*
            host = savedHost
            let url = host!.appending("/api/\(realm)")

            let apiManager = HttpApiManager(baseUrl: url)
            apiManager.getAppConfig(realm: realm, callback: { statusCode, orAppConfig, error in
                DispatchQueue.main.async {
                    if statusCode == 200 && error == nil {
                        self.appconfig = orAppConfig

                        self.performSegue(withIdentifier: "goToWebView", sender: self)
                    } else {
                        self.performSegue(withIdentifier: "goToWizardDomainView", sender: self)
//                        self.performSegue(withIdentifier: "goToProjectView", sender: self)
                    }
                }
            })*/
               
        } else {
//            self.performSegue(withIdentifier: "goToProjectView", sender: self)
            self.performSegue(withIdentifier: Segues.goToWizardDomainView, sender: self)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == Segues.goToWebView {
            let orViewController = segue.destination as! ORViewcontroller
            
            if let project = project {
  
                // TODO: replace with proper URL creation
                orViewController.targetUrl = project.targetUrl

//                orViewController.baseUrl = host
            }
        }
    }

}
