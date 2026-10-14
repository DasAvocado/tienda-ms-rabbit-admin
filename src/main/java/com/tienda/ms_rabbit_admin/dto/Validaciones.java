package com.tienda.ms_rabbit_admin.dto;

/** Reglas de nombres comunes a colas y exchanges. */
public final class Validaciones {

    private Validaciones() {}

    /**
     * 3-120 caracteres: letras, numeros, punto, guion y guion bajo; debe empezar con letra o numero.
     * Excluye el prefijo reservado "amq." (RabbitMQ no permite crearlo) y nombres con espacios.
     */
    public static final String NOMBRE = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]{2,119}$";
    public static final String MSG_NOMBRE =
            "Nombre invalido: 3-120 caracteres [A-Za-z0-9._-], debe empezar con letra/numero y no usar el prefijo 'amq.'";

    /** Routing key / patron topic: hasta 255 caracteres, sin espacios. Puede ser "" (fanout). */
    public static final String ROUTING_KEY = "^[A-Za-z0-9._*#-]{0,255}$";
    public static final String MSG_ROUTING_KEY = "Routing key invalida: hasta 255 caracteres [A-Za-z0-9._-] y comodines * #";
}
