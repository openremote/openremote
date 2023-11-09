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

    var apps: [String]?

    var appName: String?

    @IBOutlet weak var appTextInput: ORTextInput!
    @IBOutlet weak var nextButton: MDCRaisedButton!
    @IBOutlet weak var boxView: UIView!
    
    @IBOutlet weak var appsSelectionButton: UIButton!
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
        } else {
            appsSelectionButton.isHidden = true
            appTextInput.isHidden = false
        }
    }

    @IBAction func selectApp(_ sender: AnyObject) {
            dropDown.show()
        }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == Segues.goToWizardRealmView {
            switch configManager!.state {
            case .selectRealm(_, _, let realms):
                let realmViewController = segue.destination as! WizardRealmViewController
                realmViewController.realms = realms
                realmViewController.configManager = self.configManager
            default:
                fatalError("Invalid state for segue")
            }
        } else if segue.identifier == Segues.goToWebView {
            let orViewController = segue.destination as! ORViewcontroller
            
            switch configManager!.state {
            case .complete(let project):
                orViewController.targetUrl = project.targetUrl
            default:
                fatalError("Invalid state for segue")
            }
        }
    }

    @IBAction func nextButtonpressed(_ sender: UIButton) {
        selectApp()
    }
    
    private func selectApp() {
        let selectedApp: String?
        if apps != nil {
            selectedApp = dropDown.selectedItem
        } else {
            selectedApp = appName
        }

        if let selectedApp = selectedApp {
            print("Selected app >\(selectedApp)<")
            _ = try? configManager!.setApp(app: selectedApp)
            
            
            
            // TODO: check state, can we go to some other screen ?
            
            
            self.performSegue(withIdentifier: Segues.goToWizardRealmView, sender: self)
        } else {
            let alertView = UIAlertController(title: "Error", message: "Please \(apps != nil ? "select" : "enter") an application", preferredStyle: .alert)
            alertView.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))

            self.present(alertView, animated: true, completion: nil)
        }
    }
}
 
extension WizardAppViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == appTextInput.textField {
            if let s = appTextInput.textField?.text {
                appName = s.replacingCharacters(in: Range(range, in: s)!, with: string).trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        return true
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let input = textField.text, !input.isEmpty else {
            return false
        }

        if textField == appTextInput.textField {
            appTextInput.textField?.resignFirstResponder()
            selectApp()
        }

        return true
    }
}
