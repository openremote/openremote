package org.openremote.manager.client.admin.navigation;

import com.google.inject.Inject;
import org.openremote.manager.client.ManagerHistoryMapper;
import org.openremote.manager.client.admin.AdminPlace;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.realms.AdminRealmsPlace;
import org.openremote.manager.client.admin.users.AdminUsersPlace;

public class AdminNavigationPresenter implements AdminNavigation.Presenter {

    final protected AdminNavigation view;
    final protected ManagerHistoryMapper managerHistoryMapper;

    @Inject
    public AdminNavigationPresenter(AdminNavigation view,
                                    ManagerHistoryMapper managerHistoryMapper) {
        this.view = view;
        this.managerHistoryMapper = managerHistoryMapper;

        view.setPresenter(this);
    }

    @Override
    public AdminNavigation getView() {
        return view;
    }

    @Override
    public String getAdminOverviewPlaceToken() {
        return managerHistoryMapper.getToken(new AdminOverviewPlace());
    }

    @Override
    public String getAdminRealmsPlaceToken(String realm) {
        return managerHistoryMapper.getToken(new AdminRealmsPlace(realm));
    }

    @Override
    public String getAdminUsersPlaceToken(String userId) {
        return managerHistoryMapper.getToken(new AdminUsersPlace(userId));
    }

    @Override
    public void setActivePlace(AdminPlace place) {
        view.onPlaceChange(place);
    }
}
