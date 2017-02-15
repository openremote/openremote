//
//  ViewController.swift
//  or-shell
//
//  Created by Eric Bariaux on 20/01/17.
//  Copyright © 2017 OpenRemote. All rights reserved.
//

import UIKit
import AeroGearOAuth2
import AeroGearHttp

class ViewController: UIViewController {
    
    @IBOutlet weak var loginViewController: UIWebView!
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func showLoginPage(_ sender: Any) {
        let http = Http()
        let keycloakConfig = KeycloakConfig(
            clientId: "openremote",
            host: "http://192.168.99.100:8080",
            realm: "master",
            isOpenIDConnect: true)
        keycloakConfig.isWebView = true
        let oauth2Module = AccountManager.addAccountWith(config: keycloakConfig, moduleClass: OAuth2Module.self)
        http.authzModule = oauth2Module
        oauth2Module.requestAccess { (response, error) in
            var token : String
            if response != nil {
                token = response! as! String
                print(token)
                
                let orVC = ORViewcontroller()
                
                self.navigationController?.pushViewController(orVC, animated: true)
            } else if error != nil {
                let alertVC = UIAlertController(title: "Error", message: error.debugDescription, preferredStyle: UIAlertControllerStyle.alert)
                self.present(alertVC, animated: true, completion: nil)
            }
        }
    }
}



