package io.github.HenriqueMichelini.craftalism.api.security;

import java.lang.annotation.*;
import org.springframework.security.test.context.support.WithSecurityContext;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockJwtSecurityContextFactory.class)
public @interface WithMockJwt {
    String[] scopes() default { "api:read", "api:write" };
    String subject() default "minecraft-server";
    String playerUuid() default "";
}
