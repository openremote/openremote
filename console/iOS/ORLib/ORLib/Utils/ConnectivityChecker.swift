//
//  ConnectivityChecker.swift
//  ORLib
//
//  Created by Michael Rademaker on 12/02/2024.
//

import Network

protocol ConnectivityDelegate: AnyObject {
    func connectivityStatusDidChange(isConnected: Bool)
}

class ConnectivityChecker {
    private let monitor: NWPathMonitor
    private let queue: DispatchQueue
    weak var delegate: ConnectivityDelegate?
    
    init() {
        monitor = NWPathMonitor()
        queue = DispatchQueue(label: "NetworkMonitor")
    }
    
    
    func hasInternet() -> Bool {
        return monitor.currentPath.status == .satisfied
    }
    
    func startMonitoring() {
        monitor.start(queue: queue)
        monitor.pathUpdateHandler = { [weak self] path in
            self?.delegate?.connectivityStatusDidChange(isConnected: path.status == .satisfied)
        }
    }
    
    func stopMonitoring() {
        monitor.cancel()
    }
}
