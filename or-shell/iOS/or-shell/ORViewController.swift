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

class ORViewcontroller : UIViewController, WKUIDelegate, WKNavigationDelegate {

    
    override func viewDidLoad() {
        super.viewDidLoad()
        //navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .done, target: self, action: #selector(done))
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.preferences.javaScriptEnabled = true;
        webConfiguration.ignoresViewportScaleLimits = true;
        webConfiguration.preferences.javaScriptCanOpenWindowsAutomatically = true;
        let myWebView = WKWebView(frame: .zero, configuration: webConfiguration)

        myWebView.frame = view.frame
        view.addSubview(myWebView)
        
        myWebView.load(URLRequest(url: URL(string: "http://192.168.99.100:8080/console/master/index.html")!))
    }

    func done() {
        self.dismiss(animated: true, completion: nil)
    }
}

