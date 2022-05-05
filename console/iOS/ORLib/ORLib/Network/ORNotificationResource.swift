//
//  ORNotificationResource.swift
//  ORLib
//
//  Created by Michael Rademaker on 08/08/2018.
//

import UIKit

public class ORNotificationResource: NSObject, URLSessionDelegate {

    public static let sharedInstance = ORNotificationResource()
    
    private override init() {
        super.init()
    }

    public func notificationDelivered(notificationId : Int64, targetId : String) {
        if let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let host = userdefaults.string(forKey: DefaultsKey.hostKey),
           let realm = userdefaults.string(forKey: DefaultsKey.realmKey) {
            let url = host.appending("/api/\(realm)")
            guard let urlRequest = URL(string: "\(url)/notification/\(notificationId)/delivered?targetId=\(targetId)") else { return }
            let request = NSMutableURLRequest(url: urlRequest)
            request.httpMethod = "PUT"
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue : nil)
            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                DispatchQueue.main.async {
                    if (error != nil) {
                        NSLog("error %@", (error! as NSError).localizedDescription)
                        let error = NSError(domain: "", code: 0, userInfo:  [
                            NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                        ])
                        print(error)
                    }
                }
            })
            reqDataTask.resume()
        }
    }

    public func notificationAcknowledged(notificationId : Int64, targetId : String, acknowledgement: String) {
        if let userdefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement),
           let host = userdefaults.string(forKey: DefaultsKey.hostKey),
           let realm = userdefaults.string(forKey: DefaultsKey.realmKey) {
            let url = host.appending("/api/\(realm)")
            guard let urlRequest = URL(string: "\(url)/notification/\(notificationId)/acknowledged?targetId=\(targetId)") else { return }
            let request = NSMutableURLRequest(url: urlRequest)
            request.httpMethod = "PUT"

            if let json = try? JSONSerialization.data(withJSONObject: ["acknowledgement": acknowledgement], options: []) {
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = json
            }
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue : nil)
            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                DispatchQueue.main.async {
                    if (error != nil) {
                        NSLog("error %@", (error! as NSError).localizedDescription)
                        let error = NSError(domain: "", code: 0, userInfo:  [
                            NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                        ])
                        print(error)
                    }
                }
            })
            reqDataTask.resume()
        }
    }
}
