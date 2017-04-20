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
    let orViewController = ORViewcontroller()
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
        if (TokenManager.sharedInstance.hasToken && !TokenManager.sharedInstance.didLogOut) {
            let orVC = ORViewcontroller()
            self.present(orVC, animated: true, completion: nil)
            self.present(orViewController, animated: true, completion: nil)
        } else {
            TokenManager.sharedInstance.authenticate()
        }
    }
    
    func isAuthenticated() {
        TokenManager.sharedInstance.didLogOut = false
        showLoginPage()
    }
    func login() {
        self.loginButton.removeFromSuperview()
        self.showLoginPage()
    }
    
    func loadUrl(url: URL) {
        if (self.presentedViewController?.isMember(of: ORViewcontroller.self))! {
            self.orViewController.loadURL(url: url)
        }
    }
    
    func updateAssetAttribute(assetId : String, attributeName : String, rawJson : String) {
            self.orViewController.updateAssetAttribute(assetId: assetId, attributeName: attributeName, rawJson: rawJson)
    }
}



