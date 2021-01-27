package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.AttributePredicate
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.rules.AssetState

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.logging.Logger
import java.util.stream.Stream

import static org.openremote.model.query.AssetQuery.Match.BEGIN

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules

final CYCLE_TIME_MILLISECONDS = (2.5 * 60 * 60 * 1000) as long

enum SmartSwitchAttribute {
    Mode, BeginEnd, StartTime, StopTime, Enabled
}

enum SmartSwitchMode {
    NOW_ON, ON_AT, READY_AT

    static boolean matches(AssetState smartSwitch, SmartSwitchMode mode) {
        smartSwitch.value.orElse(false)
    }
}

Closure<Stream<AssetState<?>>> smartSwitchAttributesMatch = { RulesFacts facts, SmartSwitchAttribute attribute ->
    facts.matchAssetState(
            new AssetQuery().types(RoomAsset).attributes(new AttributePredicate(new StringPredicate(BEGIN, "smartSwitch" + attribute.name()), null))
    )
}

Closure<Character> getSmartSwitchName = { AssetState attribute ->
    attribute.name.charAt(attribute.name.length() - 1)
}

Closure<Optional<AssetState<?>>> smartSwitchAttributeMatch = { RulesFacts facts, AssetState smartSwitch, SmartSwitchAttribute attribute ->
    facts.matchFirstAssetState(
            new AssetQuery().types(RoomAsset).attributeName("smartSwitch" + attribute.name() + getSmartSwitchName(smartSwitch))
    )
}

rules.add()
        .name("Clear start/stop time and disable actuator when begin/end of cycle is empty")
        .when(
        { facts ->
            smartSwitchAttributesMatch(
                    facts, SmartSwitchAttribute.BeginEnd
            ).filter { beginEnd ->
                !beginEnd.value.isPresent()
            }.filter { emptyBeginEnd ->
                // Any of start time, stop time, or enabled is greater than zero
                def startTime = smartSwitchAttributeMatch(facts, emptyBeginEnd, SmartSwitchAttribute.StartTime)
                def stopTime = smartSwitchAttributeMatch(facts, emptyBeginEnd, SmartSwitchAttribute.StopTime)
                def enabled = smartSwitchAttributeMatch(facts, emptyBeginEnd, SmartSwitchAttribute.Enabled)
                (startTime.isPresent() && startTime.get().value.orElse(0l) > 0l) ||
                        (stopTime.isPresent() && stopTime.get().value.orElse(0l) > 0l) ||
                        (enabled.isPresent() && enabled.get().value.orElse(0l) > 0l)
            }.findFirst().map { emptyBeginEnd ->
                facts.bind("beginEnd", emptyBeginEnd)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState beginEnd = facts.bound("beginEnd")
            LOG.info("Empty smart switch begin/end, clearing start/stop time and disabling actuator: " + beginEnd)
            String smartSwitchName = getSmartSwitchName(beginEnd)
            facts.updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StartTime + smartSwitchName, 0)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StopTime + smartSwitchName, 0)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.Enabled + smartSwitchName, 0)
        })

rules.add()
        .name("Clear begin/end of cycle when mode is NOW_ON")
        .when(
        { facts ->
            smartSwitchAttributesMatch(
                    facts, SmartSwitchAttribute.Mode
            ).filter { mode ->
                SmartSwitchMode.matches(mode, SmartSwitchMode.NOW_ON)
            }.filter { modeNowOn ->
                // Begin/end time of cycle is greater than zero
                smartSwitchAttributeMatch(facts, modeNowOn, SmartSwitchAttribute.BeginEnd)
                        .flatMap { it.getValueAs(Long.class) }
                        .map { it > 0 }
                        .orElse(false)
            }.findFirst().map { modeNowOn ->
                facts.bind("mode", modeNowOn)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState mode = facts.bound("mode")
            LOG.info("Smart switch mode is NOW_ON, clearing begin/end of cycle: " + mode)
            String smartSwitchName = getSmartSwitchName(mode)
            facts.updateAssetState(mode.id, "smartSwitch" + SmartSwitchAttribute.BeginEnd + smartSwitchName)
        })

rules.add()
        .name("Enable actuator when mode is ON_AT and begin/end of cycle is now or in future and different than actuator start time")
        .when(
        { facts ->
            smartSwitchAttributesMatch(
                    facts, SmartSwitchAttribute.Mode
            ).filter { mode ->
                SmartSwitchMode.matches(mode, SmartSwitchMode.ON_AT)
            }.map { modeOnAt ->
                smartSwitchAttributeMatch(facts, modeOnAt, SmartSwitchAttribute.BeginEnd)
            }.filter { beginEnd ->
                // User-provided begin/end time of cycle
                beginEnd.flatMap { it.value}.filter {
                    // is now or in future
                    it >= facts.clock.timestamp
                }.map { expectedStartTime ->
                    // and actuator start time attribute
                    def startTime = beginEnd.flatMap {
                        smartSwitchAttributeMatch(facts, it, SmartSwitchAttribute.StartTime)
                    }
                    // is present
                    startTime.isPresent() &&
                            // the value is not the same as the expected start time
                            startTime.map {
                                it.value.map {
                                    it != expectedStartTime
                                }.orElse(true) // or actuator start time is empty
                            }
                }.orElse(false)
            }.findFirst().map {beginEnd ->
                facts.bind("beginEnd", beginEnd.get())
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState<Long> beginEnd = facts.bound("beginEnd")
            // Start time is user-provided begin/end (in seconds)
            def startSeconds = beginEnd.value.orElse(0l)
            // Stop time is start time plus CYCLE_TIME_MILLISECONDS
            def stopMilliseconds = startSeconds + CYCLE_TIME_MILLISECONDS
            LOG.info("Smart switch mode is ON_AT, enabling actuator with start/stop times " +
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(startSeconds), ZoneId.systemDefault()) +
                    "/" +
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(stopMilliseconds), ZoneId.systemDefault()) +
                    ": " + beginEnd)
            String smartSwitchName = getSmartSwitchName(beginEnd)
            facts.updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StartTime + smartSwitchName, startSeconds)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StopTime + smartSwitchName, stopMilliseconds)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.Enabled + smartSwitchName, 1)
        })

rules.add()
        .name("Enable actuator when mode is READY_AT and cycle will complete in future and different than actuator stop time")
        .when(
        { facts ->
            smartSwitchAttributesMatch(
                    facts, SmartSwitchAttribute.Mode
            ).filter { mode ->
                SmartSwitchMode.matches(mode, SmartSwitchMode.READY_AT)
            }.map { modeReadyAt ->
                smartSwitchAttributeMatch(facts, modeReadyAt, SmartSwitchAttribute.BeginEnd)
            }.filter { beginEnd ->
                // User-provided begin/end time of cycle
                beginEnd.flatMap { it.value}.filter {
                    // allows the cycle to complete in future
                    (it - CYCLE_TIME_MILLISECONDS) >= facts.clock.timestamp
                }.map { expectedStopTime ->
                    // and actuator stop time attribute
                    def stopTime = beginEnd.flatMap {
                        smartSwitchAttributeMatch(facts, it, SmartSwitchAttribute.StopTime)
                    }
                    // is present
                    stopTime.isPresent() &&
                            // the value is not the same as the expected stop time
                            stopTime.flatMap { it.value}.map {
                                it != expectedStopTime
                            }.orElse(true) // or actuator stop time is empty
                }.orElse(false)
            }.findFirst().map { beginEnd ->
                facts.bind("beginEnd", beginEnd.get())
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState<Long> beginEnd = facts.bound("beginEnd")
            // Start time is current time (in seconds)
            def startMilliseconds = facts.clock.timestamp
            // Stop time is user-provided smart switch time (in seconds)
            def stopMilliseconds = beginEnd.value.orElse(0l)
            LOG.info("Smart switch mode is READY_AT, enabling actuator with start/stop times " +
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(startMilliseconds), ZoneId.systemDefault()) + "/" +
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(stopMilliseconds), ZoneId.systemDefault()) +
                    ": " +
                    beginEnd)
            String smartSwitchName = getSmartSwitchName(beginEnd)
            facts.updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StartTime + smartSwitchName, startMilliseconds)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.StopTime + smartSwitchName, stopMilliseconds)
                    .updateAssetState(beginEnd.id, "smartSwitch" + SmartSwitchAttribute.Enabled + smartSwitchName, 1)
        })

rules.add()
        .name("Set mode NOW_ON when mode is ON_AT or READY_AT and actuator stop time is in the past")
        .when(
        { facts ->
            smartSwitchAttributesMatch(
                    facts, SmartSwitchAttribute.Mode
            ).filter { mode ->
                SmartSwitchMode.matches(mode, SmartSwitchMode.ON_AT) || SmartSwitchMode.matches(mode, SmartSwitchMode.READY_AT)
            }.map { modeOnAtOrReadyAt ->
                smartSwitchAttributeMatch(facts, modeOnAtOrReadyAt, SmartSwitchAttribute.StopTime)
            }.filter { stopTime ->
                // Actuator stop time attribute is present and value (in seconds) is not zero and in the past
                stopTime.flatMap { it.value}.map {
                    it > 0 && it < facts.clock.timestamp
                }.orElse(false)
            }.findFirst().map { stopTime ->
                facts.bind("stopTime", stopTime.get())
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState stopTime = facts.bound("stopTime")
            LOG.info("Smart switch actuator stop time is in the past, setting mode NOW_ON: " + stopTime)
            String smartSwitchName = getSmartSwitchName(stopTime)
            facts.updateAssetState(
                    stopTime.id,
                    "smartSwitch" + SmartSwitchAttribute.Mode + smartSwitchName,
                    SmartSwitchMode.NOW_ON.name()
            )
        })
