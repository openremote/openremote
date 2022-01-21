/*
 * Copyright 2020, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.setup;

import org.apache.commons.io.IOUtils;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.openremote.model.rules.Ruleset.Lang.GROOVY;
import static org.openremote.model.rules.Ruleset.SHOW_ON_LIST;

public class RulesTestSetup extends ManagerSetup {

    private static final Logger LOG = Logger.getLogger(RulesTestSetup.class.getName());

    public RulesTestSetup(Container container) {
        super(container);
    }

    public Long apartmentActionsRulesetId;
    public Long tenantBuildingRulesetId;
    public Long tenantSmartCityRulesetId;

    @Override
    public void onStart() throws Exception {

        KeycloakTestSetup keycloakTestSetup = setupService.getTaskOfType(KeycloakTestSetup.class);
        ManagerTestSetup managerTestSetup = setupService.getTaskOfType(ManagerTestSetup.class);

        LOG.info("Importing demo rulesets");

        // ################################ Rules demo data ###################################

        // Apartment 1
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidencePresenceDetection.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.apartment1Id, "Demo Residence - Presence Detection with motion and CO2 sensors", Ruleset.Lang.GROOVY, rules
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidenceVacationMode.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.apartment1Id, "Demo Residence - Vacation Mode", Ruleset.Lang.GROOVY, rules
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidenceAutoVentilation.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.apartment1Id, "Demo Residence - Auto Ventilation", Ruleset.Lang.GROOVY, rules
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidenceNotifyAlarmTrigger.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.apartment1Id, "Demo Residence - Notify Alarm Trigger", Ruleset.Lang.GROOVY, rules
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidenceSmartSwitch.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.apartment1Id, "Demo Residence - Smart Start Switch", Ruleset.Lang.GROOVY, rules
            );
            ruleset.setEnabled(false);
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        // Apartment 2
//        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ResidenceAllLightsOff.js")) {
//            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
//            Ruleset ruleset = new AssetRuleset(
//                    managerTestSetup.apartment2Id, "Demo Residence - All Lights Off", Ruleset.Lang.JAVASCRIPT, rules
//            );
//            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
//        }

        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/ConsoleLocation.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                    keycloakTestSetup.tenantBuilding.getRealm(), "Demo Console Location", Ruleset.Lang.GROOVY, rules
            ).setAccessPublicRead(true);
            tenantBuildingRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        // People counter
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/SmartCityCamera.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new AssetRuleset(
                    managerTestSetup.peopleCounter3AssetId, "PeopleCounter 3 Rules", GROOVY, rules
            );
            rulesetStorageService.merge(ruleset);
        }

        // SmartCity geofences
        try (InputStream inputStream = RulesTestSetup.class.getResourceAsStream("/org/openremote/test/rules/Geofence.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                    keycloakTestSetup.tenantCity.getRealm(), "Demo Geofences", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true);
            ruleset.getMeta().addOrReplace(
                new MetaItem<>(SHOW_ON_LIST));
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
    }
}
