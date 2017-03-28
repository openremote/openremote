//
//  ORViewController.swift
//  or-shell
//
//  Created by William Balcaen on 02/02/17.
//  Copyright © 2017 OpenRemote. All rights reserved.
//

import Foundation
import UIKit
import WebKit

class ORViewcontroller : UIViewController, URLSessionDelegate, WKScriptMessageHandler, WKUIDelegate, WKNavigationDelegate {
    
    var data : Data?
    var myWebView : WKWebView?
    var defaults : UserDefaults?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let webCfg:WKWebViewConfiguration = WKWebViewConfiguration()
        
        let userController:WKUserContentController = WKUserContentController()
        
        userController.add(self, name: DefaultsKey.offlineToken)
        userController.add(self, name: DefaultsKey.refreshToken)
        userController.add(self, name: DefaultsKey.idToken)
        
        var exec_template = "var iOSToken"
        if TokenManager.sharedInstance.hasToken {
            exec_template = String(format: "var iOSToken = \"%@\"; var iOSRefreshToken = \"%@\"; var iOSTokenId = \"%@\";", TokenManager.sharedInstance.offlineToken!, TokenManager.sharedInstance.refreshToken!, TokenManager.sharedInstance.idToken!)
        }
        
        let userScript:WKUserScript = WKUserScript(source: exec_template, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        userController.addUserScript(userScript)
        
        
        webCfg.userContentController = userController;
        myWebView = WKWebView(frame: view.frame, configuration: webCfg)
        myWebView?.uiDelegate = self;
        myWebView?.navigationDelegate = self;
        view.addSubview(myWebView!)
        
        //self.apiCall()
        
        self.login()
        
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.set(message.body, forKey: message.name)
        defaults?.synchronize()
        
    }
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alertView = UIAlertController(title: "debug", message: message, preferredStyle: .alert)
        let alertAction = UIAlertAction(title: "Done", style: .cancel) { (action) in
            completionHandler()
        }
        let alertActionResetToken = UIAlertAction(title: "Reset Token", style: .destructive) { (action) in
            TokenManager.sharedInstance.resetToken()
            self.dismiss(animated: true, completion: nil)
            completionHandler()
        }
        alertView.addAction(alertAction)
        alertView.addAction(alertActionResetToken)
        self.present(alertView, animated: true, completion: nil)
    }
    
    func showError(error : Error) {
        let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertControllerStyle.alert)
        let alertAction = UIAlertAction(title: "Done", style: .cancel, handler: nil)
        let alertActionResetToken = UIAlertAction(title: "Reset Token", style: .destructive) { (action) in
            TokenManager.sharedInstance.resetToken()
            self.dismiss(animated: true, completion: nil)
            
        }
        alertVC.addAction(alertAction)
        alertVC.addAction(alertActionResetToken)
        self.present(alertVC, animated: true, completion: nil)
    }
    
    func apiCall() {
        guard let tkurlRequest = URL(string: String(format:"http://%@:%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.port,Server.realm)) else { return }
        let tkRequest = NSMutableURLRequest(url: tkurlRequest)
        tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
        tkRequest.httpMethod = "POST"
        let postString = String(format:"grant_type=refresh_token&refresh_token=%@&client_id=%@",(TokenManager.sharedInstance.refreshToken )!,Client.clientId)
        tkRequest.httpBody = postString.data(using: .utf8)
        let sessionConfiguration = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfiguration)
        let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
            if (data != nil){
                do {
                    let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                    if ((jsonDictionnary["access_token"]) != nil) {
                        guard let urlRequest = URL(string: String(Server.apiTestResource)) else { return }
                        let request = NSMutableURLRequest(url: urlRequest)
                        request.addValue(String(format:"Bearer %@", jsonDictionnary["access_token"] as! String), forHTTPHeaderField: "Authorization")
                        let sessionConfiguration = URLSessionConfiguration.default
                        let session = URLSession(configuration: sessionConfiguration)
                        let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                            DispatchQueue.main.async {
                                if (error != nil) {
                                    self.showError(error: error!)
                                } else {
                                    _ = self.myWebView?.load(data!, mimeType: "text/html", characterEncodingName: "utf8", baseURL: URL(string:Server.apiTestResource)!)
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
    }
    
    func login() {
        guard let request = URL(string: String(format:"http://%@:%@/%@",Server.hostURL,Server.port,Server.initialPath)) else { return }
        _ = self.myWebView?.load(URLRequest(url: request))
    }
    
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        self.showError(error: error)
    }
    
}
