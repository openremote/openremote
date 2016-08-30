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
package org.openremote.manager.client.admin.tenant;

import org.openremote.manager.client.admin.AdminContent;
import org.openremote.manager.client.widget.FormView;

public interface AdminTenant extends AdminContent, FormView {

    interface Presenter {

        void create();

        void update();

        void delete();

        void cancel();
    }

    void setPresenter(Presenter presenter);

    void setTenantDisplayName(String displayName);

    String getTenantDisplayName();

    void setTenantDisplayNameError(boolean error);

    void setTenantRealm(String realm);

    String getTenantRealm();

    void setTenantRealmError(boolean error);

    void setTenantEnabled(Boolean enabled);

    boolean getTenantEnabled();

    void setTenantEnabledError(boolean error);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);
}
