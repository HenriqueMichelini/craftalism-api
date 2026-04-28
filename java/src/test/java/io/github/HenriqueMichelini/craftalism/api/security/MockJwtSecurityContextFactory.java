// src/test/java/.../api/security/MockJwtSecurityContextFactory.java
package io.github.HenriqueMichelini.craftalism.api.security;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class MockJwtSecurityContextFactory
    implements WithSecurityContextFactory<WithMockJwt>
{

    @Override
    public SecurityContext createSecurityContext(WithMockJwt annotation) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", annotation.subject())
            .claim("scope", String.join(" ", annotation.scopes()))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300));
        if (!annotation.playerUuid().isBlank()) {
            jwtBuilder.claim("player_uuid", annotation.playerUuid());
        }
        Jwt jwt = jwtBuilder.build();

        List<SimpleGrantedAuthority> authorities = Arrays.stream(
            annotation.scopes()
        )
            .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
            .collect(Collectors.toList());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, authorities));
        return context;
    }
}
