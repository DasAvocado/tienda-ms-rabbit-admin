package com.tienda.ms_rabbit_admin.controller;

import com.tienda.ms_rabbit_admin.dto.CrearExchangeRequest;
import com.tienda.ms_rabbit_admin.dto.ExchangeInfo;
import com.tienda.ms_rabbit_admin.dto.Validaciones;
import com.tienda.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Administracion de EXCHANGES (scope Menu-compra + rol ADMIN).
 *
 *   GET    /api/admin/exchanges             200 lista (sin los amq.* del sistema)
 *   POST   /api/admin/exchanges             201 | 400 | 409
 *   DELETE /api/admin/exchanges/{nombre}    204 | 404 | 409
 */
@RestController
@RequestMapping("/api/admin/exchanges")
@PreAuthorize("@accesoAdmin.permitido(authentication)")
public class ExchangeController {

    private final RabbitAdminService admin;

    public ExchangeController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<ExchangeInfo> listar() {
        return admin.listarExchanges();
    }

    @PostMapping
    public ResponseEntity<ExchangeInfo> crear(@Valid @RequestBody CrearExchangeRequest req) {
        ExchangeInfo creado = admin.crearExchange(req);
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + creado.nombre())).body(creado);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre) {
        admin.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }
}
