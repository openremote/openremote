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
package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

// We have GWT compiler erasure problems if this is an interface, so let's use an abstract class
@JsType
public abstract class KeycloakCallback {

    abstract public KeycloakCallback success(Consumer<Boolean> successFn);

    abstract public KeycloakCallback error(Runnable errorFn);
}
