//
//  NotificationService.swift
//  NotificationService
//
//  Created by nuc on 17/06/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UserNotifications
import ORLib

class NotificationService: UNNotificationServiceExtension {
    
    public var contentHandler: ((UNNotificationContent) -> Void)?
    public var bestAttemptContent: UNMutableNotificationContent?

    open override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        if let bestAttemptContent = bestAttemptContent {

            let categoryName = "openremoteNotification"
            bestAttemptContent.categoryIdentifier = categoryName

            //Buttons
            if let buttonsString = bestAttemptContent.userInfo[DefaultsKey.buttonsKey] as? String {
                if let buttonsData = buttonsString.data(using: .utf8) {
                    if let buttons = try? JSONDecoder().decode([ORPushNotificationButton].self, from: buttonsData) {

                        var notificationActions = [UNNotificationAction]()

                        for button in buttons {
                            if button.action != nil {
                                notificationActions.append(UNNotificationAction(identifier: button.title, title: button.title, options: UNNotificationActionOptions.foreground))
                            } else {
                                notificationActions.append(UNNotificationAction(identifier: "declineAction", title: button.title, options: UNNotificationActionOptions.destructive))
                            }
                        }

                        let category = UNNotificationCategory(identifier: categoryName, actions: notificationActions, intentIdentifiers: [], options: [])
                        let categories : Set = [category]
                        UNUserNotificationCenter.current().setNotificationCategories(categories)
                    }
                }
            }
            //Actions
            if let actionString = bestAttemptContent.userInfo[DefaultsKey.actionKey] as? String {
                if let actionsData = actionString.data(using: .utf8){
                    if let action = try? JSONDecoder().decode(ORPushNotificationAction.self, from: actionsData) {

                        bestAttemptContent.userInfo[ActionType.appUrl] = action.url
                        bestAttemptContent.userInfo[ActionType.silent] = action.silent
                        bestAttemptContent.userInfo[ActionType.openInBrowser] = action.openInBrowser
                        bestAttemptContent.userInfo[ActionType.httpMethod] = action.httpMethod ?? "GET"
                        bestAttemptContent.userInfo[DefaultsKey.dataKey] = action.data ?? "null"
                    }
                }
            }

            if let notificationIdString = bestAttemptContent.userInfo[ActionType.notificationId] as? String, let notificationId = Int64(notificationIdString) {
                if let defaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement), let consoleId = defaults.string(forKey: GeofenceProvider.consoleIdKey) {
                    ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notificationId, targetId: consoleId)
                }
            }

            contentHandler(bestAttemptContent.copy() as! UNNotificationContent)
        }
    }


    open override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
        NSLog("NotifExtension Time has expired")
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            //Actions
            if let actionString = bestAttemptContent.userInfo[DefaultsKey.actionKey] as? String {
                if let actionsData = actionString.data(using: .utf8){
                    if let action = try? JSONDecoder().decode(ORPushNotificationAction.self, from: actionsData) {

                        bestAttemptContent.userInfo[ActionType.appUrl] = action.url
                        bestAttemptContent.userInfo[ActionType.silent] = action.silent
                        bestAttemptContent.userInfo[ActionType.openInBrowser] = action.openInBrowser
                        bestAttemptContent.userInfo[ActionType.httpMethod] = action.httpMethod ?? "GET"
                        bestAttemptContent.userInfo[DefaultsKey.dataKey] = action.data ?? "null"
                    }
                }
            }
            contentHandler(bestAttemptContent)
        }
    }
}
