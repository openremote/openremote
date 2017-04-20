//
//  AppDelegate.swift
//  console
//
//  Created by William Balcaen on 14/02/17.
//  Copyright © 2017 TInSys. All rights reserved.
//

import UIKit
import AppAuth
import UserNotifications
import Firebase


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    var window: UIWindow?
    var currentAuthorizationFlow : OIDAuthorizationFlowSession?
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
        
        //FIRMessaging.messaging().remoteMessageDelegate = self
        
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
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) {
        // Print full message.
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        //self.scheduleLocalNotification()
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
        switch response.actionIdentifier {
        case ActionType.ACTION_DEEP_LINK :
            // open url
            let urlToOpen = response.notification.request.content.userInfo["appUrl"] as! String // until now we are considering anchor name (without the #)
            (self.window?.rootViewController as! ViewController).loadUrl(url: URL(string: String(format: "https://%@/%@%@", Server.hostURL, Server.navigationPath,urlToOpen))!)
            NSLog("Action asked : %@",response.actionIdentifier)
        case ActionType.ACTION_ACTUATOR :
            NSLog("Action asked : %@",response.actionIdentifier)
            
            let actions = response.notification.request.content.userInfo["actions"] as! Dictionary<String,String>
            assetId = actions["assetId"]!
            attributeName =  actions["attributeName"]!
            rawJson =  actions["rawJson"]!

            
            (self.window?.rootViewController as! ViewController).updateAssetAttribute(assetId : assetId, attributeName : attributeName, rawJson : rawJson)
        default : break
        }
        completionHandler()
    }
    
    
    func application(_ application: UIApplication, open url: URL, sourceApplication: String?, annotation: Any) -> Bool {
        if (self.currentAuthorizationFlow?.resumeAuthorizationFlow(with: url))! {
            self.currentAuthorizationFlow = nil
            return true
        }
        return false
    }
    
    
    func connectToFcm()
    {
        FIRMessaging.messaging().connect { (error) in
            if (error != nil)
            {
                //NSLog("Unable to connect with FCM. \(error)")
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
                } else {
                //    NSLog("Connected to FCM. Token is currently nil")
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
    
}



