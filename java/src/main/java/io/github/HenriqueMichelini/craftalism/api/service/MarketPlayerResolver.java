package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class MarketPlayerResolver {

    private static final String WRITE_SCOPE_AUTHORITY = "SCOPE_api:write";

    private final String trustedMinecraftServerClientId;

    MarketPlayerResolver(String trustedMinecraftServerClientId) {
        this.trustedMinecraftServerClientId = trustedMinecraftServerClientId;
    }

    UUID resolvePlayerUuid(
        JwtAuthenticationToken authentication,
        String suppliedPlayerUuid,
        String suppliedPlayerUuidHeader,
        Supplier<String> currentSnapshotVersion
    ) {
        if (authentication == null) {
            throw unavailable(currentSnapshotVersion);
        }

        Object playerUuidClaim = authentication
            .getTokenAttributes()
            .get("player_uuid");
        if (playerUuidClaim instanceof String claimValue) {
            Optional<UUID> parsed = tryParseUuid(claimValue);
            if (parsed.isPresent()) {
                return parsed.get();
            }
        }

        Optional<UUID> subject = tryParseUuid(authentication.getName());
        if (subject.isPresent()) {
            return subject.get();
        }

        Optional<String> supplied = firstText(
            suppliedPlayerUuid,
            suppliedPlayerUuidHeader
        );
        if (supplied.isPresent() && isTrustedMinecraftServer(authentication)) {
            return tryParseUuid(supplied.get()).orElseThrow(() ->
                unavailable(currentSnapshotVersion)
            );
        }

        throw unavailable(currentSnapshotVersion);
    }

    private boolean isTrustedMinecraftServer(
        JwtAuthenticationToken authentication
    ) {
        return (
            isTrustedClientIdentity(authentication) &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                    WRITE_SCOPE_AUTHORITY.equals(authority.getAuthority())
                )
        );
    }

    private boolean isTrustedClientIdentity(
        JwtAuthenticationToken authentication
    ) {
        return (
            trustedMinecraftServerClientId.equals(authentication.getName()) ||
            trustedMinecraftServerClientId.equals(
                authentication.getTokenAttributes().get("client_id")
            ) ||
            trustedMinecraftServerClientId.equals(
                authentication.getTokenAttributes().get("azp")
            )
        );
    }

    private Optional<String> firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return Optional.of(first.trim());
        }
        if (second != null && !second.isBlank()) {
            return Optional.of(second.trim());
        }
        return Optional.empty();
    }

    private Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MarketRejectionException unavailable(
        Supplier<String> currentSnapshotVersion
    ) {
        return new MarketRejectionException(
            MarketRejectionCode.API_UNAVAILABLE,
            "Authenticated player context is unavailable.",
            HttpStatus.SERVICE_UNAVAILABLE,
            currentSnapshotVersion.get()
        );
    }
}
