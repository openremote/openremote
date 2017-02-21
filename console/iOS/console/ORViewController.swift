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

    var accessToken : String = ""
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        CustomURLProtocol.accessToken = accessToken

        let myWebView = UIWebView(frame: view.frame)
        view.addSubview(myWebView)
        
        let url = URL(string:String(format:"http://%@:%@/%@",Server.hostURL,Server.port,Server.initialPath))
        let request = URLRequest(url: url!)
        myWebView.scalesPageToFit = true
        myWebView.loadRequest(request)
    }

  }
