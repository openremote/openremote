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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.openremote.model.Constants;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * A JAX-RS filter that removes the realm from the full request path at the specified path segment index and adds it as
 * a {@link org.openremote.model.Constants#REALM_PARAM_NAME} request header. If the
 * {@link org.openremote.model.Constants#REALM_PARAM_NAME} header is already set, this filter does nothing.
 */
@Provider
@PreMatching
public class RealmPathExtractorFilter implements ContainerRequestFilter {

    protected final int realmPathIndex;

   public RealmPathExtractorFilter(int realmPathIndex) {
      this.realmPathIndex = realmPathIndex;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) throws IOException {

      // Do nothing if the realm header is already set
      if (requestContext.getHeaders().containsKey(Constants.REALM_PARAM_NAME)) {
         return;
      }

      UriInfo uriInfo = requestContext.getUriInfo();
      List<PathSegment> pathSegments = uriInfo.getPathSegments();

      if (pathSegments != null && pathSegments.size() > realmPathIndex) {
         String realm = pathSegments.get(realmPathIndex).getPath();
         requestContext.getHeaders().add(Constants.REALM_PARAM_NAME, realm);

         // Build new URI with the realm segment removed
         UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
         for (int i = 0; i < pathSegments.size(); i++) {
            if (i != realmPathIndex) {
               uriBuilder.path(pathSegments.get(i).getPath());
            }
         }

         uriBuilder.replaceQuery(uriInfo.getRequestUri().getRawQuery());
         uriBuilder.fragment(uriInfo.getRequestUri().getFragment());
         URI newUri = uriBuilder.build();

         requestContext.setRequestUri(newUri);
      }
   }
}
