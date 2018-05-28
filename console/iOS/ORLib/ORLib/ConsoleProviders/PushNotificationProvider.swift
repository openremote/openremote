//
//  PushNotificationProvider.swift
//  Eindhoven
//
//  Created by Michael Rademaker on 25/05/2018.
//  Copyright © 2018 OpenRemote Inc. All rights reserved.
//

import UIKit

public class PushNotificationProvider: NSObject {

    let version = "fcm"
    
    public override init() {
        super.init()
    }
    
    public func initialize() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerInit,
            DefaultsKey.providerKey: Providers.push,
            DefaultsKey.versionKey: version,
            DefaultsKey.requiresPermissionKey: false,
            DefaultsKey.successKey: true
        ]
    }
    
    public func enable()-> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerEnable,
            DefaultsKey.providerKey: Providers.push,
            DefaultsKey.hasPermissionKey: true,
            DefaultsKey.successKey: true,
            DefaultsKey.dataKey: [DefaultsKey.token: TokenManager.sharedInstance.deviceId]
        ]
    }

    public func disbale()-> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.push
        ]
    }
}
