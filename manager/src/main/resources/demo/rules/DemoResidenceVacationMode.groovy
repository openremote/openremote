package demo.rules

import groovy.transform.ToString
import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.AssetQuery
import org.openremote.model.rules.AssetState
import org.openremote.model.rules.Assets

import java.util.logging.Logger

import static org.openremote.model.asset.AssetType.RESIDENCE
import static org.openremote.model.asset.BaseAssetQuery.Operator.GREATER_THAN
import static org.openremote.model.asset.BaseAssetQuery.Operator.LESS_EQUALS
import static org.openremote.model.attribute.AttributeExecuteStatus.REQUEST_START

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Assets assets = binding.assets

@ToString(includeNames = true)
class VacationMode {
    String residenceId
    double until
}

rules.add()
        .name("When residence has vacation until in future, add vacation mode, execute AWAY scene and disable scene timers")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().type(RESIDENCE)
                            .attributeValue("vacationUntil", GREATER_THAN, facts.clock.timestamp)
            ).filter({ residenceWithVacationUntil ->
                facts.match(VacationMode).noneMatch({
                    vacationMode -> vacationMode.residenceId == residenceWithVacationUntil.id
                })
            }).findFirst().map({ residenceWithoutVacationMode ->
                facts.bind("residenceId", residenceWithoutVacationMode.id)
                        .bind("vacationUntil", residenceWithoutVacationMode.valueAsNumber.get())
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            VacationMode vacationMode = new VacationMode(
                    residenceId: facts.bound("residenceId"),
                    until: facts.bound("vacationUntil")
            )
            LOG.info("Vacation mode enabled: " + vacationMode)

            def vacationModeExpiresMillis = vacationMode.until - facts.clock.timestamp
            facts.putTemporary(vacationModeExpiresMillis, vacationMode)

            facts.updateAssetState(vacationMode.residenceId, "awayScene", REQUEST_START)
                    .updateAssetState(vacationMode.residenceId, "disableSceneTimer", REQUEST_START)
        })

rules.add()
        .name("When residence has vacation until in past, clear it and enable scene timers")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().type(RESIDENCE)
                            .attributeValue("vacationUntil", LESS_EQUALS, facts.clock.timestamp)
            ).filter({ residenceWithVacationUntilInPast ->
                residenceWithVacationUntilInPast.getValueAsNumber().isPresent()
            }).filter({ residenceWithVacationUntilInPast ->
                facts.match(VacationMode).noneMatch({
                    vacationMode -> vacationMode.residenceId == residenceWithVacationUntilInPast.id
                })
            }).findFirst().map({ residenceWithVacationUntilInPastWithoutVacationMode ->
                facts.bind("residence", residenceWithVacationUntilInPastWithoutVacationMode)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Vacation ended in residence: " + residence.name)
            facts.updateAssetState(residence.id, "vacationUntil")
            assets.dispatch(residence.id, "enableSceneTimer", REQUEST_START)
        })

rules.add()
        .name("Remove vacation mode when residence has different vacation until")
        .when(
        { facts ->
            facts.matchFirst(VacationMode, { vacationMode ->
                facts.matchFirstAssetState(
                        new AssetQuery().type(RESIDENCE)
                                .id(vacationMode.residenceId)
                                .attributeValue("vacationUntil")
                ).map({ residence ->
                    vacationMode.until != residence.valueAsNumber.get()
                }).orElse(false)
            }).map({ vacationMode ->
                facts.bind("vacationMode", vacationMode)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            VacationMode vacationMode = facts.bound("vacationMode")
            LOG.info("Removing outdated vacation mode: " + vacationMode)
            facts.remove(vacationMode)
        })

rules.add()
        .name("Remove vacation mode when residence has no vacation until, enable scene timers")
        .when(
        { facts ->
            facts.matchFirst(VacationMode, { vacationMode ->
                !facts.matchFirstAssetState(
                        new AssetQuery().type(RESIDENCE)
                                .id(vacationMode.residenceId)
                                .attributeValue("vacationUntil")).isPresent()
            }).map({ vacationMode ->
                facts.bind("vacationMode", vacationMode)
                true
            }).orElse(false)
        })
        .then(
        { facts ->
            VacationMode vacationMode = facts.bound("vacationMode")
            LOG.info("Vacation mode disabled: " + vacationMode)
            facts.remove(vacationMode)
        })