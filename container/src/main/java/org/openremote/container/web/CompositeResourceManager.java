/*
 * Copyright 2025, OpenRemote Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * A {@link ResourceManager} that looks up resources from multiple {@code ResourceManager}
 * instances, in the specified order.
 */
public class CompositeResourceManager implements ResourceManager {

  protected final List<ResourceManager> resourceManagers = new ArrayList<>();

  public CompositeResourceManager(ResourceManager... resourceManagers) {
    this.resourceManagers.addAll(Arrays.asList(resourceManagers));
  }

  public void addResourceManager(ResourceManager resourceManager) {
    resourceManagers.add(resourceManager);
  }

  @Override
  public void close() throws IOException {
    for (ResourceManager resourceManager : resourceManagers) {
      resourceManager.close();
    }
  }

  @Override
  public Resource getResource(String path) throws IOException {
    for (ResourceManager resourceManager : resourceManagers) {
      Resource resource = resourceManager.getResource(path);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }

  @Override
  public boolean isResourceChangeListenerSupported() {
    return false;
  }

  @Override
  public void registerResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
  }

  @Override
  public void removeResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
  }
}
