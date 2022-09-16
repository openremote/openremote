//
//  WizardRealmViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 21/07/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import MaterialComponents.MaterialTextFields
import ORLib

class WizardRealmViewController: UIViewController {
    
    @IBOutlet weak var realmTextInput: ORTextInput!
    @IBOutlet weak var nextButton: MDCRaisedButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        let orGreenColor = UIColor(named: "or_green")

        nextButton.backgroundColor = orGreenColor
        nextButton.tintColor = UIColor.white
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

//        realmTextInput.textField?.delegate = self
        realmTextInput.textField?.autocorrectionType = .no
        realmTextInput.textField?.autocapitalizationType = .none
        realmTextInput.textField?.returnKeyType = .next
    }
    
    @IBAction func nextButtonpressed(_ sender: UIButton) {
        /*
        if let domain = domainName {
            requestAppConfig(domain)
        }
         */
        self.performSegue(withIdentifier: "goToWebView", sender: self)
    }

    
}
