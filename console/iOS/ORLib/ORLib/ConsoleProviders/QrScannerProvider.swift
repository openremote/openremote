//
//  QrScannerProver.swift
//  ORLib
//
//  Created by Michael Rademaker on 22/11/2021.
//

import UIKit
import AVFoundation

public class QrScannerProvider: NSObject {

    public static let cameraDisabledKey = "cameraDisabled"

    let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
    let version = "qr"

    var scannedCallback : (([String: Any]) -> (Void))?
    var scanner: QrScannerViewController?

    public func initialize(callback:@escaping ([String: Any?]) ->(Void)) {
        let cameraAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

        switch cameraAuthorizationStatus {
        case .authorized:
            callback( [
                DefaultsKey.actionKey: Actions.providerInit,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: true,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: false,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])

        case .denied:
            callback( [
                DefaultsKey.actionKey: Actions.providerInit,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: false,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: false,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])

        default:
            callback( [
                DefaultsKey.actionKey: Actions.providerInit,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: nil,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: false,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])
        }
    }

    public func enable(callback:@escaping ([String: Any]) ->(Void)) {
        userdefaults?.removeObject(forKey: QrScannerProvider.cameraDisabledKey)
        userdefaults?.synchronize()
        let cameraAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

        switch cameraAuthorizationStatus {
        case .authorized:
            callback( [
                DefaultsKey.actionKey: Actions.providerInit,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: true,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: false,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])

        case .denied:
            callback( [
                DefaultsKey.actionKey: Actions.providerInit,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: false,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: false,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])

        default:
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { granted in
                callback( [
                    DefaultsKey.actionKey: Actions.providerInit,
                    DefaultsKey.providerKey: Providers.qr,
                    DefaultsKey.versionKey: self.version,
                    DefaultsKey.hasPermissionKey: granted,
                    DefaultsKey.requiresPermissionKey: true,
                    DefaultsKey.successKey: true,
                    DefaultsKey.enabledKey: false,
                    DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
                ])
            })
        }
    }

    public func disable() -> [String: Any] {
        userdefaults?.set(true, forKey: QrScannerProvider.cameraDisabledKey)
        userdefaults?.synchronize()
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.qr
        ]
    }

    public func startScanner(currentViewController: UIViewController, callback:@escaping ([String: Any]) -> Void) {
        scanner = QrScannerViewController()
        scanner!.delegate = self
        currentViewController.present(scanner!, animated: true, completion: nil)
        scannedCallback = callback
    }
}

extension QrScannerProvider: QrScannerDelegate {

    public func codeScanned(_ codeContents: String) {
        scanner?.dismiss(animated: true) {
            self.scannedCallback?(["result": codeContents])
            self.scannedCallback = nil
        }
        scanner = nil
    }
}
