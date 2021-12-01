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
import Popover

open class ORViewcontroller : UIViewController {
    
    var data : Data?
    var menuButton: MDCFloatingButton?
    var menuList: UIView?
    var myWebView : WKWebView?
    var webProgressBar: UIProgressView?
    var defaults : UserDefaults?
    var webCfg : WKWebViewConfiguration?
    public var appConfig: ORAppConfig?
    var popover: Popover?
    public var geofenceProvider: GeofenceProvider?
    public var pushProvider: PushNotificationProvider?
    public var storageProvider: StorageProvider?
    public var qrProvider: QrScannerProvider?

    var popoverOptions: [PopoverOption]?
    
    open override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white
        self.configureAccess()
    }

    open override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if let applicationConfig = appConfig {
            loadURL(url: URL(string: applicationConfig.initialUrl.stringByURLEncoding()!)!)
        }
    }

    func sendData(data: [String: Any?]) {
        if let theJSONData = try? JSONSerialization.data(
            withJSONObject: data,
            options: []) {
            let theJSONText = String(data: theJSONData,
                                     encoding: .utf8)
            let returnMessage = "OpenRemoteConsole._handleProviderResponse('\(theJSONText ?? "null")')"
            DispatchQueue.main.async {
                self.myWebView?.evaluateJavaScript("\(returnMessage)", completionHandler: { (any, error) in
                    print(error)
                    print("JSON string = \(theJSONText!)")
                })
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
        let webFrame = CGRect(x: 0, y: sbHeight, width: view.frame.size.width, height: view.frame.size.height - sbHeight)
        myWebView = WKWebView(frame: webFrame, configuration: webCfg)
        myWebView?.autoresizingMask = [.flexibleWidth, .flexibleHeight];
        myWebView?.navigationDelegate = self;
        //add observer to get estimated progress value
        myWebView?.addObserver(self, forKeyPath: "estimatedProgress", options: .new, context: nil);

        webProgressBar = UIProgressView(progressViewStyle: .bar)
        webProgressBar?.progressTintColor = UIColor(named: "or_green")

        view.addSubview(myWebView!)
        view.addSubview(webProgressBar!)
        view.bringSubviewToFront(webProgressBar!)

        webProgressBar?.translatesAutoresizingMaskIntoConstraints = false
        webProgressBar?.leadingAnchor.constraint(equalTo: myWebView!.leadingAnchor).isActive = true
        webProgressBar?.trailingAnchor.constraint(equalTo: myWebView!.trailingAnchor).isActive = true
        webProgressBar?.topAnchor.constraint(equalTo: myWebView!.topAnchor, constant: -2).isActive = true
        webProgressBar?.heightAnchor.constraint(equalToConstant: 2).isActive = true

        if appConfig?.menuEnabled ?? false {

            menuButton = MDCFloatingButton(shape: .default)
            menuButton?.backgroundColor = UIColor(hexaRGB: appConfig?.primaryColor ?? "#43A047")
            menuButton?.translatesAutoresizingMaskIntoConstraints = false
            menuButton?.addTarget(self, action: #selector(self.pressed(sender:)), for: .touchUpInside)
            menuButton?.setImage(#imageLiteral(resourceName: "ic_menu"), for: .normal)
            menuButton?.setImage(#imageLiteral(resourceName: "ic_menu"), for: .selected)
            menuButton?.setImage(#imageLiteral(resourceName: "ic_menu"), for: .highlighted)
            menuButton?.isHidden = true

            if let secondColor = appConfig?.secondaryColor {
                menuButton?.tintColor = UIColor(hexaRGB: secondColor)
            }

            view.addSubview(menuButton!)
            view.bringSubviewToFront(menuButton!)

            let constraints: Array<NSLayoutConstraint> = {
                if appConfig!.menuPosition == "BOTTOM_RIGHT" {
                    self.popoverOptions = [
                        .type(.up),
                        .arrowSize(.zero),
                    ]
                    return [
                        menuButton!.rightAnchor.constraint(equalTo: self.view.rightAnchor, constant: -20),
                        menuButton!.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -20)
                    ]
                } else if appConfig!.menuPosition == "TOP_RIGHT" {
                    self.popoverOptions = [
                        .type(.down),
                        .arrowSize(.zero),
                    ]
                    return [
                        menuButton!.rightAnchor.constraint(equalTo: self.view.rightAnchor, constant: -20),
                        menuButton!.topAnchor.constraint(equalTo: self.view.topAnchor, constant: sbHeight)
                    ]
                } else if appConfig!.menuPosition == "TOP_LEFT" {
                    self.popoverOptions = [
                        .type(.down),
                        .arrowSize(.zero),
                    ]
                    return [
                        menuButton!.leftAnchor.constraint(equalTo: self.view.leftAnchor, constant: 20),
                        menuButton!.topAnchor.constraint(equalTo: self.view.topAnchor, constant: sbHeight)
                    ]
                }
                self.popoverOptions = [
                    .type(.up),
                    .arrowSize(.zero),

                ]
                return [
                    menuButton!.leftAnchor.constraint(equalTo: self.view.leftAnchor, constant: 20),
                    menuButton!.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -20)
                ]
            }()

            NSLayoutConstraint.activate(constraints)
        }
    }

    @objc func pressed(sender: UIButton!) {
        let linkCount = appConfig?.links?.count ?? 0
        let tableView = UITableView(frame: CGRect(x: 0, y: 0, width: self.view.frame.width / 2, height: CGFloat(linkCount * 60)))
        tableView.delegate = self
        tableView.dataSource = self
        tableView.isScrollEnabled = false
        self.popover = Popover(options: self.popoverOptions)
        self.popover!.show(tableView, fromView: self.menuButton!)
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
        alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

        self.present(alertView, animated: true, completion: nil)

        if let applicationConfig = appConfig {
            self.myWebView?.load(URLRequest(url: URL(string: applicationConfig.url.stringByURLEncoding()!)!))
        }
    }

}

extension ORViewcontroller: UITableViewDelegate {

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        self.popover?.dismiss()
        loadURL(url: URL(string: appConfig!.links![indexPath.row].pageLink.stringByURLEncoding()!)!)
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60
    }
}

extension ORViewcontroller: UITableViewDataSource {

    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int{
        return appConfig?.links?.count ?? 0
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.textLabel?.text = appConfig!.links![indexPath.row].displayText
        return cell
    }
}

extension ORViewcontroller: WKScriptMessageHandler {
    public func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
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
                                        if let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
                                           let project = userdefaults.string(forKey: DefaultsKey.projectKey),
                                           let realm = userdefaults.string(forKey: DefaultsKey.realmKey) {
                                            geofenceProvider?.enable(baseUrl: "https://\(project).openremote.io/api/\(realm)", consoleId: consoleId,  callback: { enableData in
                                                self.sendData(data: enableData)
                                                DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(5)) {
                                                    self.geofenceProvider?.fetchGeofences()
                                                }
                                            })
                                        }
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
                                            let alertController = UIAlertController(title: "Location permission denied",
                                                                                    message: "In order to get the location it's necessary to give permissions to the app. Do you want to open the settings?",
                                                                                    preferredStyle: .alert)
                                            if let settingsUrl = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(settingsUrl) {
                                                alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: { alertAction in
                                                    if UIApplication.shared.canOpenURL(settingsUrl) {
                                                        UIApplication.shared.open(settingsUrl, completionHandler: { (success) in
                                                            print("Settings opened: \(success)") // Prints true
                                                        })
                                                    }
                                                }))
                                                alertController.addAction(UIAlertAction(title: "Not now", style: .cancel, handler: nil))
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
                                    qrProvider?.startScanner(currentViewController: self) { scannedData in
                                        self.sendData(data: scannedData)
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
                if let config = appConfig, let button = menuButton {
                    if config.url.stringByURLEncoding() == navigationAction.request.url?.absoluteString {
                        button.isHidden = false
                    }
                }

                decisionHandler(.allow)
            }
        }
    }

    public func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        if let response = navigationResponse.response as? HTTPURLResponse {
            if response.statusCode != 200 && response.statusCode != 204 {
                decisionHandler(.cancel)

                handleError(errorCode: response.statusCode, description: "Error in request", failingUrl: response.url?.absoluteString ?? "", isForMainFrame: true)
                return
            }
        }
        decisionHandler(.allow)
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
