//
//  ViewController.swift
//  or-shell
//
//  Created by Eric Bariaux on 20/01/17.
//  Copyright © 2017 OpenRemote. All rights reserved.
//

import UIKit
import AppAuth

class ViewController: UIViewController {
    var authState : OIDAuthState?
    @IBOutlet weak var loginViewController: UIWebView!
    override func viewDidLoad() {
        super.viewDidLoad()
        let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
        NotificationCenter.default.addObserver(self, selector: #selector(isAuthenticated), name: notificationName, object: nil)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        showLoginPage()
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func showLoginPage() {
        if (TokenManager.sharedInstance.hasToken) {
            let orVC = ORViewcontroller()
            self.present(orVC, animated: true, completion: nil)
        } else {
            TokenManager.sharedInstance.authenticate()
        }
    }
    
    func isAuthenticated() {
        showLoginPage()
    }
}



