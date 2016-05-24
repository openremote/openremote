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
import org.openremote.manager.client.admin.RealmMapper;
import org.openremote.manager.client.admin.navigation.AdminNavigation;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;
import org.openremote.manager.shared.http.ObjectMapperCallback;
import org.openremote.manager.shared.security.RealmsResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AdminRealmActivity
    extends AbstractAdminActivity<AdminRealmPlace, AdminRealm>
    implements AdminRealm.Presenter {

    private static final Logger LOG = Logger.getLogger(AdminRealmActivity.class.getName());

    final protected ManagerMessages managerMessages;
    final protected PlaceController placeController;
    final protected EventBus eventBus;
    final protected RequestService requestService;
    final protected RealmsResource realmsResource;
    final protected RealmMapper realmMapper;

    protected String realmId;

    @Inject
    public AdminRealmActivity(AdminView adminView,
                              AdminNavigation.Presenter adminNavigationPresenter,
                              AdminRealm view,
                              ManagerMessages managerMessages,
                              PlaceController placeController,
                              EventBus eventBus,
                              RequestService requestService,
                              RealmsResource realmsResource,
                              RealmMapper realmMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.eventBus = eventBus;
        this.requestService = requestService;
        this.realmsResource = realmsResource;
        this.realmMapper = realmMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    protected AppActivity<AdminRealmPlace> init(AdminRealmPlace place) {
        realmId = place.getRealmId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
        adminContent.setPresenter(this);

        if (realmId != null) {
            loadRealm();
        } else {
            adminContent.setRealm(new RealmRepresentation());
        }
    }

    @Override
    public void createRealm(RealmRepresentation realm) {

    }

    @Override
    public void updateRealm(RealmRepresentation realm) {

    }

    @Override
    public void deleteRealm(RealmRepresentation realm) {

    }

    protected void loadRealm() {
        realmsResource.getRealm(
            requestService.createRequestParams(new ObjectMapperCallback<>(
                    realmMapper,
                    200,
                    adminContent::setRealm,
                    ex -> eventBus.dispatch(new ShowFailureEvent(
                        managerMessages.failureLoadingResource(ex.getMessage()),
                        10000
                    ))
                )
            ),
            realmId
        );
    }
}
