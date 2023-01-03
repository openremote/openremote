//
//  SettingsViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 06/12/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit

class SettingsViewController: UITableViewController {
  
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 1;
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        return tableView.dequeueReusableCell(withIdentifier: "ProjectCell", for: indexPath)
    }
    
}
