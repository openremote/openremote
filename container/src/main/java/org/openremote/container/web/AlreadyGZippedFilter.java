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
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * A simple servlet filter that checks if the response has already been gzipped and if so, sets the
 * gzip header.
 */
public class AlreadyGZippedFilter implements Filter {

  protected Set<String> alreadyGzippedContentTypes;

  public static class GzipHeaderWrapper extends HttpServletResponseWrapper {

    private final Set<String> gzippedContentTypes;

    public GzipHeaderWrapper(HttpServletResponse response, Set<String> gzippedContentTypes) {
      super(response);
      this.gzippedContentTypes = gzippedContentTypes;
    }

    /** The DefaultServlet will call this method to set the content type. */
    @Override
    public void setContentType(String type) {
      // Set the original content type
      super.setContentType(type);

      if (type != null) {
        boolean isMatch = gzippedContentTypes.stream().anyMatch(type::startsWith);

        if (isMatch) {
          super.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
      }
    }

    @Override
    public void setHeader(String name, String value) {
      if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name) && !"gzip".equalsIgnoreCase(value)) {
        // It's trying to set Content-Encoding to something else.
        // If we know this response *is* gzipped, ignore it.
        // This is advanced; you can omit this block for now if you want.
        String contentType = getContentType();
        boolean isMatch = gzippedContentTypes.stream().anyMatch(contentType::startsWith);
        if (isMatch) {
          return; // Ignore the attempt to change the header
        }
      }
      super.setHeader(name, value);
    }

    @Override
    public void setContentLength(int len) {}

    @Override
    public void setContentLengthLong(long len) {}
  }

  public AlreadyGZippedFilter(Set<String> alreadyGzippedContentTypes) {
    this.alreadyGzippedContentTypes = alreadyGzippedContentTypes;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (response instanceof HttpServletResponse httpResponse) {
      // Create a wrapper so we can set the header before the response is committed
      GzipHeaderWrapper responseWrapper =
          new GzipHeaderWrapper(httpResponse, alreadyGzippedContentTypes);

      chain.doFilter(request, responseWrapper);
    } else {
      chain.doFilter(request, response);
    }
  }
}
