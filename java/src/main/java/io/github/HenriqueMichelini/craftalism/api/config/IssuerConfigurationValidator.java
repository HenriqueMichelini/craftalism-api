package io.github.HenriqueMichelini.craftalism.api.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IssuerConfigurationValidator {

    private final String resourceServerIssuer;
    private final String expectedIssuer;

    public IssuerConfigurationValidator(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String resourceServerIssuer,
        @Value("${craftalism.auth.expected-issuer-uri}") String expectedIssuer
    ) {
        this.resourceServerIssuer = resourceServerIssuer;
        this.expectedIssuer = expectedIssuer;
    }

    @PostConstruct
    public void validateIssuer() {
        String normalizedResourceServerIssuer = normalize(resourceServerIssuer);
        String normalizedExpectedIssuer = normalize(expectedIssuer);

        if (!normalizedResourceServerIssuer.equals(normalizedExpectedIssuer)) {
            throw new IllegalStateException(
                "Issuer mismatch detected. spring.security.oauth2.resourceserver.jwt.issuer-uri=" +
                resourceServerIssuer +
                " but craftalism.auth.expected-issuer-uri=" +
                expectedIssuer +
                ". Align API/Auth/Deployment AUTH_ISSUER_URI configuration before startup."
            );
        }
    }

    private String normalize(String issuer) {
        String value = issuer == null ? "" : issuer.trim();
        if (value.isEmpty()) {
            return value;
        }
        URI parsed = URI.create(value);
        String normalized = parsed.toString();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
