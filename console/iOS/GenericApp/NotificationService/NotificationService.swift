//
//  NotificationService.swift
//  NotificationService
//
//  Created by nuc on 17/06/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import UserNotifications
import ORLib

class NotificationService: ORNotificationService {
    
    override init() {
        ORAppGroup.entitlement = "io.openremote.app"
        super.init()
    }
}
