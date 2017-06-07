//
//  AppDelegate.swift
//  console
//
//  Created by William Balcaen on 14/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import UIKit
import UserNotifications
import Firebase


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate, URLSessionDelegate {
    
    var window: UIWindow?
    let gcmMessageIDKey = "gcm.message_id"
    var reachabilityAlert : UIAlertController?
    var reachabilityAlertShown = false
    let internetReachability = Reachability.forInternetConnection()
    
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: {_, _ in })
        
        FIRApp.configure()
        
        application.registerForRemoteNotifications()
        NotificationCenter.default.addObserver(self, selector: #selector(self.tokenRefreshNotification), name: NSNotification.Name.firInstanceIDTokenRefresh, object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(self.reachabilityChanged(note:)),
                                               name: NSNotification.Name.reachabilityChanged,
                                               object: internetReachability)
        internetReachability?.startNotifier()
        self.updateReachabilityStatus(reachability: internetReachability!)
        return true
    }
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.internetReachability?.startNotifier()
            self.updateReachabilityStatus(reachability: self.internetReachability!)
        }
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        connectToFcm()
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.internetReachability?.startNotifier()
            self.updateReachabilityStatus(reachability: self.internetReachability!)
        }
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        reachabilityAlert?.dismiss(animated: true, completion: nil)
        reachabilityAlertShown = false
        internetReachability?.stopNotifier()
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        FIRInstanceID.instanceID().setAPNSToken(deviceToken, type: FIRInstanceIDAPNSTokenType.unknown)
        //FIRInstanceID.instanceID().setAPNSToken(deviceToken, type: FIRInstanceIDAPNSTokenType.Prod)
        
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        completionHandler(UIBackgroundFetchResult.noData)
    }
    
    //MARK : - UNUserNotificationCenterDelegate
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .sound])
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        var assetId : String = ""
        var attributeName : String = ""
        var rawJson : String = ""
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        switch response.actionIdentifier {
        case ActionType.ACTION_DEEP_LINK :
            if let urlToOpen = response.notification.request.content.userInfo["appUrl"] { // until now we are considering anchor name (without the #)
                guard let urlRequest = URL(string:String(format: "https://%@/%@%@", Server.hostURL, Server.navigationPath, urlToOpen as! String)) else { return }
                (self.window?.rootViewController as! ViewController).loadUrl(url:urlRequest)
                NSLog("Action asked : %@",response.actionIdentifier)
            }
        case ActionType.ACTION_ACTUATOR :
            NSLog("Action asked : %@",response.actionIdentifier)
            
            if let actions = response.notification.request.content.userInfo["actions"] {
                assetId = (actions as! Dictionary<String,String>)["assetId"]!
                attributeName =  (actions as! Dictionary<String,String>)["attributeName"]!
                rawJson =  (actions as! Dictionary<String,String>)["rawJson"]!
            }
            
            (self.window?.rootViewController as! ViewController).updateAssetAttribute(assetId : assetId, attributeName : attributeName, rawJson : rawJson)
        default : break
        }
        if let alertId = response.notification.request.content.userInfo["alertId"] {

        TokenManager.sharedInstance.getAccessToken { (accessTokenResult) in
            switch accessTokenResult {
            case .Failure(let error) :
                UIApplication.shared.isNetworkActivityIndicatorVisible = false
                ErrorManager.showError(error: error!)
            case .Success(let accessToken) :
                guard let urlRequest = URL(string: String(format:"%@%i", Server.deleteNotifiedAlertResource, alertId as! Int)) else { return }
                let request = NSMutableURLRequest(url: urlRequest)
                request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField:"Content-Type");
                request.httpMethod = "DELETE"
                let postString = String(format:"token=%@&device_id=%@", TokenManager.sharedInstance.deviceId!, (UIDevice.current.identifierForVendor?.uuidString)!)
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
                                let error = NSError(domain: "", code: 0, userInfo:  [
                                    NSLocalizedDescriptionKey :  NSLocalizedString("ErrorSendingDeviceId", value: "Could not delete server alert", comment: "")
                                    ])
                                ErrorManager.showError(error: error)
                            } else {
                                NSLog("Deleted notification alert %i",alertId as! Int)
                            }
                        }
                    }
                })
                reqDataTask.resume()
            }
        
        }
        completionHandler()
        }
    }
    
    
    func application(_ application: UIApplication, open url: URL, sourceApplication: String?, annotation: Any) -> Bool {
            return true
    }
    
    
    func connectToFcm()
    {
        FIRMessaging.messaging().connect { (error) in
            if (error != nil)
            {
                if let token = FIRInstanceID.instanceID().token() {
                    NSLog("Connected to FCM. Token is %@",token as String)
                    TokenManager.sharedInstance.storeDeviceId(token: token)
                }
            }
            else
            {
                if let token = FIRInstanceID.instanceID().token() {
                    NSLog("Connected to FCM. Token is %@",token as String)
                    TokenManager.sharedInstance.storeDeviceId(token: token)
                }
            }
        }
    }
    
    func tokenRefreshNotification(notification: NSNotification)
    {
        // Connect to FCM since connection may have failed when attempted before having a token.
        connectToFcm()
    }
    
    func reachabilityChanged(note: NSNotification) {
        if let reachability = note.object as? Reachability {
            updateReachabilityStatus(reachability: reachability)
        }
    }
    
    private func updateReachabilityStatus(reachability: Reachability) {
        if reachability.currentReachabilityStatus() == NetworkStatus.NotReachable {
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



