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
        let bundleString = Bundle.main.bundleIdentifier ?? "keycloak"
        let keycloakConfig = Config.init(base: "http://192.168.99.100:8080/auth", authzEndpoint: "realms/master/protocol/openid-connect/auth", redirectURL: "\(bundleString)://oauth2Callback", accessTokenEndpoint: "realms/master/protocol/openid-connect/token", clientId: "or-manager", refreshTokenEndpoint: "realms/master/protocol/openid-connect/token", revokeTokenEndpoint: "realms/master/protocol/openid-connect/logout", isOpenIDConnect: true, userInfoEndpoint: "realms/master/protocol/openid-connect/userinfo", scopes: ["openid", "email", "profile"], clientSecret: nil, accountId: nil, isWebView: true)
        /*let keycloakConfig = KeycloakConfig(
         clientId: "or-manager",
         host: "http://192.168.99.100:8080/",
         realm: "master",
         isOpenIDConnect: true)*/
        //keycloakConfig.accountId = "admin"
        let oauth2Module = AccountManager.addAccountWith(config: keycloakConfig, moduleClass: OAuth2Module.self)
        http.authzModule = oauth2Module
        oauth2Module.login {(accessToken: AnyObject?, claims: OpenIdClaim?, error: NSError?) in
            if let userInfo = claims {
                if let name = userInfo.name {
                    print("name ",name)
                }
            }
        }

    }
}


