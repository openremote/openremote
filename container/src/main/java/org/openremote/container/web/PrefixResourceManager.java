/*
 * Copyright 2026, OpenRemote Inc.
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

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import java.io.IOException;

/**
 * A {@link ResourceManager} that mounts a delegate at a path, so it only answers the requests below
 * that path and resolves them relative to it. Requests outside the path resolve to null, which lets
 * a {@link CompositeResourceManager} fall through to its remaining managers.
 */
public class PrefixResourceManager implements ResourceManager {

  protected final String pathPrefix;
  protected final ResourceManager delegate;

  public PrefixResourceManager(String pathPrefix, ResourceManager delegate) {
    this.pathPrefix = pathPrefix.startsWith("/") ? pathPrefix : "/" + pathPrefix;
    this.delegate = delegate;
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public Resource getResource(String path) throws IOException {
    String absolutePath = path.startsWith("/") ? path : "/" + path;

    if (absolutePath.equals(pathPrefix)) {
      return delegate.getResource("/");
    }
    if (!absolutePath.startsWith(pathPrefix + "/")) {
      return null;
    }

    return delegate.getResource(absolutePath.substring(pathPrefix.length()));
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
