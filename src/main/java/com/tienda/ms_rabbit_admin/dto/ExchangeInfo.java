package com.tienda.ms_rabbit_admin.dto;

public record ExchangeInfo(String nombre, String tipo, boolean durable, boolean autoDelete) {}
