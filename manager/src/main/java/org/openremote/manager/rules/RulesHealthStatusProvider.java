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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.ContainerHealthStatusProvider;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

public class RulesHealthStatusProvider implements ContainerHealthStatusProvider {

    public static final String NAME = "rules";
    public static final String VERSION = "1.0";

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
    public String getHealthStatusVersion() {
        return VERSION;
    }

    @Override
    public Value getHealthStatus() {
        int totalEngines = rulesService.tenantEngines.size() + rulesService.assetEngines.size();
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

        ObjectValue tenantEngines = Values.createObject();

        for (RulesEngine<TenantRuleset> tenantEngine : rulesService.tenantEngines.values()) {
            if (!tenantEngine.isRunning()) {
                stoppedEngines++;
            }
            if (tenantEngine.isError()) {
                errorEngines++;
            }

            tenantEngines.put(tenantEngine.getId().getRealmId().orElse(""), getEngineHealthStatus(tenantEngine));
        }

        ObjectValue assetEngines = Values.createObject();

        for (RulesEngine<AssetRuleset> assetEngine : rulesService.assetEngines.values()) {
            if (!assetEngine.isRunning()) {
                stoppedEngines++;
            }

            if (assetEngine.isError()) {
                errorEngines++;
            }

            assetEngines.put(assetEngine.getId().getAssetId().orElse(""), getEngineHealthStatus(assetEngine));
        }

        ObjectValue objectValue = Values.createObject();
        objectValue.put("totalEngines", totalEngines);
        objectValue.put("stoppedEngines", stoppedEngines);
        objectValue.put("errorEngines", errorEngines);
        if (rulesService.globalEngine != null) {
            objectValue.put("global", getEngineHealthStatus(rulesService.globalEngine));
        }
        objectValue.put("tenant", tenantEngines);
        objectValue.put("asset", assetEngines);
        return objectValue;
    }

    protected ObjectValue getEngineHealthStatus(RulesEngine rulesEngine) {
        boolean isError = rulesEngine.isError();
        int totalDeployments = rulesEngine.deployments.size();
        int executionErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        int compilationErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        ObjectValue val = Values.createObject();
        val.put("isRunning", rulesEngine.isRunning());
        val.put("isError", isError);
        val.put("totalDeployments", totalDeployments);
        val.put("executionErrorDeployments", executionErrorDeployments);
        val.put("compilationErrorDeployments", compilationErrorDeployments);

        ObjectValue deployments = Values.createObject();

        for (Object obj : rulesEngine.deployments.values()) {
            RulesetDeployment deployment = (RulesetDeployment)obj;
            ObjectValue dVal = Values.createObject();
            dVal.put("name", deployment.getName());
            dVal.put("status", deployment.getStatus().name());
            dVal.put("error", deployment.getError() != null ? deployment.getError().getMessage() : null);
            deployments.put(Long.toString(deployment.getId()), dVal);
        }

        val.put("deployments", deployments);

        return val;
    }
}
