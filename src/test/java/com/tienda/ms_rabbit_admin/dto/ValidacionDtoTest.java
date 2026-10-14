package com.tienda.ms_rabbit_admin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que la API no acepte nombres vacios ni configuraciones invalidas. */
class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colaConNombreVacioEsInvalida() {
        var req = new CrearColaRequest("", null, null, null, null, null, null, null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void colaConEspaciosOPrefijoAmqEsInvalida() {
        assertFalse(validator.validate(new CrearColaRequest("mi cola", null, null, null, null, null, null, null)).isEmpty());
        assertFalse(validator.validate(new CrearColaRequest("amq.mia", null, null, null, null, null, null, null)).isEmpty());
    }

    @Test
    void ttlDemasiadoBajoEsInvalido() {
        var req = new CrearColaRequest("cola.ok", true, false, 10, null, null, null, null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void colaValida() {
        var req = new CrearColaRequest("facturacion.orden-creada", true, false, 60000, 1000, "pedidos360.dlx", null, true);
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void exchangeConTipoDesconocidoEsInvalido() {
        assertFalse(validator.validate(new CrearExchangeRequest("ex.ok", "otro", true, false)).isEmpty());
    }

    @Test
    void bindingSinRoutingKeyEsInvalido() {
        assertFalse(validator.validate(new BindingRequest("pedidos360.events", "cola.ok", null)).isEmpty());
    }
}
