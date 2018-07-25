package org.openremote.test.failure

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.notification.Notification
import org.openremote.model.notification.PushNotificationAction
import org.openremote.model.notification.PushNotificationButton
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.query.filter.TenantPredicate
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users
import org.openremote.model.value.Values

RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications

rules.add()
        .name("Notify master user out of scope")
        .when(
        { facts ->
            return !facts.getOptional("fired").isPresent()
        })
        .then(
        { facts ->
            facts.put("fired", true)

            def userQuery = users.query()
            userQuery.tenantPredicate = new TenantPredicate("master")
            def targetUsers = userQuery.getResults()

            Notification notification = new Notification(
                "ApartmentAlarm",
                new PushNotificationMessage()
                    .setTitle("False Alarm")
                    .setBody("This alarm should not be sent"),
                new Notification.Targets(Notification.TargetType.USER, targetUsers)
            )

            // Send the notification to non existent user
            notifications.send(notification)
        })
