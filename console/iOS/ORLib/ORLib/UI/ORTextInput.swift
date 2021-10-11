//
//  ORTextInput.swift
//  GenericApp
//
//  Created by Michael Rademaker on 26/10/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UIKit
import MaterialComponents.MaterialTextFields

@IBDesignable  // Lets you change a UIView instance in storyboard to this class, so you can visualize it and add constraints to it
public class ORTextInput: UIView {

    private var textInput: MDCTextField!
    private var controller: MDCTextInputControllerFilled!
    private let textColor = UIColor(named: "or_green")

    private var placeholderText = ""

    @IBInspectable var setPlaceholderText: String {
        get {
            return placeholderText
        }
        set(str) {
            placeholderText = str
        }
    }

    public var textField: UITextField? {
        get {
            return textInput
        }
    }

    public override func layoutSubviews() {
        setupInputView()
        setupContoller()
    }

    private func setupInputView(){
        if let _ = self.viewWithTag(1){return}

        textInput = MDCTextField()
        textInput.tag = 1
        textInput.translatesAutoresizingMaskIntoConstraints = false
        textInput.placeholder = placeholderText
        textInput.clearButtonMode = .never

        self.addSubview(textInput)

        NSLayoutConstraint.activate([
            (textInput.topAnchor.constraint(equalTo: self.topAnchor)),
            (textInput.bottomAnchor.constraint(equalTo: self.bottomAnchor)),
            (textInput.leadingAnchor.constraint(equalTo: self.leadingAnchor)),
            (textInput.trailingAnchor.constraint(equalTo: self.trailingAnchor))
        ])
    }

    private func setupContoller(){
        controller = MDCTextInputControllerFilled(textInput: textInput)

        controller.activeColor = textColor
        controller.floatingPlaceholderActiveColor = textColor

    }
}
