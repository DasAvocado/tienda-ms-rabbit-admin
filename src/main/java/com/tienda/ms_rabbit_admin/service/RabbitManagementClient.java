package com.tienda.ms_rabbit_admin.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Lectura de colas, exchanges y bindings desde la API HTTP de RabbitMQ (GET /api/queues/{vhost}, ...). */
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;
    private final String vhost;

    public RabbitManagementClient(@Qualifier("rabbitManagementRestClient") RestClient http,
                                  @Value("${app.rabbit.management.vhost:/}") String vhost) {
        this.http = http;
        this.vhost = vhost;
    }

    public List<Map<String, Object>> colas() {
        return get("/api/queues/{vhost}");
    }

    public List<Map<String, Object>> exchanges() {
        return get("/api/exchanges/{vhost}");
    }

    public List<Map<String, Object>> bindings() {
        return get("/api/bindings/{vhost}");
    }

    private List<Map<String, Object>> get(String ruta) {
        // El vhost "/" se codifica como %2F al expandir la variable de la URI.
        List<Map<String, Object>> datos = http.get().uri(ruta, vhost).retrieve().body(LISTA);
        return datos == null ? List.of() : datos;
    }
}
