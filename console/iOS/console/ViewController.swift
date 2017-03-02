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
        let defaults = UserDefaults.standard
        if let offlineToken = defaults.object(forKey: "offlineToken")  {
            
            // refresh_token using stored offline token
            guard let tkurlRequest = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.port,Server.realm)) else { return }
            let tkRequest = NSMutableURLRequest(url: tkurlRequest)
            tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
            tkRequest.httpMethod = "POST"
            let postString = String(format:"grant_type=refresh_token&refresh_token=%@&client_id=openremote",(offlineToken as! String))
            tkRequest.httpBody = postString.data(using: .utf8)
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration)
            let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
                if (data != nil){
                    do {
                        let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                        if ((jsonDictionnary["access_token"]) != nil) {
                            guard let urlRequest = URL(string: Server.apiTestResource) else { return }
                            let request = NSMutableURLRequest(url: urlRequest)
                            request.addValue(String(format:"Bearer %@", jsonDictionnary["access_token"] as! String), forHTTPHeaderField: "Authorization")
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
                        } else {
                            if let httpResponse = response as? HTTPURLResponse {
                                let error = NSError(domain: "", code: httpResponse.statusCode, userInfo: jsonDictionnary)
                                self.showError(error: error)
                            } else {
                                let error = NSError(domain: "", code: 0, userInfo: jsonDictionnary)
                                self.showError(error: error)
                            }
                        }
                    }
                    catch let error as NSError {
                        self.showError(error: error)
                    }
                } else {
                    self.showError(error: error!)
                }
            })
            req.resume()
        } else {
            let authorizationEndoint = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/auth",Server.hostURL,Server.port,Server.realm))
            let tokenEndpoint = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.port,Server.realm))
            let configuration = OIDServiceConfiguration(authorizationEndpoint: authorizationEndoint!, tokenEndpoint: tokenEndpoint!)
            let authRequest = OIDAuthorizationRequest(configuration: configuration, clientId: Client.clientId, scopes: ["offline_access","openid"], redirectURL: URL(string:String(format:"%@://oauth2Callback",Bundle.main.bundleIdentifier!))!, responseType: OIDResponseTypeCode, additionalParameters: nil)
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
                            defaults.set(self.authState?.refreshToken, forKey: "offlineToken")
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
    }
    
    func showError(error : Error) {
        let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertControllerStyle.alert)
        let alertAction = UIAlertAction(title: "Done", style: UIAlertActionStyle.cancel) { alertAction in
            print("action")
        }
        alertVC.addAction(alertAction)
        self.present(alertVC, animated: true, completion: nil)
    }
    
}



