package com.tienda.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /api/admin/bindings   { "exchange": "pedidos360.events", "cola": "facturacion.orden-creada", "routingKey": "orden.creada" } */
public record BindingRequest(
        @NotBlank(message = "exchange es obligatorio")
        @Pattern(regexp = Validaciones.NOMBRE, message = "exchange: " + Validaciones.MSG_NOMBRE)
        String exchange,

        @NotBlank(message = "cola es obligatoria")
        @Pattern(regexp = Validaciones.NOMBRE, message = "cola: " + Validaciones.MSG_NOMBRE)
        String cola,

        @NotNull(message = "routingKey es obligatoria (use \"\" para exchanges fanout)")
        @Pattern(regexp = Validaciones.ROUTING_KEY, message = Validaciones.MSG_ROUTING_KEY)
        String routingKey) {}
