//
//  ProjectCell.swift
//  GenericApp
//
//  Created by Eric Bariaux on 06/01/2023.
//  Copyright © 2023 OpenRemote. All rights reserved.
//

import UIKit
import ORLib


class ProjectTableViewCell: UITableViewCell {
    
    @IBOutlet weak var domainLabel: UILabel!
    @IBOutlet weak var appLabel: UILabel!
    @IBOutlet weak var realmLabel: UILabel!

    var project: ProjectConfig?
    
    func setProject(_ project: ProjectConfig) {
        self.project = project
        domainLabel.text = project.domain
        appLabel.text = "App: \(project.app)"
        realmLabel.text = project.realm != nil ? "Realm: \(project.realm!)" : ""
    }
}
