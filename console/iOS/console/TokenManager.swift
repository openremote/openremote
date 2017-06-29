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
        myWebView.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.synchronize()
        if defaults?.value(forKey: DefaultsKey.token) != nil &&
            defaults?.value(forKey: DefaultsKey.refreshToken) != nil &&
            defaults?.value(forKey: DefaultsKey.idToken) != nil {
            hasToken = true
            offlineToken = defaults?.value(forKey: DefaultsKey.token) as? String
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
        let userController:WKUserContentController = WKUserContentController()
            
        userController.add(self, name: "int")
        
        let exec_template : String? = ""
            
        let userScript:WKUserScript = WKUserScript(source: exec_template!, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        userController.addUserScript(userScript)
            
        webCfg.userContentController = userController;
        
        myWebView = WKWebView(frame: viewController.view.frame, configuration: webCfg)
        myWebView.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView.uiDelegate = self;
        myWebView.navigationDelegate = self;
        viewController.view.addSubview(myWebView)
        if (UIApplication.shared.keyWindow?.rootViewController?.presentedViewController == nil) {
            UIApplication.shared.keyWindow?.rootViewController?.present(viewController, animated: true, completion: nil)
        }
        
        guard let request = URL(string: String(format:"%@://%@/%@",Server.scheme, Server.hostURL,Server.initialPath)) else { return }
        myWebView.load(URLRequest(url: request))
    }
    
    func resetToken() {
        hasToken = false
        offlineToken = nil
        refreshToken = nil
        idToken = nil
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.removeObject(forKey: DefaultsKey.token)
        defaults?.removeObject(forKey: DefaultsKey.refreshToken)
        defaults?.removeObject(forKey: DefaultsKey.idToken)
        authenticate()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
            let jsonDictionnary = message.body as? [String : Any]
            let type = jsonDictionnary?["type"] as! String
            switch (type){
            case "token":
                let tokenJsonDictionnary = jsonDictionnary?["value"] as? [String : String]
                if (tokenJsonDictionnary?["token"] != nil &&
                    tokenJsonDictionnary?["refreshToken"] != nil &&
                    tokenJsonDictionnary?["idToken"] != nil){
                        offlineToken = tokenJsonDictionnary?["token"]! ?? nil
                        refreshToken = tokenJsonDictionnary?["refreshToken"]! ?? nil
                        idToken = tokenJsonDictionnary?["idToken"]! ?? nil
                        defaults?.set(offlineToken, forKey: DefaultsKey.token)
                        defaults?.set(refreshToken, forKey: DefaultsKey.refreshToken)
                        defaults?.set(idToken, forKey: DefaultsKey.idToken)
                }
                if (offlineToken != nil && refreshToken != nil && idToken != nil && !didLogOut) {
                    self.hasToken = true
                    if let vc = viewController.presentingViewController as? ViewController {
                        vc.isInError = false
                    }
                    UIApplication.shared.isNetworkActivityIndicatorVisible = false
                    self.viewController.dismiss(animated: false, completion: {
                        let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
                        NotificationCenter.default.post(name: notificationName, object: nil)
                    })
                    let exec_template = "readValue = { token: \(offlineToken ?? "null"), refreshToken: \(refreshToken ?? "null"),idToken: \(idToken ?? "null")};"
                    myWebView.evaluateJavaScript(exec_template, completionHandler: nil)
                }
               /* do {
                    let json = try JSONSerialization.jsonObject(with: (((jsonDictionnary?["value"]!) as! String).data(using: String.Encoding.utf8)!), options: .allowFragments)
                    let jsonDictionnary = json as? [String : String]
                    offlineToken = jsonDictionnary?["token"]! ?? nil
                    refreshToken = jsonDictionnary?["refreshToken"]! ?? nil
                    idToken = jsonDictionnary?["idToken"]! ?? nil
                    defaults?.set(offlineToken, forKey: DefaultsKey.token)
                    defaults?.set(refreshToken, forKey: DefaultsKey.refreshToken)
                    defaults?.set(idToken, forKey: DefaultsKey.idToken)
                    if (offlineToken != nil && refreshToken != nil && idToken != nil && !didLogOut) {
                        self.hasToken = true
                        if let vc = viewController.presentingViewController as? ViewController {
                            vc.isInError = false
                        }
                        UIApplication.shared.isNetworkActivityIndicatorVisible = false
                        self.viewController.dismiss(animated: false, completion: {
                            let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
                            NotificationCenter.default.post(name: notificationName, object: nil)
                        })
                        let exec_template = "readValue = { token: \(offlineToken ?? "null"), refreshToken: \(refreshToken ?? "null"),idToken: \(idToken ?? "null")};"
                        myWebView.evaluateJavaScript(exec_template, completionHandler: nil)
                    }
                } catch {
                    print("Error deserializing JSON: \(error)")
                }*/
            default:
                print("Unknown message type: \(type ?? "")")
            }
        
    }
    
    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        var exec_template : String? = nil
        switch (prompt){
        case "token":
            if offlineToken != nil && refreshToken != nil && idToken != nil && didLogOut == false {
                exec_template = "readValue = { token: \(offlineToken ?? "null"), refreshToken: \(refreshToken ?? "null"),idToken: \(idToken ?? "null")};"
            }

        default:
            print("Unknown message type: \(prompt )")
        }

        completionHandler(exec_template)
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
            TokenManager.sharedInstance.sendDeviceId()
        } else {
            deviceId = defaults?.object(forKey: DefaultsKey.deviceId) as! String?
        }
    }
    
    func sendDeviceId() {
        if (TokenManager.sharedInstance.deviceId != nil && TokenManager.sharedInstance.hasToken) {
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
                                    NSLog("Device not registred ",response.debugDescription)
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
    }
    
    func getAccessToken(callback: @escaping (AccesTokenResult<String>) -> ()) {
        guard let tkurlRequest = URL(string: String(format:"\(Server.scheme)://%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL,Server.realm)) else { return }
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
