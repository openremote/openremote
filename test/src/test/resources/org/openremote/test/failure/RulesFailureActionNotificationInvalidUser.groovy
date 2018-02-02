package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.notification.AlertNotification
import org.openremote.model.rules.Users

RulesBuilder rules = binding.rules
Users users = binding.users

rules.add()
        .name("Notify non-existent user")
        .when(
        { facts ->
            true
        })
        .then(
        { facts ->
            users.storeAndNotify("doesnotexist", new AlertNotification("Foo", "Bar"))
        })
