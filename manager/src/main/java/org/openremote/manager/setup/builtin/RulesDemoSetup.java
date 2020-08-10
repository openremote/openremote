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
package org.openremote.manager.setup.builtin;

import org.apache.commons.io.IOUtils;
import org.openremote.container.Container;
import org.openremote.manager.setup.AbstractManagerSetup;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.value.Values;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class RulesDemoSetup extends AbstractManagerSetup {

    private static final Logger LOG = Logger.getLogger(RulesDemoSetup.class.getName());

    public RulesDemoSetup(Container container) {
        super(container);
    }

    public Long tenantSmartCityRulesetId;

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        ManagerDemoSetup managerDemoSetup = setupService.getTaskOfType(ManagerDemoSetup.class);

        LOG.info("Importing demo rulesets");

        // ################################ Rules demo data ###################################

        // People counter
//        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoSmartCityCamera.groovy")) {
//            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
//            Ruleset ruleset = new AssetRuleset(
//                managerDemoSetup.peopleCounter3AssetId, "PeopleCounter 3 Rules", GROOVY, rules
//            );
//            rulesetStorageService.merge(ruleset);
//        }

        // SmartCity geofences
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/DeKuip.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "De Kuip", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true).addMeta("showOnMap", Values.create(true)).addMeta("showOnList", Values.create(true));
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/Euromast.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Euromast", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true).addMeta("showOnMap", Values.create(true)).addMeta("showOnList", Values.create(true));
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/Markthal.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Markthal", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true).addMeta("showOnMap", Values.create(true)).addMeta("showOnList", Values.create(true));
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/MarkthalChargersInUse.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Markthal: All chargers in use", Ruleset.Lang.JSON, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/OnsParkBrightStrongWinds.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Ons Park: Brighten lights", Ruleset.Lang.JSON, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/OnsParkDimLightWinds.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Ons Park: Dim lights", Ruleset.Lang.JSON, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/StationCrowded.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Station: Crowded square", Ruleset.Lang.JSON, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/EnvironmentAlerts.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Environment monitoring: Alerts", Ruleset.Lang.JSON, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/TotalPowerConsumption.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Total power consumption", Ruleset.Lang.FLOW, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/TotalSolarProduction.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Total power production", Ruleset.Lang.FLOW, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/RotterdamPowerBalance.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "De Rotterdam: Power balance", Ruleset.Lang.FLOW, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/ParkingOccupiedPercentage.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new TenantRuleset(
                keycloakDemoSetup.tenantCity.getRealm(), "Parking: Occupied spaces", Ruleset.Lang.FLOW, rules
            );
            tenantSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
    }
}
