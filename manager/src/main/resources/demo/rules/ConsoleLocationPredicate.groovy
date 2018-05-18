package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.BaseAssetQuery
import org.openremote.model.notification.AlertNotification

RulesBuilder rules = binding.rules

rules.add()
        .name("Geofence - Airport Schiphol")
        .when(
        { facts ->
            def consoles = facts.matchAssetState(new AssetQuery()
                    .type(AssetType.CONSOLE)
                    .location(new BaseAssetQuery.RadialLocationPredicate(4000, 4.761891d, 52.309197d)))
            .collect()

            consoles.size() > 0
        })
        .then(
        { facts ->

            AlertNotification alert = new AlertNotification(
                    title: "Welcome in The Netherlands",
                    message: "Have a look at the UNaLab program"
            )
            alert.addLinkAction("Yes Please", "https://www.eindhoven.nl/en/unalab-program")
            alert.addLinkAction("Not now", "#")

        })

rules.add()
        .name("Geofence - Eindhoven Airport")
        .when(
        { facts ->
            def consoles = facts.matchFirstAssetState(new AssetQuery()
                    .type(AssetType.CONSOLE)
                    .location(new BaseAssetQuery.RadialLocationPredicate(1000, 5.391885d, 51.458181d)))
            .collect()

            consoles.size() > 0
        })
        .then(
        { facts ->
            AlertNotification alert = new AlertNotification(
                    title: "Welcome in Eindhoven",
                    message: "Have a look at the UNaLab program"
            )
            alert.addLinkAction("Yes Please", "https://www.eindhoven.nl/en/unalab-program")
            alert.addLinkAction("Not now", "#")
        })

rules.add()
        .name("Geofence - van Abbe Museum")
        .when(
        { facts ->
            def consoles = facts.matchFirstAssetState(new AssetQuery()
                    .type(AssetType.CONSOLE)
                    .location(new BaseAssetQuery.RadialLocationPredicate(75, 5.481743d, 51.434402d)))
            .collect()

            consoles.size() > 0
        })
        .then(
        { facts ->
            AlertNotification alert = new AlertNotification(
                    title: "Welcome in UNaLab consortium meeting",
                    message: "Have a look at the program for our meeting in Eindhoven"
            )
            alert.addLinkAction("Yes Please", "https://www.eindhoven.nl/en/unalab-program")
            alert.addLinkAction("Not now", "#")
        })

rules.add()
        .name("Geofence - Keizersgracht")
        .when(
        { facts ->
            def consoles = facts.matchFirstAssetState(new AssetQuery()
                    .type(AssetType.CONSOLE)
                    .location(new BaseAssetQuery.RadialLocationPredicate(100, 5.475926d, 51.437987d)))
            .collect()

            consoles.size() > 0
        })
        .then(
        { facts ->
            AlertNotification alert = new AlertNotification(
                    title: "Hello, you are now at Keizersgracht",
                    message: "How do you like this tour so far?"
            )
            alert.addLinkAction("Rate it", "https://www.eindhoven.nl/en/unalab-survey-2")
            alert.addLinkAction("Not now", "#")
        })

rules.add()
        .name("Geofence - Cafe Centraal")
        .when(
        { facts ->
            def consoles = facts.matchFirstAssetState(new AssetQuery()
                    .type(AssetType.CONSOLE)
                    .location(new BaseAssetQuery.RadialLocationPredicate(100, 5.478801d, 51.439351d)))
                    .collect()

            consoles.size() > 0
        })
        .then(
        { facts ->
            AlertNotification alert = new AlertNotification(
                    title: "Welcome at Cafe Centraal",
                    message: "Now you have visited the NBS sites in Eindhoven, let us know your favorite project."
            )
            alert.addLinkAction("Fill in the survey", "https://www.eindhoven.nl/en/unalab-program#survey")
            alert.addLinkAction("Not now", "#")
        })
