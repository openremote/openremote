//
//  PushNotificationProvider.swift
//  Eindhoven
//
//  Created by Michael Rademaker on 25/05/2018.
//  Copyright © 2018 OpenRemote Inc. All rights reserved.
//

import UIKit
import UserNotifications

public class PushNotificationProvider: NSObject {

    let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
    let version = "fcm"
    public var consoleId: String = ""
    public static let pushDisabledKey = "pushDisabled"
    
    public override init() {
        super.init()
    }
    
    public func initialize(callback:@escaping ([String: Any?]) ->(Void)) {
        UNUserNotificationCenter.current().getNotificationSettings { (settings) in
            switch settings.authorizationStatus {

            case .authorized, .provisional:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: true,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.enabledKey: false,
                    DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: PushNotificationProvider.pushDisabledKey) ?? false
                    ])

            case .denied:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.enabledKey: false,
                    DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: PushNotificationProvider.pushDisabledKey) ?? false
                    ])

            default:
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: nil,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.enabledKey: false,
                    DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: PushNotificationProvider.pushDisabledKey) ?? false
                    ])
            }
        }
    }
    
    public func enable(consoleId: String, callback:@escaping ([String: Any]) ->(Void)) {
        self.consoleId = consoleId
        userdefaults?.set(self.consoleId, forKey: GeofenceProvider.consoleIdKey)
        userdefaults?.removeObject(forKey: PushNotificationProvider.pushDisabledKey)
        userdefaults?.synchronize()
        UNUserNotificationCenter.current().getNotificationSettings { (settings) in

            switch settings.authorizationStatus {
            case .authorized, .provisional:
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                callback([
                    DefaultsKey.actionKey: Actions.providerEnable,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.hasPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.dataKey: ["token": self.userdefaults?.string(forKey: DefaultsKey.fcmTokenKey)]
                    ])

            case .denied:
                callback([
                    DefaultsKey.actionKey: Actions.providerEnable,
                    DefaultsKey.providerKey: Providers.push,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.successKey: true,
                    DefaultsKey.dataKey: ["token": self.userdefaults?.string(forKey: DefaultsKey.fcmTokenKey)]
                    ])

            default:
                UNUserNotificationCenter.current().requestAuthorization(
                    options: [.alert, .badge, .sound],
                    completionHandler: {granted, _ in
                        if granted {
                            DispatchQueue.main.async {
                                UIApplication.shared.registerForRemoteNotifications()
                            }
                        }

                        callback([
                            DefaultsKey.actionKey: Actions.providerEnable,
                            DefaultsKey.providerKey: Providers.push,
                            DefaultsKey.hasPermissionKey: granted,
                            DefaultsKey.successKey: true,
                            DefaultsKey.dataKey: ["token": self.userdefaults?.string(forKey: DefaultsKey.fcmTokenKey)]
                            ])
                })

            }
        }
    }

    public func disable()-> [String: Any] {
        userdefaults?.set(true, forKey: PushNotificationProvider.pushDisabledKey)
        userdefaults?.synchronize()
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.push
        ]
    }
}
