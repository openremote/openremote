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
package org.openremote.manager.client.mvp;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;

import java.util.Collection;

/**
 * Our own activity class because we don't want their event bus crap.
 */
public abstract class AppActivity<T extends Place>  {

    public String mayStop() {
        return null;
    }

    public void onCancel() {
    }

    public void onStop() {
    }

    public abstract AppActivity<T> init(T place);

    /**
     * Any registrations added to the supplied collection will be unregistered automatically when the activity stops.
     */
    abstract public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations);
}
