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

    var host: String?
    var project: ProjectConfig?
    
    var displaySettings = false
   
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if (displaySettings) {
            self.performSegue(withIdentifier: Segues.goToSettingsView, sender: self)
            displaySettings = false
            return
        }
        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let projectsData = userDefaults.data(forKey: DefaultsKey.projectsConfigurationKey),
           let selectedProjectId = userDefaults.string(forKey: DefaultsKey.projectKey) {
            
            let projects = try? JSONDecoder().decode([ProjectConfig].self, from: projectsData)
            
            if let projects = projects {
                print("Known projects \(projects)")
                print("Selected project \(selectedProjectId)")
                
                if let selectedProject = projects.first(where: { $0.id == selectedProjectId } ) {
                    project = selectedProject
                    self.performSegue(withIdentifier: Segues.goToWebView, sender: self)
                    return
                }
            }
        }
        self.performSegue(withIdentifier: Segues.goToWizardDomainView, sender: self)
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
