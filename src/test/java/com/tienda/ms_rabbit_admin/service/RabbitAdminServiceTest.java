package com.tienda.ms_rabbit_admin.service;

import com.tienda.ms_rabbit_admin.dto.BindingRequest;
import com.tienda.ms_rabbit_admin.dto.CrearColaRequest;
import com.tienda.ms_rabbit_admin.dto.CrearExchangeRequest;
import com.tienda.ms_rabbit_admin.exception.ConflictoException;
import com.tienda.ms_rabbit_admin.exception.RecursoNoEncontradoException;
import com.tienda.ms_rabbit_admin.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Pruebas unitarias (sin RabbitMQ real): AmqpAdmin y RabbitTemplate son mocks. */
class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class); // execute(...) sin excepcion = el exchange existe
        RabbitManagementClient management = mock(RabbitManagementClient.class);
        service = new RabbitAdminService(amqpAdmin, rabbitTemplate, management, List.of("pedidos360.events", "ordenes.checkout"));
    }

    @Test
    void creaColaConDlq() {
        when(amqpAdmin.getQueueInfo(anyString())).thenReturn(null); // no existen

        var info = service.crearCola(new CrearColaRequest("facturacion.orden-creada", true, false,
                null, null, "pedidos360.dlx", null, true));

        assertEquals("facturacion.orden-creada", info.nombre());
        assertEquals("pedidos360.dlx", info.argumentos().get("x-dead-letter-exchange"));
        assertEquals("facturacion.orden-creada.dlq", info.argumentos().get("x-dead-letter-routing-key"));
        verify(amqpAdmin, times(2)).declareQueue(any(Queue.class)); // cola + DLQ
        verify(amqpAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void rechazaRoutingKeyDeDlqSinExchange() {
        var req = new CrearColaRequest("cola.prueba", true, false, null, null, null, "x.dlq", false);
        assertThrows(SolicitudInvalidaException.class, () -> service.crearCola(req));
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void rechazaColaDuplicada() {
        when(amqpAdmin.getQueueInfo("cola.prueba")).thenReturn(new QueueInformation("cola.prueba", 0, 0));
        var req = new CrearColaRequest("cola.prueba", true, false, null, null, null, null, false);
        assertThrows(ConflictoException.class, () -> service.crearCola(req));
    }

    @Test
    void noEliminaColasProtegidas() {
        assertThrows(ConflictoException.class, () -> service.eliminarCola("ordenes.checkout", false, false));
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarColaInexistenteDa404() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarCola("no.existe", false, false));
    }

    @Test
    void noEliminaColaConMensajesSiSePideVacia() {
        when(amqpAdmin.getQueueInfo("cola.llena")).thenReturn(new QueueInformation("cola.llena", 5, 0));
        assertThrows(ConflictoException.class, () -> service.eliminarCola("cola.llena", false, true));
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminaColaVacia() {
        when(amqpAdmin.getQueueInfo("cola.vacia")).thenReturn(new QueueInformation("cola.vacia", 0, 0));
        service.eliminarCola("cola.vacia", true, true);
        verify(amqpAdmin).deleteQueue("cola.vacia");
    }

    @Test
    void exchangeDuplicadoDa409() {
        // execute() no lanza excepcion -> la declaracion pasiva encontro el exchange
        assertThrows(ConflictoException.class,
                () -> service.crearExchange(new CrearExchangeRequest("facturacion.events", "topic", true, false)));
        verify(amqpAdmin, never()).declareExchange(any(Exchange.class));
    }

    @Test
    void bindingAColaInexistenteDa404() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearBinding(new BindingRequest("pedidos360.events", "no.existe", "orden.creada")));
    }
}
