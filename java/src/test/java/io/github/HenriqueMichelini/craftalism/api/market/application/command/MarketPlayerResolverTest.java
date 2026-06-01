package io.github.HenriqueMichelini.craftalism.api.market.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class MarketPlayerResolverTest {

    private static final UUID PLAYER_UUID = UUID.fromString(
        "110e8400-e29b-41d4-a716-446655440000"
    );
    private static final UUID SUPPLIED_UUID = UUID.fromString(
        "210e8400-e29b-41d4-a716-446655440000"
    );

    private final MarketPlayerResolver resolver = new MarketPlayerResolver(
        "minecraft-server"
    );

    @Test
    void resolvePlayerUuid_prefersPlayerUuidClaim() {
        UUID resolved = resolver.resolvePlayerUuid(
            authentication("not-a-uuid", PLAYER_UUID.toString(), null),
            SUPPLIED_UUID.toString(),
            null,
            () -> "market:any"
        );

        assertEquals(PLAYER_UUID, resolved);
    }

    @Test
    void resolvePlayerUuid_usesSubjectUuidWhenClaimIsMissing() {
        UUID resolved = resolver.resolvePlayerUuid(
            authentication(PLAYER_UUID.toString(), null, null),
            null,
            null,
            () -> "market:any"
        );

        assertEquals(PLAYER_UUID, resolved);
    }

    @Test
    void resolvePlayerUuid_allowsTrustedMinecraftServerSuppliedUuid() {
        UUID resolved = resolver.resolvePlayerUuid(
            authentication("minecraft-server", null, "minecraft-server"),
            SUPPLIED_UUID.toString(),
            null,
            () -> "market:any"
        );

        assertEquals(SUPPLIED_UUID, resolved);
    }

    @Test
    void resolvePlayerUuid_rejectsUntrustedSuppliedUuidWithCurrentSnapshot() {
        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                resolver.resolvePlayerUuid(
                    authentication("not-a-uuid", null, "untrusted-client"),
                    SUPPLIED_UUID.toString(),
                    null,
                    () -> "market:current"
                )
        );

        assertEquals(MarketRejectionCode.API_UNAVAILABLE, exception.getCode());
        assertEquals("market:current", exception.getSnapshotVersion());
    }

    private JwtAuthenticationToken authentication(
        String subject,
        String playerUuidClaim,
        String clientId
    ) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300));
        if (playerUuidClaim != null) {
            builder.claim("player_uuid", playerUuidClaim);
        }
        if (clientId != null) {
            builder.claim("client_id", clientId);
        }
        return new JwtAuthenticationToken(
            builder.build(),
            List.of(new SimpleGrantedAuthority("SCOPE_api:write"))
        );
    }
}
