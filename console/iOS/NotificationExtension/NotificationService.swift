//
//  NotificationService.swift
//  NotificationExtension
//
//  Created by William Balcaen on 16/03/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import UserNotifications

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)
        NSLog("NotifExtension Change content : %@ ", bestAttemptContent?.userInfo ?? "")
        if let bestAttemptContent = bestAttemptContent {
            // Call backend to get payload and adapt title, body and actions
            let defaults = UserDefaults(suiteName: AppGroup.entitlement)
            defaults?.synchronize()
            bestAttemptContent.title = "You received an alarm from blok61 :"
            bestAttemptContent.body = "payload received from backend..."
            guard let tkurlRequest = URL(string:String(format: "http://%@:%@/auth/realms/%@/protocol/openid-connect/token",Server.hostURL, Server.port,Server.realm))
                else { return }
            let tkRequest = NSMutableURLRequest(url: tkurlRequest)
            tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
            tkRequest.httpMethod = "POST"
            let postString = String(format: "grant_type=refresh_token&refresh_token=%@&client_id=openremote", defaults!.object(forKey: DefaultsKey.refreshToken) as! CVarArg)
            tkRequest.httpBody = postString.data(using: .utf8)
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration)
            let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
                if (data != nil){
                    do {
                        let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                        if ((jsonDictionnary["access_token"]) != nil) {
                            guard let urlRequest = URL(string: Server.apiTestResource) else { return }
                            let request = NSMutableURLRequest(url: urlRequest)
                            request.addValue(String(format:"Bearer %@", jsonDictionnary["access_token"] as! String), forHTTPHeaderField: "Authorization")
                            let sessionConfiguration = URLSessionConfiguration.default
                            let session = URLSession(configuration: sessionConfiguration)
                            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                                DispatchQueue.main.async {
                                    if (error != nil) {
                                        bestAttemptContent.body = error.debugDescription
                                    } else {
                                        bestAttemptContent.body = String(data: data!, encoding: .utf8)!
                                        contentHandler(bestAttemptContent)
                                    }
                                }
                            })
                            reqDataTask.resume()
                        } else {
                            if let httpResponse = response as? HTTPURLResponse {
                                let error = NSError(domain: "", code: httpResponse.statusCode, userInfo: jsonDictionnary)
                                bestAttemptContent.body = error.debugDescription
                                contentHandler(bestAttemptContent)
                            } else {
                                let error = NSError(domain: "", code: 0, userInfo: jsonDictionnary)
                                bestAttemptContent.body = error.debugDescription
                                contentHandler(bestAttemptContent)
                            }
                        }
                    }
                    catch let error as NSError {
                        bestAttemptContent.body = error.debugDescription
                        contentHandler(bestAttemptContent)
                    }
                } else {
                    bestAttemptContent.body = error.debugDescription
                    contentHandler(bestAttemptContent)
                }
            })
            req.resume()

        }
    }
    
    override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
        NSLog("NotifExtension Time has expired")
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            bestAttemptContent.title = "You received an alarm from blok61 :"
            bestAttemptContent.body = "Please open application to check what's happening"
            contentHandler(bestAttemptContent)
        }
    }
}
