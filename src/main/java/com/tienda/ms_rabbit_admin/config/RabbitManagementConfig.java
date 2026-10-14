package com.tienda.ms_rabbit_admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia la API de administracion de RabbitMQ (plugin management, puerto 15672).
 * Solo se usa para LISTAR recursos; crear/eliminar se hace por AMQP con AmqpAdmin (RabbitAdmin).
 * La conexion AMQP (host, usuario, clave) la configura Spring Boot con spring.rabbitmq.*.
 */
@Configuration
public class RabbitManagementConfig {

    @Bean
    public RestClient rabbitManagementRestClient(@Value("${app.rabbit.management.url}") String url,
                                                 @Value("${spring.rabbitmq.username}") String usuario,
                                                 @Value("${spring.rabbitmq.password}") String clave) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeaders(h -> h.setBasicAuth(usuario, clave))
                .build();
    }
}
