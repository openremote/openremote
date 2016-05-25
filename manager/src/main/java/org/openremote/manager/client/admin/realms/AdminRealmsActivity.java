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
package org.openremote.manager.client.admin.realms;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.manager.client.admin.AbstractAdminActivity;
import org.openremote.manager.client.admin.AdminView;
import org.openremote.manager.client.admin.RealmArrayMapper;
import org.openremote.manager.client.admin.navigation.AdminNavigation;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;
import org.openremote.manager.shared.http.ObjectMapperCallback;
import org.openremote.manager.shared.security.RealmsResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AdminRealmsActivity
    extends AbstractAdminActivity<AdminRealmsPlace, AdminRealms>
    implements AdminRealms.Presenter {

    private static final Logger LOG = Logger.getLogger(AdminRealmsActivity.class.getName());

    final protected ManagerMessages managerMessages;
    final protected PlaceController placeController;
    final protected RequestService requestService;
    final protected RealmsResource realmsResource;
    final protected RealmArrayMapper realmArrayMapper;

    @Inject
    public AdminRealmsActivity(AdminView adminView,
                               AdminNavigation.Presenter adminNavigationPresenter,
                               AdminRealms view,
                               ManagerMessages managerMessages,
                               PlaceController placeController,
                               RequestService requestService,
                               RealmsResource realmsResource,
                               RealmArrayMapper realmArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.requestService = requestService;
        this.realmsResource = realmsResource;
        this.realmArrayMapper = realmArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
        adminContent.setPresenter(this);

        realmsResource.getRealms(requestService.createRequestParams(new ObjectMapperCallback<>(
                realmArrayMapper,
                200,
                adminContent::setRealms,
                ex -> eventBus.dispatch(new ShowFailureEvent(
                    managerMessages.failureLoadingResource(ex.getMessage()),
                    10000
                ))
            )
        ));
    }

    @Override
    public void onRealmSelected(RealmRepresentation realm) {
        placeController.goTo(new AdminRealmPlace(realm.getRealm()));
    }

    @Override
    public void createRealm() {
        placeController.goTo(new AdminRealmPlace());
    }
}
