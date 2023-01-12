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

        navigationItem.rightBarButtonItem = self.editButtonItem;
        navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .done, target: self, action: #selector(doneTapped))
        navigationItem.title = "Projects"
    }
    
    override func setEditing(_ editing: Bool, animated: Bool) {
        if editing {
            navigationItem.leftBarButtonItem = nil
        } else {
            navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .done, target: self, action: #selector(doneTapped))
        }
        super.setEditing(editing, animated: animated)
    }

    @objc func doneTapped() {
        self.dismiss(animated: true)
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 2
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return section == 0 ? projects.count : 1
    }
    
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        return indexPath.section == 0
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.section == 0 {
            let cell: ProjectTableViewCell = tableView.dequeueReusableCell(withIdentifier: "ProjectCell", for: indexPath) as! ProjectTableViewCell
            let project = projects[indexPath.row]
            cell.setProject(project)
            cell.accessoryType = project.id == selectedProjectId ? .checkmark : .none
            return cell
        } else {
            return tableView.dequeueReusableCell(withIdentifier: "AddProjectCell", for: indexPath)
        }
    }
    
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            let removedProject = projects.remove(at: indexPath.row)
            if selectedProjectId == removedProject.id {
                selectProject(id: projects.first?.id)
                /*
                if projects.count > 0 {
                    if let firstCell = tableView.cellForRow(at: IndexPath(row: 0, section: 0)) {
                        firstCell.accessoryType = .checkmark
                    }
                }
                 This did not help with proper display of selection checkmark
                 */
            }
            
            do {
                if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
                    let data = try JSONEncoder().encode(projects)
                    userDefaults.setValue(data, forKey: DefaultsKey.projectsConfigurationKey)
                }
                tableView.deleteRows(at: [indexPath], with: .fade)
            } catch {
                print(error.localizedDescription)
            }
        }
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)

        if let currentlySelectedProject = projects.first(where:{ $0.id == selectedProjectId }),
           let cellIndex = projects.firstIndex(of: currentlySelectedProject),
           let previousCell = tableView.cellForRow(at: IndexPath(row: cellIndex, section: indexPath.section)) {
                previousCell.accessoryType = .none
        }

        let project = projects[indexPath.row]
        selectProject(id: project.id)
        if let cell = tableView.cellForRow(at: indexPath) {
            cell.accessoryType = .checkmark
        }
    }
    
    private func selectProject(id: String?) {
        selectedProjectId = id
        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
            if id != nil {
                userDefaults.setValue(id, forKey: DefaultsKey.projectKey)
            } else {
                userDefaults.removeObject(forKey: DefaultsKey.projectKey)
            }
        }
    }
}
