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
package org.openremote.app.client.notifications;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.model.notification.NotificationResource;

public class NotificationsModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(NotificationEditor.class).to(NotificationEditorImpl.class).in(Singleton.class);
        bind(NotificationsView.class).to(NotificationsViewImpl.class).in(Singleton.class);
        bind(NotificationsActivity.class);
    }


    @Provides
    @Singleton
    public native NotificationResource getNotificationResource()  /*-{
        return $wnd.openremote.REST.NotificationResource;
    }-*/;
}
