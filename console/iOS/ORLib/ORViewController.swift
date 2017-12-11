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

open class ORViewcontroller : UIViewController, URLSessionDelegate, WKScriptMessageHandler, WKUIDelegate, WKNavigationDelegate {
    
    public var data : Data?
    public var myWebView : WKWebView?
    public var defaults : UserDefaults?
    public var webCfg : WKWebViewConfiguration?
    
    open override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white
    }
    
    open override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.configureAccess()
    }
    
    open func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let jsonDictionnary = message.body as? [String : Any]
        let type = jsonDictionnary?["type"] as! String
        switch (type) {
        case "token":
            let tokenJsonDictionnary = jsonDictionnary?["data"] as? [String : String]
            TokenManager.sharedInstance.storeTokens(tokenJsonDictionnary: tokenJsonDictionnary)
            
        default:
            print("Unknown message type: \(type )")
        }
        
    }
    
    open func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        NSLog("error %@", message)
        TokenManager.sharedInstance.resetTokenAndAuthenticate()
        self.dismiss(animated: true, completion: nil)
        completionHandler()
    }
    
    open func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        var exec_template : String? = nil
        switch (prompt) {
        case "token":
            if TokenManager.sharedInstance.offlineToken != nil && TokenManager.sharedInstance.refreshToken != nil && TokenManager.sharedInstance.idToken != nil {
                exec_template = "{ \"token\": \"\(TokenManager.sharedInstance.offlineToken ?? "null")\", \"refreshToken\": \"\(TokenManager.sharedInstance.refreshToken ?? "null")\",\"idToken\": \"\(TokenManager.sharedInstance.idToken ?? "null")\"}"
            }
            
        default:
            print("Unknown message type: \(prompt )")
        }
        
        completionHandler(exec_template)
    }
    
    open func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if (navigationAction.request.url?.absoluteString.contains("logout"))! {
            TokenManager.sharedInstance.logout()
            decisionHandler(.cancel)
            self.dismiss(animated: false, completion: nil)
        } else {
            let app = UIApplication.shared
            if navigationAction.targetFrame == nil, let url = navigationAction.request.url{
                if app.canOpenURL(url) {
                    app.open(url, options: [:], completionHandler: nil)
                    decisionHandler(.cancel)
                }
            } else {
                decisionHandler(.allow)
            }
        }
    }
    
    open func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        if let response = navigationResponse.response as? HTTPURLResponse {
            if response.statusCode != 200 && response.statusCode != 204 {
                decisionHandler(.cancel)
                NSLog("error http status : %@  message : %@", response.statusCode, response.description)
                let error = NSError(domain: "", code: 0, userInfo:  [
                    NSLocalizedDescriptionKey :  NSLocalizedString("HTTPErrorReturned", value: "Connection Error", comment: "")
                    ])
                if let vc = self.presentingViewController as? ORLoginViewController {
                    vc.isInError = true
                }
                self.dismiss(animated: true) {
                    ErrorManager.showError(error:error)
                }
            }
        }
        decisionHandler(.allow)
    }
    
    open func login() {
        guard let request = URL(string: String(format:"\(ORServer.scheme)://%@/%@", ORServer.hostURL, ORServer.initialPath)) else { return }
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        _ = self.myWebView?.load(URLRequest(url: request))
    }
    
    open func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
        UIApplication.shared.isNetworkActivityIndicatorVisible = false
        if let vc = self.presentingViewController as? ORLoginViewController {
            vc.isInError = true
        }
        self.dismiss(animated: true) {
            ErrorManager.showError(error: NSError(domain: "networkError", code: 0, userInfo:[
                NSLocalizedDescriptionKey :  NSLocalizedString("FailedLoadingPage", value: "Could not load page in OR", comment: "")
                ]))
        }
    }
    
    open func webView(_ webView: WKWebView, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == ORServer.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
        }
    }
    
    open func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = false
    }
    
    open func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == ORServer.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
        }
    }
    
    open func configureAccess() {
        let webCfg:WKWebViewConfiguration = WKWebViewConfiguration()
        let userController:WKUserContentController = WKUserContentController()
        
        userController.add(self, name: "int")
        
        let exec_template : String? = ""
        let userScript:WKUserScript = WKUserScript(source: exec_template!, injectionTime: .atDocumentStart, forMainFrameOnly: true)
        userController.addUserScript(userScript)
        
        webCfg.userContentController = userController;
        let sbHeight = UIApplication.shared.statusBarFrame.height
        let webFrame = CGRect(x : 0,y : sbHeight,width : view.frame.size.width,height : view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg)
        myWebView?.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView?.uiDelegate = self;
        myWebView?.navigationDelegate = self;
        view.addSubview(myWebView!)
        
        self.login()
    }
    
    open func loadURL(url : URL) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        _ = self.myWebView?.load(URLRequest(url:url))
    }
    
    open func updateAssetAttribute(assetId : String, attributeName : String, rawJson : String) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        TokenManager.sharedInstance.getAccessToken { (accessTokenResult) in
            switch accessTokenResult {
            case .Failure(let error) :
                UIApplication.shared.isNetworkActivityIndicatorVisible = false
                ErrorManager.showError(error: error!)
            case .Success(let accessToken) :
                guard let urlRequest = URL(string: String(String(format: "\(ORServer.scheme)://%@/%@/asset/%@/attribute/%@", ORServer.hostURL, ORServer.realm, assetId, attributeName))) else { return }
                let request = NSMutableURLRequest(url: urlRequest)
                request.httpMethod = "PUT"
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = rawJson.data(using: .utf8)
                request.addValue(String(format:"Bearer %@", accessToken!), forHTTPHeaderField: "Authorization")
                let sessionConfiguration = URLSessionConfiguration.default
                let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue : nil)
                let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                    DispatchQueue.main.async {
                        UIApplication.shared.isNetworkActivityIndicatorVisible = false
                        if (error != nil) {
                            NSLog("error %@", (error! as NSError).localizedDescription)
                            let error = NSError(domain: "", code: 0, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            ErrorManager.showError(error: error)
                        }
                    }
                })
                reqDataTask.resume()
            }
        }
    }
}
