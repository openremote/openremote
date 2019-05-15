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
    public var isLoading = false

    var geofenceProvider: GeofenceProvider?
    var pushProvider: PushNotificationProvider?
    var storageProvider: StorageProvider?
    
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
        if let type = jsonDictionnary?["type"] as? String {
            switch (type) {
            case "provider":
                if let postMessageDict = jsonDictionnary?[DefaultsKey.dataKey] as? [String: Any] {
                    if let action = postMessageDict[DefaultsKey.actionKey] as? String {
                        if let provider = postMessageDict[DefaultsKey.providerKey] as? String {
                            if provider == Providers.push {
                                if action == Actions.providerInit {
                                    pushProvider = PushNotificationProvider()
                                    pushProvider!.initialize(callback: { initalizeData in
                                        self.sendData(data: initalizeData)
                                    })
                                } else if action == Actions.providerEnable {
                                    if let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String {
                                        pushProvider?.enable(consoleId: consoleId, callback: { enableData in
                                            self.sendData(data: enableData)
                                        })
                                    }
                                } else if action == Actions.providerDisable {
                                    if let disableData = pushProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                }
                            } else if provider == Providers.geofence {
                                if action == Actions.providerInit {
                                    geofenceProvider = GeofenceProvider()
                                    let initializeData = geofenceProvider!.initialize()
                                    sendData(data: initializeData)
                                } else if action == Actions.providerEnable {
                                    if let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String {
                                        geofenceProvider?.enable(baseUrl: "\(ORServer.baseUrl)api/\(ORServer.realm)", consoleId: consoleId,  callback: { enableData in
                                            self.sendData(data: enableData)
                                            DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(5)) {
                                                self.geofenceProvider?.fetchGeofences()
                                            }
                                        })
                                    }
                                } else if action == Actions.geofenceRefresh {
                                    geofenceProvider?.refreshGeofences()
                                } else if action == Actions.providerDisable {
                                    if let disableData = geofenceProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                }
                            } else if provider == Providers.storage {
                                if action == Actions.providerInit {
                                    storageProvider = StorageProvider()
                                    let initializeData = storageProvider!.initialize()
                                    sendData(data: initializeData)
                                } else if action == Actions.providerEnable {
                                    if let enableData = storageProvider?.enable() {
                                        sendData(data: enableData)
                                    }
                                } else if action == Actions.store {
                                    if let key = postMessageDict["key"] as? String {
                                        storageProvider?.store(key: key, data: postMessageDict["value"] as? String)
                                    }
                                } else if action == Actions.retrieve {
                                    if let key = postMessageDict["key"] as? String {
                                        if let retrieveData = storageProvider?.retrieve(key: key) {
                                            sendData(data: retrieveData)
                                        }
                                    }
                                } else if action == Actions.providerDisable {
                                    if let disableData = storageProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                }
                            }
                        }
                    }
                }
            default:
                print("Unknown message type: \(type )")
            }
        }
    }

    open func sendData(data: [String: Any?]) {
        if let theJSONData = try? JSONSerialization.data(
            withJSONObject: data,
            options: []) {
            let theJSONText = String(data: theJSONData,
                                     encoding: .utf8)
            let returnMessage = "OpenRemoteConsole._handleProviderResponse('\(theJSONText ?? "null")')"
            DispatchQueue.main.async {
                self.myWebView?.evaluateJavaScript("\(returnMessage)", completionHandler: { (any, error) in
                    print("JSON string = \(theJSONText!)")
                })
            }
        }
    }
    
    open func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        NSLog("error %@", message)
        TokenManager.sharedInstance.resetTokenAndAuthenticate()
        self.dismiss(animated: true, completion: nil)
        completionHandler()
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
                    app.open(url, options: convertToUIApplicationOpenExternalURLOptionsKeyDictionary([:]), completionHandler: nil)
                    decisionHandler(.cancel)
                }
            } else {
                decisionHandler(.allow)
            }
        }
        isLoading = false
    }
    
    open func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        if let response = navigationResponse.response as? HTTPURLResponse {
            if response.statusCode != 200 && response.statusCode != 204 {
                decisionHandler(.cancel)

                if response.url?.lastPathComponent == "token" && response.statusCode == 400 {
                    TokenManager.sharedInstance.resetToken()
                }

                isLoading = false
                handleError(errorCode: response.statusCode, description: "Error in request", failingUrl: response.url?.absoluteString ?? "", isForMainFrame: true)
                return
            }
        }
        isLoading = false
        decisionHandler(.allow)
    }
    
    open func login() {
        guard let request = URL(string: String(format:"\(ORServer.scheme)://%@/%@", ORServer.hostURL, ORServer.initialPath)) else { return }
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        _ = self.myWebView?.load(URLRequest(url: request))
        isLoading = true
    }
    
    open func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
        UIApplication.shared.isNetworkActivityIndicatorVisible = false
        if let err = error as? URLError {

            let httpCode: Int
            switch(err.code) {
            case .cannotFindHost:
                httpCode = 404
            default:
                httpCode = 500
            }

            handleError(errorCode: httpCode, description: err.localizedDescription, failingUrl: err.failureURLString ?? "", isForMainFrame: true)
        } else {
            handleError(errorCode: 0, description: error.localizedDescription, failingUrl: webView.url?.absoluteString ?? "", isForMainFrame: true)
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
        let sbHeight: CGFloat
        if #available(iOS 11.0, *) {
            sbHeight = UIApplication.shared.keyWindow?.safeAreaInsets.top ?? UIApplication.shared.statusBarFrame.height
        } else {
            sbHeight = UIApplication.shared.statusBarFrame.height
        }
        let webFrame = CGRect(x : 0,y : sbHeight,width : view.frame.size.width,height : view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg)
        myWebView?.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView?.uiDelegate = self;
        myWebView?.navigationDelegate = self;
        view.addSubview(myWebView!)
    }
    
    open func loadURL(url : URL) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        _ = self.myWebView?.load(URLRequest(url:url))
        isLoading = true
    }

    internal func handleError(errorCode: Int, description: String, failingUrl: String, isForMainFrame: Bool) {
        NSLog("%@", "Error requesting '\(failingUrl)': \(errorCode) (\(description))")

        // This will be the URL loaded into the webview itself (false for images etc. of the main page)
        if isForMainFrame {

            // Check page load error URL
            let errorUrl = ORServer.errorUrl
            if let errorURL = URL(string: errorUrl), ORServer.errorUrl != failingUrl {
                NSLog("%@", "Loading error URL: \(errorUrl)")
                loadURL(url: errorURL)
            }
        } else {

            if ORServer.ignorePageErrors {
                return;
            }

            let error = NSError(domain: "", code: 0, userInfo:  [
                NSLocalizedDescriptionKey :  NSLocalizedString("HTTPErrorReturned", value: "Connection Error", comment: "Error requesting '\(failingUrl)': \(errorCode) (\(description))")
                ])
            if let vc = self.presentingViewController as? ORLoginViewController {
                vc.isInError = true
            }
            self.dismiss(animated: true) {
                ErrorManager.showError(error:error)
            }
        }
    }
    
    open func updateAssetAttribute(assetId : String, attributeName : String, rawJson : String) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        TokenManager.sharedInstance.getAccessToken { (accessTokenResult) in
            switch accessTokenResult {
            case .Failure(let error) :
                UIApplication.shared.isNetworkActivityIndicatorVisible = false
                self.handleError(errorCode: 0, description: error!.localizedDescription, failingUrl: "", isForMainFrame: false)
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
                            self.handleError(errorCode: 0, description: error!.localizedDescription, failingUrl: "", isForMainFrame: false)
                        }
                    }
                })
                reqDataTask.resume()
            }
        }
    }
}

// Helper function inserted by Swift 4.2 migrator.
fileprivate func convertToUIApplicationOpenExternalURLOptionsKeyDictionary(_ input: [String: Any]) -> [UIApplication.OpenExternalURLOptionsKey: Any] {
	return Dictionary(uniqueKeysWithValues: input.map { key, value in (UIApplication.OpenExternalURLOptionsKey(rawValue: key), value)})
}
