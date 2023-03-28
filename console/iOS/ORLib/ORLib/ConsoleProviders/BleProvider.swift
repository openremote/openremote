//
//  BleProvider.swift
//  ORLib
//
//  Created by Michael Rademaker on 15/03/2023.
//

import UIKit
import CoreBluetooth

public class BleProvider: NSObject {
    public static let bluetoothDisabledKey = "bluetoothDisabled"
    
    let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
    let version = "ble"
    
    private var centralManager: CBCentralManager?
    private var devices: Set<CBPeripheral> = []
    private var scanDevicesCallback: (([String: Any]) -> (Void))?
    private var scanTimer: Timer?
    private var connectedDevice: CBPeripheral?
    
    var alertBluetoothCallback : (() -> (Void))?
    
    public override init() {
        super.init()
    }
    
    public func initialize() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerInit,
            DefaultsKey.providerKey: Providers.geofence,
            DefaultsKey.versionKey: version,
            DefaultsKey.requiresPermissionKey: true,
            DefaultsKey.hasPermissionKey: CBCentralManager.authorization == .allowedAlways,
            DefaultsKey.successKey: true,
            DefaultsKey.enabledKey: false,
            DefaultsKey.disabledKey: userdefaults?.bool(forKey: BleProvider.bluetoothDisabledKey) ?? false
        ]
    }
    
    public func enable(callback:@escaping ([String: Any]) -> (Void)) {
        userdefaults?.removeObject(forKey: BleProvider.bluetoothDisabledKey)
        userdefaults?.synchronize()
        callback([
            DefaultsKey.actionKey: Actions.providerEnable,
            DefaultsKey.providerKey: Providers.ble,
            DefaultsKey.hasPermissionKey: CBCentralManager.authorization == .allowedAlways,
            DefaultsKey.successKey: true
        ])
    }
    
    public func disable() -> [String: Any] {
        if ((self.centralManager?.isScanning) != nil) {
            self.centralManager?.stopScan()
        }
        scanTimer?.invalidate()
        userdefaults?.set(true, forKey: BleProvider.bluetoothDisabledKey)
        userdefaults?.synchronize()
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.ble
        ]
    }
    
    public func scanForDevices(callback:@escaping ([String: Any]) -> (Void)) {
        if centralManager == nil {
            centralManager = CBCentralManager()
            centralManager!.delegate = self
        }
        self.devices.removeAll()
        scanDevicesCallback = callback
        
        if self.centralManager?.state == .poweredOn {
            self.startScan()
        }
        else if self.centralManager!.state == .poweredOff || self.centralManager!.state == .unauthorized {
            alertBluetoothCallback?()
        }
    }
    
    public func connectoToDevice(deviceId: UUID, callback:@escaping ([String: Any]) -> (Void)) {
        if let device = devices.first(where: {$0.identifier == deviceId}) {
            centralManager?.connect(device)
        }
    }
    
    private func startScan() {
        self.centralManager!.scanForPeripherals(withServices: nil)
        scanTimer = Timer.scheduledTimer(withTimeInterval: 3, repeats: false, block: { timer in
            self.centralManager?.stopScan()
            self.scanDevicesCallback?([
                DefaultsKey.actionKey : Actions.scanBleDevices,
                DefaultsKey.providerKey : Providers.ble,
                DefaultsKey.dataKey : ["devices" : self.devices.map {
                    [
                        "name" : $0.name ?? "Unknown",
                        "address" : $0.identifier.uuidString
                    ]
                }]
            ])
        })
    }
}

extension BleProvider : CBCentralManagerDelegate {
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            self.startScan()
            break
        case .poweredOff:
            if central.isScanning {
                central.stopScan()
            }
            alertBluetoothCallback?()
            break
            // Alert user : turn on Bluetooth
        case .resetting:
            break
            // Wait for next state update and consider logging interruption of Bluetooth service
        case .unauthorized:
            alertBluetoothCallback?()
            break
            // Alert user : enable Bluetooth permission in app Settings
        case .unsupported:
            break
            // Alert user their device does not support Bluetooth and app will not work as expected
        case .unknown:
            break
            // Wait for next state update
        @unknown default:
            break
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        print(advertisementData)
        print("\(peripheral.name ?? "Unknown")")
        devices.insert(peripheral)
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        self.connectedDevice = peripheral
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }
    
    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        
    }
    
    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        
    }
}

extension BleProvider: CBPeripheralDelegate {
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else {
            return
        }
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else {
                return
            }
        for characteristic in characteristics {
            print(characteristic.uuid)
            characteristic.properties
            peripheral.discoverDescriptors(for: characteristic)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverDescriptorsFor characteristic: CBCharacteristic, error: Error?) {
        guard let descriptors = characteristic.descriptors else { return }
         
        
    }
}
