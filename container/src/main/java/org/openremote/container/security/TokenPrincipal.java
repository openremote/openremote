package org.openremote.container.security;

import com.nimbusds.jwt.JWTClaimsSet;

import java.security.Principal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class TokenPrincipal implements Principal {

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
        this.resourceRoles = Optional.ofNullable(claimsSet.getJSONObjectClaim(RESOURCE_ACCESS_CLAIM))
                .map(map -> map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (List<String>)entry.getValue()))).orElse(Collections.emptyMap());
        this.realmRoles = Optional.ofNullable(claimsSet.getStringListClaim(REALM_ACCESS_CLAIM)).orElse(Collections.emptyList());
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

    // TODO: Decide the role lookup logic
    public boolean isUserInRole(String role) {
        return realmRoles.contains(role) || resourceRoles.values().stream().anyMatch(roles -> roles.contains(role));
    }

    public boolean isUserInRealmRole(String role) {
        return realmRoles.contains(role);
    }
}
