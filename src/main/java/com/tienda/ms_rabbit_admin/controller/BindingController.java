package com.tienda.ms_rabbit_admin.controller;

import com.tienda.ms_rabbit_admin.dto.BindingInfo;
import com.tienda.ms_rabbit_admin.dto.BindingRequest;
import com.tienda.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administracion de BINDINGS exchange -> cola (scope Menu-compra + rol ADMIN).
 *
 *   GET    /api/admin/bindings     200 lista
 *   POST   /api/admin/bindings     201 | 400 | 404     {"exchange","cola","routingKey"}
 *   DELETE /api/admin/bindings     204 | 400 | 404 | 409  (mismo cuerpo que POST)
 */
@RestController
@RequestMapping("/api/admin/bindings")
@PreAuthorize("@accesoAdmin.permitido(authentication)")
public class BindingController {

    private final RabbitAdminService admin;

    public BindingController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<BindingInfo> listar() {
        return admin.listarBindings();
    }

    @PostMapping
    public ResponseEntity<BindingInfo> crear(@Valid @RequestBody BindingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(admin.crearBinding(req));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@Valid @RequestBody BindingRequest req) {
        admin.eliminarBinding(req);
        return ResponseEntity.noContent().build();
    }
}
