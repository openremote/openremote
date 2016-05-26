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
package org.openremote.manager.client.user;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class UserModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(UserControls.class).to(UserControlsImpl.class).in(Singleton.class);
        bind(UserControls.Presenter.class).to(UserControlsPresenter.class).in(Singleton.class);

        bind(UserAccountView.class).to(UserAccountViewImpl.class).in(Singleton.class);
        bind(UserAccountActivity.class);
    }

}
