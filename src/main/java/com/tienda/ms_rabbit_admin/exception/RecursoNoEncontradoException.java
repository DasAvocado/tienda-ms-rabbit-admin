package com.tienda.ms_rabbit_admin.exception;

/** 404: la cola / exchange / binding no existe en RabbitMQ. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
}
