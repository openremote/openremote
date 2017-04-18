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
    var webCfg : WKWebViewConfiguration?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white
        self.configureAccess()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.set(message.body, forKey: message.name)
        defaults?.synchronize()
        
    }
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        NSLog("error %@", message)
        let error = NSError(domain: "", code: 0, userInfo:  [
            NSLocalizedDescriptionKey :  NSLocalizedString("ErrorGettingLoginScreen", value: "Could not login, your access token probably expired", comment: "")
            ])
        let alertView = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: .alert)
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
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if (navigationAction.request.url?.absoluteString.contains("logout"))! {
            decisionHandler(.allow)
            TokenManager.sharedInstance.didLogOut = true
            self.dismiss(animated: false, completion: nil)
        } else {
            decisionHandler(.allow)
        }
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
    
    // just for testing purpose (ask backend to get list of notifications or send a notification to device)
    func apiCall() {
        guard let tkurlRequest = URL(string: String(format:"https://%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.realm)) else { return }
        let tkRequest = NSMutableURLRequest(url: tkurlRequest)
        tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
        tkRequest.httpMethod = "POST"
        let postString = String(format:"grant_type=refresh_token&refresh_token=%@&client_id=%@",(TokenManager.sharedInstance.refreshToken )!,Client.clientId)
        tkRequest.httpBody = postString.data(using: .utf8)
        let sessionConfiguration = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
        let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
            if (data != nil){
                do {
                    let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                    if ((jsonDictionnary["access_token"]) != nil) {
                        guard let urlRequest = URL(string: String(Server.apiTestResource)) else { return }
                        let request = NSMutableURLRequest(url: urlRequest)
                        request.httpMethod = "GET" //change to post to create a notification alert
                        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                        request.httpBody = "{\"title\" :\" notif alert title\",\"message\" : \"notif alert message\",\"appUrl\" : \"http://www.openremote.org\",\"actions\": [ {\"title\" : \"Open link\" , \"type\": \"ACTION_DEEP_LINK\"},{\"title\" : \"background call\" , \"type\": \"ACTION_ACTUATOR\"} ]}".data(using: .utf8)
                        request.addValue(String(format:"Bearer %@", jsonDictionnary["access_token"] as! String), forHTTPHeaderField: "Authorization")
                        let sessionConfiguration = URLSessionConfiguration.default
                        let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue : nil)
                        let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                            DispatchQueue.main.async {
                                if (error != nil) {
                                    NSLog("error %@", (error as! NSError).localizedDescription)
                                    let error = NSError(domain: "", code: 0, userInfo:  [
                                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                        ])
                                    self.showError(error: error)
                                } else {
                                    print(response.debugDescription)
                                    _ = self.myWebView?.load(data!, mimeType: "text/html", characterEncodingName: "utf8", baseURL: URL(string:Server.apiTestResource)!)
                                }
                            }
                        })
                        reqDataTask.resume()
                    } else {
                        if let httpResponse = response as? HTTPURLResponse {
                            NSLog("error %@", httpResponse)
                            let error = NSError(domain: "", code: httpResponse.statusCode, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            self.showError(error: error)
                        } else {
                            if (error != nil) {
                                NSLog("error %@", (error as! NSError).localizedDescription as String)
                            }
                            let error = NSError(domain: "", code: 0, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            self.showError(error: error)
                        }
                    }
                }
                catch let error as NSError {
                    NSLog("error %@", error.localizedDescription)
                    let error = NSError(domain: "", code: 0, userInfo:  [
                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorDeserializingJSON", value: "Could not convert received data", comment: "")
                        ])
                    self.showError(error: error)
                }
            } else {
                NSLog("error %@", (error as! NSError).localizedDescription)
                let error = NSError(domain: "", code: 0, userInfo:  [
                    NSLocalizedDescriptionKey :  NSLocalizedString("NoDataReceived", value: "Did not receive any data", comment: "")
                    ])
                self.showError(error: error)
            }
        })
        req.resume()
    }
    
    func login() {
        guard let request = URL(string: String(format:"https://%@/%@",Server.hostURL,Server.initialPath)) else { return }
        _ = self.myWebView?.load(URLRequest(url: request))
    }
    
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
        showError(error: NSError(domain: "networkError", code: 0, userInfo:[
            NSLocalizedDescriptionKey :  NSLocalizedString("FailedLoadingPage", value: "Could not load page", comment: "")
            ]))
    }
    
    func webView(_ webView: WKWebView, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == Server.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
            
        }
    }
    
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == Server.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
            
        }
    }
    
    func configureAccess() {
        webCfg = WKWebViewConfiguration()
        let userController:WKUserContentController = WKUserContentController()
        
        userController.add(self, name: DefaultsKey.offlineToken)
        userController.add(self, name: DefaultsKey.refreshToken)
        userController.add(self, name: DefaultsKey.idToken)
        
        var exec_template = "var iOSToken"
        if TokenManager.sharedInstance.hasToken {
            exec_template = String(format: "var iOSToken = \"%@\"; var iOSRefreshToken = \"%@\"; var iOSTokenId = \"%@\";", TokenManager.sharedInstance.offlineToken!, TokenManager.sharedInstance.refreshToken!, TokenManager.sharedInstance.idToken!)
        }
        
        let userScript:WKUserScript = WKUserScript(source: exec_template, injectionTime: .atDocumentStart, forMainFrameOnly: true)
        userController.addUserScript(userScript)
        
        webCfg?.userContentController = userController;
        let sbHeight = UIApplication.shared.statusBarFrame.height
        let webFrame = CGRect(x : 0,y : sbHeight,width : view.frame.size.width,height : view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg!)
        myWebView?.uiDelegate = self;
        myWebView?.navigationDelegate = self;
        view.addSubview(myWebView!)
        
        self.apiCall()
        //self.login()
    }
    
    func loadURL(url : URL) {
        _ = self.myWebView?.load(URLRequest(url:url))
    }
    
}
