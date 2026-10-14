package com.tienda.ms_rabbit_admin.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Datos del usuario extraidos del JWT emitido por Microsoft Entra ID. */
public final class UsuarioActual {

    private UsuarioActual() {}

    /** Identificador estable del usuario en el tenant (claim "oid"; si no existe, "sub"). */
    public static String id(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    /**
     * Correo con el que el usuario inicio sesion.
     * Prioridad: claim "email" (buzon real; se agrega en Azure > Configuracion de token > Notificacion opcional "email")
     * -> "preferred_username" (lo que muestra MSAL en el frontend) -> "upn".
     */
    public static String email(Jwt jwt) {
        for (String claim : new String[] {"email", "preferred_username", "upn"}) {
            String valor = jwt.getClaimAsString(claim);
            if (valor != null && valor.contains("@")) return valor;
        }
        return "";
    }

    public static String nombre(Jwt jwt) {
        String nombre = jwt.getClaimAsString("name");
        return nombre != null ? nombre : email(jwt);
    }
}
