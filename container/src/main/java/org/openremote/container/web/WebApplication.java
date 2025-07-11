/*
 * Copyright 2016, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.web;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.openremote.model.Container;

import jakarta.ws.rs.core.Application;

public class WebApplication extends Application {

  private static final Logger LOG = Logger.getLogger(WebApplication.class.getName());

  protected final Container container;
  protected final Set<Class<?>> classes;
  protected final Set<Object> singletons;

  public WebApplication(
      Container container, Collection<Class<?>> apiClasses, Collection<Object> apiSingletons) {
    this.container = container;
    this.classes = apiClasses != null ? new HashSet<>(apiClasses) : null;
    this.singletons = apiSingletons != null ? new HashSet<>(apiSingletons) : null;
  }

  @Override
  public Set<Class<?>> getClasses() {
    return classes;
  }

  @SuppressWarnings("deprecation")
  @Override
  public Set<Object> getSingletons() {
    return singletons;
  }

  public Container getContainer() {
    return container;
  }
}
