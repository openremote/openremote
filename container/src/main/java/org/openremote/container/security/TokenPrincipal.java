package org.openremote.container.security;

import com.nimbusds.jwt.JWTClaimsSet;

import java.security.Principal;

public class TokenPrincipal implements Principal {

    protected final JWTClaimsSet claimsSet;

    public TokenPrincipal(JWTClaimsSet claimsSet) {
        this.claimsSet = claimsSet;
    }

    @Override
    public String getName() {
        return claimsSet.getSubject();
    }

    public JWTClaimsSet getClaims() {
        return claimsSet;
    }
}
