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
import MaterialComponents.MaterialButtons

open class ORViewcontroller : UIViewController {
    
    var data : Data?
    var myWebView : WKWebView?
    var webProgressBar: UIProgressView?
    var defaults : UserDefaults?
    var webCfg : WKWebViewConfiguration?
    public var geofenceProvider: GeofenceProvider?
    public var pushProvider: PushNotificationProvider?
    public var storageProvider: StorageProvider?
    public var qrProvider: QrScannerProvider?
    public var bleProvider: BleProvider?
    
    public var baseUrl: String?
    
    public var targetUrl: String?
    
    open override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white
        self.configureAccess()
    }
    
    open override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if let targetUrl = targetUrl {
            if let urlString = targetUrl.stringByURLEncoding() {
                if let url = URL(string: urlString) {
                    loadURL(url: url)
                }
            }
        }
    }
    
    func sendData(data: [String: Any?]) {
        if let theJSONData = try? JSONSerialization.data(
            withJSONObject: data,
            options: []) {
            if let theJSONText = String(data: theJSONData,
                                        encoding: .utf8) {
                print("notifyClient with message: \(theJSONText)")
                let returnMessage = "OpenRemoteConsole._handleProviderResponse('\(theJSONText)')"
                DispatchQueue.main.async {
                    self.myWebView?.evaluateJavaScript("\(returnMessage)", completionHandler: { (any, error) in
                        if let err = error {
                            print(err)
                        }
                    })
                }
            }
        }
    }
    
    func configureAccess() {
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
        
        webCfg.allowsInlineMediaPlayback = true
        let webFrame = CGRect(x: 0, y: sbHeight, width: view.frame.size.width, height: view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg)
        myWebView?.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView?.navigationDelegate = self;
        //add observer to get estimated progress value
        myWebView?.addObserver(self, forKeyPath: "estimatedProgress", options: .new, context: nil);
        myWebView?.allowsBackForwardNavigationGestures = true
        
        webProgressBar = UIProgressView(progressViewStyle: .bar)
        webProgressBar?.progressTintColor = UIColor(named: "or_green")
        
        if #available(macOS 13.3, iOS 16.4, tvOS 16.4, *) {
            myWebView?.isInspectable = true
        }
        
        view.addSubview(myWebView!)
        view.addSubview(webProgressBar!)
        view.bringSubviewToFront(webProgressBar!)
        
        webProgressBar?.translatesAutoresizingMaskIntoConstraints = false
        webProgressBar?.leadingAnchor.constraint(equalTo: myWebView!.leadingAnchor).isActive = true
        webProgressBar?.trailingAnchor.constraint(equalTo: myWebView!.trailingAnchor).isActive = true
        webProgressBar?.topAnchor.constraint(equalTo: myWebView!.topAnchor, constant: -2).isActive = true
        webProgressBar?.heightAnchor.constraint(equalToConstant: 2).isActive = true
    }
    
    public override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
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
    
    public func loadURL(url : URL) {
        _ = self.myWebView?.load(URLRequest(url:url))
    }
    
    internal func handleError(errorCode: Int, description: String, failingUrl: String, isForMainFrame: Bool) {
        print("Error requesting '\(failingUrl)': \(errorCode) (\(description))")
        
        let alertView = UIAlertController(title: "Error", message: "Error requesting '\(failingUrl)': \(errorCode) (\(description))", preferredStyle: .alert)
        
        if self.presentingViewController != nil {
            alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in self.dismiss(animated: true)} ))
        } else {
            alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        }
        self.present(alertView, animated: true, completion: nil)
        
        /*
         if (false) {
         //TODO need to have case to return to home url of config or go back to wizard to setup project enviroment
         //            self.myWebView?.load(URLRequest(url: URL(string: url.stringByURLEncoding()!)!))
         } else {
         if self.presentingViewController != nil {
         self.dismiss(animated: true) {
         
         // TODO: this original code is causing error
         // self.presentingViewController!.present(alertView, animated: true, completion: nil)
         }
         } else {
         self.present(alertView, animated: true, completion: nil)
         }
         }
         */
    }
}

extension ORViewcontroller: WKScriptMessageHandler {
    public func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        print("Received WebApp message \(message.body)")
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
                                    let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String
                                    pushProvider?.enable(consoleId: consoleId, callback: { enableData in
                                        self.sendData(data: enableData)
                                    })
                                    
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
                                    let consoleId = postMessageDict[GeofenceProvider.consoleIdKey] as? String
                                    if let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement){
                                        let host = userdefaults.string(forKey: DefaultsKey.hostKey) ?? ""
                                        let realm = userdefaults.string(forKey: DefaultsKey.realmKey) ?? ""
                                        let baseUrl = host.isEmpty ? "" : host.appending("/api/\(realm)")
                                        geofenceProvider?.enable(baseUrl: baseUrl, consoleId: consoleId,  callback: { enableData in
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
                            case Providers.qr:
                                switch (action) {
                                case Actions.providerInit:
                                    qrProvider = QrScannerProvider()
                                    qrProvider!.initialize(callback: { initializeData in
                                        self.sendData(data: initializeData)
                                    })
                                case Actions.providerEnable:
                                    qrProvider?.enable(callback: { enableData in
                                        self.sendData(data: enableData)
                                    })
                                case Actions.providerDisable:
                                    if let disableData = qrProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                case Actions.scanQr:
                                    qrProvider?.startScanner(currentViewController: self, startScanCallback: { startScanData in
                                        self.sendData(data: startScanData)
                                    }, scannedCallback:
                                    { scannedData in
                                        self.sendData(data: scannedData)
                                    })
                                default:
                                    print("Wrong action \(action) for \(provider)")
                                }
                            case Providers.ble:
                                switch (action) {
                                case Actions.providerInit:
                                    bleProvider = BleProvider()
                                    bleProvider!.alertBluetoothCallback = {
                                        let alertController = UIAlertController(title: "Bluetooth disabled", message: "Please turn on bluetooth to scan for devices", preferredStyle: .alert)
                                        alertController.addAction(UIAlertAction(title: "OK", style: .default) { alertAction in
                                            let url = URL(string: UIApplication.openSettingsURLString)
                                            let app = UIApplication.shared
                                            app.open(url!, options: [:])
                                        })
                                        alertController.addAction(UIAlertAction(title: "Not now", style: .cancel))
                                        self.present(alertController, animated: true)
                                    }
                                    self.sendData(data: bleProvider!.initialize())
                                case Actions.providerEnable:
                                    bleProvider?.enable(callback: { enableData in
                                        self.sendData(data: enableData)
                                    })
                                case Actions.providerDisable:
                                    if let disableData = bleProvider?.disable() {
                                        sendData(data: disableData)
                                    }
                                case Actions.scanBleDevices:
                                    bleProvider?.scanForDevices { scanData in
                                        self.sendData(data: scanData)
                                    }
                                case Actions.connectToBleDevice:
                                    if let deviceId = postMessageDict["address"] as? String {
                                        bleProvider?.connectoToDevice(deviceId: deviceId) { connectData in
                                            self.sendData(data: connectData)
                                        }
                                    }
                                case Actions.sendToBleDevice:
                                    if let attributeId = postMessageDict["attributeId"] as? String, let value = postMessageDict["value"] {
                                        if let data = try? JSONSerialization.data(
                                            withJSONObject: value,
                                            options: []) {
                                            bleProvider?.sendToDevice(attributeId: attributeId, value: data) { sendData in
                                                self.sendData(data: sendData)
                                            }
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
    
    public func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        showProgressView()
    }
    
    public func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if (navigationAction.request.url?.absoluteString.starts(with: "webbrowser"))! {
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
                if let baseUrl = self.baseUrl {
                    webView.loadCookiesFromStorage(for: baseUrl) {
                        decisionHandler(.allow)
                    }
                } else {
                    decisionHandler(.allow)
                }
            }
        }
    }
    
    public func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        if let response = navigationResponse.response as? HTTPURLResponse {
            if response.statusCode != 200 && response.statusCode != 204 {
                decisionHandler(.cancel)
                
                if 400...499 ~= response.statusCode {
                    if let baseUrl = self.baseUrl {
                        webView.clearCookies(for: baseUrl, completion: nil)
                    }
                }
                
                handleError(errorCode: response.statusCode, description: "Error in request", failingUrl: response.url?.absoluteString ?? "", isForMainFrame: true)
                return
            }
        }
        if let baseUrl = self.baseUrl {
            webView.writeCookiesToStorage(for: baseUrl) {
                decisionHandler(.allow)
            }
        } else {
            decisionHandler(.allow)
        }
    }
    
    public func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
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
    
    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        hideProgressView()
    }
    
    public func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        NSLog("error %@", error.localizedDescription)
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
