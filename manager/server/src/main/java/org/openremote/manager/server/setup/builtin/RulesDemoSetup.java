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
package org.openremote.manager.server.setup.builtin;

import org.apache.commons.io.IOUtils;
import org.openremote.container.Container;
import org.openremote.manager.server.setup.AbstractManagerSetup;
import org.openremote.manager.shared.rules.Ruleset;
import org.openremote.manager.shared.rules.TenantRuleset;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class RulesDemoSetup extends AbstractManagerSetup {

    private static final Logger LOG = Logger.getLogger(RulesDemoSetup.class.getName());

    public RulesDemoSetup(Container container) {
        super(container);
    }

    public Long apartmentActionsRulesetId;

    @Override
    public void execute() throws Exception {

        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        ManagerDemoSetup managerDemoSetup = setupService.getTaskOfType(ManagerDemoSetup.class);

        LOG.info("Importing demo rulesets");

        // ################################ Rules demo data ###################################

        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoApartmentAllLightsOff.drl")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new TenantRuleset("Demo Apartment - All Lights Off", keycloakDemoSetup.customerATenant.getId(), rules);
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoApartmentPresenceDetection.drl")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new TenantRuleset("Demo Apartment - Presence Detection", keycloakDemoSetup.customerATenant.getId(), rules);
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoApartmentVacationMode.drl")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new TenantRuleset("Demo Apartment - Vacation Mode", keycloakDemoSetup.customerATenant.getId(), rules);
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
        try (InputStream inputStream = RulesDemoSetup.class.getResourceAsStream("/demo/rules/DemoApartmentLHSClockCustomFact.drl")) {
            String rules = IOUtils.toString(inputStream, Charset.forName("utf-8"));
            Ruleset ruleset = new TenantRuleset("Demo Apartment - Clock Custom Fact", keycloakDemoSetup.customerBTenant.getId(), rules);
            apartmentActionsRulesetId = rulesetStorageService.merge(ruleset).getId();
        }
    }
}
