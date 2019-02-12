//
//  StorageProvider.swift
//  GoogleToolboxForMac
//
//  Created by Michael Rademaker on 05/02/2019.
//

import UIKit

class StorageProvider: NSObject {

    let userdefaults = UserDefaults(suiteName: ORAppGroup.entitlement)

    public func initialize() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerInit,
            DefaultsKey.providerKey: Providers.storage,
            DefaultsKey.versionKey: "1.0.0",
            DefaultsKey.requiresPermissionKey: true,
            DefaultsKey.hasPermissionKey: true,
            DefaultsKey.successKey: true,
            DefaultsKey.enabledKey: false
        ]
    }

    public func enable() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerEnable,
            DefaultsKey.providerKey: Providers.storage,
            DefaultsKey.hasPermissionKey: true,
            DefaultsKey.successKey: true,
        ]
    }

    public func store(key: String, data: String?) {
        if let dataToStore = data {
            userdefaults?.set(dataToStore, forKey: key)
        } else {
            userdefaults?.removeObject(forKey: key)
        }
        userdefaults?.synchronize()
    }

    public func retrieve(key: String) -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.retrieve,
            DefaultsKey.providerKey: Providers.storage,
            "key": key,
            "value": userdefaults?.string(forKey:key) ?? "null"
        ]
    }

    public func disable()-> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.storage
        ]
    }
}
