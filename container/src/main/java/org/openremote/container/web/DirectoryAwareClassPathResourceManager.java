/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.container.web;

import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;

import java.io.IOException;

/**
 *
 */
public class DirectoryAwareClassPathResourceManager extends ClassPathResourceManager {

   public DirectoryAwareClassPathResourceManager(ClassLoader loader, Package p) {
      super(loader, p);
   }

   public DirectoryAwareClassPathResourceManager(ClassLoader classLoader, String prefix) {
      super(classLoader, prefix);
   }

   public DirectoryAwareClassPathResourceManager(ClassLoader classLoader) {
      super(classLoader);
   }

   @Override
   public Resource getResource(String path) throws IOException {
      Resource res = super.getResource(path);

      // Check if this is actually a directory not a file
      if (res == null || (res.getContentLength() != null && res.getContentLength() == 0)) {

         Resource dirRes = super.getResource(path + "/");

         // The DefaultServlet will see dirRes.isDirectory() == true and force a redirect
         if (dirRes != null) {
            return dirRes;
         }
      }

      return res;
   }
}
