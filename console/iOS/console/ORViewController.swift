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

class ORViewcontroller : UIViewController, URLSessionDelegate {

    var data : Data?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        let myWebView = UIWebView(frame: view.frame)
        view.addSubview(myWebView)
        
        myWebView.scalesPageToFit = true
        myWebView.load(self.data!, mimeType: "text/html", textEncodingName: "UTF-8", baseURL: URL(string: "http://192.168.99.100:8080/")!)
    }

  }
