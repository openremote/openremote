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
        
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.preferences.javaScriptEnabled = true;
        webConfiguration.ignoresViewportScaleLimits = true;
        webConfiguration.preferences.javaScriptCanOpenWindowsAutomatically = true;
        let myWebView = WKWebView(frame: .zero, configuration: webConfiguration)
        myWebView.translatesAutoresizingMaskIntoConstraints = false
        myWebView.navigationDelegate = self

        view.addSubview(myWebView)
        
        [myWebView.topAnchor.constraint(equalTo: view.topAnchor),
         myWebView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
         myWebView.leftAnchor.constraint(equalTo: view.leftAnchor),
         myWebView.rightAnchor.constraint(equalTo: view.rightAnchor)].forEach  { anchor in
            anchor.isActive = true
        }
        
        var resourceURLString = Bundle.main.path(forResource: "index", ofType: "html")?.addingPercentEncoding(withAllowedCharacters: CharacterSet.urlPathAllowed);
        resourceURLString = "file://"+resourceURLString!
        let myURL = URL(string: resourceURLString!)
        myWebView.frame = view.frame
        myWebView.loadFileURL(myURL!, allowingReadAccessTo: myURL!)
    }
}

