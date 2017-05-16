//
//  TokenManager.swift
//  console
//
//  Created by William Balcaen on 09/03/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import Foundation
import UIKit
import WebKit

class TokenManager:NSObject, WKScriptMessageHandler, WKUIDelegate, WKNavigationDelegate, URLSessionDelegate {
    
    var offlineToken : String?
    var refreshToken : String?
    var idToken : String?
    var deviceId : String?
    var hasToken: Bool
    var viewController : TokenManagerViewController
    var myWebView : WKWebView
    var didLogOut : Bool = false

    
    static let sharedInstance = TokenManager()
    
    override init() {
        viewController = TokenManagerViewController()
        let webCfg:WKWebViewConfiguration = WKWebViewConfiguration()
        myWebView = WKWebView(frame: viewController.view.frame, configuration: webCfg)
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.synchronize()
        if let token = defaults?.value(forKey: DefaultsKey.offlineToken) {
            hasToken = true
            offlineToken = token as? String
            refreshToken = defaults?.value(forKey: DefaultsKey.refreshToken) as? String
            idToken = defaults?.value(forKey: DefaultsKey.idToken) as? String
        } else {
            offlineToken = nil
            refreshToken = nil
            idToken = nil
            deviceId = nil
            hasToken = false
        }
        deviceId = defaults?.value(forKey: DefaultsKey.deviceId) as? String
        super.init()
    }
    
    func authenticate() {
        NSLog("authenticate")
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        let webCfg:WKWebViewConfiguration = WKWebViewConfiguration()
        if (!didLogOut) {
            let userController:WKUserContentController = WKUserContentController()
            
            userController.add(self, name: DefaultsKey.offlineToken)
            userController.add(self, name: DefaultsKey.refreshToken)
            userController.add(self, name: DefaultsKey.idToken)
            
            var exec_template = "var iOSToken"
            if let offlineToken = defaults?.object(forKey: DefaultsKey.offlineToken) {
                NSLog("offlinetoken exists")
                let refreshToken = defaults?.object(forKey: DefaultsKey.refreshToken)
                let idToken = defaults?.object(forKey: DefaultsKey.idToken)
                exec_template = String(format: "var iOSToken = \"%@\"; var iOSRefreshToken = \"%@\"; var iOSTokenId = \"%@\";", offlineToken as! String, refreshToken as! String, idToken as! String)
            }
            
            let userScript:WKUserScript = WKUserScript(source: exec_template, injectionTime: .atDocumentStart, forMainFrameOnly: false)
            userController.addUserScript(userScript)
            
            
            webCfg.userContentController = userController;
        } else {
            let userController:WKUserContentController = WKUserContentController()
            
            userController.add(self, name: DefaultsKey.offlineToken)
            userController.add(self, name: DefaultsKey.refreshToken)
            userController.add(self, name: DefaultsKey.idToken)
            
            var exec_template = "var iOSToken"
            exec_template = String(format: "var iOSToken = \"%@\"; var iOSRefreshToken = \"%@\"; var iOSTokenId = \"%@\";", "", "", "")
            
            let userScript:WKUserScript = WKUserScript(source: exec_template, injectionTime: .atDocumentStart, forMainFrameOnly: false)
            userController.addUserScript(userScript)
            
            
            webCfg.userContentController = userController;
        }
        
        myWebView = WKWebView(frame: viewController.view.frame, configuration: webCfg)
        myWebView.uiDelegate = self;
        myWebView.navigationDelegate = self;
        viewController.view.addSubview(myWebView)
        UIApplication.shared.keyWindow?.rootViewController?.present(viewController, animated: true, completion: nil)
        guard let request = URL(string: String(format:"https://%@/%@",Server.hostURL,Server.initialPath)) else { return }
        myWebView.load(URLRequest(url: request))
    }
    
    func resetToken() {
        hasToken = false
        offlineToken = nil
        refreshToken = nil
        idToken = nil
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.removeObject(forKey: DefaultsKey.offlineToken)
        defaults?.removeObject(forKey: DefaultsKey.refreshToken)
        defaults?.removeObject(forKey: DefaultsKey.idToken)
        authenticate()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.set(message.body, forKey: message.name)
        switch message.name {
        case DefaultsKey.offlineToken:
            offlineToken = message.body as? String
        case DefaultsKey.refreshToken:
            refreshToken = message.body as? String
        case DefaultsKey.idToken:
            idToken = message.body as? String
        default:
            NSLog("unknown message received from javascript : %@", message.name)
        }
        if (offlineToken != nil && refreshToken != nil && idToken != nil && !didLogOut) {
            self.hasToken = true
            UIApplication.shared.isNetworkActivityIndicatorVisible = false
            self.viewController.dismiss(animated: true, completion: {
                let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
                NotificationCenter.default.post(name: notificationName, object: nil)
            })
        }
    }
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alertView = UIAlertController(title: "debug", message: message, preferredStyle: .alert)
        let alertAction = UIAlertAction(title: "Done", style: .cancel) { (action) in
        }
        NSLog("javascript message %@",message)
        alertView.addAction(alertAction)
        viewController.present(alertView, animated: true, completion: nil)
        completionHandler()
    }
    
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
        UIApplication.shared.isNetworkActivityIndicatorVisible = false
        if let vc = viewController.presentingViewController as? ViewController {
            vc.isInError = true
        }
        self.viewController.dismiss(animated: true) { 
            ErrorManager.showError(error: NSError(domain: "networkError", code: 0, userInfo:[
                NSLocalizedDescriptionKey :  NSLocalizedString("FailedLoadingPage", value: "Could not load page", comment: "")
                ]))
        }
        
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
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        if let response = navigationResponse.response as? HTTPURLResponse {
            if response.statusCode != 200 && response.statusCode != 204 {
                decisionHandler(.cancel)
                NSLog("error http status : %i ",response.statusCode)
                let error = NSError(domain: "", code: 0, userInfo:  [
                    NSLocalizedDescriptionKey :  NSLocalizedString("HTTPErrorReturned", value: "Connection Error", comment: "")
                    ])
                if let vc = viewController.presentingViewController as? ViewController {
                    vc.isInError = true
                }
                self.viewController.dismiss(animated: true) {
                    ErrorManager.showError(error: error)
                }
            }
        }
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        let errorCode = (error as NSError).code
        UIApplication.shared.isNetworkActivityIndicatorVisible = false
        if errorCode != NSURLErrorCancelled {
            NSLog("error %@", error.localizedDescription)
            ErrorManager.showError(error: NSError(domain: "navigationError", code: 0, userInfo:[
                NSLocalizedDescriptionKey :  NSLocalizedString("navigationError", value: "Could not navigate to page", comment: "")
                ]))
        }
    }
    
    func storeDeviceId(token : String) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        if deviceId == nil  || (deviceId != token){
            deviceId = token
            defaults?.set(token, forKey: DefaultsKey.deviceId)
        } else {
            deviceId = defaults?.object(forKey: DefaultsKey.deviceId) as! String?
        }
    }
    
    func sendDeviceId() {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        self.getAccessToken { (accessTokenResult) in
            switch accessTokenResult {
            case .Failure(let error) :
                    UIApplication.shared.isNetworkActivityIndicatorVisible = false
                    ErrorManager.showError(error: error!)
            case .Success(let accessToken) :
            guard let urlRequest = URL(string: String(Server.registerDeviceResource)) else { return }
            let request = NSMutableURLRequest(url: urlRequest)
            request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
            request.httpMethod = "PUT"
            let postString = String(format:"token=%@&device_id=%@", TokenManager.sharedInstance.deviceId!, (UIDevice.current.identifierForVendor?.uuidString)!)
            request.httpBody = postString.data(using: .utf8)
            request.addValue(String(format:"Bearer %@", accessToken!), forHTTPHeaderField: "Authorization")
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                DispatchQueue.main.async {
                    UIApplication.shared.isNetworkActivityIndicatorVisible = false
                    if (error != nil) {
                        NSLog("error %@", (error! as NSError).localizedDescription)
                        let error = NSError(domain: "", code: 0, userInfo:  [
                            NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                            ])
                        ErrorManager.showError(error: error)
                    } else {
                        if let httpStatus = response as? HTTPURLResponse , httpStatus.statusCode != 204 {
                            let error = NSError(domain: "", code: 0, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorSendingDeviceId", value: "Could not register your device for notifications", comment: "")
                                ])
                            ErrorManager.showError(error: error)
                        } else {
                            NSLog("Device registred with token %@",postString)
                            let notificationName = Notification.Name(NotificationsNames.isdeviceIdSent)
                            NotificationCenter.default.post(name: notificationName, object: nil)
                        }
                    }
                }
            })
            reqDataTask.resume()
        }
        }
    }
    
    func getAccessToken(callback: @escaping (AccesTokenResult<String>) -> ()) {
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
                        callback(AccesTokenResult.Success(jsonDictionnary["access_token"] as? String))
                    } else {
                        if let httpResponse = response as? HTTPURLResponse {
                            NSLog("error %@", httpResponse)
                            let error = NSError(domain: "", code: httpResponse.statusCode, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            callback(AccesTokenResult.Failure(error))
                        } else {
                            if (error != nil) {
                                NSLog("error %@", (error! as NSError).localizedDescription as String)
                            }
                            let error = NSError(domain: "", code: 0, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            callback(AccesTokenResult.Failure(error))
                        }
                    }
                }
                catch let error as NSError {
                    NSLog("error %@", error.localizedDescription)
                    let error = NSError(domain: "", code: 0, userInfo:  [
                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorDeserializingJSON", value: "Could not convert received data", comment: "")
                        ])
                    callback(AccesTokenResult.Failure(error))
                }
            } else {
                NSLog("error %@", (error! as NSError).localizedDescription)
                let error = NSError(domain: "", code: 0, userInfo:  [
                    NSLocalizedDescriptionKey :  NSLocalizedString("NoDataReceived", value: "Did not receive any data", comment: "")
                    ])
                callback(AccesTokenResult.Failure(error))
            }
        })
        req.resume()
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
}
