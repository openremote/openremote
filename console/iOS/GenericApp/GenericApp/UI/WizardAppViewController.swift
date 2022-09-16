//
//  WizardAppViewController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 18/06/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import UIKit
import MaterialComponents.MaterialTextFields
import ORLib
import DropDown

class WizardAppViewController: UIViewController {

    var configManager: ConfigManager?
    
    var appName: String?
    var appconfig: ORAppConfig?
    var host: String?
    
    var apps: [String]?
    
    var dropDown = DropDown()

    @IBOutlet weak var appTextInput: ORTextInput!
    @IBOutlet weak var nextButton: MDCRaisedButton!
    
    @IBOutlet weak var appsSelectionButton: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        let orGreenColor = UIColor(named: "or_green")

        nextButton.backgroundColor = orGreenColor
        nextButton.tintColor = UIColor.white
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        appTextInput.textField?.delegate = self
        appTextInput.textField?.autocorrectionType = .no
        appTextInput.textField?.autocapitalizationType = .none
        appTextInput.textField?.returnKeyType = .next
        
        if let apps = apps {
            dropDown.anchorView = appsSelectionButton
            // The list of items to display. Can be changed dynamically
            dropDown.dataSource = apps

            dropDown.selectionAction = { [weak self] (index, item) in
                self?.appsSelectionButton.setTitle(item, for: .normal)
            }
            
            appsSelectionButton.isHidden = false
            appTextInput.isHidden = true
        }
    }

    @IBAction func selectApp(_ sender: AnyObject) {
            dropDown.show()
        }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "goToWizardRealmView" {
            
        }
/*        if segue.identifier == "goToWebView" {
            let orViewController = segue.destination as! ORViewcontroller
            
        }*/
    }

    @IBAction func nextButtonpressed(_ sender: UIButton) {
        print("Selected app " + (dropDown.selectedItem ?? "none"))
        /*
        if let domain = domainName {
            requestAppConfig(domain)
        }
         */
        self.performSegue(withIdentifier: "goToWizardRealmView", sender: self)
    }

  
/*
    @IBAction func connectButtonpressed(_ sender: UIButton) {
        if let project = projectName, let realm = realmName {
            requestAppConfig(project, realm)
        }
    }
     */
}
 
extension WizardAppViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == appTextInput.textField {
            appName = appTextInput.textField?.text?.appending(string).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return true
    }

    /*
    fileprivate func requestAppConfig(_ project: String, _ realm: String) {
        host = project.isUrl() ? project : "https://\(project).openremote.app/"
        let url = project.isUrl() ? project.appending("/api/\(realm)") : "https://\(project).openremote.app/api/\(realm)"
        
        let apiManager = ApiManager(baseUrl: url)
        apiManager.getAppConfig(realm: realm, callback: { statusCode, orAppConfig, error in
            DispatchQueue.main.async {
                if (statusCode == 200 || statusCode == 404) && error == nil {
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
     */

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let input = textField.text, !input.isEmpty else {
            return false
        }

        if textField == appTextInput.textField, let app = appName {
            appTextInput.textField?.resignFirstResponder()
//            requestAppConfig(domain)
        }

        return true
    }
}
