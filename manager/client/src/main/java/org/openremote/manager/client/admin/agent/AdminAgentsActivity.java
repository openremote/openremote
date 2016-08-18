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
package org.openremote.manager.client.admin.agent;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.admin.AbstractAdminActivity;
import org.openremote.manager.client.admin.AdminView;
import org.openremote.manager.client.admin.navigation.AdminNavigation;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.ngsi.EntityResource;
import org.openremote.manager.shared.ngsi.params.EntityListParams;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AdminAgentsActivity
    extends AbstractAdminActivity<AdminAgentsPlace, AdminAgents>
    implements AdminAgents.Presenter {

    final protected ManagerMessages managerMessages;
    final protected PlaceController placeController;
    final protected RequestService requestService;
    final protected EntityResource assetsResource;
    final protected AgentArrayMapper agentArrayMapper = new AgentArrayMapper();

    @Inject
    public AdminAgentsActivity(AdminView adminView,
                               AdminNavigation.Presenter adminNavigationPresenter,
                               AdminAgents view,
                               ManagerMessages managerMessages,
                               PlaceController placeController,
                               RequestService requestService,
                               EntityResource assetsResource) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.requestService = requestService;
        this.assetsResource = assetsResource;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
        adminContent.setPresenter(this);

        requestService.execute(
            agentArrayMapper,
            requestParams -> {
                assetsResource.getEntities(requestParams, new EntityListParams().type(Agent.TYPE));
            },
            200,
            adminContent::setAgents,
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
    }

    @Override
    public void onAgentSelected(Agent agent) {
        placeController.goTo(new AdminAgentPlace(agent.getId()));
    }

    @Override
    public void createAgent() {
        placeController.goTo(new AdminAgentPlace());
    }
}
