//
//  WizardView.swift
//  GenericApp
//
//  Created by Eric Bariaux on 07/09/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import SwiftUI

struct WizardView: View {
    
    @State private var domainName = ""
    
    var body: some View {
        NavigationView {
            Form() {
                Text("Enter your app domain")
                TextField("domain", text: $domainName)
                Spacer()
            }
            .navigationBarTitle("Settings")
            .navigationBarItems(trailing: NavigationLink("Next") {
                Text("Next")
            })
        }
    }
}

struct WizardView_Previews: PreviewProvider {
    static var previews: some View {
        WizardView()
    }
}
