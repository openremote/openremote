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
import DropDown


class WizardRealmViewController: UIViewController {
    
    var configManager: ConfigManager?

    var realms: [String]?

    var realmName: String?
    
    @IBOutlet weak var realmTextInput: ORTextInput!
    @IBOutlet weak var nextButton: MDCRaisedButton!
    @IBOutlet weak var boxView: UIView!
    
    @IBOutlet weak var realmsSelectionButton: UIButton!
    var dropDown = DropDown()

    override func viewDidLoad() {
        super.viewDidLoad()

        let orGreenColor = UIColor(named: "or_green")

        nextButton.backgroundColor = orGreenColor
        nextButton.tintColor = UIColor.white

        boxView.layer.cornerRadius = 10
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        realmTextInput.textField?.delegate = self
        realmTextInput.textField?.autocorrectionType = .no
        realmTextInput.textField?.autocapitalizationType = .none
        realmTextInput.textField?.returnKeyType = .next
        
        if let realms = realms {
            dropDown.anchorView = realmsSelectionButton
            // The list of items to display. Can be changed dynamically
            dropDown.dataSource = realms

            dropDown.selectionAction = { [weak self] (index, item) in
                self?.realmsSelectionButton.setTitle(item, for: .normal)
            }
            
            realmsSelectionButton.isHidden = false
            realmTextInput.isHidden = true
        } else {
            realmsSelectionButton.isHidden = true
            realmTextInput.isHidden = false
        }

    }
    
    @IBAction func selectRealm(_ sender: AnyObject) {
            dropDown.show()
        }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == Segues.goToWebView {
            let orViewController = segue.destination as! ORViewcontroller
            
            switch configManager!.state {
            case .complete(let project):
                orViewController.targetUrl = project.targetUrl
            default:
                fatalError("We should never come to this screen in that state")
            }
            
            
            // TODO: based on configManager?.appInfos retrieve providers -> this is done by selectRealm on ConfigMgr
            
            
            
//            orViewController.targetUrl = "https://demo.openremote.io/manager/?realm=smartcity&consoleProviders=geofence push storage&consoleAutoEnable=true#!geofences"
        }
    }
    
    @IBAction func nextButtonpressed(_ sender: UIButton) {
        
        // TODO: handle errors and have a proper error message
        
            
        // TODO: handle the case realm is selected from menu
        
            
        let state = try? configManager!.setRealm(realm: realmName)
        switch state {
        case let .complete(project):
            if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
                do {
                    var projects: [ProjectConfig]
                    if let projectsData = userDefaults.data(forKey: DefaultsKey.projectsConfigurationKey) {
                        projects = (try? JSONDecoder().decode([ProjectConfig].self, from: projectsData)) ?? []
                        projects.append(project)
                    } else {
                        projects = [project]
                    }
                    let data = try JSONEncoder().encode(projects)
                    userDefaults.setValue(data, forKey: DefaultsKey.projectsConfigurationKey)
                    userDefaults.setValue(project.id, forKey: DefaultsKey.projectKey)
                    self.performSegue(withIdentifier: Segues.goToWebView, sender: self)
                } catch {
                    break // Fall through to error message
                }
            }
        default:
            break // Fall through to error message
        }


        // If we reached here, an error occured
        
        
        // TODO: proper error message
        let alertView = UIAlertController(title: "Error", message: "TODO", preferredStyle: .alert)
        alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        self.present(alertView, animated: true, completion: nil)

    }
}

extension WizardRealmViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == realmTextInput.textField {
            if let s = realmTextInput.textField?.text {
                realmName = s.replacingCharacters(in: Range(range, in: s)!, with: string).trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }

        return true
    }

    /*
    fileprivate func requestAppConfig(_ domain: String) {
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
    }
     */

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let input = textField.text, !input.isEmpty else {
            return false
        }

        if textField == realmTextInput.textField, let realm = realmName {
            realmTextInput.textField?.resignFirstResponder()
//            requestAppConfig(domain)
        }

        return true
    }
}
