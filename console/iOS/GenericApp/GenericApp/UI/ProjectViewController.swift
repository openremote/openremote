//
//  ProjectViewController.swift
//  GenericApp
//
//  Created by Michael Rademaker on 26/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UIKit
import MaterialComponents.MaterialTextFields
import ORLib

class ProjectViewController: UIViewController {

    var projectName: String?
    var realmName: String?
    var appconfig: ORAppConfig?
    var host: String?

    @IBOutlet weak var projectTextInput: ORTextInput!
    @IBOutlet weak var realmTextInput: ORTextInput!
    @IBOutlet weak var connectButton: MDCRaisedButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        let orGreenColor = UIColor(named: "or_green")

        connectButton.backgroundColor = orGreenColor
        connectButton.tintColor = UIColor.white
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        projectTextInput.textField?.delegate = self
        projectTextInput.textField?.autocorrectionType = .no
        projectTextInput.textField?.autocapitalizationType = .none
        projectTextInput.textField?.returnKeyType = .next

        realmTextInput.textField?.delegate = self
        realmTextInput.textField?.autocorrectionType = .no
        realmTextInput.textField?.autocapitalizationType = .none
        realmTextInput.textField?.returnKeyType = .continue
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "goToWebView" {
            let orViewController = segue.destination as! ORViewcontroller
            orViewController.appConfig = self.appconfig
        }
    }

    @IBAction func connectButtonpressed(_ sender: UIButton) {
        if let project = projectName, let realm = realmName {
            requestAppConfig(project, realm)
        }
    }
}

extension ProjectViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == projectTextInput.textField {
            projectName = projectTextInput.textField?.text?.appending(string).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if textField == realmTextInput.textField {
            realmName = realmTextInput?.textField?.text?.appending(string).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return true
    }

    fileprivate func requestAppConfig(_ project: String, _ realm: String) {
        host = project.isUrl() ? project : "https://\(project).openremote.app/"
        let url = project.isUrl() ? project.appending("/api/\(realm)") : "https://\(project).openremote.app/api/\(realm)"
        
        let apiManager = ApiManager(baseUrl: url)
        apiManager.getAppConfig(realm: realm, callback: { statusCode, orAppConfig, error in
            DispatchQueue.main.async {
                if statusCode == 200 && error == nil {
                    let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
                    userDefaults?.set(self.host, forKey: DefaultsKey.hostKey)
                    userDefaults?.set(realm, forKey: DefaultsKey.realmKey)
                    self.appconfig = orAppConfig

                    self.performSegue(withIdentifier: "goToWebView", sender: self)
                } else {
                    let alertView = UIAlertController(title: "Error", message: "Error occurred getting app config. Check your input and try again", preferredStyle: .alert)
                    alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

                    self.present(alertView, animated: true, completion: nil)
                }
            }
        })
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let input = textField.text, !input.isEmpty else {
            return false
        }

        if textField == projectTextInput.textField {
            realmTextInput.textField?.becomeFirstResponder()
        } else if textField == realmTextInput.textField, let project = projectName, let realm = realmName {
            realmTextInput.textField?.resignFirstResponder()
            requestAppConfig(project, realm)
        }

        return true
    }
}
