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

import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import CoreLocation

@UIApplicationMain
open class ORAppDelegate: UIResponder, UIApplicationDelegate, URLSessionDelegate {

    public var window: UIWindow?
    public let gcmMessageIDKey = "gcm.message_id"
    public var reachabilityAlert : UIAlertController?
    public var reachabilityAlertShown = false
    public let internetReachability = Reachability()

    private var geofenceProvider : GeofenceProvider?

    open func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        // if the app was launched because of geofencing

#if DEBUG
        var paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        let documentsDirectory = paths[0]
        let fileName = "\(Date()).log"
        let logFilePath = (documentsDirectory as NSString).appendingPathComponent(fileName)
        freopen(logFilePath.cString(using: String.Encoding.ascii)!, "a+", stderr)
#endif

        if launchOptions?[UIApplicationLaunchOptionsKey.location] != nil {
            NSLog("%@", "App started from location update")
            // create new GeofenceProvider which creates a CLLocationManager that will receive the location update
            geofenceProvider = GeofenceProvider()
            if CLLocationManager.authorizationStatus() == .authorizedAlways {
                geofenceProvider?.locationManager.startMonitoringSignificantLocationChanges()
            }
        } else {
            FirebaseApp.configure()
            Messaging.messaging().delegate = self

            NotificationCenter.default.addObserver(self,
                                                   selector: #selector(self.reachabilityChanged(note:)),
                                                   name: NSNotification.Name.reachabilityChanged,
                                                   object: internetReachability)
            do {
                try internetReachability?.startNotifier()
                self.updateReachabilityStatus(reachability: internetReachability!)
            } catch {
                NSLog("%@", "Unable to start notifier")
            }
        }
        return true
    }

    open func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }

    open func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }

    open func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            do {
                try self.internetReachability?.startNotifier()
                self.updateReachabilityStatus(reachability: self.internetReachability!)
            } catch {
                NSLog("%@", "Unable to start notifier")
            }
        }
    }

    open func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            do {
                try self.internetReachability?.startNotifier()
                self.updateReachabilityStatus(reachability: self.internetReachability!)
            } catch {
                NSLog("%@", "Unable to start notifier")
            }
        }
    }

    open func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }

    open func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    open func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if let action = userInfo[DefaultsKey.actionKey] as? String {
            if action == Actions.geofenceRefresh {
                (self.window?.rootViewController as! ORViewcontroller).geofenceProvider?.refreshGeofences()
            }
        }
        if let notificationIdString = userInfo[ActionType.notificationId] as? String, let notificationId = Int64(notificationIdString) {
            if let defaults = UserDefaults(suiteName: ORAppGroup.entitlement), let consoleId = defaults.string(forKey: GeofenceProvider.consoleIdKey) {
                ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notificationId, targetId: consoleId)
            }
        }

        completionHandler(UIBackgroundFetchResult.newData)
    }

    open func application(_ application: UIApplication, open url: URL, sourceApplication: String?, annotation: Any) -> Bool {
        return true
    }

    @objc open func reachabilityChanged(note: NSNotification) {
        if let reachability = note.object as? Reachability {
            updateReachabilityStatus(reachability: reachability)
        }
    }

    private func updateReachabilityStatus(reachability: Reachability) {
        if reachability.connection == .none {
            if (!reachabilityAlertShown) {
                let topWindow = UIWindow(frame: UIScreen.main.bounds)
                topWindow.rootViewController = UIViewController()
                topWindow.windowLevel = UIWindowLevelAlert + 1

                reachabilityAlert = UIAlertController(title: "Network Error", message: "Your device seems to be offline", preferredStyle: .alert)
                let reachabilityRetryAction = UIAlertAction(title: "Retry", style: .default, handler: { (action) in
                    self.reachabilityAlertShown = false
                    self.updateReachabilityStatus(reachability: self.internetReachability!)
                    topWindow.isHidden = true
                })
                let reachabilityCancelAction = UIAlertAction(title: "Cancel", style: .cancel, handler: { (action) in
                    self.reachabilityAlertShown = false
                    self.reachabilityAlert?.dismiss(animated: true, completion: nil)
                    topWindow.isHidden = true
                })
                reachabilityAlert?.addAction(reachabilityRetryAction)
                reachabilityAlert?.addAction(reachabilityCancelAction)
                topWindow.makeKeyAndVisible()
                topWindow.rootViewController?.present(reachabilityAlert!, animated: true, completion: nil)
                reachabilityAlertShown = true
            }
        } else {
            if (reachabilityAlertShown) {
                reachabilityAlert?.dismiss(animated: true, completion: nil)
                reachabilityAlertShown = false
            }
        }

    }

    open func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == ORServer.hostURL {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling, nil)
            }
        }
    }
}

extension ORAppDelegate : UNUserNotificationCenterDelegate {
    
    open func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        var notificationId : Int64? = nil

        if let notificationIdString = userInfo[ActionType.notificationId] as? String{
            notificationId = Int64(notificationIdString)
        }
        if let notiId = notificationId, let defaults = UserDefaults(suiteName: ORAppGroup.entitlement), let consoleId = defaults.string(forKey: GeofenceProvider.consoleIdKey) {
            ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notiId, targetId: consoleId)
        }

        completionHandler([.alert, .sound])
    }

    open func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {

        let userInfo = response.notification.request.content.userInfo
        var notificationId : Int64? = nil
        var consoleId : String?

        if let notificationIdString = userInfo[ActionType.notificationId] as? String{
            notificationId = Int64(notificationIdString)
        }

        if let defaults = UserDefaults(suiteName: ORAppGroup.entitlement) {
            consoleId = defaults.string(forKey: GeofenceProvider.consoleIdKey);
        }

        NSLog("%@", "Action chosen: \(response.actionIdentifier)")

        switch response.actionIdentifier {
        case UNNotificationDefaultActionIdentifier:
            if let notiId = notificationId, let conId = consoleId {
                ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notiId, targetId: conId)
            }

            if let urlToOpen = userInfo[ActionType.appUrl] as? String {
                let urlRequest: URL?
                if urlToOpen.hasPrefix("http") || urlToOpen.hasPrefix("https") {
                    urlRequest = URL(string:urlToOpen)
                } else {
                    urlRequest = URL(string:String(format: "%@://%@/%@%@", ORServer.scheme, ORServer.hostURL, ORServer.navigationPath, urlToOpen))
                }
                if let url = urlRequest{
                    if let openInBrowser = userInfo[ActionType.openInBrowser] as? Bool, openInBrowser {
                        NSLog("%@", "Open in browser: \(url)")
                        UIApplication.shared.open(url)
                    } else {
                        NSLog("%@", "Open in app: \(url)")
                        (self.window?.rootViewController as! ORViewcontroller).loadURL(url:url)
                    }
                }
            }
        case UNNotificationDismissActionIdentifier,
             "declineAction":
            if let notiId = notificationId, let conId = consoleId {
                ORNotificationResource.sharedInstance.notificationAcknowledged(notificationId: notiId, targetId: conId, acknowledgement: "")
            }
        default :
            if let notiId = notificationId, let conId = consoleId {
                ORNotificationResource.sharedInstance.notificationAcknowledged(notificationId: notiId, targetId: conId, acknowledgement: response.actionIdentifier)
            }
            if let buttonsString = userInfo[DefaultsKey.buttonsKey] as? String {
                if let buttonsData = buttonsString.data(using: .utf8) {
                    if let buttons = try? JSONDecoder().decode([ORPushNotificationButton].self, from: buttonsData) {
                        for button in buttons {
                            if button.title == response.actionIdentifier {
                                if let action = button.action {
                                    let urlRequest: URL?
                                    if action.url.hasPrefix("http") || action.url.hasPrefix("https") {
                                        urlRequest = URL(string:action.url)
                                    } else {
                                        urlRequest = URL(string:String(format: "%@://%@/%@%@", ORServer.scheme, ORServer.hostURL, ORServer.navigationPath, action.url))
                                    }
                                    if let url = urlRequest {
                                        if action.silent {
                                            let request = NSMutableURLRequest(url: url)
                                            request.httpMethod = action.httpMethod ?? "GET"
                                            if let body = action.data {
                                                request.httpBody = body.data(using: .utf8)
                                                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                                            }
                                            let session = URLSession(configuration: URLSessionConfiguration.default, delegate: nil, delegateQueue : nil)
                                            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler:{ data, response, error in
                                                if (error != nil) {
                                                    NSLog("error %@", (error! as NSError).localizedDescription)
                                                }
                                            })
                                            reqDataTask.resume()
                                        } else if action.openInBrowser {
                                            NSLog("%@", "Open in browser: \(url)")
                                            UIApplication.shared.open(url)
                                        } else {
                                            NSLog("%@", "Open in app: \(url)")
                                            (self.window?.rootViewController as! ORViewcontroller).loadURL(url:url)
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
        completionHandler()
    }
}

extension ORAppDelegate : MessagingDelegate {
    open func messaging(_ messaging: Messaging, didRefreshRegistrationToken fcmToken: String) {
        print("Firebase registration token: \(fcmToken)")
        TokenManager.sharedInstance.storeDeviceId(token: fcmToken)
    }

    open func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String) {
        print("Firebase registration token: \(fcmToken)")
        TokenManager.sharedInstance.storeDeviceId(token: fcmToken)
    }
}

