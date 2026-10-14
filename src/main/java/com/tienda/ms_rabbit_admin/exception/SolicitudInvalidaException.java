package com.tienda.ms_rabbit_admin.exception;

/** 400: combinacion de parametros invalida (validaciones que no cubren las anotaciones del DTO). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) { super(mensaje); }
}
