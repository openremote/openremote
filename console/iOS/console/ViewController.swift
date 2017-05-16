//
//  ViewController.swift
//  or-shell
//
//  Created by Eric Bariaux on 20/01/17.
//  Copyright © 2017 OpenRemote. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    var isInError : Bool = false
    let loginButton = UIButton(type: .roundedRect)
    let orViewController = ORViewcontroller()
    
    @IBOutlet weak var loginViewController: UIWebView!
    override func viewDidLoad() {
        super.viewDidLoad()
        let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
        NotificationCenter.default.addObserver(self, selector: #selector(isAuthenticated), name: notificationName, object: nil)
        let deviceIdSentName = Notification.Name(NotificationsNames.isdeviceIdSent)
        NotificationCenter.default.addObserver(self, selector: #selector(isdeviceIdSent), name: deviceIdSentName, object: nil)

    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if isInError {
            loginButton.isHidden = false
        } else {
            loginButton.isHidden = true
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if (!isInError) {
            showLoginPage()
        } else {
            loginButton.setTitle("Login", for: .normal)
            loginButton.addTarget(self, action: #selector(login), for: .touchUpInside)
            loginButton.frame = self.view.frame
            self.view.addSubview(loginButton)
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    func showLoginPage() {
        if (TokenManager.sharedInstance.hasToken && !TokenManager.sharedInstance.didLogOut) {
            isInError = false
            self.present(orViewController, animated: true, completion: nil)
        } else {
            TokenManager.sharedInstance.authenticate()
            isInError = true
        }
    }
    
    func isAuthenticated() {
        TokenManager.sharedInstance.didLogOut = false
        TokenManager.sharedInstance.sendDeviceId()
    }
    
    func isdeviceIdSent() {
        showLoginPage()
    }
    
    func login() {
        self.loginButton.removeFromSuperview()
        self.showLoginPage()
    }
    
    func loadUrl(url: URL) {
        self.orViewController.loadURL(url: url)
    }
    
    func updateAssetAttribute(assetId : String, attributeName : String, rawJson : String) {
        self.orViewController.updateAssetAttribute(assetId: assetId, attributeName: attributeName, rawJson: rawJson)
    }
}



