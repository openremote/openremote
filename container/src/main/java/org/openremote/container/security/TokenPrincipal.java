/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.container.security;

import com.nimbusds.jwt.JWTClaimsSet;

import java.security.Principal;
import java.text.ParseException;
import java.util.*;

public class TokenPrincipal implements Principal, AuthContext {

    public static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    public static final String CLIENT_ID_CLAIM = "azp";
    public static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    public static final String REALM_ACCESS_CLAIM = "realm_access";
    protected final JWTClaimsSet claimsSet;
    protected final List<String> realmRoles;
    protected final Map<String, List<String>> resourceRoles;

    @SuppressWarnings("unchecked")
    public TokenPrincipal(JWTClaimsSet claimsSet) throws Exception {
        this.claimsSet = claimsSet;
        this.resourceRoles = parseResourceAccessRoles(claimsSet);
        this.realmRoles = parseRealmRoles(claimsSet);
    }

    @Override
    public String getName() {
        return claimsSet.getSubject();
    }

    public JWTClaimsSet getClaims() {
        return claimsSet;
    }

    public String getRealm() {
        return claimsSet.getIssuer().substring(claimsSet.getIssuer().lastIndexOf('/') + 1);
    }

    public String getSubject() {
        return claimsSet.getSubject();
    }

    public String getUsername() {
        try {
            return Optional.ofNullable(claimsSet.getClaimAsString(PREFERRED_USERNAME_CLAIM)).orElseGet(claimsSet::getSubject);
        } catch (ParseException e) {
            return null;
        }
    }

    public String getClientId() {
        try {
            return claimsSet.getClaimAsString(CLIENT_ID_CLAIM);
        } catch (ParseException e) {
            return null;
        }
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public List<String> getResourceRoles(String resource) {
        return resourceRoles.get(resource);
    }

    public boolean isUserInRole(String role) {
        return realmRoles.contains(role) || resourceRoles.values().stream().anyMatch(roles -> roles.contains(role));
    }

    public boolean isUserInResourceRole(String role, String resource) {
        return resourceRoles.getOrDefault(resource, Collections.emptyList()).contains(role);
    }

    @Override
    public String getAuthenticatedRealmName() {
        return getRealm();
    }

    @Override
    public String getUserId() {
        return getSubject();
    }

    @Override
    public boolean hasRealmRole(String role) {
        return realmRoles.contains(role);
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return isUserInResourceRole(role, resource);
    }

    protected static Map<String, List<String>> parseResourceAccessRoles(JWTClaimsSet claimsSet) throws ParseException {
        Map<String, Object> resourceAccess = claimsSet.getJSONObjectClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> out = new HashMap<>(resourceAccess.size());
        for (Map.Entry<String, Object> e : resourceAccess.entrySet()) {
            String resource = e.getKey();
            Object value = e.getValue();

            if (!(value instanceof Map<?, ?> resourceObj)) {
                continue; // unexpected shape
            }

            out.put(resource, extractStringList(resourceObj.get("roles")));
        }
        return out;
    }

    protected static List<String> parseRealmRoles(JWTClaimsSet claimsSet) throws ParseException {
        Map<String, Object> realmAccess = claimsSet.getJSONObjectClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null || realmAccess.isEmpty()) {
            // Non-keycloak fallback: allow "roles" claim if you support it elsewhere
            List<String> rolesFallback = claimsSet.getStringListClaim("roles");
            return rolesFallback != null ? rolesFallback : Collections.emptyList();
        }
        return extractStringList(realmAccess.get("roles"));
    }

    protected static List<String> extractStringList(Object rolesValue) {
        if (!(rolesValue instanceof List<?> list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o == null) continue;
            // Be tolerant: JSON libs sometimes deliver non-String primitives
            out.add(String.valueOf(o));
        }
        return out;
    }
}
