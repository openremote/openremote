import UIKit
import CoreLocation

public class GeofenceProvider: NSObject, URLSessionDelegate {

    public static let baseUrlKey = "baseUrl"
    public static let consoleIdKey = "consoleId"
    public static let geoPostUrlsKey = "geoPostUrls"
    public static let geoDisabledKey = "geoDisabled"

    let version = "ORConsole"
    let geofenceFetchEndpoint = "rules/geofences/"
    let locationManager = CLLocationManager()
    let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement)
    var geoPostUrls = [String:[String]]()
    var enableCallback : (([String: Any]) -> (Void))?
    var getLocationCallback : (([String: Any]) -> (Void))?

    public var baseURL: String = ""
    public var consoleId: String = ""

    private var enteredLocation: (String, [String : Any]?)? = nil
    private var exitedLocation: (String, [String : Any]?)? = nil
    private var sendQueued = false

    public override init() {
        super.init()
        locationManager.delegate = self
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        self.baseURL = userdefaults?.string(forKey: GeofenceProvider.baseUrlKey) ?? ""
        self.consoleId = userdefaults?.string(forKey: GeofenceProvider.consoleIdKey) ?? ""
        self.geoPostUrls = userdefaults?.value(forKey: GeofenceProvider.geoPostUrlsKey) as? [String:[String]] ?? [String:[String]]()
    }

    public func initialize() -> [String: Any] {
        return [
            DefaultsKey.actionKey: Actions.providerInit,
            DefaultsKey.providerKey: Providers.geofence,
            DefaultsKey.versionKey: version,
            DefaultsKey.requiresPermissionKey: true,
            DefaultsKey.hasPermissionKey: checkPermission(),
            DefaultsKey.successKey: true,
            DefaultsKey.enabledKey: false,
            DefaultsKey.disabledKey: userdefaults?.bool(forKey: GeofenceProvider.geoDisabledKey) ?? false
        ]
    }

    private func checkPermission() -> Bool {
        if CLLocationManager.locationServicesEnabled() {
            if CLLocationManager.authorizationStatus() == .authorizedAlways {
                return true
            }
        }
        return false
    }

    private func registerPermissions() {
        locationManager.requestAlwaysAuthorization()
    }

    public func addGeofence(geofence: GeofenceDefinition) {
        let region = CLCircularRegion(center: CLLocationCoordinate2DMake(geofence.lat, geofence.lng), radius: geofence.radius, identifier:geofence.id)
        locationManager.startMonitoring(for: region)
        geoPostUrls[geofence.id] = [geofence.httpMethod, geofence.url]
        userdefaults?.set(geoPostUrls, forKey: GeofenceProvider.geoPostUrlsKey)
        userdefaults?.synchronize()
    }

    public func removeGeofence(id: String) {
        for region in locationManager.monitoredRegions {
            guard let circularRegion = region as? CLCircularRegion, circularRegion.identifier == id else {continue}
            locationManager.stopMonitoring(for: circularRegion)
            geoPostUrls.removeValue(forKey: circularRegion.identifier)
            userdefaults?.set(geoPostUrls, forKey: GeofenceProvider.geoPostUrlsKey)
            userdefaults?.synchronize()
            break
        }
    }

    public func clearAllRegions() {
        for region in locationManager.monitoredRegions {
            locationManager.stopMonitoring(for: region)
        }
        geoPostUrls = [String:[String]]()
        userdefaults?.set(geoPostUrls, forKey: GeofenceProvider.geoPostUrlsKey)
        userdefaults?.synchronize()
    }

    public func enable(baseUrl: String, consoleId: String, callback:@escaping ([String: Any]) ->(Void)) {
        self.baseURL = baseUrl
        self.consoleId = consoleId
        userdefaults?.set(self.baseURL, forKey: GeofenceProvider.baseUrlKey)
        userdefaults?.set(self.consoleId, forKey: GeofenceProvider.consoleIdKey)
        userdefaults?.removeObject(forKey: GeofenceProvider.geoDisabledKey)
        userdefaults?.synchronize()
        enableCallback = callback
        if checkPermission() {
            self.locationManager.startMonitoringSignificantLocationChanges()
            enableCallback?([
                DefaultsKey.actionKey: Actions.providerEnable,
                DefaultsKey.providerKey: Providers.geofence,
                DefaultsKey.hasPermissionKey: checkPermission(),
                DefaultsKey.successKey: true
                ])
        } else {
            if CLLocationManager.authorizationStatus() == .notDetermined {
                registerPermissions()
            } else {
                enableCallback?([
                    DefaultsKey.actionKey: Actions.providerEnable,
                    DefaultsKey.providerKey: Providers.geofence,
                    DefaultsKey.hasPermissionKey: false,
                    DefaultsKey.successKey: true
                    ])
            }
        }
    }

    public func disable()-> [String: Any] {
        locationManager.stopMonitoringSignificantLocationChanges()
        userdefaults?.set(true, forKey: GeofenceProvider.geoDisabledKey)
        userdefaults?.removeObject(forKey: GeofenceProvider.baseUrlKey)
        userdefaults?.removeObject(forKey: GeofenceProvider.consoleIdKey)
        userdefaults?.synchronize()
        return [
            DefaultsKey.actionKey: Actions.providerDisable,
            DefaultsKey.providerKey: Providers.geofence
        ]
    }

    public func getLocation(callback:@escaping ([String: Any]) -> (Void)) {
        if CLLocationManager.authorizationStatus() == .denied {
            callback([
                DefaultsKey.actionKey: Actions.getLocation,
                DefaultsKey.providerKey: Providers.geofence,
                DefaultsKey.dataKey: nil
            ])
        } else {
            getLocationCallback = callback
            locationManager.startUpdatingLocation()
        }
    }

    public func refreshGeofences() {
        fetchGeofences()
    }

    func fetchGeofences(callback: (([GeofenceDefinition]) -> ())? = nil)  {
        if let tkurlRequest = URL(string: "\(baseURL)/\(geofenceFetchEndpoint)\(consoleId)") {
            let tkRequest = NSMutableURLRequest(url: tkurlRequest)
            tkRequest.httpMethod = "GET"
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
            let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
                if (data != nil){
                    guard let geofences = try? JSONDecoder().decode([GeofenceDefinition].self, from: data!) else {
                        callback?([])
                        return
                    }
                    self.clearAllRegions()
                    NSLog("%@", "Geofences count: \(geofences.count)")
                    print("Geofences count: \(geofences.count)")
                    for geofence in geofences {
                        self.addGeofence(geofence: geofence)
                    }
                    callback?(geofences)
                } else {
                    NSLog("error %@", (error! as NSError).localizedDescription)
                    let error = NSError(domain: "", code: 0, userInfo:  [
                        NSLocalizedDescriptionKey :  NSLocalizedString("NoDataReceived", value: "Did not receive any data", comment: "")
                        ])
                    NSLog("%@", error)
                    callback?([])
                }
            })
            req.resume()
        } else {
            callback?([])
        }
    }

    private func queueSendLocation(geofenceId: String, data:[String: Any]?) {
        synced(lock: self) {
            if let locationData = data {
                enteredLocation = (geofenceId, locationData)
            } else {
                exitedLocation = (geofenceId, nil)

                if enteredLocation?.0 == geofenceId {
                    enteredLocation = nil
                }
            }

            if !sendQueued {
                sendQueued = true
                DispatchQueue.global().asyncAfter(deadline: .now() + .seconds(2)) {
                    self.doSendLocation()
                }
            }
        }
    }

    private func doSendLocation() {
        synced(lock: self) {
            var success = false

            if exitedLocation != nil {
                if sendLocation(geofenceId: exitedLocation!.0, data: exitedLocation!.1) {
                    exitedLocation = nil
                    success = true
                }
            } else if enteredLocation != nil {
                if sendLocation(geofenceId: enteredLocation!.0, data: enteredLocation!.1) {
                    enteredLocation = nil
                    success = true
                }
            }

            if exitedLocation != nil || enteredLocation != nil {
                // Schedule another send
                let delay = success ? 5 : 10
                DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(delay)) {
                    self.doSendLocation()
                }
            } else {
                sendQueued = false
            }
        }
    }

    private func sendLocation(geofenceId: String, data: [String : Any]?)-> Bool {
        return synced(lock: self) {
            var succes = false

            guard let urlData = geoPostUrls[geofenceId] else {
                return succes
            }
            guard let url = URL(string: "\(baseURL)\(urlData[1])") else { return succes}

            let request = NSMutableURLRequest(url: url)
            request.addValue("application/json", forHTTPHeaderField:"Content-Type");
            request.httpMethod = urlData[0]

            if (data != nil) {
                if let postBody = try? JSONSerialization.data(withJSONObject: data!, options: []) {
                    request.httpBody = postBody
                }
            } else {
                request.httpBody = "null".data(using: .utf8)
            }

            let semaphore = DispatchSemaphore(value: 0)
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
            let req = session.dataTask(with: request as URLRequest, completionHandler: { (responseData, response, error) in
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
                    NSLog("%@", "sendLocation succeded with data: \(String(describing: data))")
                    succes = true
                } else {
                    NSLog("%@", "sendLocation failed")
                    if let err = error as NSError? {
                        NSLog("%@", err)
                    }
                }
                semaphore.signal()
            })
            req.resume()
            semaphore.wait()
            return succes
        }
    }

    private func synced<T>(lock: Any, closure: () throws -> T) rethrows -> T {
        objc_sync_enter(lock)
        defer {
            objc_sync_exit(lock)
        }
        
        return try closure()
    }

    public class GeofenceDefinition: NSObject, Decodable {
        public var id: String = ""
        public var lat: Double = 0.0
        public var lng: Double = 0.0
        public var radius: Double = 0.0
        public var httpMethod: String = ""
        public var url: String = ""
    }
}

extension GeofenceProvider: CLLocationManagerDelegate {
    public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        NSLog("%@", "Entered geofence with id: \(region.identifier)")

        guard let circularRegion = region as? CLCircularRegion else {return}

        let postData = [
            "type": "Point",
            "coordinates": [circularRegion.center.longitude, circularRegion.center.latitude]
            ] as [String : Any]

        queueSendLocation(geofenceId: circularRegion.identifier, data: postData)
    }

    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        NSLog("%@", "Exited geofence with id: \(region.identifier)")

        guard let circularRegion = region as? CLCircularRegion else {return}

        queueSendLocation(geofenceId: circularRegion.identifier, data: nil)
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status != .notDetermined {
            enableCallback?([
                DefaultsKey.actionKey: Actions.providerEnable,
                DefaultsKey.providerKey: Providers.geofence,
                DefaultsKey.hasPermissionKey: checkPermission(),
                DefaultsKey.successKey: true
                ])
        } else if status == .authorizedAlways {
            self.locationManager.startMonitoringSignificantLocationChanges()
        }
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let lastLocation = locations.last {
            getLocationCallback?([
                DefaultsKey.actionKey: Actions.getLocation,
                DefaultsKey.providerKey: Providers.geofence,
                DefaultsKey.dataKey: ["latitude": lastLocation.coordinate.latitude, "longitude": lastLocation.coordinate.longitude]
                ])
            locationManager.stopUpdatingLocation()
        }
    }

    public func startMonitoringSignificantLocationChanges() {
        locationManager.startMonitoringSignificantLocationChanges()
    }
}
