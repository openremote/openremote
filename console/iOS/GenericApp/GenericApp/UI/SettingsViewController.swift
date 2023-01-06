//
//  SettingsViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 06/12/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import ORLib

class SettingsViewController: UITableViewController {
  
    private var projects = [ProjectConfig]()
    private var selectedProjectId: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
            selectedProjectId = userDefaults.string(forKey: DefaultsKey.projectKey)
            if let projectsData = userDefaults.data(forKey: DefaultsKey.projectsConfigurationKey) {
                projects = (try? JSONDecoder().decode([ProjectConfig].self, from: projectsData)) ?? []
            }
        }

    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return projects.count;
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell: ProjectTableViewCell = tableView.dequeueReusableCell(withIdentifier: "ProjectCell", for: indexPath) as! ProjectTableViewCell
        cell.setProject(projects[indexPath.row])
        return cell
    }
    
}
