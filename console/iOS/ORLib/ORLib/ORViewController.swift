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

open class ORViewcontroller : UIViewController {
    
    public var data : Data?
    public var myWebView : WKWebView?
    public var webProgressBar: UIProgressView?
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

    open func login() {
        guard let request = URL(string: String(format:"\(ORServer.scheme)://%@/%@", ORServer.hostURL, ORServer.initialPath)) else { return }
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        _ = self.myWebView?.load(URLRequest(url: request))
        isLoading = true
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
        let webFrame = CGRect(x: 0, y: sbHeight, width: view.frame.size.width, height: view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg)
        myWebView?.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView?.navigationDelegate = self;
        //add observer to get estimated progress value
        myWebView?.addObserver(self, forKeyPath: "estimatedProgress", options: .new, context: nil);

        webProgressBar = UIProgressView(progressViewStyle: .bar)
        webProgressBar?.progressTintColor = UIColor.mainColorAccent

        view.addSubview(myWebView!)
        view.addSubview(webProgressBar!)
        view.bringSubviewToFront(webProgressBar!)

        webProgressBar?.translatesAutoresizingMaskIntoConstraints = false
        webProgressBar?.leadingAnchor.constraint(equalTo: myWebView!.leadingAnchor).isActive = true
        webProgressBar?.trailingAnchor.constraint(equalTo: myWebView!.trailingAnchor).isActive = true
        webProgressBar?.topAnchor.constraint(equalTo: myWebView!.topAnchor, constant: -2).isActive = true
        webProgressBar?.heightAnchor.constraint(equalToConstant: 2).isActive = true
    }

    override open func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "estimatedProgress" {
            if let webView = myWebView {
                webProgressBar?.progress = Float(webView.estimatedProgress);
            }
        }
    }

    func showProgressView() {
        if let progressBar = webProgressBar {
            UIView.animate(withDuration: 0.5, delay: 0, options: .curveEaseInOut, animations: {
                progressBar.alpha = 1
            }, completion: nil)
        }
    }

    func hideProgressView() {
        if let progressBar = webProgressBar {
            UIView.animate(withDuration: 0.5, delay: 0, options: .curveEaseInOut, animations: {
                progressBar.alpha = 0
            }, completion: nil)
        }
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

extension ORViewcontroller: URLSessionDelegate {
    open func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == ORServer.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
        }
    }
}

extension ORViewcontroller: WKScriptMessageHandler {
    open func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let jsonDictionnary = message.body as? [String : Any]
        if let type = jsonDictionnary?["type"] as? String {
            switch (type) {
            case "provider":
                if let postMessageDict = jsonDictionnary?[DefaultsKey.dataKey] as? [String: Any] {
                    if let action = postMessageDict[DefaultsKey.actionKey] as? String {
                        if let provider = postMessageDict[DefaultsKey.providerKey] as? String  {
                            switch (provider) {
                            case Providers.push:
                                switch(action) {
                                case Actions.providerInit:
                                    pushProvider = PushNotificationProvider()
                                    pushProvider!.initialize(callback: { initalizeData in
                                        self.sendData(data: initalizeData)
                                    })
                                case Actions.providerEnable:
                                    if let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String {
                                        pushProvider?.enable(consoleId: consoleId, callback: { enableData in
                                            self.sendData(data: enableData)
                                        })
                                    }
                                case Actions.providerDisable:
                                    if let disableData = pushProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                default:
                                    print("Wrong action \(action) for \(provider)")
                                }
                            case Providers.geofence:
                                switch(action) {
                                case Actions.providerInit:
                                    geofenceProvider = GeofenceProvider()
                                    let initializeData = geofenceProvider!.initialize()
                                    sendData(data: initializeData)
                                case Actions.providerEnable:
                                    if let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String {
                                        geofenceProvider?.enable(baseUrl: "\(ORServer.baseUrl)api/\(ORServer.realm)", consoleId: consoleId,  callback: { enableData in
                                            self.sendData(data: enableData)
                                            DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(5)) {
                                                self.geofenceProvider?.fetchGeofences()
                                            }
                                        })
                                    }
                                case Actions.providerDisable:
                                    if let disableData = geofenceProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                case Actions.geofenceRefresh:
                                    geofenceProvider?.refreshGeofences()
                                case Actions.getLocation:
                                    geofenceProvider?.getLocation(callback: { locationData in
                                        if (locationData["data"] as? [String:Any]) == nil {
                                            let alertController = UIAlertController(title: NSLocalizedString(LocalizableString.LocationPermissionTitle, comment: ""),
                                                                                    message: NSLocalizedString(LocalizableString.LocationPermissionMessage, comment: ""),
                                                                                    preferredStyle: .alert)
                                            if let settingsUrl = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(settingsUrl) {
                                                alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: { alertAction in
                                                    if UIApplication.shared.canOpenURL(settingsUrl) {
                                                        UIApplication.shared.open(settingsUrl, completionHandler: { (success) in
                                                            print("Settings opened: \(success)") // Prints true
                                                        })
                                                    }
                                                }))
                                                alertController.addAction(UIAlertAction(title: NSLocalizedString(LocalizableString.LocationPermissionNotNow, comment: ""), style: .cancel, handler: nil))
                                            } else {
                                                alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                                            }
                                            self.present(alertController, animated: true, completion: nil)
                                        }
                                        self.sendData(data: locationData)
                                    })
                                default:
                                    print("Wrong action \(action) for \(provider)")
                                }
                            case Providers.storage:
                                switch(action) {
                                case Actions.providerInit:
                                    storageProvider = StorageProvider()
                                    let initializeData = storageProvider!.initialize()
                                    sendData(data: initializeData)
                                case Actions.providerEnable:
                                    if let enableData = storageProvider?.enable() {
                                        sendData(data: enableData)
                                    }
                                case Actions.providerDisable:
                                    if let disableData = storageProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                case Actions.store:
                                    if let key = postMessageDict["key"] as? String {
                                        storageProvider?.store(key: key, data: postMessageDict["value"] as? String)
                                    }
                                case Actions.retrieve:
                                    if let key = postMessageDict["key"] as? String {
                                        if let retrieveData = storageProvider?.retrieve(key: key) {
                                            sendData(data: retrieveData)
                                        }
                                    }
                                default:
                                    print("Wrong action \(action) for \(provider)")
                                }
                            default:
                                print("Unknown provider type: \(provider )")
                            }
                        }
                    }
                }
            default:
                print("Unknown message type: \(type )")
            }
        }
    }
}

extension ORViewcontroller: WKNavigationDelegate {
    open func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        NSLog("error %@", message)
        TokenManager.sharedInstance.resetTokenAndAuthenticate()
        self.dismiss(animated: true, completion: nil)
        completionHandler()
    }

    public func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        showProgressView()
    }

    open func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if (navigationAction.request.url?.absoluteString.contains("logout"))! {
            TokenManager.sharedInstance.logout()
            decisionHandler(.cancel)
            self.dismiss(animated: false, completion: nil)
        } else if (navigationAction.request.url?.absoluteString.starts(with: "webbrowser"))! {
            if let url = navigationAction.request.url, var components = URLComponents(url: url, resolvingAgainstBaseURL: false) {
                components.scheme = "https"
                if let newUrl = components.url {
                    UIApplication.shared.open(newUrl)
                }
            }
            decisionHandler(.cancel)
        }  else {
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
        hideProgressView()
    }

    public func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
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
        hideProgressView()
    }
}

// Helper function inserted by Swift 4.2 migrator.
fileprivate func convertToUIApplicationOpenExternalURLOptionsKeyDictionary(_ input: [String: Any]) -> [UIApplication.OpenExternalURLOptionsKey: Any] {
    return Dictionary(uniqueKeysWithValues: input.map { key, value in (UIApplication.OpenExternalURLOptionsKey(rawValue: key), value)})
}
