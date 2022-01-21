/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.event;

import org.openremote.container.security.AuthContext;
import org.openremote.model.event.shared.EventSubscription;

import java.util.function.BiFunction;

/**
 * Authorizes event subscriptions.
 * <p>
 * Note that authorization might fail because the client doesn't have the necessary
 * permissions. It might also fail if the subscription is invalid, for example, if
 * a required filter is not supplied or if the filter is not valid.
 */
@FunctionalInterface
public interface EventSubscriptionAuthorizer {
    boolean authorise(String requestRealm, AuthContext authContext, EventSubscription<?> eventSubscription);
}
