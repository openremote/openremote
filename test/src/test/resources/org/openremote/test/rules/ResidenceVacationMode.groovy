/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.setup.integration.rules

import groovy.transform.ToString
import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.AttributePredicate
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.query.filter.ValueEmptyPredicate
import org.openremote.model.rules.AssetState
import org.openremote.model.rules.Assets

import java.util.logging.Logger

import static org.openremote.model.query.AssetQuery.Operator.GREATER_THAN
import static org.openremote.model.query.AssetQuery.Operator.LESS_EQUALS
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
        .name("When residence has vacation until in future, add vacation mode, execute DAY scene and disable scene timers")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset)
                            .attributeValue("vacationUntil", GREATER_THAN, facts.clock.currentTimeMillis)
            ).filter { residenceWithVacationUntil ->
                facts.match(VacationMode).noneMatch {
                    vacationMode -> vacationMode.residenceId == residenceWithVacationUntil.id
                }
            }.findFirst().map { residenceWithoutVacationMode ->
                facts.bind("residenceId", residenceWithoutVacationMode.id)
                        .bind("vacationUntil", residenceWithoutVacationMode.value.get())
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            VacationMode vacationMode = new VacationMode(
                    residenceId: facts.bound("residenceId"),
                    until: facts.bound("vacationUntil")
            )
            LOG.info("Vacation mode enabled: " + vacationMode)

            def vacationModeExpiresMillis = vacationMode.until - facts.clock.currentTimeMillis
            facts.putTemporary(vacationModeExpiresMillis, vacationMode)

            facts.updateAssetState(vacationMode.residenceId, "dayScene", REQUEST_START)
                    .updateAssetState(vacationMode.residenceId, "disableSceneTimer", REQUEST_START)
        })

rules.add()
        .name("When residence has vacation until in past, clear it and enable scene timers")
        .when(
        { facts ->
            facts.matchAssetState(
                    new AssetQuery().types(BuildingAsset)
                            .attributeValue("vacationUntil", LESS_EQUALS, facts.clock.currentTimeMillis)
            ).filter { residenceWithVacationUntilInPast ->
                residenceWithVacationUntilInPast.getValue(Double.class).isPresent()
            }.filter { residenceWithVacationUntilInPast ->
                facts.match(VacationMode).noneMatch {
                    vacationMode -> vacationMode.residenceId == residenceWithVacationUntilInPast.id
                }
            }.findFirst().map { residenceWithVacationUntilInPastWithoutVacationMode ->
                facts.bind("residence", residenceWithVacationUntilInPastWithoutVacationMode)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            AssetState residence = facts.bound("residence")
            LOG.info("Vacation ended in residence: " + residence.assetName)
            facts.updateAssetState(residence.id, "vacationUntil")
            assets.dispatch(residence.id, "enableSceneTimer", REQUEST_START)
        })

rules.add()
        .name("Remove vacation mode when residence has different vacation until")
        .when(
        { facts ->
            facts.matchFirst(VacationMode) { vacationMode ->
                facts.matchFirstAssetState(
                        new AssetQuery().types(BuildingAsset)
                                .ids(vacationMode.residenceId)
                                .attributes(new AttributePredicate(new StringPredicate("vacationUntil"), new ValueEmptyPredicate().negate(true)))
                ).map { residence ->
                    vacationMode.until != residence.value.get()
                }.orElse(false)
            }.map { vacationMode ->
                facts.bind("vacationMode", vacationMode)
                true
            }.orElse(false)
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
            facts.matchFirst(VacationMode) { vacationMode ->
                !facts.matchFirstAssetState(
                        new AssetQuery().types(BuildingAsset)
                                .ids(vacationMode.residenceId)
                                .attributes(new AttributePredicate(new StringPredicate("vacationUntil"), new ValueEmptyPredicate().negate(true)))).isPresent()
            }.map { vacationMode ->
                facts.bind("vacationMode", vacationMode)
                true
            }.orElse(false)
        })
        .then(
        { facts ->
            VacationMode vacationMode = facts.bound("vacationMode")
            LOG.info("Vacation mode disabled: " + vacationMode)
            facts.remove(vacationMode)
        })
