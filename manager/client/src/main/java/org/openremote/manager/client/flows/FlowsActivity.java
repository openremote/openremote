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
package org.openremote.manager.client.flows;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetInfoArrayMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class FlowsActivity
        extends AppActivity<FlowsPlace>
        implements FlowsView.Presenter {

    private static final Logger LOG = Logger.getLogger(FlowsActivity.class.getName());

    final FlowsView view;
    final Environment environment;
    final PlaceHistoryMapper historyMapper;
    final AssetResource assetResource;
    final AssetInfoArrayMapper assetInfoArrayMapper;
    final AssetMapper assetMapper;

    @Inject
    public FlowsActivity(FlowsView view,
                         Environment environment,
                         PlaceHistoryMapper historyMapper,
                         AssetResource assetResource,
                         AssetInfoArrayMapper assetInfoArrayMapper,
                         AssetMapper assetMapper) {
        this.view = view;
        this.environment = environment;
        this.historyMapper = historyMapper;
        this.assetResource = assetResource;
        this.assetInfoArrayMapper = assetInfoArrayMapper;
        this.assetMapper = assetMapper;
    }

    @Override
    protected AppActivity<FlowsPlace> init(FlowsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setWidget(view.asWidget());
        view.setPresenter(this);
/* TODO
        environment.getRequestService().execute(
            assetInfoArrayMapper,
            requestParams -> {
                // TODO We should query only agents with controller2 connectors
                assetResource.getByType(requestParams, AssetType.AGENT.getValue());
            },
            200,
            view::setAgents,
            ex -> handleRequestException(ex, environment)
        );
        */
    }

    @Override
    public void onStop() {
        view.setPresenter(null);
        super.onStop();
    }

    @Override
    public String getFlowsHistoryToken() {
        return historyMapper.getToken(new FlowsPlace());
    }

    @Override
    public void onAgentSelected(AssetInfo assetInfo) {
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> {
                assetResource.get(requestParams, assetInfo.getId());
            },
            200,
            agentAsset -> {
                Attributes attributes = new Attributes(agentAsset.getAttributes());
                Agent agent = new Agent(attributes);
                if ("urn:openremote:connector:controller2".equals(agent.getConnectorType())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("http://");
                    sb.append(attributes.get("host").getValueAsString());
                    sb.append(":");
                    sb.append(attributes.get("port").getValueAsString());
                    // TODO hardcoded dimensions
                    sb.append("/webconsole/?consoleWidth=320&consoleHeight=568&showConsoleFrame=false&showWelcome=false&showToolbar=false&backgroundColor=444444");
                    sb.append("&controllerURL=");
                    sb.append("http://");
                    sb.append(attributes.get("host").getValueAsString());
                    sb.append(":");
                    sb.append(attributes.get("port").getValueAsString());
                    sb.append("/controller");
                    sb.append("&panelName=iPhone5"); // TODO hardcoded panel name
                    view.setFrameSourceUrl(sb.toString());
                } else {
                    // TODO: Remove when we only have controller2 agents
                    environment.getEventBus().dispatch(new ShowInfoEvent(
                        "The selected agent is not an OpenRemote 2.x controller, no console available."
                    ));
                }
            },
            ex -> handleRequestException(ex, environment)
        );
    }
}
