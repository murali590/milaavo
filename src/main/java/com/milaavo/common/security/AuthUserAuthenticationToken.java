package com.milaavo.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class AuthUserAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthUser authUser;
    private final Jwt jwt;

    public AuthUserAuthenticationToken(
            AuthUser authUser,
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.authUser = authUser;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public AuthUser getPrincipal() {
        return authUser;
    }

    @Override
    public Jwt getCredentials() {
        return jwt;
    }
}