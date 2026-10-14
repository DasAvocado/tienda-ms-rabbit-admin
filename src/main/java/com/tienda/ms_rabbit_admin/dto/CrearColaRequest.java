package com.tienda.ms_rabbit_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /api/admin/queues
 * <pre>
 * { "nombre": "facturacion.orden-creada", "durable": true, "autoDelete": false,
 *   "ttlMs": 60000, "maxLength": 1000,
 *   "deadLetterExchange": "pedidos360.dlx", "deadLetterRoutingKey": "facturacion.orden-creada.dlq" }
 * </pre>
 */
public record CrearColaRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String nombre,

        Boolean durable,      // por defecto true
        Boolean autoDelete,   // por defecto false

        @Min(value = 1000, message = "ttlMs debe ser >= 1000 ms")
        @Max(value = 604_800_000, message = "ttlMs no puede superar 7 dias")
        Integer ttlMs,

        @Min(value = 1, message = "maxLength debe ser >= 1")
        @Max(value = 1_000_000, message = "maxLength no puede superar 1.000.000")
        Integer maxLength,

        @Pattern(regexp = Validaciones.NOMBRE, message = "deadLetterExchange: " + Validaciones.MSG_NOMBRE)
        String deadLetterExchange,

        @Pattern(regexp = Validaciones.ROUTING_KEY, message = "deadLetterRoutingKey: " + Validaciones.MSG_ROUTING_KEY)
        String deadLetterRoutingKey,

        /** true = crea tambien "<nombre>.dlq" y la enlaza al deadLetterExchange (requiere deadLetterExchange). */
        Boolean crearDlq) {}
