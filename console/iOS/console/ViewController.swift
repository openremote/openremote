/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
    
    func showLoginPage() {
        if (TokenManager.sharedInstance.hasToken && !TokenManager.sharedInstance.didLogOut) {
            isInError = false
            if (self.presentedViewController == nil) {
            self.present(orViewController, animated: true, completion: nil)
            }
        } else {
            TokenManager.sharedInstance.authenticate()
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



