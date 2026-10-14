package com.tienda.ms_rabbit_admin.dto;

import java.time.Instant;
import java.util.Map;

/** Cuerpo estandar de error de la API. */
public record ErrorApi(int status, String error, String mensaje, Map<String, String> campos, String fecha) {
    public static ErrorApi de(int status, String error, String mensaje, Map<String, String> campos) {
        return new ErrorApi(status, error, mensaje, campos, Instant.now().toString());
    }
}
