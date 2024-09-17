/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.rules;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.system.HealthStatusProvider;

import java.util.HashMap;
import java.util.Map;

public class RulesHealthStatusProvider implements HealthStatusProvider {

    public static final String NAME = "rules";
    protected RulesService rulesService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        rulesService = container.getService(RulesService.class);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public String getHealthStatusName() {
        return NAME;
    }

    @Override
    public Object getHealthStatus() {
        int totalEngines = rulesService.realmEngines.size() + rulesService.assetEngines.size();
        int stoppedEngines = 0;
        int errorEngines = 0;

        if (rulesService.globalEngine != null) {
            totalEngines++;
            if (!rulesService.globalEngine.isRunning()) {
                stoppedEngines++;
            }
            if (rulesService.globalEngine.isError()) {
                errorEngines++;
            }
        }

        Map<String, Object> realmEngines = new HashMap<>();

        for (RulesEngine<RealmRuleset> realmEngine : rulesService.realmEngines.values()) {
            if (!realmEngine.isRunning()) {
                stoppedEngines++;
            }
            if (realmEngine.isError()) {
                errorEngines++;
            }

            realmEngines.put(realmEngine.getId().getRealm().orElse(""), getEngineHealthStatus(realmEngine));
        }

        Map<String, Object> assetEngines = new HashMap<>();

        for (RulesEngine<AssetRuleset> assetEngine : rulesService.assetEngines.values()) {
            if (!assetEngine.isRunning()) {
                stoppedEngines++;
            }

            if (assetEngine.isError()) {
                errorEngines++;
            }

            assetEngines.put(assetEngine.getId().getAssetId().orElse(""), getEngineHealthStatus(assetEngine));
        }

        Map<String, Object> objectValue = new HashMap<>();
        objectValue.put("totalEngines", totalEngines);
        objectValue.put("stoppedEngines", stoppedEngines);
        objectValue.put("errorEngines", errorEngines);
        if (rulesService.globalEngine != null) {
            objectValue.put("global", getEngineHealthStatus(rulesService.globalEngine));
        }
        objectValue.put("realm", realmEngines);
        objectValue.put("asset", assetEngines);
        return objectValue;
    }

    protected Map<String, Object> getEngineHealthStatus(RulesEngine<?> rulesEngine) {
        boolean isError = rulesEngine.isError();
        int totalDeployments = rulesEngine.deployments.size();
        int executionErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        int compilationErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        Map<String, Object> val = new HashMap<>();
        val.put("isRunning", rulesEngine.isRunning());
        val.put("isError", isError);
        val.put("totalDeployments", totalDeployments);
        val.put("executionErrorDeployments", executionErrorDeployments);
        val.put("compilationErrorDeployments", compilationErrorDeployments);

        Map<String, Object> deployments = new HashMap<>();

        for (RulesetDeployment deployment : rulesEngine.deployments.values()) {
            Map<String, Object> dVal = new HashMap<>();
            dVal.put("name", deployment.getName());
            dVal.put("status", deployment.getStatus().name());
            dVal.put("error", deployment.getError() != null ? deployment.getError().getMessage() : null);
            deployments.put(Long.toString(deployment.getId()), dVal);
        }

        val.put("deployments", deployments);

        return val;
    }
}
