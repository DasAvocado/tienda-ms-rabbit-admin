# ms-rabbit-admin

Microservicio **administrador de RabbitMQ** de Pedidos360: crea, consulta y elimina **colas, exchanges y bindings** mediante una API REST.

- Puerto: **8086** · Java 21 · Spring Boot 4.1.1
- Lógica encapsulada en `RabbitAdminService` (los controladores solo llaman métodos de alto nivel).
- Crear/eliminar: AMQP con `AmqpAdmin` (RabbitAdmin). Listar: API HTTP del plugin management (`:15672`).
- Seguridad: JWT de Microsoft Entra ID, scope `Menu-compra` + rol **ADMIN** (`SECURITY_ENABLED=false` solo para Postman en local).
- Validación: DTOs con Bean Validation (`@NotBlank`, `@Pattern`, `@Min/@Max`) + reglas de combinación en el servicio. Errores en JSON (`ErrorApi`).

## Endpoints

| Método y ruta | Cuerpo / parámetros | Respuestas |
|---|---|---|
| `GET /api/admin/queues` | – | 200 lista `{nombre, mensajes, consumidores, durable, argumentos}` |
| `GET /api/admin/queues/{nombre}` | – | 200 · 404 |
| `POST /api/admin/queues` | `{"nombre","durable","autoDelete","ttlMs","maxLength","deadLetterExchange","deadLetterRoutingKey","crearDlq"}` | 201 · 400 · 409 |
| `DELETE /api/admin/queues/{nombre}` | `?ifUnused=true&ifEmpty=true` (opcionales) | 204 · 404 · 409 |
| `DELETE /api/admin/queues/{nombre}/messages` | – (purga, p. ej. una DLQ) | 200 `{"eliminados": n}` · 404 |
| `GET /api/admin/exchanges` | – | 200 |
| `POST /api/admin/exchanges` | `{"nombre","tipo":"direct\|topic\|fanout\|headers","durable","autoDelete"}` | 201 · 400 · 409 |
| `DELETE /api/admin/exchanges/{nombre}` | – | 204 · 404 · 409 |
| `GET /api/admin/bindings` | – | 200 |
| `POST /api/admin/bindings` | `{"exchange","cola","routingKey"}` | 201 · 400 · 404 |
| `DELETE /api/admin/bindings` | `{"exchange","cola","routingKey"}` | 204 · 400 · 404 · 409 |

Códigos: **400** datos inválidos (nombre vacío, con espacios, prefijo `amq.`, tipo desconocido, TTL fuera de rango, `deadLetterRoutingKey` sin `deadLetterExchange`…) · **404** no existe · **409** ya existe / recurso protegido / cola con mensajes o consumidores · **502** RabbitMQ no disponible.

Los recursos que usan los microservicios (`pedidos360.events`, `pedidos360.dlx`, `ordenes.checkout`, …) están en `app.rabbit.protegidos` y **no se pueden eliminar** desde la API.

### Ejemplo (Postman): nueva cola con DLQ
```http
POST http://localhost:8086/api/admin/queues
Authorization: Bearer <token ADMIN>
Content-Type: application/json

{ "nombre": "facturacion.orden-creada", "deadLetterExchange": "pedidos360.dlx", "crearDlq": true, "ttlMs": 60000 }
```
Luego `POST /api/admin/bindings` `{"exchange":"pedidos360.events","cola":"facturacion.orden-creada","routingKey":"orden.creada"}` y compra algo: el mensaje aparece en la nueva cola (panel `:15672`).

Error de validación:
```json
{ "status": 400, "error": "Bad Request", "mensaje": "Datos de entrada invalidos",
  "campos": { "nombre": "El nombre de la cola es obligatorio" }, "fecha": "2026-10-14T16:00:00Z" }
```

La colección `postman/Pedidos360-RabbitAdmin.postman_collection.json` trae todas las peticiones.

## Ejecutar en local
```bash
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
# sin token desde Postman (solo pruebas):  SECURITY_ENABLED=false ./mvnw spring-boot:run
./mvnw test                     # pruebas unitarias (no necesitan RabbitMQ)
```

## Desplegar en EC2
Igual que los demás servicios: `cp .env.example .env && docker compose up -d --build`. Security Group: puerto **8086**.
API Gateway: `ANY /api/admin/{proxy+}` → `http://<IP>:8086/api/admin/{proxy}`.
