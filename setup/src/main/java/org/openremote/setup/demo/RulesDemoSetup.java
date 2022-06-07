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
package org.openremote.setup.demo;

import org.apache.commons.io.IOUtils;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RealmRuleset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.openremote.model.rules.Ruleset.SHOW_ON_LIST;

public class RulesDemoSetup extends ManagerSetup {

    private static final Logger LOG = Logger.getLogger(RulesDemoSetup.class.getName());

    public RulesDemoSetup(Container container) {
        super(container);
    }

    public Long realmSmartCityRulesetId;

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        ManagerDemoSetup managerDemoSetup = setupService.getTaskOfType(ManagerDemoSetup.class);

        LOG.info("Importing demo rulesets");

        // ################################ Rules demo data ###################################

        // SmartCity geofences
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/DeKuip.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "De Kuip", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true);
            ruleset.getMeta().addOrReplace(
                new MetaItem<>(SHOW_ON_LIST));
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/Euromast.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Euromast", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true);
            ruleset.getMeta().addOrReplace(
                new MetaItem<>(SHOW_ON_LIST));
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/Markthal.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Markthal", Ruleset.Lang.JSON, rules
            ).setAccessPublicRead(true);
            ruleset.getMeta().addOrReplace(
                new MetaItem<>(SHOW_ON_LIST));
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/MarkthalChargersInUse.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Markthal: All chargers in use", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/OnsParkBrightStrongWinds.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Ons Park: Brighten lights", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/OnsParkDimLightWinds.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Ons Park: Dim lights", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/StationCrowded.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Station: Crowded square", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/EnvironmentAlerts.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Environment monitoring: Alerts", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/TotalPowerConsumption.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Total power consumption", Ruleset.Lang.FLOW, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/TotalSolarProduction.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Total power production", Ruleset.Lang.FLOW, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/RotterdamPowerBalance.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "De Rotterdam: Power balance", Ruleset.Lang.FLOW, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/ParkingOccupiedPercentage.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                keycloakDemoSetup.realmCity.getName(), "Parking: Occupied spaces", Ruleset.Lang.FLOW, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/ParkingFull.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                    keycloakDemoSetup.realmCity.getName(), "Parking: Almost full", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/RotterdamBatteryUse.json")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                    keycloakDemoSetup.realmCity.getName(), "De Rotterdam: Battery use", Ruleset.Lang.JSON, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/smartcity/LightGroupOnOff.flow")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Ruleset ruleset = new RealmRuleset(
                    keycloakDemoSetup.realmCity.getName(), "Light group: On/Off", Ruleset.Lang.FLOW, rules
            );
            realmSmartCityRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
    }
}
