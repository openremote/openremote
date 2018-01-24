package demo.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.BaseAssetQuery.Operator
import org.openremote.model.rules.AssetState

import java.util.logging.Logger
import java.util.stream.Stream

import static org.openremote.model.asset.AssetType.RESIDENCE
import static org.openremote.model.asset.AssetType.ROOM
import static org.openremote.model.asset.BaseAssetQuery.Operator.*

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules

final VENTILATION_LEVEL_LOW = 64d
final VENTILATION_LEVEL_MEDIUM = 128d
final VENTILATION_LEVEL_HIGH = 255d

final VENTILATION_THRESHOLD_MEDIUM_CO2_TIME_WINDOW = "15m"
final VENTILATION_THRESHOLD_MEDIUM_CO2_LEVEL = 700d
final VENTILATION_THRESHOLD_MEDIUM_HUMIDITY_TIME_WINDOW = "1m"
final VENTILATION_THRESHOLD_MEDIUM_HUMIDITY = 70d

final VENTILATION_THRESHOLD_HIGH_CO2_TIME_WINDOW = "5m"
final VENTILATION_THRESHOLD_HIGH_CO2_LEVEL = 1000d
final VENTILATION_THRESHOLD_HIGH_HUMIDITY_TIME_WINDOW = "1m"
final VENTILATION_THRESHOLD_HIGH_HUMIDITY = 85d

final VENTILATION_THRESHOLD_MEDIUM_TIME_WINDOW = "30m"
final VENTILATION_THRESHOLD_HIGH_TIME_WINDOW = "15m"

Closure<Stream<AssetState>> residenceWithVentilationAutoMatch =
        { RulesFacts facts, Operator operator, double ventilationLevelThreshold ->
            facts.matchAssetState(
                    new AssetQuery().type(RESIDENCE).attributeValue("ventilationAuto", true)
            ).filter({ residenceWithVentilationAuto ->
                facts.matchFirstAssetState(
                        new AssetQuery().id(residenceWithVentilationAuto.id)
                                .attributeValue("ventilationLevel", operator, ventilationLevelThreshold)
                ).isPresent()
            })
        }

Closure<Optional<AssetState>> roomAboveThresholdMatch =
        { RulesFacts facts, AssetState residence, String attribute, double threshold, String timeWindow ->
            facts.matchAssetState(
                    new AssetQuery().type(ROOM).parent(residence.id).attributeValue(attribute, GREATER_THAN, 0)
            ).max({ roomWithLevelAboveZero ->
                roomWithLevelAboveZero.valueAsNumber.orElse(0)
            }).filter({ roomWithMaxLevel ->
                roomWithMaxLevel.valueAsNumber.filter({ v -> v > threshold }).isPresent()
            }).filter({ roomAboveThreshold ->
                // No value below threshold in time window
                facts.matchAssetEvent(
                        new AssetQuery().id(roomAboveThreshold.id).attributeValue(attribute, LESS_EQUALS, threshold)
                ).filter(facts.clock.last(timeWindow)).count() == 0
            })
        }

Closure<Boolean> roomsBelowThresholdMatch =
        { RulesFacts facts, AssetState residence, String attribute, double threshold, String timeWindow ->
            facts.matchAssetEvent(
                    new AssetQuery().type(ROOM).parent(residence.id).attributeValue(attribute, GREATER_THAN, threshold)
            ).filter(facts.clock.last(timeWindow)).count() == 0
        }

rules.add()
        .name("Auto ventilation is on, level is below MEDIUM, CO2 or humidity above MEDIUM, set level MEDIUM")
        .when(
        { facts ->
            residenceWithVentilationAutoMatch(
                    facts, LESS_THAN, VENTILATION_LEVEL_MEDIUM
            ).filter({ residence ->
                def roomWithTooMuchCO2 = roomAboveThresholdMatch(
                        facts, residence, "co2Level", VENTILATION_THRESHOLD_MEDIUM_CO2_LEVEL, VENTILATION_THRESHOLD_MEDIUM_CO2_TIME_WINDOW
                )
                def roomWithTooMuchHumidity = roomAboveThresholdMatch(
                        facts, residence, "humidity", VENTILATION_THRESHOLD_MEDIUM_HUMIDITY, VENTILATION_THRESHOLD_MEDIUM_HUMIDITY_TIME_WINDOW
                )
                roomWithTooMuchCO2.isPresent() || roomWithTooMuchHumidity.isPresent()
            }).findFirst().map({ residence ->
                facts.bind("residence", residence)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Ventilation auto and too much CO2/humidity in a room, switching to MEDIUM: " + residence.name)
            facts.updateAssetState(residence.id, "ventilationLevel", VENTILATION_LEVEL_MEDIUM)
        })

rules.add()
        .name("Auto ventilation is on, level is above LOW, CO2 or humidity below MEDIUM, set level LOW")
        .priority(20) // Run after "set level MEDIUM" rule
        .when(
        { facts ->
            residenceWithVentilationAutoMatch(
                    facts, GREATER_THAN, VENTILATION_LEVEL_LOW
            ).filter({ residence ->
                def roomsBelowThresholdCO2 = roomsBelowThresholdMatch(
                        facts, residence, "co2Level", VENTILATION_THRESHOLD_MEDIUM_CO2_LEVEL, VENTILATION_THRESHOLD_MEDIUM_TIME_WINDOW
                )
                def roomsBelowThresholdHumidity = roomsBelowThresholdMatch(
                        facts, residence, "humidity", VENTILATION_THRESHOLD_MEDIUM_HUMIDITY, VENTILATION_THRESHOLD_MEDIUM_TIME_WINDOW
                )
                roomsBelowThresholdCO2 && roomsBelowThresholdHumidity
            }).findFirst().map({ residence ->
                facts.bind("residence", residence)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Ventilation auto and low CO2/humidity in all rooms, switching to LOW: " + residence.name)
            facts.updateAssetState(residence.id, "ventilationLevel", VENTILATION_LEVEL_LOW)
        })

rules.add()
        .name("Auto ventilation is on, level is below HIGH, CO2 or humidity above HIGH, set level HIGH")
        .when(
        { facts ->
            residenceWithVentilationAutoMatch(
                    facts, LESS_THAN, VENTILATION_LEVEL_HIGH
            ).filter({ residence ->
                def roomWithTooMuchCO2 = roomAboveThresholdMatch(
                        facts, residence, "co2Level", VENTILATION_THRESHOLD_HIGH_CO2_LEVEL, VENTILATION_THRESHOLD_HIGH_CO2_TIME_WINDOW
                )
                def roomWithTooMuchHumidity = roomAboveThresholdMatch(
                        facts, residence, "humidity", VENTILATION_THRESHOLD_HIGH_HUMIDITY, VENTILATION_THRESHOLD_HIGH_HUMIDITY_TIME_WINDOW
                )
                roomWithTooMuchCO2.isPresent() || roomWithTooMuchHumidity.isPresent()
            }).findFirst().map({ residence ->
                facts.bind("residence", residence)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Ventilation auto and too much CO2/humidity in a room, switching to HIGH: " + residence.name)
            facts.updateAssetState(residence.id, "ventilationLevel", VENTILATION_LEVEL_HIGH)
        })

rules.add()
        .name("Auto ventilation is on, level is above MEDIUM, CO2 or humidity below MEDIUM, set level MEDIUM")
        .priority(10) // Run before "set level LOW" rule
        .when(
        { facts ->
            residenceWithVentilationAutoMatch(
                    facts, GREATER_THAN, VENTILATION_LEVEL_MEDIUM
            ).filter({ residence ->
                def roomsBelowThresholdCo2 = roomsBelowThresholdMatch(
                        facts, residence, "co2Level", VENTILATION_THRESHOLD_MEDIUM_CO2_LEVEL, VENTILATION_THRESHOLD_HIGH_TIME_WINDOW
                )
                def roomsBelowThresholdHumidity = roomsBelowThresholdMatch(
                        facts, residence, "humidity", VENTILATION_THRESHOLD_MEDIUM_HUMIDITY, VENTILATION_THRESHOLD_HIGH_TIME_WINDOW
                )
                roomsBelowThresholdCo2 && roomsBelowThresholdHumidity
            }).findFirst().map({ residence ->
                facts.bind("residence", residence)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Ventilation auto and low CO2/humidity in all rooms, switching to MEDIUM: " + residence.name)
            facts.updateAssetState(residence.id, "ventilationLevel", VENTILATION_LEVEL_MEDIUM)
        })
