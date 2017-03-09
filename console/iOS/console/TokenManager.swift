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

class TokenManager:NSObject, WKScriptMessageHandler, WKUIDelegate, WKNavigationDelegate {
    
    var offlineToken : String?
    var refreshToken : String?
    var idToken : String?
    var hasToken: Bool
    var viewController : TokenManagerViewController
    
    static let sharedInstance = TokenManager()
    
    override init() {
        viewController = TokenManagerViewController()
        if let token = UserDefaults.standard.value(forKey: DefaultsKey.offlineToken) {
            hasToken = true
            offlineToken = token as? String
            refreshToken = UserDefaults.standard.value(forKey: DefaultsKey.refreshToken) as? String
            idToken = UserDefaults.standard.value(forKey: DefaultsKey.idToken) as? String
        } else {
            offlineToken = nil
            refreshToken = nil
            idToken = nil
            hasToken = false
        }
        super.init()
    }
    
    func authenticate() {
        let defaults = UserDefaults.standard
        let webCfg:WKWebViewConfiguration = WKWebViewConfiguration()
        
        let userController:WKUserContentController = WKUserContentController()
        
        userController.add(self, name: DefaultsKey.offlineToken)
        userController.add(self, name: DefaultsKey.refreshToken)
        userController.add(self, name: DefaultsKey.idToken)
        
        var exec_template = "var iOSToken"
        if let offlineToken = defaults.object(forKey: DefaultsKey.offlineToken) {
            let refreshToken = defaults.object(forKey: DefaultsKey.refreshToken)
            let idToken = defaults.object(forKey: DefaultsKey.idToken)
            exec_template = String(format: "var iOSToken = \"%@\"; var iOSRefreshToken = \"%@\"; var iOSTokenId = \"%@\";", offlineToken as! String, refreshToken as! String, idToken as! String)
        }
        
        let userScript:WKUserScript = WKUserScript(source: exec_template, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        userController.addUserScript(userScript)
        
        
        webCfg.userContentController = userController;
        let myWebView = WKWebView(frame: viewController.view.frame, configuration: webCfg)
        myWebView.uiDelegate = self;
        myWebView.navigationDelegate = self;
        viewController.view.addSubview(myWebView)
        UIApplication.shared.keyWindow?.rootViewController?.present(viewController, animated: true, completion: nil)
        guard let request = URL(string: String(format:"http://%@:%@/%@",Server.hostURL,Server.port,Server.initialPath)) else { return }
        myWebView.load(URLRequest(url: request))
    }
    
    func resetToken() {
        hasToken = false
        offlineToken = nil
        refreshToken = nil
        idToken = nil
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: DefaultsKey.offlineToken)
        defaults.removeObject(forKey: DefaultsKey.refreshToken)
        defaults.removeObject(forKey: DefaultsKey.idToken)
        authenticate()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        let defaults = UserDefaults.standard
        defaults.set(message.body, forKey: message.name)
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
        if (offlineToken != nil && refreshToken != nil && idToken != nil ) {
            self.hasToken = true
            self.viewController.dismiss(animated: true, completion: {
                let notificationName = Notification.Name(NotificationsNames.isAuthenticated)
                NotificationCenter.default.post(name: notificationName, object: nil)
            })
        }
        
    }
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alertView = UIAlertController(title: "debug", message: message, preferredStyle: .alert)
        let alertAction = UIAlertAction(title: "Done", style: .cancel) { (action) in
            completionHandler()
        }
        alertView.addAction(alertAction)
        viewController.present(alertView, animated: true, completion: nil)
    }
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        showError(error: error)
    }
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        let errorCode = (error as NSError).code
        if errorCode != NSURLErrorCancelled {
            showError(error: error)
            
        }
    }
    func showError(error : Error) {
        let alertVC = UIAlertController(title: "Error", message: error.localizedDescription, preferredStyle: UIAlertControllerStyle.alert)
        let alertAction = UIAlertAction(title: "Done", style: .cancel) { (action) in
            self.viewController.dismiss(animated: true, completion: nil)
        }
        
        alertVC.addAction(alertAction)
        viewController.present(alertVC, animated: true, completion: nil)
    }
}
