//
//  WizardDomainViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 18/06/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import MaterialComponents.MaterialTextFields
import ORLib

class WizardDomainViewController: UIViewController {

    var configManager: ConfigManager?

    var domainName: String?
    var appconfig: ORAppConfig?
    var host: String?
    
    var apps: [String]?

    @IBOutlet weak var domainTextInput: ORTextInput!
    @IBOutlet weak var nextButton: MDCRaisedButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        let orGreenColor = UIColor(named: "or_green")

        nextButton.backgroundColor = orGreenColor
        nextButton.tintColor = UIColor.white
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        domainTextInput.textField?.delegate = self
        domainTextInput.textField?.autocorrectionType = .no
        domainTextInput.textField?.autocapitalizationType = .none
        domainTextInput.textField?.returnKeyType = .next
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "goToWizardAppView" {
            let appViewController = segue.destination as! WizardAppViewController
            appViewController.apps = self.apps
            appViewController.configManager = self.configManager
        } else if segue.identifier == "goToWizardRealmView" {
            let realmViewController = segue.destination as! WizardRealmViewController
            realmViewController.configManager = self.configManager
        }
        
        // TODO: prepare to go to Web view
        
    }

    @IBAction func nextButtonpressed(_ sender: UIButton) {
        if let domain = domainName {
            requestAppConfig(domain)
        }
    }
}

extension WizardDomainViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == domainTextInput.textField {
            if let s = domainTextInput.textField?.text {
                domainName = s.replacingCharacters(in: Range(range, in: s)!, with: string).trimmingCharacters(in: .whitespacesAndNewlines)
                nextButton.isEnabled = !(domainName?.isEmpty ?? true)
            } else {
                nextButton.isEnabled = false
            }
        }
        return true
    }

    fileprivate func requestAppConfig(_ domain: String) {
        configManager = ConfigManager(apiManagerFactory: { url in
            HttpApiManager(baseUrl: url)
        })

        async {
            do {
                let state = try await configManager!.setDomain(domain: domain)
                print("State \(state)")
                switch state {
                case .selectDomain:
                    // Something wrong, we just set the domain
                    let alertView = UIAlertController(title: "Error", message: "Error occurred getting app config. Check your input and try again", preferredStyle: .alert)
                    alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

                    self.present(alertView, animated: true, completion: nil)
                case .selectApp(_, let apps):
                    self.apps = apps
                    self.performSegue(withIdentifier: "goToWizardAppView", sender: self)
                case .selectRealm(_, _, _):
                    self.performSegue(withIdentifier: "goToWizardRealmView", sender: self)
                case.complete(_):
                    self.performSegue(withIdentifier: "goToWebView", sender: self)
                }
            } catch {
                let alertView = UIAlertController(title: "Error", message: "Error occurred getting app config. Check your input and try again", preferredStyle: .alert)
                alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                self.present(alertView, animated: true, completion: nil)
            }
        }
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let input = textField.text, !input.isEmpty else {
            return false
        }

        if textField == domainTextInput.textField, let domain = domainName {
            domainTextInput.textField?.resignFirstResponder()
            requestAppConfig(domain)
        }

        return true
    }
}
