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
        }
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
            domainName = domainTextInput.textField?.text?.appending(string).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return true
    }

    fileprivate func requestAppConfig(_ domain: String) {
//        host = domain.isUrl() ? domain : "https://\(domain).openremote.app/"
//        let url = domain.isUrl() ? domain.appending("/api/master") : "https://\(domain).openremote.app/api/master"
        
//        let apiManager = HttpApiManager(baseUrl: url)
        configManager = ConfigManager(apiManagerFactory: { url in
            HttpApiManager(baseUrl: url)
        })

        async {
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
            case .selectRealm(let baseURL, let realms):
                self.performSegue(withIdentifier: "goToWizardRealmView", sender: self)
            case.complete(let baseURL, let app, let realm):
                self.performSegue(withIdentifier: "goToWebView", sender: self)
            }
        }

        /*
        async {
            do {
                try await print("Console config \(apiManager.getConsoleConfig())")
            } catch {
                print("Error in getConsoleConfig call \(error)")
            }
            do {
                try await print("Apps \(apiManager.getApps())")
            } catch {
                print("Error in getApps call \(error)")
            }
        }
         */
        
        
        /*
        apiManager.getConsoleConfig(callback: { statusCode, consoleConfig, error in
            DispatchQueue.main.async {
                if (statusCode == 200 || statusCode == 404) && error == nil {
//                    let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
//                    userDefaults?.set(self.host, forKey: DefaultsKey.hostKey)
                    let cc = consoleConfig ?? ORConsoleConfig()
                    print(cc)
                    
                    // TODO: app != nil -> don't ask user for app
                    if let selectedApp = cc.app {
                        
                    }
                    
                    
                    // allowedApps == nil -> get list of apps
                    if cc.allowedApps == nil {
                        apiManager.getApps(callback: { statusCode, apps, error in
                            DispatchQueue.main.async {
                                if (statusCode == 200 || statusCode == 404) && error == nil {
                                    let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
                                    userDefaults?.set(self.host, forKey: DefaultsKey.hostKey)
                                    
                                    self.apps = apps
                //                    userDefaults?.set(realm, forKey: DefaultsKey.realmKey)
                //                    self.appconfig = orAppConfig

                                    self.performSegue(withIdentifier: "goToWizardAppView", sender: self)
                                    
                                    
                                // FIXME: this is temporary for compatibility with older managers
                                } else if statusCode == 403 {
                                    // Should select "manager" as the app
                                    
                                    
                                    
                                    
                                } else {
                                    let alertView = UIAlertController(title: "Error", message: "Error occurred getting app config. Check your input and try again", preferredStyle: .alert)
                                    alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

                                    self.present(alertView, animated: true, completion: nil)
                                }
                            }
                        })
                    } else {
                        self.apps = cc.allowedApps
                    }
                    
//                    userDefaults?.set(realm, forKey: DefaultsKey.realmKey)
//                    self.appconfig = orAppConfig

//                    self.performSegue(withIdentifier: "goToWebView", sender: self)
                } else {
                    let alertView = UIAlertController(title: "Error", message: "Error occurred getting app config. Check your input and try again", preferredStyle: .alert)
                    alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

                    self.present(alertView, animated: true, completion: nil)
                }
            }

        })*/
        
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
