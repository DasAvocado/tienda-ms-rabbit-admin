package com.tienda.ms_rabbit_admin.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regla de acceso a la API de administracion: scope "Menu-compra" + rol ADMIN de Entra ID.
 * Se usa con @PreAuthorize("@accesoAdmin.permitido(authentication)").
 * Con SECURITY_ENABLED=false (solo pruebas locales) se permite todo.
 */
@Component("accesoAdmin")
public class AccesoAdmin {

    private final boolean securityEnabled;

    public AccesoAdmin(@Value("${app.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean permitido(Authentication authentication) {
        if (!securityEnabled) return true;
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains("SCOPE_Menu-compra") && authorities.contains("ROLE_ADMIN");
    }
}
