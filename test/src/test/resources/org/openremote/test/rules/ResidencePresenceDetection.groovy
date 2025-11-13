/*
Uses 'motionSensor' and optional 'co2Level' of a room asset to set the 'presenceDetected'
and 'lastPresenceDetected' attributes of the room. Presence detection is based on motion
sensing and if available for a room, increasing CO2 level over a time window. The assumption
is that the 'motionSensor' is set to 0 or 1, depending on whether motion has been detected
in a room over a time window that is internal to the sensor (e.g. the last minute). The
'presenceDetected' attribute of the residence is set depending on whether any child room
assets have presence set.
*/
package org.openremote.setup.integration.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.query.AssetQuery
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.util.Pair

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import static org.openremote.model.query.AssetQuery.Operator.GREATER_THAN
import static org.openremote.model.query.AssetQuery.Operator.LESS_THAN

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules

rules.add()
        .name("Set presence detected flag of room when motion is detected and (optional) CO2 increased")
        .when(
        { facts ->
            // Any room where the presence detected flag is not set
            facts.matchAssetState(
                    new AssetQuery().types(RoomAsset).attributeValue("presenceDetected", false)
            ).flatMap { roomWithoutPresence ->
                // and the motion sensor has been triggered
                facts.matchAssetState(
                        new AssetQuery().ids(roomWithoutPresence.id).attributeValue("motionSensor", GREATER_THAN, 0)
                )
            }.findFirst().map { roomWithMotionSensorTriggered ->

                facts.bind("room", roomWithMotionSensorTriggered)
                true

                /* Results in many false negatives when somebody is moving in the room but
                   windows are open, and therefore CO2 is not increasing

                // and there is a CO2 sensor in the room
                facts.matchFirstAssetState(
                        new AssetQuery().id(roomWithMotionSensorTriggered.id).attributeName("co2Level")
                ).map({ co2Level ->
                    // and there are sensor events
                    facts.matchAssetEvent(
                            new AssetQuery().id(roomWithMotionSensorTriggered.id).attributeName("co2Level")
                    ).filter(
                            // in the last 12 minutes
                            facts.clock.last("12m")
                    ).map({ co2LevelFact ->
                        co2LevelFact.fact
                    }
                    ).filter({ coLevel ->
                        // with increasing values
                        coLevel.isValueGreaterThanOldValue()
                    }).count() >= 2 // at least 2
                }).orElse(true) // or we don't have a CO2 sensor
                */

            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState room = facts.bound("room")
            LOG.info("Presence detected in room: " + room.assetName + " [" + room.id + "]")
            facts.updateAssetState(room.id, "presenceDetected", true)
        })

rules.add()
        .name("Clear presence detected flag of room if no motion is detected and (optional) CO2 did not increase")
        .when(
        { facts ->
            // Any room where the presence detected flag is set
            facts.matchAssetState(
                    new AssetQuery().types(RoomAsset).attributeValue("presenceDetected", true)
            ).flatMap { roomWithPresence ->
                // and the motion sensor has not been triggered
                facts.matchAssetState(
                        new AssetQuery().ids(roomWithPresence.id).attributeValue("motionSensor", LESS_THAN, 1)
                )
            }.findFirst().map { roomWithoutMotionSensorTriggered ->

                facts.bind("room", roomWithoutMotionSensorTriggered)

                // and there is a CO2 sensor in the room
                facts.matchFirstAssetState(
                        new AssetQuery().ids(roomWithoutMotionSensorTriggered.id).attributeName("co2Level")
                ).map { co2Level ->
                    // and there are no sensor events
                    facts.matchAssetEvent(
                            new AssetQuery().ids(roomWithoutMotionSensorTriggered.id).attributeName("co2Level")
                    ).filter { coLevelFact ->
                        // with increasing values
                        coLevelFact.getFact().isValueGreaterThanOldValue()
                        // in the last 12 minutes
                    }.filter{facts.clock.now.minus(12, ChronoUnit.MINUTES).toEpochMilli() < it.timestamp}.count() == 0
                }.orElse(true) // or we don't have a CO2 sensor

            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState room = facts.bound("room")
            LOG.info("Presence gone in residence room: " + room.assetName + " [" + room.id + "]")
            facts.updateAssetState(room.id, "presenceDetected", false)
        })

rules.add()
        .name("Update presence detected timestamp of room with last trigger of motion sensor")
        .when(
        { facts ->
            // Any room where the presence detected flag is set
            facts.matchAssetState(
                    new AssetQuery().types(RoomAsset).attributeValue("presenceDetected", true)
            ).flatMap { roomWithPresence ->
                // and the motion sensor has been triggered
                facts.matchAssetState(
                        new AssetQuery().ids(roomWithPresence.id)
                                .attributeValue("motionSensor", GREATER_THAN, 0)
                ).flatMap { roomWithMotionSensorTriggered ->
                    // and the last presence detection timestamp is not set or is older than the motion sensor timestamp
                    facts.matchAssetState(
                            new AssetQuery().ids(roomWithMotionSensorTriggered.id)
                                    .attributeName("lastPresenceDetected")
                    ).filter({
                        !it.value.isPresent() || it.value.timestamp
                    }).map { outdatedLastPresenceDetected ->
                        // keep the room and the new timestamp
                        new Pair<AttributeInfo, Double>(outdatedLastPresenceDetected, roomWithMotionSensorTriggered.timestamp)
                    }
                }
            }.findFirst().map { pair ->
                facts.bind("room", pair.key).bind("lastPresenceTimestamp", pair.value)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState room = facts.bound("room")
            LOG.info("Motion sensor triggered, updating last presence in residence room: " + room.assetName + " [" + room.id + "]")
            facts.updateAssetState(room.id, "lastPresenceDetected", facts.bound("lastPresenceTimestamp") as Double)
        })

rules.add()
        .name("Set presence detected flag of residence if presence is detected in any room")
        .when(
        { facts ->
            // A room where the presence detected flag is set
            facts.matchAssetState(
                    new AssetQuery().types(RoomAsset).attributeValue("presenceDetected", true)
            ).flatMap { roomWithPresence ->
                // and a residence parent where the presence detected flag is not set
                facts.matchAssetState(
                        new AssetQuery()
                                .ids(roomWithPresence.parentId)
                                .attributeValue("presenceDetected", false)
                )
            }.findFirst().map { residenceWithoutPresence ->
                facts.bind("residence", residenceWithoutPresence)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Presence detected in residence: " + residence.assetName + " [" + residence.id + "]")
            facts.updateAssetState(residence.id, "presenceDetected", true)
        })

rules.add()
        .name("Clear presence detected flag of residence when no presence is detected in any room")
        .when(
        { facts ->
            // A residence where the presence detected flag is set
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset).attributeValue("presenceDetected", true)
            ).filter { residenceWithPresence ->
                // and no room child of that residence has presence
                facts.matchAssetState(
                        new AssetQuery().types(RoomAsset).parents(residenceWithPresence.id)
                                .attributeValue("presenceDetected", true)
                ).count() == 0
            }.findFirst().map { residenceWithPresence ->
                facts.bind("residence", residenceWithPresence)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Presence gone in residence: " + residence.assetName + " [" + residence.id + "]")
            facts.updateAssetState(residence.id, "presenceDetected", false)
        })
