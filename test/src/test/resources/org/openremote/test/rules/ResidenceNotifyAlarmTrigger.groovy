package org.openremote.test.rules

import groovy.transform.ToString
import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.notification.Notification
import org.openremote.model.notification.PushNotificationAction
import org.openremote.model.notification.PushNotificationButton
import org.openremote.model.query.AssetQuery
import org.openremote.model.notification.PushNotificationMessage
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.UserAssetPredicate
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger
import java.util.stream.Collectors

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications

@ToString(includeNames = true)
class AlarmTrigger {
    String residenceId
    String roomName
}

/**
 * When this temporary fact is present, don't alert users again.
 */
@ToString(includeNames = true)
class AlertSilence {
    static String DURATION = "PT30M"
    String residenceId
}

rules.add()
        .name("Create alarm trigger when residence alarm is enabled and presence is detected in any room")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset).attributeValue("alarmEnabled", true)
            ).filter { residenceWithAlarmEnabled ->
                !facts.matchFirst(AlarmTrigger) { alarmTrigger ->
                    alarmTrigger.residenceId == residenceWithAlarmEnabled.id
                }.isPresent()
            }.map { residenceWithoutAlarmTrigger ->
                // Map to Optional<AssetState<?>> of the "first" room in the residence with presence detected
                facts.matchFirstAssetState(
                        new AssetQuery().types(RoomAsset)
                                .parents(residenceWithoutAlarmTrigger.id)
                                .attributeValue("presenceDetected", true)
                )
            }.filter {
                it.isPresent()
            }.map {
                it.get()
            }.findFirst().map { roomWithPresence ->
                facts.bind("residenceId", roomWithPresence.parentId)
                        .bind("roomName", roomWithPresence.assetName)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AlarmTrigger alarmTrigger = new AlarmTrigger(
                    residenceId: facts.bound("residenceId"),
                    roomName: facts.bound("roomName")
            )
            LOG.info("Alarm enabled and presence detected in residence, creating: $alarmTrigger")
            facts.put(alarmTrigger)
        })

rules.add()
        .name("Remove alarm trigger when no presence is detected in any room of residence")
        .when(
        { facts ->
            facts.matchFirst(AlarmTrigger) { alarmTrigger ->
                !facts.matchFirstAssetState(
                        new AssetQuery().types(RoomAsset)
                                .parents(alarmTrigger.residenceId)
                                .attributeValue("presenceDetected", true)
                ).isPresent()
            }.map { alarmTrigger ->
                facts.bind("alarmTrigger", alarmTrigger)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AlarmTrigger alarmTrigger = facts.bound("alarmTrigger")
            LOG.info("No presence in any room, removing: " + alarmTrigger)
            facts.remove(alarmTrigger)
        })

rules.add()
        .name("Remove alarm trigger when residence alarm is disabled")
        .when(
        { facts ->
            facts.matchFirst(AlarmTrigger) { alarmTrigger ->
                facts.matchFirstAssetState(
                        new AssetQuery().types(BuildingAsset)
                                .ids(alarmTrigger.residenceId)
                                .attributeValue("alarmEnabled", false)
                ).isPresent()
            }.map { alarmTrigger ->
                facts.bind("alarmTrigger", alarmTrigger)
                true
            }.orElse(false)
        }).then(
        { facts ->
            AlarmTrigger alarmTrigger = facts.bound("alarmTrigger")
            LOG.info("Alarm disabled, removing: " + alarmTrigger)
            facts.remove(alarmTrigger)
        })

rules.add()
        .name("Alert user when alarm has been triggered and not done so already")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset).attributeValue("alarmEnabled", true)
            ).map { residenceWithAlarmEnabled ->
                facts.matchFirst(AlarmTrigger, { alarmTrigger ->
                    alarmTrigger.residenceId == residenceWithAlarmEnabled.id
                })
            }.filter { alarmTrigger ->
                // there is an alarm trigger and alerts are not silenced for this residence
                alarmTrigger.isPresent() &&
                        !facts.matchFirst(AlertSilence, { alertSilence ->
                            alertSilence.residenceId == alarmTrigger.get().residenceId
                        }).isPresent()
            }.map {
                it.get()
            }.findFirst().map { alarmTrigger ->
                facts.bind("alarmTrigger", alarmTrigger)
                true
            }.orElse(false)
        }).then(
        { facts ->
            AlarmTrigger alarmTrigger = facts.bound("alarmTrigger")

            // Get users linked to this residence
            UserQuery query = new UserQuery()
                    .asset(new UserAssetPredicate(alarmTrigger.residenceId))

            Collection<String> userIds = users.getResults(query).collect(Collectors.toList())

            LOG.info("Alerting users of " + alarmTrigger + ": " + userIds)

            List<Notification.Target> targets = userIds.stream().map{new Notification.Target(Notification.TargetType.USER, it)}.collect(Collectors.toList())
            Notification notification = new Notification(
                    "ApartmentAlarm",
                    new PushNotificationMessage()
                            .setTitle("Apartment Alarm")
                            .setBody("Aanwezigheid in " + alarmTrigger.roomName + " (" + facts.clock.time + ").")
                            .setButtons([
                            new PushNotificationButton("Details", new PushNotificationAction("#security")),
                            new PushNotificationButton("Alarm uit", PushNotificationAction.writeAttributeValueAction(new AttributeRef(alarmTrigger.residenceId, "alarmEnabled"), false))
                    ]),
                    targets, null, null
            )

            // Send the notification to all matched users
            notifications.send(notification)

            AlertSilence alertSilence = new AlertSilence(
                    residenceId: alarmTrigger.residenceId
            )
            facts.putTemporary(AlertSilence.DURATION, alertSilence)
        })

rules.add()
        .name("Remove alert silence when residence alarm is disabled")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset).attributeValue("alarmEnabled", false)
            ).map { residenceWithAlarmEnabled ->
                facts.matchFirst(AlertSilence) { alertSilence ->
                    alertSilence.residenceId == residenceWithAlarmEnabled.id
                }
            }.filter {
                it.isPresent()
            }.map {
                it.get()
            }.findFirst().map { alertSilence ->
                facts.bind("alertSilence", alertSilence)
                true
            }.orElse(false)
        }).then(
        { facts ->
            AlertSilence alertSilence = facts.bound("alertSilence")
            LOG.info("Alarm disabled, removing: " + alertSilence)
            facts.remove(alertSilence)
        })
