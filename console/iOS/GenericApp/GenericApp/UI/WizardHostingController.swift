//
//  WizardHostingController.swift
//  GenericApp
//
//  Created by Eric Bariaux on 07/09/2022.
//  Copyright © 2022 OpenRemote. All rights reserved.
//

import SwiftUI

class WizardHostingController : UIHostingController<WizardView> {
    
    init() {
        super.init(rootView: WizardView())
    }
    
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder, rootView: WizardView())
    }
    
    // TODO: pass something to WizardView init so it can dismiss its presentation / unwind Segue
    
}
