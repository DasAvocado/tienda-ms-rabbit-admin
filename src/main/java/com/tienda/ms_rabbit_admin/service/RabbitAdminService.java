package com.tienda.ms_rabbit_admin.service;

import com.tienda.ms_rabbit_admin.dto.*;
import com.tienda.ms_rabbit_admin.exception.ConflictoException;
import com.tienda.ms_rabbit_admin.exception.RecursoNoEncontradoException;
import com.tienda.ms_rabbit_admin.exception.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODA la logica de administracion de RabbitMQ (crear / consultar / eliminar colas, exchanges y bindings).
 * Los controladores solo llaman a estos metodos de alto nivel y no conocen AmqpAdmin ni el protocolo AMQP.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final String SUFIJO_DLQ = ".dlq";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementClient management;
    private final Set<String> protegidos;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
                              RabbitTemplate rabbitTemplate,
                              RabbitManagementClient management,
                              @Value("${app.rabbit.protegidos:}") List<String> protegidos) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.management = management;
        this.protegidos = Set.copyOf(protegidos);
    }

    // ====================================================================== COLAS

    public List<ColaInfo> listarColas() {
        return management.colas().stream()
                .map(m -> new ColaInfo(str(m.get("name")), num(m.get("messages")), (int) num(m.get("consumers")),
                        (Boolean) m.get("durable"), mapa(m.get("arguments"))))
                .toList();
    }

    public ColaInfo obtenerCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        return new ColaInfo(info.getName(), info.getMessageCount(), info.getConsumerCount(), null, null);
    }

    public ColaInfo crearCola(CrearColaRequest req) {
        boolean crearDlq = Boolean.TRUE.equals(req.crearDlq());
        String dlx = vacioANull(req.deadLetterExchange());
        String dlRoutingKey = vacioANull(req.deadLetterRoutingKey());

        // Validaciones de combinacion de parametros (no se pueden expresar con anotaciones simples)
        if (dlRoutingKey != null && dlx == null) {
            throw new SolicitudInvalidaException("deadLetterRoutingKey requiere deadLetterExchange");
        }
        if (crearDlq && dlx == null) {
            throw new SolicitudInvalidaException("crearDlq=true requiere deadLetterExchange");
        }
        if (Boolean.FALSE.equals(req.durable()) && crearDlq) {
            throw new SolicitudInvalidaException("Una cola con DLQ debe ser durable (durable=true)");
        }
        if (existeCola(req.nombre())) {
            throw new ConflictoException("La cola '" + req.nombre() + "' ya existe");
        }
        if (dlx != null && !existeExchange(dlx)) {
            throw new SolicitudInvalidaException("El deadLetterExchange '" + dlx + "' no existe; crealo primero");
        }

        String nombreDlq = req.nombre() + SUFIJO_DLQ;
        if (crearDlq && dlRoutingKey == null) dlRoutingKey = nombreDlq;

        QueueBuilder builder = Boolean.FALSE.equals(req.durable())
                ? QueueBuilder.nonDurable(req.nombre())
                : QueueBuilder.durable(req.nombre());
        if (Boolean.TRUE.equals(req.autoDelete())) builder.autoDelete();
        if (req.ttlMs() != null) builder.ttl(req.ttlMs());
        if (req.maxLength() != null) builder.maxLength(req.maxLength());
        if (dlx != null) builder.deadLetterExchange(dlx);
        if (dlRoutingKey != null) builder.deadLetterRoutingKey(dlRoutingKey);

        Queue cola = builder.build();
        amqpAdmin.declareQueue(cola);
        log.info("[ADMIN] Cola creada: {} args={}", cola.getName(), cola.getArguments());

        if (crearDlq) {
            if (!existeCola(nombreDlq)) {
                amqpAdmin.declareQueue(QueueBuilder.durable(nombreDlq).build());
            }
            amqpAdmin.declareBinding(new Binding(nombreDlq, Binding.DestinationType.QUEUE, dlx, dlRoutingKey, null));
            log.info("[ADMIN] DLQ creada: {} <- {} ({})", nombreDlq, dlx, dlRoutingKey);
        }
        return new ColaInfo(cola.getName(), 0, 0, cola.isDurable(), cola.getArguments());
    }

    /**
     * @param soloSiNoUsada true = falla (409) si la cola tiene consumidores
     * @param soloSiVacia   true = falla (409) si la cola tiene mensajes
     */
    public void eliminarCola(String nombre, boolean soloSiNoUsada, boolean soloSiVacia) {
        verificarNoProtegido(nombre);
        ColaInfo info = obtenerCola(nombre); // 404 si no existe
        if (soloSiNoUsada && info.consumidores() > 0) {
            throw new ConflictoException("La cola '" + nombre + "' tiene " + info.consumidores() + " consumidor(es)");
        }
        if (soloSiVacia && info.mensajes() > 0) {
            throw new ConflictoException("La cola '" + nombre + "' tiene " + info.mensajes() + " mensaje(s)");
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("[ADMIN] Cola eliminada: {}", nombre);
    }

    /** Vacia la cola (util para limpiar una DLQ despues de revisarla). Devuelve los mensajes eliminados. */
    public int purgarCola(String nombre) {
        obtenerCola(nombre); // 404 si no existe
        int eliminados = amqpAdmin.purgeQueue(nombre);
        log.info("[ADMIN] Cola purgada: {} ({} mensajes)", nombre, eliminados);
        return eliminados;
    }

    // ====================================================================== EXCHANGES

    public List<ExchangeInfo> listarExchanges() {
        return management.exchanges().stream()
                .filter(m -> !str(m.get("name")).isEmpty() && !str(m.get("name")).startsWith("amq."))
                .map(m -> new ExchangeInfo(str(m.get("name")), str(m.get("type")),
                        Boolean.TRUE.equals(m.get("durable")), Boolean.TRUE.equals(m.get("auto_delete"))))
                .toList();
    }

    public ExchangeInfo crearExchange(CrearExchangeRequest req) {
        if (existeExchange(req.nombre())) {
            throw new ConflictoException("El exchange '" + req.nombre() + "' ya existe");
        }
        boolean durable = !Boolean.FALSE.equals(req.durable());
        boolean autoDelete = Boolean.TRUE.equals(req.autoDelete());

        ExchangeBuilder builder = new ExchangeBuilder(req.nombre(), req.tipo()).durable(durable);
        if (autoDelete) builder.autoDelete();
        Exchange exchange = builder.build();

        amqpAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} ({})", req.nombre(), req.tipo());
        return new ExchangeInfo(req.nombre(), req.tipo(), durable, autoDelete);
    }

    public void eliminarExchange(String nombre) {
        verificarNoProtegido(nombre);
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        amqpAdmin.deleteExchange(nombre);
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }

    // ====================================================================== BINDINGS

    public List<BindingInfo> listarBindings() {
        return management.bindings().stream()
                .filter(m -> !str(m.get("source")).isEmpty())          // omite los bindings implicitos del exchange por defecto
                .filter(m -> "queue".equals(m.get("destination_type")))
                .map(m -> new BindingInfo(str(m.get("source")), str(m.get("destination")), str(m.get("routing_key"))))
                .toList();
    }

    public BindingInfo crearBinding(BindingRequest req) {
        verificarExistenAmbos(req.exchange(), req.cola());
        amqpAdmin.declareBinding(binding(req.exchange(), req.cola(), req.routingKey()));
        log.info("[ADMIN] Binding creado: {} --{}--> {}", req.exchange(), req.routingKey(), req.cola());
        return new BindingInfo(req.exchange(), req.cola(), req.routingKey());
    }

    public void eliminarBinding(BindingRequest req) {
        verificarNoProtegido(req.cola());
        verificarExistenAmbos(req.exchange(), req.cola());
        amqpAdmin.removeBinding(binding(req.exchange(), req.cola(), req.routingKey()));
        log.info("[ADMIN] Binding eliminado: {} --{}--> {}", req.exchange(), req.routingKey(), req.cola());
    }

    // ====================================================================== apoyo

    boolean existeCola(String nombre) {
        return amqpAdmin.getQueueInfo(nombre) != null;
    }

    /** Declaracion PASIVA: RabbitMQ responde 404 NOT_FOUND si el exchange no existe (no lo crea). */
    boolean existeExchange(String nombre) {
        try {
            rabbitTemplate.execute(canal -> canal.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpException e) {
            if (esNotFound(e)) return false;
            throw e;
        }
    }

    private void verificarExistenAmbos(String exchange, String cola) {
        if (!existeExchange(exchange)) throw new RecursoNoEncontradoException("El exchange '" + exchange + "' no existe");
        if (!existeCola(cola)) throw new RecursoNoEncontradoException("La cola '" + cola + "' no existe");
    }

    private void verificarNoProtegido(String nombre) {
        if (protegidos.contains(nombre)) {
            throw new ConflictoException("'" + nombre + "' es parte de la topologia de Pedidos360 y no se puede eliminar desde la API");
        }
    }

    private static Binding binding(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private static boolean esNotFound(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m != null && (m.contains("NOT_FOUND") || m.contains("reply-code=404"))) return true;
        }
        return false;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }
}
