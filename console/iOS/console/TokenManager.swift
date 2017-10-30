/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import Foundation
import UIKit
import WebKit

class TokenManager:NSObject, URLSessionDelegate {
    
    var offlineToken : String?
    var refreshToken : String?
    var idToken : String?
    var deviceId : String?
    var hasToken: Bool
    
    static let sharedInstance = TokenManager()
    
    override init() {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.synchronize()
        if defaults?.value(forKey: DefaultsKey.token) != nil
            && defaults?.value(forKey: DefaultsKey.refreshToken) != nil
            && defaults?.value(forKey: DefaultsKey.idToken) != nil {
            hasToken = true
            offlineToken = defaults?.value(forKey: DefaultsKey.token) as? String
            refreshToken = defaults?.value(forKey: DefaultsKey.refreshToken) as? String
            idToken = defaults?.value(forKey: DefaultsKey.idToken) as? String
        } else {
            hasToken = false
            offlineToken = nil
            refreshToken = nil
            idToken = nil
        }
        deviceId = defaults?.value(forKey: DefaultsKey.deviceId) as? String
        super.init()
    }
    
    func storeTokens(tokenJsonDictionnary : [String : String]?) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        
        if (tokenJsonDictionnary?["token"] != nil
            && tokenJsonDictionnary?["refreshToken"] != nil
            && tokenJsonDictionnary?["idToken"] != nil) {
            offlineToken = tokenJsonDictionnary?["token"]! ?? nil
            refreshToken = tokenJsonDictionnary?["refreshToken"]! ?? nil
            idToken = tokenJsonDictionnary?["idToken"]! ?? nil
            defaults?.set(offlineToken, forKey: DefaultsKey.token)
            defaults?.set(refreshToken, forKey: DefaultsKey.refreshToken)
            defaults?.set(idToken, forKey: DefaultsKey.idToken)
            hasToken = true;
            
            // We just got a token, try to send the FCM token, we might not have been able to talk to backend when we got it initialy
            sendDeviceId()
        }
    }
    
    func logout() {
        resetToken()
    }
    
    func resetToken() {
        hasToken = false
        offlineToken = nil
        refreshToken = nil
        idToken = nil
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        defaults?.removeObject(forKey: DefaultsKey.token)
        defaults?.removeObject(forKey: DefaultsKey.refreshToken)
        defaults?.removeObject(forKey: DefaultsKey.idToken)
    }
    
    func resetTokenAndAuthenticate() {
        resetToken()
        //        authenticate()
    }
    
    func storeDeviceId(token : String) {
        let defaults = UserDefaults(suiteName: AppGroup.entitlement)
        deviceId = token
        defaults?.set(token, forKey: DefaultsKey.deviceId)
        sendDeviceId()
    }
    
    func sendDeviceId() {
        if (deviceId != nil && hasToken) {
            UIApplication.shared.isNetworkActivityIndicatorVisible = true
            self.getAccessToken { (accessTokenResult) in
                switch accessTokenResult {
                case .Failure(let error) :
                    UIApplication.shared.isNetworkActivityIndicatorVisible = false
                    ErrorManager.showError(error: error!)
                case .Success(let accessToken) :
                    guard let urlRequest = URL(string: String(Server.registerDeviceResource)) else { return }
                    let request = NSMutableURLRequest(url: urlRequest)
                    request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
                    request.httpMethod = "PUT"
                    let postString = String(format:"token=%@&device_id=%@", self.deviceId!, (UIDevice.current.identifierForVendor?.uuidString)!)
                    request.httpBody = postString.data(using: .utf8)
                    request.addValue(String(format:"Bearer %@", accessToken!), forHTTPHeaderField: "Authorization")
                    let sessionConfiguration = URLSessionConfiguration.default
                    let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
                    let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler: { (data, response, error) in
                        DispatchQueue.main.async {
                            UIApplication.shared.isNetworkActivityIndicatorVisible = false
                            if (error != nil) {
                                NSLog("error %@", (error! as NSError).localizedDescription)
                                let error = NSError(domain: "", code: 0, userInfo:  [
                                    NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                    ])
                                ErrorManager.showError(error: error)
                            } else {
                                if let httpStatus = response as? HTTPURLResponse , httpStatus.statusCode != 204 {
                                    NSLog("Device not registred ",response.debugDescription)
                                    let error = NSError(domain: "", code: 0, userInfo:  [
                                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorSendingDeviceId", value: "Could not register your device for notifications", comment: "")
                                        ])
                                    ErrorManager.showError(error: error)
                                } else {
                                    NSLog("Device registred with token %@",postString)
                                    let notificationName = Notification.Name(NotificationsNames.isdeviceIdSent)
                                    NotificationCenter.default.post(name: notificationName, object: nil)
                                }
                            }
                        }
                    })
                    reqDataTask.resume()
                }
            }
        }
    }
    
    func getAccessToken(callback: @escaping (AccesTokenResult<String>) -> ()) {
        guard let tkurlRequest = URL(string: String(format:"\(Server.scheme)://%@/auth/realms/%@/protocol/openid-connect/token", Server.hostURL, Server.realm)) else { return }
        let tkRequest = NSMutableURLRequest(url: tkurlRequest)
        tkRequest.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
        tkRequest.httpMethod = "POST"
        let postString = String(format:"grant_type=refresh_token&refresh_token=%@&client_id=%@", refreshToken!, Client.clientId)
        
        tkRequest.httpBody = postString.data(using: .utf8)
        let sessionConfiguration = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfiguration, delegate: self, delegateQueue: nil)
        let req = session.dataTask(with: tkRequest as URLRequest, completionHandler: { (data, response, error) in
            if (data != nil){
                do {
                    let jsonDictionnary: Dictionary = try JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                    if ((jsonDictionnary["access_token"]) != nil) {
                        callback(AccesTokenResult.Success(jsonDictionnary["access_token"] as? String))
                    } else {
                        if let httpResponse = response as? HTTPURLResponse {
                            NSLog("error %@", httpResponse)
                            self.resetToken()
                            let error = NSError(domain: "", code: httpResponse.statusCode, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            callback(AccesTokenResult.Failure(error))
                        } else {
                            if (error != nil) {
                                NSLog("error %@", (error! as NSError).localizedDescription as String)
                            }
                            self.resetToken()
                            let error = NSError(domain: "", code: 0, userInfo:  [
                                NSLocalizedDescriptionKey :  NSLocalizedString("ErrorCallingAPI", value: "Could not get data", comment: "")
                                ])
                            callback(AccesTokenResult.Failure(error))
                        }
                    }
                }
                catch let error as NSError {
                    NSLog("error %@", error.localizedDescription)
                    let error = NSError(domain: "", code: 0, userInfo:  [
                        NSLocalizedDescriptionKey :  NSLocalizedString("ErrorDeserializingJSON", value: "Could not convert received data", comment: "")
                        ])
                    callback(AccesTokenResult.Failure(error))
                }
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
    
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == Server.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
        }
    }
}
