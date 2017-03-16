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
        print("NotifExtension Change content ",bestAttemptContent?.userInfo ?? "")
        if let bestAttemptContent = bestAttemptContent {
            // Call backend to get payload and adapt title, body and actions
            bestAttemptContent.title = "You received an alarm from blok61 :"
            bestAttemptContent.body = "payload received from backend..."
            guard let tkurlRequest = URL(string: "http://192.168.99.100:8080/auth/realms/blok61/protocol/openid-connect/token") else { return }
            let tkRequest = NSMutableURLRequest(url: tkurlRequest)
            tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
            tkRequest.httpMethod = "POST"
            let postString = "grant_type=refresh_token&refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDZXZKWnZHVmdKUWRyZTlGU1kzelF6NWZDZ080WW5iaHd1Ukg2WW9yVkZnIn0.eyJqdGkiOiIzOGI5YTQxOC1hNTZkLTQ1NDYtOTBkOS02OWY0NWEzZWM3ZGUiLCJleHAiOjAsIm5iZiI6MCwiaWF0IjoxNDg5NjcwNjYzLCJpc3MiOiJodHRwOi8vMTkyLjE2OC45OS4xMDA6ODA4MC9hdXRoL3JlYWxtcy9ibG9rNjEiLCJhdWQiOiJvcGVucmVtb3RlIiwic3ViIjoiZTk2M2Y3ZWYtNWUyMy00Njk1LWE5M2QtOTU3Mjk0ODYwM2MxIiwidHlwIjoiT2ZmbGluZSIsImF6cCI6Im9wZW5yZW1vdGUiLCJub25jZSI6IjQ2Nzg3MThlLTdhMGUtNDliNy1hNDQyLTA1ODk2YmFkY2I0YiIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6ImU5YmJiODlkLTk2ZDUtNDM0Ny1hMzVhLTFmZjM0OTRmNjRlNCIsImNsaWVudF9zZXNzaW9uIjoiMTliODY0NTAtYWEzMS00OGNlLWE4ODEtMzliNjM0NDkzOThjIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJvcGVucmVtb3RlIjp7InJvbGVzIjpbIndyaXRlOmFzc2V0cyIsInJlYWQ6bWFwIiwicmVhZDphc3NldHMiLCJ3cml0ZTp1c2VyIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fX0.APnuPAlcyEnGYlOIX8W4znbJHhc-HyNc8tmKrsqVkkudf0Y3Dspgjjc3z907hBW0K_Lh0CXtWMMulZk-AcbTohNq3pnnohNsGEfXKuqWeIRjJwiArItwo5u5XgTveLWn7fPTMl8IplGqVptUCdBex1QKmMa_PfFLRLb9Xjh4XI5MKGp1qy_vL8NH1xhKud1quyTLivxGrZpsC8Dyln2VUGbD5nFCYlzVv4zqTk0K3laaW52v9y5AAp2iYLgjoxrW-WBDExfTWdz9TQ3t5q35_GnyyzlE-uBYvppKMxZ4Hb0YkAslEvKYgUnplRx9UD9EBw8auG1fRQ4e1C4_4PqoWw&client_id=openremote"
            tkRequest.httpBody = postString.data(using: .utf8)
            let sessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: sessionConfiguration)
            let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
                if (data != nil){
                    do {
                        let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                        if ((jsonDictionnary["access_token"]) != nil) {
                            guard let urlRequest = URL(string: "http://192.168.99.100:8080/blok61/asset") else { return }
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
        print("NotifExtension Time has expired")
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }
}
