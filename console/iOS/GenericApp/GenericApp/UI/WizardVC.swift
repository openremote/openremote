//
//  WizardVC.swift
//  GenericApp
//
//  Created by Eric Bariaux on 12/09/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import SwiftUI

class WizardVC: UIViewController {
    
    @IBSegueAction func addSwiftUIView(_ coder: NSCoder) -> UIViewController? {
        print("Ask to embed view !")
        return UIHostingController(coder: coder, rootView: WizardView())
    }
    
}
