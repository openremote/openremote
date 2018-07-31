//
//  PushNotificationProvider.swift
//  Eindhoven
//
//  Created by Michael Rademaker on 25/05/2018.
//  Copyright © 2018 OpenRemote Inc. All rights reserved.
//

import UIKit
import UserNotifications

class PushNotificationProvider: NSObject {

    let version = "fcm"
    
    public override init() {
        super.init()
    }
    
    public func initialize(callback:@escaping ([String: Any]) ->(Void)) {
        UNUserNotificationCenter.current().getNotificationSettings { (settings) in
            switch settings.authorizationStatus {

            case .authorized:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: true,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true
                    ])

            case .denied:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true
                    ])

            case .notDetermined:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true
                    ])
            }
        }
    }
    
    public func enable(callback:@escaping ([String: Any]) ->(Void)) {
        UNUserNotificationCenter.current().getNotificationSettings { (settings) in

            switch settings.authorizationStatus {
            case .authorized:
                callback([
                    DefaultsKey.actionKey: Actions.providerEnable,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.hasPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.dataKey: [DefaultsKey.token: TokenManager.sharedInstance.deviceId]
                    ])

            case .denied:
                callback([
                    DefaultsKey.actionKey: Actions.providerEnable,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.successKey: true,
                    DefaultsKey.dataKey: [DefaultsKey.token: TokenManager.sharedInstance.deviceId]
                    ])

            case .notDetermined:
                let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
                UNUserNotificationCenter.current().requestAuthorization(
                    options: authOptions,
                    completionHandler: {granted, _ in
                        callback([
                            DefaultsKey.actionKey: Actions.providerEnable,
                            DefaultsKey.providerKey: Providers.push,
                            DefaultsKey.hasPermissionKey: granted,
                            DefaultsKey.successKey: true,
                            DefaultsKey.dataKey: [DefaultsKey.token: TokenManager.sharedInstance.deviceId]
                            ])
                })

            }
        }
    }

    public func disbale()-> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.push
        ]
    }
}
