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
        // Do any additional setup after loading the view, typically from a nib.
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func showLoginPage(_ sender: Any) {
        let authorizationEndoint = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/auth",Server.hostURL,Server.port,Server.realm))
        let tokenEndpoint = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.port,Server.realm))
        let configuration = OIDServiceConfiguration(authorizationEndpoint: authorizationEndoint!, tokenEndpoint: tokenEndpoint!)
        let authRequest = OIDAuthorizationRequest(configuration: configuration, clientId: Client.clientId, scopes: ["offline_access"], redirectURL: URL(string:String(format:"%@://oauth2Callback",Bundle.main.bundleIdentifier!))!, responseType: OIDResponseTypeCode, additionalParameters: nil)
        let appDelegate :AppDelegate = UIApplication.shared.delegate as! AppDelegate
        
        appDelegate.currentAuthorizationFlow = OIDAuthState.authState(byPresenting: authRequest, presenting: self, callback: { (state, error) in
            if (error != nil) {
                self.showError(error: error!)
            } else {
                self.authState = state
                self.authState?.performAction(freshTokens: { (accessToken, idToken, error) in
                    if (error != nil) {
                        self.showError(error: error!)
                    } else {
                        guard let urlRequest = URL(string: Server.apiTestResource) else { return }
                        let request = NSMutableURLRequest(url: urlRequest)
                        request.addValue(String(format:"Bearer %@", accessToken!), forHTTPHeaderField: "Authorization")
                        let sessionConfiguration = URLSessionConfiguration.default
                        let session = URLSession(configuration: sessionConfiguration)
                        let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                            DispatchQueue.main.async {
                                if (error != nil) {
                                    self.showError(error: error!)
                                } else {
                                    let orVC = ORViewcontroller()
                                    orVC.data = data
                                    self.navigationController?.pushViewController(orVC, animated: true)
                                }
                            }
                        })
                        reqDataTask.resume()
                    }
                })
            }
        })
    }
    
    func showError(error : Error) {
        let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertControllerStyle.actionSheet)
        self.present(alertVC, animated: true, completion: nil)
    }
    
}



