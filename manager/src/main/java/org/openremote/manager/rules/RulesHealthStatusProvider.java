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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;

public class RulesHealthStatusProvider implements HealthStatusProvider, ContainerService {

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

        ObjectNode realmEngines = ValueUtil.createJsonObject();

        for (RulesEngine<RealmRuleset> realmEngine : rulesService.realmEngines.values()) {
            if (!realmEngine.isRunning()) {
                stoppedEngines++;
            }
            if (realmEngine.isError()) {
                errorEngines++;
            }

            realmEngines.set(realmEngine.getId().getRealm().orElse(""), getEngineHealthStatus(realmEngine));
        }

        ObjectNode assetEngines = ValueUtil.createJsonObject();

        for (RulesEngine<AssetRuleset> assetEngine : rulesService.assetEngines.values()) {
            if (!assetEngine.isRunning()) {
                stoppedEngines++;
            }

            if (assetEngine.isError()) {
                errorEngines++;
            }

            assetEngines.set(assetEngine.getId().getAssetId().orElse(""), getEngineHealthStatus(assetEngine));
        }

        ObjectNode objectValue = ValueUtil.createJsonObject();
        objectValue.put("totalEngines", totalEngines);
        objectValue.put("stoppedEngines", stoppedEngines);
        objectValue.put("errorEngines", errorEngines);
        if (rulesService.globalEngine != null) {
            objectValue.set("global", getEngineHealthStatus(rulesService.globalEngine));
        }
        objectValue.set("realm", realmEngines);
        objectValue.set("asset", assetEngines);
        return objectValue;
    }

    protected ObjectNode getEngineHealthStatus(RulesEngine<?> rulesEngine) {
        boolean isError = rulesEngine.isError();
        int totalDeployments = rulesEngine.deployments.size();
        int executionErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        int compilationErrorDeployments = rulesEngine.getExecutionErrorDeploymentCount();
        ObjectNode val = ValueUtil.createJsonObject();
        val.put("isRunning", rulesEngine.isRunning());
        val.put("isError", isError);
        val.put("totalDeployments", totalDeployments);
        val.put("executionErrorDeployments", executionErrorDeployments);
        val.put("compilationErrorDeployments", compilationErrorDeployments);

        ObjectNode deployments = ValueUtil.createJsonObject();

        for (Object obj : rulesEngine.deployments.values()) {
            RulesetDeployment deployment = (RulesetDeployment)obj;
            ObjectNode dVal = ValueUtil.createJsonObject();
            dVal.put("name", deployment.getName());
            dVal.put("status", deployment.getStatus().name());
            dVal.put("error", deployment.getError() != null ? deployment.getError().getMessage() : null);
            deployments.set(Long.toString(deployment.getId()), dVal);
        }

        val.set("deployments", deployments);

        return val;
    }
}
