package io.github.HenriqueMichelini.craftalism.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IssuerConfigurationValidatorTest {

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(
            IssuerConfigurationValidator.class
        );

    @Test
    void startsWhenIssuerMatchesExpected() {
        contextRunner
            .withPropertyValues(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000",
                "craftalism.auth.expected-issuer-uri=http://localhost:9000"
            )
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsFastWhenIssuerMismatchesExpected() {
        contextRunner
            .withPropertyValues(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000",
                "craftalism.auth.expected-issuer-uri=http://craftalism-auth-server:9000"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("Issuer mismatch detected");
            });
    }
}
