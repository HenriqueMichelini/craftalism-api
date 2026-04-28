package io.github.HenriqueMichelini.craftalism.api.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {
        http
            .cors(Customizer.withDefaults()) // uses the bean below
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    // Dashboard is currently a static SPA with no OAuth2 login
                    // flow, so read-only API routes must be public.
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/**")
                    .hasAuthority("SCOPE_api:write")
                    .requestMatchers(HttpMethod.PUT, "/api/**")
                    .hasAuthority("SCOPE_api:write")
                    .requestMatchers(HttpMethod.PATCH, "/api/**")
                    .hasAuthority("SCOPE_api:write")
                    .requestMatchers(HttpMethod.DELETE, "/api/**")
                    .hasAuthority("SCOPE_api:write")
                    .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(
                oauth2 -> oauth2.jwt(Customizer.withDefaults()) // validate Bearer JWTs
            );

        return http.build();
    }

    /**
     * CORS must be declared here so Spring Security's filter chain picks it up.
     * The existing CorsConfig (WebMvcConfigurer) only runs after security, so
     * preflight OPTIONS requests were being rejected with 401 before reaching MVC.
     * You can delete CorsConfig.java entirely once this bean is in place.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
            List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:25565"
            )
        );
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
