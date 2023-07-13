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
    private var connectToDeviceCallback: (([String: Any]) -> (Void))?
    private var sendToDeviceCallback: (([String: Any]) -> (Void))?
    private var scanTimer: Timer?
    private var connectedDevice: CBPeripheral?
    private var selectedCharacteristic: CBCharacteristic?
    private var deviceServices: [CBService] = []
    private var deviceCharacteristics: [CBCharacteristic] = []
    private var dataToSend: Data?
    private var sendDataIndex = 0
    private var maxDataLength = 182 // according to documentation
    
    var alertBluetoothCallback : (() -> (Void))?
    
    public override init() {
        super.init()
    }
    
    public func initialize() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerInit,
            DefaultsKey.providerKey: Providers.ble,
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
    
    public func connectoToDevice(deviceId: String, callback:@escaping ([String: Any]) -> (Void)) {
        connectToDeviceCallback = callback
        deviceServices.removeAll()
        deviceCharacteristics.removeAll()
        if let device = devices.first(where: {$0.identifier.uuidString == deviceId}) {
            centralManager?.connect(device)
        }
    }
    
    public func sendToDevice(attributeId: String, value: Data, callback:@escaping ([String: Any]) -> (Void)) {
        if let device = self.connectedDevice {
            sendToDeviceCallback = callback
            if let characteristic = deviceCharacteristics.first(where: {$0.uuid.uuidString == attributeId}) {
                self.maxDataLength = self.connectedDevice!.maximumWriteValueLength(for: characteristic.properties.contains(.writeWithoutResponse) ? .withoutResponse : .withResponse)
                selectedCharacteristic = characteristic
                dataToSend = value
                sendDataIndex = 0
                sendData(characteristic: characteristic)
            }
        } else {
            callback([DefaultsKey.actionKey: "SEND_TO_DEVICE", DefaultsKey.providerKey: "ble", DefaultsKey.successKey: false])
        }
    }
    
    private func startScan() {
        self.centralManager!.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
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
        devices.insert(peripheral)
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        self.connectedDevice = peripheral
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }
    
    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        connectToDeviceCallback?([
            DefaultsKey.actionKey : Actions.connectToBleDevice,
            DefaultsKey.providerKey : Providers.ble,
            DefaultsKey.successKey : false
        ])
    }
    
    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        self.connectedDevice = nil
    }
}

extension BleProvider: CBPeripheralDelegate {
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else {
            return
        }
        deviceServices.append(contentsOf: services)
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else {
            return
        }
        if let index = deviceServices.firstIndex(of: service) {
            deviceServices.remove(at: index)
        }
        deviceCharacteristics.append(contentsOf: characteristics)
        
        for characteristic in characteristics.filter({ $0.properties.contains(.read)}) {
            self.connectedDevice?.readValue(for: characteristic)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if deviceServices.isEmpty {
            connectToDeviceCallback?([
                DefaultsKey.actionKey : Actions.connectToBleDevice,
                DefaultsKey.providerKey : Providers.ble,
                DefaultsKey.successKey : true,
                DefaultsKey.dataKey: [
                    "attributes" : deviceCharacteristics.map { characteristic in
                        [
                            "attributeId": characteristic.uuid.uuidString,
                            "isReadable": characteristic.properties.contains(.read),
                            "isWritable": characteristic.properties.contains(.write) || characteristic.properties.contains(.writeWithoutResponse),
                            "value": characteristic.value != nil ? decodeValue(from: characteristic.value!) : nil
                        ] as [String : Any?]
                    }
                ]
            ])
        }
    }
    
    public func peripheralIsReady(toSendWriteWithoutResponse peripheral: CBPeripheral) {
        if let characteristic = selectedCharacteristic {
            if let data = dataToSend {
                if sendDataIndex < data.count {
                    sendData(characteristic: characteristic)
                }
            }
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        if let err = error {
            sendToDeviceCallback?([DefaultsKey.actionKey: "SEND_TO_DEVICE", DefaultsKey.providerKey: "ble", DefaultsKey.successKey: false])
        } else {
            if let data = dataToSend {
                if sendDataIndex >= data.count {
                    sendToDeviceCallback?([DefaultsKey.actionKey: "SEND_TO_DEVICE", DefaultsKey.providerKey: "ble", DefaultsKey.successKey: true])
                } else {
                    sendData(characteristic: characteristic)
                }
            }
        }
    }
    
    func sendData(characteristic: CBCharacteristic) {
        if let data = dataToSend {
            if sendDataIndex >= data.count {
                // All data has been sent
                return
            }
            
            var sending = true
            
            while sending {
                var amountToSend = data.count - sendDataIndex
                
                if amountToSend > maxDataLength {
                    amountToSend = maxDataLength
                }
                
                let chunk = data.subdata(in: sendDataIndex..<sendDataIndex+amountToSend)
                self.connectedDevice?.writeValue(chunk, for: characteristic, type: characteristic.properties.contains(.writeWithoutResponse) ? .withoutResponse : .withResponse)
                
                sendDataIndex += amountToSend
                
                if sendDataIndex >= data.count {
                    // All data has been sent
                    sending = false
                }
            }
        }
    }
    
    func decodeValue(from data: Data) -> Any {
       
            if let string = String(data: data, encoding: .utf8) {
                let escapedJsonString = string.replacingOccurrences(of: "\\", with: "\\\\")
                                                   .replacingOccurrences(of: "\"", with: "\\\"")

                return escapedJsonString
            } else {
                return data
            }
       
    }
}
