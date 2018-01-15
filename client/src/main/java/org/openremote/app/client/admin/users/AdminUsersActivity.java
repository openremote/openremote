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
package org.openremote.app.client.admin.users;

import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.AbstractAdminActivity;
import org.openremote.app.client.admin.AdminView;
import org.openremote.app.client.admin.TenantArrayMapper;
import org.openremote.app.client.admin.UserArrayMapper;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.admin.users.edit.AdminUserEditPlace;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.security.TenantResource;
import org.openremote.model.security.User;
import org.openremote.model.security.UserResource;

import javax.inject.Inject;
import java.util.Collection;

public class AdminUsersActivity
    extends AbstractAdminActivity<AdminUsersPlace, AdminUsers>
    implements AdminUsers.Presenter {

    final protected Environment environment;
    final protected TenantResource tenantResource;
    final protected TenantArrayMapper tenantArrayMapper;
    final protected UserResource userResource;
    final protected UserArrayMapper userArrayMapper;

    protected String realm;

    @Inject
    public AdminUsersActivity(Environment environment,
                              AdminView adminView,
                              AdminNavigation.Presenter adminNavigationPresenter,
                              AdminUsers view,
                              TenantResource tenantResource,
                              TenantArrayMapper tenantArrayMapper,
                              UserResource userResource,
                              UserArrayMapper userArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
        this.userResource = userResource;
        this.userArrayMapper = userArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    protected AppActivity<AdminUsersPlace> init(AdminUsersPlace place) {
        realm = place.getRealm();
        return super.init(place);
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.setFormBusy(true);
        environment.getApp().getRequests().sendAndReturn(
            tenantArrayMapper,
            tenantResource::getAll,
            200,
            tenants -> {
                adminContent.setTenants(tenants, realm);
                adminContent.setFormBusy(false);
            }
        );

        if (realm != null) {
            adminContent.setFormBusy(true);
            environment.getApp().getRequests().sendAndReturn(
                userArrayMapper,
                requestParams -> userResource.getAll(requestParams, realm),
                200,
                users -> {
                    adminContent.setUsers(users);
                    adminContent.setFormBusy(false);
                }
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
    }

    @Override
    public void onTenantSelected(String realm) {
        environment.getPlaceController().goTo(new AdminUsersPlace(realm));
    }

    @Override
    public void onUserSelected(User user) {
        environment.getPlaceController().goTo(new AdminUserEditPlace(realm, user.getId()));
    }
}
