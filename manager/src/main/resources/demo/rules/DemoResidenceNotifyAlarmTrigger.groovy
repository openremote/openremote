package demo.rules

import groovy.transform.ToString
import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.AssetQuery
import org.openremote.model.notification.AlertNotification
import org.openremote.model.rules.Users
import org.openremote.model.user.UserQuery
import org.openremote.model.value.Values

import java.util.logging.Logger

import static org.openremote.model.asset.AssetType.RESIDENCE
import static org.openremote.model.asset.AssetType.ROOM

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users

@ToString(includeNames = true)
class AlarmTrigger {
    String residenceId
    String residenceName
    String roomName
}

/**
 * When this temporary fact is present, don't alert users again.
 */
@ToString(includeNames = true)
class AlertSilence {
    static String DURATION = "30m"
    String residenceName
    String residenceId
}

rules.add()
        .name("Create alarm trigger when residence alarm is enabled and presence is detected in any room")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().type(RESIDENCE).attributeValue("alarmEnabled", true)
            ).filter { residenceWithAlarmEnabled ->
                !facts.matchFirst(AlarmTrigger) { alarmTrigger ->
                    alarmTrigger.residenceId == residenceWithAlarmEnabled.id
                }.isPresent()
            }.map { residenceWithoutAlarmTrigger ->
                // Map to Optional<AssetState> of the "first" room in the residence with presence detected
                facts.matchFirstAssetState(
                        new AssetQuery().type(ROOM)
                                .parent(residenceWithoutAlarmTrigger.id)
                                .attributeValue("presenceDetected", true)
                )
            }.filter {
                it.isPresent()
            }.map {
                it.get()
            }.findFirst().map { roomWithPresence ->
                facts.bind("residenceId", roomWithPresence.parentId)
                        .bind("residenceName", roomWithPresence.parentName)
                        .bind("roomName", roomWithPresence.name)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AlarmTrigger alarmTrigger = new AlarmTrigger(
                    residenceId: facts.bound("residenceId"),
                    residenceName: facts.bound("residenceName"),
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
                        new AssetQuery().type(ROOM)
                                .parent(alarmTrigger.residenceId)
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
                        new AssetQuery().type(RESIDENCE)
                                .id(alarmTrigger.residenceId)
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
                    new AssetQuery().type(RESIDENCE).attributeValue("alarmEnabled", true)
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

            AlertNotification alert = new AlertNotification(
                    title: "Apartment Alarm",
                    message: "Aanwezigheid in " + alarmTrigger.roomName + " (" + facts.clock.time + ")."
            )
            alert.addLinkAction("Details", "#security")
            alert.addActuatorAction("Alarm uit", alarmTrigger.residenceId, "alarmEnabled", Values.create(false).toJson())

            // This only includes users which have a device notification token (registered console device)!
            List<String> userIds = users
                    .query()
                    .asset(new UserQuery.AssetPredicate(alarmTrigger.residenceId))
                    .getResults()

            LOG.info("Alerting users of " + alarmTrigger + ": " + userIds)
            userIds.forEach({ userId ->
                users.storeAndNotify(userId, alert)
            })

            AlertSilence alertSilence = new AlertSilence(
                    residenceName: alarmTrigger.residenceName,
                    residenceId: alarmTrigger.residenceId
            )
            facts.putTemporary(AlertSilence.DURATION, alertSilence)
        })

rules.add()
        .name("Remove alert silence when residence alarm is disabled")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().type(RESIDENCE).attributeValue("alarmEnabled", false)
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
