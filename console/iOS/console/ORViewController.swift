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

class ORViewcontroller : UIViewController, URLSessionDelegate, UIWebViewDelegate {

    var accessToken : String = ""
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        CustomURLProtocol.accessToken = accessToken
        
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.preferences.javaScriptEnabled = true;
        webConfiguration.ignoresViewportScaleLimits = true;
        webConfiguration.preferences.javaScriptCanOpenWindowsAutomatically = true;

        let myWebView = UIWebView(frame: view.frame)
        view.addSubview(myWebView)
        myWebView.delegate = self
        
        let url = URL(string: "http://192.168.99.100:8080/console/index.html")
        let request = URLRequest(url: url!)
        
        myWebView.loadRequest(request)
    }

  }
