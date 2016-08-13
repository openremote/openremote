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
import org.openremote.manager.shared.connector.AgentResource;
import org.openremote.manager.shared.connector.Connector;

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
    final protected AgentResource agentResource;
    final protected AgentMapper agentMapper = new AgentMapper();
    final protected ConnectorArrayMapper connectorArrayMapper = new ConnectorArrayMapper();

    /*
    final protected Consumer<ConstraintViolation[]> validationErrorHandler = violations -> {
        for (ConstraintViolation violation : violations) {
            if (violation.getPath() != null) {
                if (violation.getPath().endsWith("displayName")) {
                    adminContent.setTenantDisplayNameError(true);
                }
                if (violation.getPath().endsWith("realm")) {
                    adminContent.setTenantRealmError(true);
                }
                if (violation.getPath().endsWith("enabled")) {
                    adminContent.setTenantEnabledError(true);
                }
            }
            adminContent.addFormMessageError(violation.getMessage());
        }
        adminContent.setFormBusy(false);
    };
    */

    protected String id;
    protected Connector[] connectors;
    protected Agent agent;

    @Inject
    public AdminAgentActivity(AdminView adminView,
                              AdminNavigation.Presenter adminNavigationPresenter,
                              AdminAgent view,
                              ManagerMessages managerMessages,
                              PlaceController placeController,
                              EventBus eventBus,
                              SecurityService securityService,
                              RequestService requestService,
                              AgentResource agentResource) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.eventBus = eventBus;
        this.securityService = securityService;
        this.requestService = requestService;
        this.agentResource = agentResource;
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
/*
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        requestService.execute(
            agentMapper,
            requestParams -> {
                tenantResource.create(requestParams, tenant);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.tenantCreated(tenant.getDisplayName())
                ));
                placeController.goTo(new AdminTenantsPlace());
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
*/
    }

    @Override
    public void update() {
/*
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        requestService.execute(
            tenantMapper,
            requestParams -> {
                tenantResource.update(requestParams, id, tenant);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                adminContent.addFormMessageSuccess(managerMessages.tenantUpdated(tenant.getDisplayName()));
                this.id = tenant.getRealm();
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
*/
    }

    @Override
    public void delete() {
/*
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        requestService.execute(
            requestParams -> {
                tenantResource.delete(requestParams, this.id);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.tenantDeleted(tenant.getDisplayName())
                ));
                placeController.goTo(new AdminTenantsPlace());
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
*/
    }

    @Override
    public void cancel() {
        placeController.goTo(new AdminAgentsPlace());
    }

    @Override
    public void onConnectorSelected(Connector connector) {
        LOG.info("### CONNECTOR SELECTED: " + connector);
    }

    protected void loadConnectors(Runnable onSuccess) {
        requestService.execute(
            connectorArrayMapper,
            agentResource::getConnectors,
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
        adminContent.setConnectors(connectors);

        Connector assignedConnector = null;
        if (agent.getConnectorType() != null) {
            for (Connector connector : connectors) {
                if (connector.getType().equals(agent.getConnectorType())) {
                    assignedConnector = connector;
                    break;
                }
            }
        }
        adminContent.setAssignedConnector(assignedConnector);

        adminContent.setName(agent.getName());
        adminContent.setDescription(agent.getDescription());
        adminContent.setAgentEnabled(agent.isEnabled());
    }

    protected void readFromView() {
        agent.setName(adminContent.getName());
        agent.setDescription(adminContent.getDescription());
        agent.setEnabled(adminContent.getAgentEnabled());
    }

    protected void clearViewFieldErrors() {
        adminContent.setNameError(false);
        adminContent.setDescriptionError(false);
        adminContent.setAgentEnabledError(false);
    }
}
