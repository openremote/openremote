/*
 * Copyright 2016, OpenRemote Inc.
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
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class RulesDemoSetup extends AbstractManagerSetup {

    private static final Logger LOG = Logger.getLogger(RulesDemoSetup.class.getName());

    public RulesDemoSetup(Container container) {
        super(container);
    }

    public Long apartmentActionsRulesetId;
    public Long customerARulesetId;

    @Override
    public void onStart() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        ManagerDemoSetup managerDemoSetup = setupService.getTaskOfType(ManagerDemoSetup.class);

        LOG.info("Importing demo rulesets");

        // ################################ Rules demo data ###################################

        // Apartment 1
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidencePresenceDetection.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - Presence Detection with motion and CO2 sensors", managerDemoSetup.apartment1Id, rules, Ruleset.Lang.GROOVY
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidenceVacationMode.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - Vacation Mode", managerDemoSetup.apartment1Id, rules, Ruleset.Lang.GROOVY
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidenceAutoVentilation.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - Auto Ventilation", managerDemoSetup.apartment1Id, rules, Ruleset.Lang.GROOVY
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidenceNotifyAlarmTrigger.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - Notify Alarm Trigger", managerDemoSetup.apartment1Id, rules, Ruleset.Lang.GROOVY
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidenceSmartSwitch.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - Smart Start Switch", managerDemoSetup.apartment1Id, rules, Ruleset.Lang.GROOVY
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        // Apartment 2
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoResidenceAllLightsOff.js")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new AssetRuleset(
                "Demo Residence - All Lights Off", managerDemoSetup.apartment2Id, rules, Ruleset.Lang.JAVASCRIPT
            );
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/ConsoleLocationPredicate.groovy")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new TenantRuleset(
                "Demo Console Location", keycloakDemoSetup.customerATenant.getId(), rules, Ruleset.Lang.GROOVY
            );
            customerARulesetId = rulesetStorageService.merge(ruleset).getId();
        }
    }
}
