package com.tienda.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** POST /api/admin/exchanges   { "nombre": "facturacion.events", "tipo": "topic", "durable": true } */
public record CrearExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "direct|topic|fanout|headers", message = "tipo debe ser: direct, topic, fanout o headers")
        String tipo,

        Boolean durable,      // por defecto true
        Boolean autoDelete) {} // por defecto false
