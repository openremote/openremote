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
import org.openremote.app.client.admin.AbstractAdminActivity;
import org.openremote.app.client.admin.AdminView;
import org.openremote.app.client.admin.TenantArrayMapper;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.TenantResource;

import javax.inject.Inject;
import java.util.Collection;

public class AdminTenantsActivity
    extends AbstractAdminActivity<AdminTenantsPlace, AdminTenants>
    implements AdminTenants.Presenter {

    final protected Environment environment;
    final protected TenantResource tenantResource;
    final protected TenantArrayMapper tenantArrayMapper;

    @Inject
    public AdminTenantsActivity(Environment environment,
                                AdminView adminView,
                                AdminNavigation.Presenter adminNavigationPresenter,
                                AdminTenants view,
                                TenantResource tenantResource,
                                TenantArrayMapper tenantArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
        adminContent.setPresenter(this);

        adminContent.setFormBusy(true);
        environment.getApp().getRequestService().sendAndReturn(
            tenantArrayMapper,
            tenantResource::getAll,
            200,
            tenants -> {
                adminContent.setTenants(tenants);
                adminContent.setFormBusy(false);
            }
        );

        adminContent.setCreateTenantHistoryToken(
            environment.getPlaceHistoryMapper().getToken(new AdminTenantPlace())
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
    }

    @Override
    public void onTenantSelected(Tenant tenant) {
        environment.getPlaceController().goTo(new AdminTenantPlace(tenant.getRealm()));
    }
}
