package com.tienda.ms_rabbit_admin.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Este microservicio actua como OAuth2 Resource Server: valida por si mismo los JWT de Azure
 * (firma, vigencia, issuer y audience), aunque el API Gateway ya los valide en el borde.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtConverter,
                                            @Value("${app.security.enabled:true}") boolean securityEnabled) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        if (securityEnabled) {
            // Toda la API de administracion exige JWT valido; scope + rol ADMIN se validan con @PreAuthorize.
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        } else {
            // SOLO PARA PRUEBAS LOCALES (SECURITY_ENABLED=false): Postman sin token.
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    /** "scp" -> SCOPE_xxx   y   "roles" -> ROLE_XXX */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${app.security.require-roles:true}") boolean requireRoles) {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter(); // lee "scope" y "scp"

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())));
            }
            // Solo para depurar: si REQUIRE_ROLES=false, un usuario sin roles se trata como CLIENTE.
            if (!requireRoles && (roles == null || roles.isEmpty())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.azure.tenant-id}") String tenantId,
                          @Value("${app.azure.client-id}") String clientId) {
        String issuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        String jwks = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();

        OAuth2TokenValidator<Jwt> conIssuer = JwtValidators.createDefaultWithIssuer(issuer); // issuer + exp/nbf
        List<String> audienciasValidas = List.of(clientId, "api://" + clientId);
        OAuth2TokenValidator<Jwt> conAudience = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.stream().anyMatch(audienciasValidas::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token no fue emitido para esta API (audience)", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(conIssuer, conAudience));
        return decoder;
    }
}
