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


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.openremote.model.Constants.REALM_PARAM_NAME;

/**
 * A servlet {@link Filter} that removes the realm from the full request path at the specified path segment index and
 * adds it as a {@link org.openremote.model.Constants#REALM_PARAM_NAME} request header. If the
 * {@link org.openremote.model.Constants#REALM_PARAM_NAME} header is already set, this filter does nothing.
 */
public class RealmPathExtractorFilter implements Filter {

    private final int realmPathIndex;

    public RealmPathExtractorFilter(int realmPathIndex) {
        this.realmPathIndex = realmPathIndex;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // Do nothing if header already present
        if (req.getHeader(REALM_PARAM_NAME) != null) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = req.getRequestURI(); // includes contextPath
        String contextPath = Optional.ofNullable(req.getContextPath()).orElse("");
        String pathWithinApp = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;

        // Split into non-empty segments
        List<String> segments = new ArrayList<>();
        for (String s : pathWithinApp.split("/")) {
            if (!s.isEmpty()) segments.add(s);
        }

        if (segments.size() <= realmPathIndex) {
            // Not enough segments to extract realm; just continue
            chain.doFilter(request, response);
            return;
        }

        String realm = segments.get(realmPathIndex);

        // Build a rewritten path with the realm segment removed
        StringBuilder rewrittenPath = new StringBuilder();
        rewrittenPath.append(contextPath);
        for (int i = 0; i < segments.size(); i++) {
            if (i == realmPathIndex) continue;
            rewrittenPath.append("/").append(segments.get(i));
        }
        if (rewrittenPath.isEmpty()) {
            rewrittenPath.append("/");
        }

        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(req) {

            @Override
            public String getHeader(String name) {
                if (REALM_PARAM_NAME.equalsIgnoreCase(name)) {
                    return realm;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (REALM_PARAM_NAME.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(realm));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> names = new LinkedHashSet<>();
                Enumeration<String> e = super.getHeaderNames();
                while (e.hasMoreElements()) {
                    names.add(e.nextElement());
                }
                // Ensure it exists even if missing originally
                names.add(REALM_PARAM_NAME);
                return Collections.enumeration(names);
            }

            @Override
            public String getRequestURI() {
                return rewrittenPath.toString();
            }

            @Override
            public StringBuffer getRequestURL() {
                // Rebuild from original URL but swap path. Keep scheme/host/port.
                StringBuffer original = super.getRequestURL();
                try {
                    URL url = new URL(original.toString());
                    int port = url.getPort();
                    String authority = port == -1 ? url.getHost() : (url.getHost() + ":" + port);
                    return new StringBuffer(url.getProtocol() + "://" + authority + getRequestURI());
                } catch (MalformedURLException ex) {
                    // Fallback: best-effort
                    return new StringBuffer(original.substring(0, original.indexOf(super.getRequestURI())) + getRequestURI());
                }
            }
        };

        chain.doFilter(wrapped, response);
    }
}
