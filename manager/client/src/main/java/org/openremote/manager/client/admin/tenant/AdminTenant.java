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
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.validation.ConstraintViolation;

public interface AdminTenant extends AdminContent {

    interface Presenter {

        void createTenant(Tenant realm, Runnable onComplete);

        void updateTenant(Tenant realm, Runnable onComplete);

        void deleteTenant(Tenant realm, Runnable onComplete);

        void cancel();
    }

    void setPresenter(Presenter presenter);

    void setTenant(Tenant realm);

    void showErrors(ConstraintViolation[] violations);

    void showSuccess(String message);
}
