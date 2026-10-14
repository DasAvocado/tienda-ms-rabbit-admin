package com.tienda.ms_rabbit_admin.dto;

import java.util.Map;

/** Estado de una cola. */
public record ColaInfo(String nombre, long mensajes, int consumidores, Boolean durable, Map<String, Object> argumentos) {}
