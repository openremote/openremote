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
package org.openremote.app.client.admin.tenant;

import org.openremote.app.client.Environment;
import org.openremote.app.client.TenantMapper;
import org.openremote.app.client.admin.AbstractAdminActivity;
import org.openremote.app.client.admin.AdminView;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.security.Tenant;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.interop.Consumer;
import org.openremote.model.security.TenantResource;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.app.client.http.RequestExceptionHandler.handleRequestException;

public class AdminTenantActivity
    extends AbstractAdminActivity<AdminTenantPlace, AdminTenant>
    implements AdminTenant.Presenter {

    final protected Environment environment;
    final protected TenantResource tenantResource;
    final protected TenantMapper tenantMapper;

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

    protected String realm;
    protected Tenant tenant;

    @Inject
    public AdminTenantActivity(Environment environment,
                               AdminView adminView,
                               AdminNavigation.Presenter adminNavigationPresenter,
                               AdminTenant view,
                               TenantResource tenantResource,
                               TenantMapper tenantMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.tenantResource = tenantResource;
        this.tenantMapper = tenantMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<AdminTenantPlace> init(AdminTenantPlace place) {
        realm = place.getRealm();
        return super.init(place);
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.clearFormMessages();
        clearViewFieldErrors();
        adminContent.enableCreate(false);
        adminContent.enableUpdate(false);
        adminContent.enableDelete(false);

        if (realm != null) {
            loadTenant();
        } else {
            tenant = new Tenant();
            writeToView();
            adminContent.enableCreate(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
        adminContent.clearFormMessages();
        clearViewFieldErrors();
    }

    @Override
    public void create() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getApp().getRequestService().sendWith(
            tenantMapper,
            requestParams -> {
                tenantResource.create(requestParams, tenant);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().tenantCreated(tenant.getDisplayName())
                ));
                environment.getPlaceController().goTo(new AdminTenantsPlace());
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void update() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getApp().getRequestService().sendWith(
            tenantMapper,
            requestParams -> {
                tenantResource.update(requestParams, realm, tenant);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().tenantUpdated(tenant.getDisplayName())
                ));
                this.realm = tenant.getRealm();
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void delete() {
        adminContent.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(this.realm),
            () -> {
                adminContent.setFormBusy(true);
                adminContent.clearFormMessages();
                clearViewFieldErrors();
                environment.getApp().getRequestService().send(
                    requestParams -> {
                        tenantResource.delete(requestParams, this.realm);
                    },
                    204,
                    () -> {
                        adminContent.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowSuccessEvent(
                            environment.getMessages().tenantDeleted(tenant.getDisplayName())
                        ));
                        environment.getPlaceController().goTo(new AdminTenantsPlace());
                    },
                    ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
                );
            }
        );
    }

    @Override
    public void cancel() {
        environment.getPlaceController().goTo(new AdminTenantsPlace());
    }

    protected void loadTenant() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequestService().sendAndReturn(
            tenantMapper,
            requestParams -> tenantResource.get(requestParams, realm),
            200,
            tenant -> {
                this.tenant = tenant;
                this.realm = tenant.getRealm();
                writeToView();
                adminContent.setFormBusy(false);
                adminContent.enableCreate(false);
                adminContent.enableUpdate(true);
                adminContent.enableDelete(true);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void writeToView() {
        adminContent.setTenantDisplayName(tenant.getDisplayName());
        adminContent.setTenantRealm(tenant.getRealm());
        adminContent.setTenantEnabled(tenant.getEnabled());
    }

    protected void readFromView() {
        tenant.setDisplayName(adminContent.getTenantDisplayName());
        tenant.setRealm(adminContent.getTenantRealm());
        tenant.setEnabled(adminContent.getTenantEnabled());
    }

    protected void clearViewFieldErrors() {
        adminContent.setTenantDisplayNameError(false);
        adminContent.setTenantRealmError(false);
        adminContent.setTenantEnabledError(false);
    }
}
