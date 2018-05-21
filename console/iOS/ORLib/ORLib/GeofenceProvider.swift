//
//  GeofenceProvider.swift
//  ORLib
//
//  Created by Michael Rademaker on 17/05/2018.
//  Copyright © 2018 Open Remote Inc. All rights reserved.
//

import UIKit
import CoreLocation

class GeofenceProvider: NSObject {

    let providerKey = "geofence"
    let locationManager = CLLocationManager()

    public var consoleId: String

    public func checkPermission() -> Bool {
        if CLLocationManager.locationServicesEnabled() {
            if CLLocationManager.authorizationStatus() == .authorizedAlways{
                return true
            }
        }
        return false
    }

    public func clearAllRegions() {
        for region in locationManager.monitoredRegions {
            locationManager.stopMonitoring(for: region)
        }
    }

    public func registerPermissions() {
        locationManager.requestAlwaysAuthorization()
    }

    public func addGeofence(geofence: GeofenceDefinition) {
        locationManager.startMonitoring(for: geofence)
    }

    public func removeGeofence(id: String) {
        for region in locationManager.monitoredRegions {
            guard let circularRegion = region as? GeofenceDefinition, circularRegion.identifier == id else {continue}
            locationManager.stopMonitoring(for: circularRegion)
            break
        }
    }

    public func enable(callback: @escaping (AccesTokenResult<String>) -> ()) {
        guard let tkurlRequest = URL(string: String(format:"\(ORServer.scheme)://%@/auth/realms/%@/protocol/openid-connect/token", ORServer.hostURL, ORServer.realm)) else { return }
        let tkRequest = NSMutableURLRequest(url: tkurlRequest)
        tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
        tkRequest.httpMethod = "POST"

        let postData = [
            "action":"PROVIDER_ENABLE",
            "provider": providerKey,
            "data": [
                "baseUrl":String(format:"\(ORServer.scheme)://%@/", ORServer.hostURL),
                "consoleId": consoleId
            ]
        ]

        tkRequest.httpBody = JSONSerialization.data(withJSONObject: postData, options: [])
        let sessionConfiguration = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
        let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
            if (data != nil){
                
            } else {
                NSLog("error %@", (error! as NSError).localizedDescription)
                let error = NSError(domain: "", code: 0, userInfo:  [
                    NSLocalizedDescriptionKey :  NSLocalizedString("NoDataReceived", value: "Did not receive any data", comment: "")
                    ])
                callback(AccesTokenResult.Failure(error))
            }
        })
        req.resume()
    }

    class GeofenceDefinition: CLCircularRegion {
        public let id: String
        public let lat: double
        public let lng: double
        public let radius: int
        public let postUrl: String
    }
}

extension GeofenceProvider: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        guard let geoDefinition = region as? GeofenceDefinition else {
            return
        }


    }
}
