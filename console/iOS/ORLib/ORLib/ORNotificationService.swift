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

import UserNotifications

open class ORNotificationService: UNNotificationServiceExtension, URLSessionDelegate {

    public var contentHandler: ((UNNotificationContent) -> Void)?
    public var bestAttemptContent: UNMutableNotificationContent?

    open override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)
        print("NotifExtension Change content : %@ ", bestAttemptContent?.userInfo ?? "")
        if let bestAttemptContent = bestAttemptContent {
            print(bestAttemptContent.userInfo)
            if let notificationIdString = bestAttemptContent.userInfo[ActionType.notificationId] as? String, let notificationId = Int64(notificationIdString) {
                if let consoleId = UserDefaults.standard.string(forKey: GeofenceProvider.consoleIdKey) {
                    ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notificationId, targetId: consoleId)
                }
            }

            //Actions
            if let actionString = bestAttemptContent.userInfo[DefaultsKey.actionKey] as? String {
                if let actionsData = actionString.data(using: .utf8){
                    if let action = try? JSONDecoder().decode(ORPushNotificationAction.self, from: actionsData) {
                        let categoryName = "openremoteNotification"
                        bestAttemptContent.categoryIdentifier = categoryName

                        bestAttemptContent.userInfo[ActionType.appUrl] = action.url
                        bestAttemptContent.userInfo[ActionType.silent] = action.silent
                        bestAttemptContent.userInfo[ActionType.openInBrowser] = action.openInBrowser
                        bestAttemptContent.userInfo[ActionType.httpMethod] = action.httpMethod ?? "GET"
                        bestAttemptContent.userInfo[DefaultsKey.dataKey] = action.data ?? "null"

                        let category = UNNotificationCategory(identifier: categoryName, actions: [], intentIdentifiers: [], options: [])
                        let categories : Set = [category]
                        UNUserNotificationCenter.current().setNotificationCategories(categories)
                    }
                }
                contentHandler(bestAttemptContent)
            }

            //Buttons
            if let buttonsString = bestAttemptContent.userInfo[DefaultsKey.buttonsKey] as? String {
                if let buttonsData = buttonsString.data(using: .utf8) {
                    if let buttons = try? JSONDecoder().decode([ORPushNotificationButton].self, from: buttonsData) {

                        let categoryName = "openremoteNotification"
                        bestAttemptContent.categoryIdentifier = categoryName
                        var notificationActions = [UNNotificationAction]()

                        for button in buttons {
                            if let action = button.action {
                                bestAttemptContent.userInfo[ActionType.appUrl] = action.url
                                bestAttemptContent.userInfo[ActionType.silent] = action.silent
                                bestAttemptContent.userInfo[ActionType.openInBrowser] = action.openInBrowser
                                bestAttemptContent.userInfo[ActionType.httpMethod] = action.httpMethod ?? "GET"
                                bestAttemptContent.userInfo[DefaultsKey.dataKey] = action.data ?? "null"
                                notificationActions.append(UNNotificationAction(identifier: "openURLAction", title: button.title, options: UNNotificationActionOptions.foreground))
                            } else {
                                notificationActions.append(UNNotificationAction(identifier: "declineAction", title: button.title, options: UNNotificationActionOptions.foreground))
                            }
                        }

                        let category = UNNotificationCategory(identifier: categoryName, actions: notificationActions, intentIdentifiers: [], options: [])
                        let categories : Set = [category]
                        UNUserNotificationCenter.current().setNotificationCategories(categories)
                    }
                }
            }
            contentHandler(bestAttemptContent)
        }
    }


    open override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
        NSLog("NotifExtension Time has expired")
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            bestAttemptContent.title = "You received an alarm from blok61 :"
            bestAttemptContent.body = "Please open application to check what's happening"
            contentHandler(bestAttemptContent)
        }
    }

    open func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            if challenge.protectionSpace.host == ORServer.hostURL  {
                completionHandler(.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
            } else {
                completionHandler(.performDefaultHandling,nil)
            }
        }
    }
}

