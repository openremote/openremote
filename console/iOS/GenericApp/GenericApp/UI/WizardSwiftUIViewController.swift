//
//  WizardSwiftUIViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 07/09/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import SwiftUI

class WizardSwiftUIViewController: UIViewController {

    @IBOutlet weak var containingView: UIView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let childController = UIHostingController(rootView: WizardView())
        addChild(childController)
        childController.view.frame = containingView.bounds
        containingView.addSubview(childController.view)
    }
}





