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
    var isGrantedNotificationAccess:Bool = false
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        setActions()
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: {_, _ in })
        
        FIRMessaging.messaging().remoteMessageDelegate = self
        
        FIRApp.configure()
        
        application.registerForRemoteNotifications()
        NotificationCenter.default.addObserver(self, selector: #selector(self.tokenRefreshNotification), name: NSNotification.Name.firInstanceIDTokenRefresh, object: nil)
        

        return true
    }
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        connectToFcm()
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        FIRInstanceID.instanceID().setAPNSToken(deviceToken, type: FIRInstanceIDAPNSTokenType.unknown)
        //FIRInstanceID.instanceID().setAPNSToken(deviceToken, type: FIRInstanceIDAPNSTokenType.Prod)
        
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) {
        // Print full message.
        print(userInfo)
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        //self.scheduleLocalNotification()
        print(userInfo)
        completionHandler(UIBackgroundFetchResult.noData)
    }
    
    //MARK : - UNUserNotificationCenterDelegate
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .sound])
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        print("Action asked : ",response.actionIdentifier)
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
                print("Unable to connect with FCM. \(error)")
            }
            else
            {
                if let token = FIRInstanceID.instanceID().token() {
                    print("Connected to FCM. Token is ",token as String)
                } else {
                    print("Connected to FCM. Token is currently nil")
                }
            }
        }
    }
    
    func tokenRefreshNotification(notification: NSNotification)
    {
        if let refreshedToken = FIRInstanceID.instanceID().token()
        {
            print("InstanceID token: \(refreshedToken)")
        }
        // Connect to FCM since connection may have failed when attempted before having a token.
        connectToFcm()
    }
    
    
}

func setActions() {
    let openApp = UNNotificationAction(
        identifier: "openApp",
        title: "Open Application",
        options : [.foreground]
    )
    let dismiss = UNNotificationAction(
        identifier: "dismiss",
        title: "Dismiss",
        options: [.destructive]
    )
    let category = UNNotificationCategory(
        identifier: "blok61Notification",
        actions: [openApp, dismiss],
        intentIdentifiers: []
    )
    
    UNUserNotificationCenter.current().setNotificationCategories([category])
}


extension AppDelegate : FIRMessagingDelegate {
    // Receive data message on iOS 10 devices while app is in the foreground.
    func applicationReceivedRemoteMessage(_ remoteMessage: FIRMessagingRemoteMessage) {
        print(remoteMessage.appData)
    }
}

