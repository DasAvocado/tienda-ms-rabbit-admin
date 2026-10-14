package com.tienda.ms_rabbit_admin.exception;

/** 409: el recurso ya existe, esta protegido o no cumple la condicion para eliminarlo. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) { super(mensaje); }
}
