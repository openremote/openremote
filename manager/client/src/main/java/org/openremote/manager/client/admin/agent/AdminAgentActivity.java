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
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.AgentResource;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.connector.ConnectorResource;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AdminAgentActivity
    extends AbstractAdminActivity<AdminAgentPlace, AdminAgent>
    implements AdminAgent.Presenter {

    private static final Logger LOG = Logger.getLogger(AdminAgentActivity.class.getName());

    final protected ManagerMessages managerMessages;
    final protected PlaceController placeController;
    final protected EventBus eventBus;
    final protected SecurityService securityService;
    final protected RequestService requestService;
    final protected ConnectorResource connectorResource;
    final protected AgentResource agentResource;
    final protected AgentMapper agentMapper;
    final protected ConnectorArrayMapper connectorArrayMapper;

    protected String id;
    protected Connector[] connectors;
    protected Agent agent;
    protected Connector assignedConnector;

    // This is a dummy we use when the Agent's assigned connector is not installed
    protected final Connector notFoundConnector = new Connector();
    protected final String notFoundConnectorId = "NOT_FOUND_CONNECTOR_ID";

    @Inject
    public AdminAgentActivity(AdminView adminView,
                              AdminNavigation.Presenter adminNavigationPresenter,
                              AdminAgent view,
                              ManagerMessages managerMessages,
                              PlaceController placeController,
                              EventBus eventBus,
                              SecurityService securityService,
                              RequestService requestService,
                              ConnectorResource connectorResource,
                              AgentResource agentResource,
                              AgentMapper agentMapper,
                              ConnectorArrayMapper connectorArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.eventBus = eventBus;
        this.securityService = securityService;
        this.requestService = requestService;
        this.connectorResource = connectorResource;
        this.agentResource = agentResource;
        this.agentMapper = agentMapper;
        this.connectorArrayMapper = connectorArrayMapper;

        notFoundConnector.setId(notFoundConnectorId);
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<AdminAgentPlace> init(AdminAgentPlace place) {
        id = place.getId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        adminContent.enableCreate(false);
        adminContent.enableUpdate(false);
        adminContent.enableDelete(false);

        adminContent.setFormBusy(true);
        if (id != null) {
            loadConnectors(() -> loadAgent(() -> {
                writeToView();
                adminContent.setFormBusy(false);
                adminContent.enableCreate(false);
                adminContent.enableUpdate(true);
                adminContent.enableDelete(true);
            }));
        } else {
            loadConnectors(() -> {
                agent = new Agent();
                writeToView();
                adminContent.setFormBusy(false);
                adminContent.enableCreate(true);
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
    }

    @Override
    public void create() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        requestService.execute(
            agentMapper,
            requestParams -> {
                agentResource.create(requestParams, agent);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.agentCreated(agent.getName())
                ));
                placeController.goTo(new AdminAgentsPlace());
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void update() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        requestService.execute(
            agentMapper,
            requestParams -> {
                agentResource.update(requestParams, id, agent);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                adminContent.addFormMessageSuccess(managerMessages.agentUpdated(agent.getName()));
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void delete() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        requestService.execute(
            requestParams -> {
                agentResource.delete(requestParams, this.id);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.agentDeleted(agent.getName())
                ));
                placeController.goTo(new AdminAgentsPlace());
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void cancel() {
        placeController.goTo(new AdminAgentsPlace());
    }

    @Override
    public void onConnectorSelected(Connector connector) {
        assignedConnector = connector;
        adminContent.setAssignedConnector(connector);
    }

    protected void loadConnectors(Runnable onSuccess) {
        requestService.execute(
            connectorArrayMapper,
            connectorResource::getConnectors,
            200,
            connectors -> {
                this.connectors = connectors;
                onSuccess.run();
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    protected void loadAgent(Runnable onSuccess) {
        requestService.execute(
            agentMapper,
            requestParams -> agentResource.get(requestParams, id),
            200,
            agent -> {
                this.agent = agent;
                this.id = agent.getId();
                onSuccess.run();
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    protected void writeToView() {
        findAssignedConnector();
        adminContent.setConnectors(connectors);
        adminContent.setAssignedConnector(assignedConnector);
        adminContent.setName(agent.getName());
        adminContent.setDescription(agent.getDescription());
        adminContent.setAgentEnabled(agent.isEnabled());
    }

    protected void readFromView() {
        setAssignedConnector();
        agent.setName(adminContent.getName());
        agent.setDescription(adminContent.getDescription());
        agent.setEnabled(adminContent.getAgentEnabled());
    }

    protected void findAssignedConnector() {
        assignedConnector = null;
        if (agent != null && agent.getConnectorType() != null) {
            for (Connector connector : connectors) {
                if (connector.getType().equals(agent.getConnectorType())) {
                    assignedConnector = connector;
                    break;
                }
            }
            if (assignedConnector != null) {
                assignedConnector.readSettings(agent);
            } else if (!Agent.NO_CONNECTOR_ASSIGNED_TYPE.equals(agent.getConnectorType())){
                notFoundConnector.setName(
                    agent.getConnectorType() + " (" + managerMessages.connectorNotInstalled() + ")"
                );
                assignedConnector = notFoundConnector;
            }
        }
    }

    protected void setAssignedConnector() {
        if (assignedConnector != null) {
            if (!assignedConnector.getId().equals(notFoundConnectorId)) {
                agent.setConnectorType(assignedConnector.getType());
                assignedConnector.writeSettings(agent);
            }
        } else {
            agent.setConnectorType(null);
            agent.setConnectorSettings(null);
        }
    }

    protected void clearViewFieldErrors() {
        adminContent.setNameError(false);
        adminContent.setDescriptionError(false);
        adminContent.setAgentEnabledError(false);
    }
}
