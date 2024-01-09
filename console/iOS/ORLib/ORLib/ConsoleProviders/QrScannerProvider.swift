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
            callback([
                DefaultsKey.actionKey: Actions.providerEnable,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: true,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: true,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])
            
        case .denied:
            callback([
                DefaultsKey.actionKey: Actions.providerEnable,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: false,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: true,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? false
            ])
            
        default:
            callback([
                DefaultsKey.actionKey: Actions.providerEnable,
                DefaultsKey.providerKey: Providers.qr,
                DefaultsKey.versionKey: self.version,
                DefaultsKey.hasPermissionKey: nil,
                DefaultsKey.requiresPermissionKey: true,
                DefaultsKey.successKey: true,
                DefaultsKey.enabledKey: true,
                DefaultsKey.disabledKey: self.userdefaults?.bool(forKey: QrScannerProvider.cameraDisabledKey) ?? true
            ])
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
    
    public func startScanner(currentViewController: UIViewController, startScanCallback:@escaping ([String: Any]) -> Void, scannedCallback:@escaping ([String: Any]) -> Void) {
        let cameraAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
        
        switch cameraAuthorizationStatus {
        case .authorized:
            scanner = QrScannerViewController()
            scanner!.delegate = self
            self.scanner?.modalPresentationStyle = .fullScreen
            currentViewController.present(scanner!, animated: true, completion: nil)
            self.scannedCallback = scannedCallback
            startScanCallback(
                [
                    DefaultsKey.actionKey: Actions.scanQr,
                    DefaultsKey.providerKey: Providers.qr,
                    DefaultsKey.successKey: true,
                ]
            )
        default:
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { granted in
                if granted {
                    DispatchQueue.main.async {
                        self.scanner = QrScannerViewController()
                        self.scanner!.delegate = self
                        self.scanner?.modalPresentationStyle = .fullScreen
                        currentViewController.present(self.scanner!, animated: true, completion: nil)
                        self.scannedCallback = scannedCallback
                        startScanCallback(
                            [
                                DefaultsKey.actionKey: Actions.scanQr,
                                DefaultsKey.providerKey: Providers.qr,
                                DefaultsKey.successKey: true,
                            ]
                        )
                    }
                } else {
                    startScanCallback(
                        [
                            DefaultsKey.actionKey: Actions.scanQr,
                            DefaultsKey.providerKey: Providers.qr,
                            DefaultsKey.successKey: false,
                        ]
                    )
                    let alertController = UIAlertController(title: "Camera permission needed", message: "In order to scan QR codes, access to the camera is needed. Would you like to enable it now?", preferredStyle: .alert)
                    alertController.addAction(UIAlertAction(title: "Yes", style: .default, handler: {alertAction in
                        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
                            return
                        }
                        
                        if UIApplication.shared.canOpenURL(settingsUrl) {
                            UIApplication.shared.open(settingsUrl, completionHandler: nil)
                        }
                    }))
                    alertController.addAction(UIAlertAction(title: "No", style: .cancel, handler: nil))
                    DispatchQueue.main.async {
                        currentViewController.present(alertController, animated: true, completion: nil)
                    }
                }
            })
        }
    }
}

extension QrScannerProvider: QrScannerDelegate {
    
    public func codeScanned(_ codeContents: String?) {
        scanner?.dismiss(animated: true) {
            self.scannedCallback?(
                [
                    DefaultsKey.actionKey: Actions.scanResult,
                    DefaultsKey.providerKey: Providers.qr,
                    DefaultsKey.dataKey: ["result": codeContents ?? "CANCELED"]
                ]
            )
            self.scannedCallback = nil
        }
        scanner = nil
    }
}
