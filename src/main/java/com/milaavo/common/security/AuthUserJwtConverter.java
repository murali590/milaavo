package com.milaavo.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AuthUserJwtConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = parseUserId(jwt.getSubject());

        return new AuthUserAuthenticationToken(
                new AuthUser(userId),
                jwt,
                List.of());
    }

    private UUID parseUserId(String subject) {
        if (subject == null) {
            throw new InvalidBearerTokenException("Token has no subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new InvalidBearerTokenException("Token subject is not a valid user id");
        }
    }
}