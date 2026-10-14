package com.tienda.ms_rabbit_admin.controller;

import com.tienda.ms_rabbit_admin.dto.ColaInfo;
import com.tienda.ms_rabbit_admin.dto.CrearColaRequest;
import com.tienda.ms_rabbit_admin.dto.Validaciones;
import com.tienda.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Administracion de COLAS (scope Menu-compra + rol ADMIN).
 *
 *   GET    /api/admin/queues                    200 lista de colas
 *   GET    /api/admin/queues/{nombre}           200 | 404
 *   POST   /api/admin/queues                    201 | 400 | 409
 *   DELETE /api/admin/queues/{nombre}           204 | 404 | 409   (?ifUnused=true&ifEmpty=true)
 *   DELETE /api/admin/queues/{nombre}/messages  200 {"eliminados": n} | 404   (purga, p.ej. una DLQ)
 */
@RestController
@RequestMapping("/api/admin/queues")
@PreAuthorize("@accesoAdmin.permitido(authentication)")
public class ColaController {

    private final RabbitAdminService admin;

    public ColaController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<ColaInfo> listar() {
        return admin.listarColas();
    }

    @GetMapping("/{nombre}")
    public ColaInfo obtener(@PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre) {
        return admin.obtenerCola(nombre);
    }

    @PostMapping
    public ResponseEntity<ColaInfo> crear(@Valid @RequestBody CrearColaRequest req) {
        ColaInfo creada = admin.crearCola(req);
        return ResponseEntity.created(URI.create("/api/admin/queues/" + creada.nombre())).body(creada);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre,
            @RequestParam(defaultValue = "false") boolean ifUnused,
            @RequestParam(defaultValue = "false") boolean ifEmpty) {
        admin.eliminarCola(nombre, ifUnused, ifEmpty);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{nombre}/messages")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Integer> purgar(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre) {
        return Map.of("eliminados", admin.purgarCola(nombre));
    }
}
