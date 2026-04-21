# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Descripción del proyecto
Sistema de gestión para restaurantes. Maneja órdenes, menú, inventario, empleados,
clientes, delivery, ventas, gastos e ingresos. Multi-restaurante desde su raíz
(cada entidad pertenece a un `restaurant_id`).

---

## Stack tecnológico

| Componente | Versión / Detalle |
|------------|-------------------|
| Lenguaje | Java 21 (target); JVM Maven: 25.0.2; JVM IDE: 21 (`java-21-openjdk`) |
| Framework | Spring Boot **4.0.5** |
| ORM | Spring Data JPA + Hibernate 7.x |
| Base de datos | MySQL 8.4 (local, puerto 3306) |
| Build | Maven |
| Seguridad | Spring Security + JWT (jjwt 0.11.5) — sesiones en tabla `sessions` |
| Mappers | MapStruct 1.5.5 (`@Mapper(componentModel = "spring")`) |
| Documentación | SpringDoc OpenAPI 2.5 → Swagger UI en `/swagger-ui.html` |
| Validación | `spring-boot-starter-validation` (jakarta.validation) |
| Testing | JUnit 5 + Mockito · H2 en memoria para tests de integración (`@ActiveProfiles("test")`) |
| Logging | Logback (`logback-spring.xml`) + `logstash-logback-encoder` 8.0 para JSON en prod |
| Lombok | Disponible; usado en services (`@Slf4j`). **No usar en entidades** — tienen getters/setters manuales. |

**Puerto del servidor:** `8082` (configurado en `env.properties` vía `spring.config.import`)

**CORS:** Orígenes permitidos: `http://localhost:5173` (Vite frontend dev) y `http://your-frontend-domain.com`. Configurado en `CorsConfig.java`.

---

## Comandos del proyecto

```bash
# Compilar (también genera mappers MapStruct en target/generated-sources/)
mvn clean install

# Arrancar en desarrollo
mvn spring-boot:run

# Arrancar con perfil específico
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Correr tests
mvn test

# Test específico
mvn test -Dtest=NombreDelTest

# JAR ejecutable
mvn clean package -DskipTests
```

---

## Testing

### Tests unitarios (Mockito)
- Clase: `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`
- En Spring Boot 4.x `@MockBean` se reemplaza por `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`)

### Tests de integración
- Clase: `@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- El perfil `test` activa `src/test/resources/application-test.properties` que apunta a H2 en memoria con `ddl-auto=create-drop`
- `@AutoConfigureMockMvc` viene de `org.springframework.boot.webmvc.test.autoconfigure` (package reorganizado en Spring Boot 4)
- No requiere MySQL — H2 recrea el schema desde las entidades en cada ejecución
- H2 URL usa `MODE=MySQL;NON_KEYWORDS=VALUE` para compatibilidad con sintaxis MySQL
- Los repos pueden mockearse selectivamente con `@MockitoBean` mientras el stack de seguridad completo permanece real (`SecurityConfig`, `JwtAuthFilter`, `JwtUtil`, `BCryptPasswordEncoder`)
- `ObjectMapper` se instancia directamente en los tests de integración (`new ObjectMapper()`), no como `@Autowired`, porque no siempre está auto-configurado como bean en el perfil `test`

### Asserciones
- Tests unitarios y de integración usan **AssertJ**: `assertThat(...)`, `assertThatThrownBy(...).isInstanceOf(...).hasMessageContaining(...)`

```bash
# Correr solo tests de integración
mvn test -Dtest="*IntegrationTest"

# Correr solo tests unitarios
mvn test -Dtest="*ServiceImplTest"
```

---

## Estructura de paquetes

```
com.nortcali.api
├── config/              # SecurityConfig, CorsConfig, JwtAuthFilter, OpenApiConfig
├── security/            # JwtUtil — generación y validación de tokens JWT
├── controller/          # REST Controllers — solo reciben y delegan al service
├── service/             # Interfaces de servicio
│   └── impl/            # Implementaciones (@Service @Transactional)
├── repository/          # Interfaces JPA (JpaRepository<Entity, Long>)
├── entity/              # Entidades JPA
│   ├── enums/           # Enums del dominio (OrderStatus, PaymentMethod, etc.)
│   └── converter/       # AttributeConverter para enums → lowercase en DB
├── dto/
│   ├── request/         # DTOs de entrada — clases normales con @Valid
│   └── response/        # DTOs de salida — Java records inmutables
├── mapper/              # MapStruct mappers (entity ↔ DTO)
├── exception/           # Excepciones custom + GlobalExceptionHandler
└── util/                # FolioGenerator y helpers
```

**Notas arquitectónicas no obvias:**
- **JWT flow:** `JwtUtil` (en `security/`) genera y valida tokens; `JwtAuthFilter` (en `config/`) es el filtro de Spring Security que los intercepta en cada request.
- **GeoService:** Country, State y City comparten una sola interfaz `GeoService` y su impl `GeoServiceImpl` — no hay servicios separados por entidad.
- **EmployeeDetailsService:** Implementa `UserDetailsService` de Spring Security; carga el empleado por username para la autenticación JWT.
- **AuthController — excepción a la regla:** Es el único controller que inyecta repos directamente (`EmployeeRepository`, `SessionRepository`) y `JwtUtil` + `EmployeeMapper`. No existe `AuthService` — la lógica de login/logout/refresh es orquestación delgada que vive en el controller. El resto de controllers siguen la regla estrictamente.
- **SecurityConfig — modelo de acceso:**

  | Nivel | Endpoints |
  |---|---|
  | `permitAll` | `/auth/login`, `/auth/logout`, Swagger, `GET /actuator/health` |
  | `ADMIN` | `/employee-roles/**`, `/countries/**`, `/states/**`, `/cities/**` |
  | `ADMIN` o `MANAGER` | `/restaurants` y `/restaurants/{id}` (CRUD del restaurante), `/units/**`, `/sales-sources/**` |
  | Cualquier autenticado | Todo lo demás (órdenes, menú, inventario, caja, etc.) |

  `@EnableMethodSecurity` activo — se pueden añadir `@PreAuthorize` en controllers/services.
  Los roles vienen de `Employee.role` (String); `EmployeeDetailsService` los convierte a `ROLE_<NOMBRE>`.

- **Validación de sesión en `JwtAuthFilter`:** Además de la firma JWT (jjwt), el filtro consulta `sessions` para verificar `is_active=true` y `expires_at > now()`. Si la sesión es inválida, NO se setea autenticación (no hace `return 401` directo) — Spring Security resuelve 401 para endpoints protegidos y permite el paso a `permitAll` como `/logout`. Las sesiones expiradas se marcan `is_active=false` automáticamente.
- **Paginación:** Los endpoints paginados usan `@ParameterObject @PageableDefault(size=20) Pageable` y devuelven `Page<T>`. Endpoints paginados: `orders`, `sales`, `expenses`, `incomes`.
- **GET listas incluyen inactivos:** Los endpoints de lista devuelven todos los registros incluyendo `isActive = false`. El filtrado por activos es responsabilidad del consumidor.
- **Logging:** `logback-spring.xml` en `src/main/resources/` — el appender activo se selecciona por `<springProfile>`. Dev: consola coloreada. Prod: JSON a stdout + archivo rotativo (`logs/{app}.log`). Test: consola mínima. Los niveles (`logging.level.*`) se controlan desde los `application-{profile}.properties`.

---

## Convenciones de código

### General
- Entidades **NUNCA** se exponen en endpoints — siempre DTOs
- Toda respuesta en `ResponseEntity<?>`
- `@ControllerAdvice` → `GlobalExceptionHandler` en `exception/`
- Logging con `@Slf4j` — nunca `System.out.println`
- Fechas en UTC internamente (`LocalDateTime.now(ZoneOffset.UTC)`)
- Nombres de clases/métodos/variables en **inglés**; comentarios en **español**

### Controllers
- Solo: recibir, validar con `@Valid`, delegar al service, devolver `ResponseEntity`
- Sin lógica de negocio. Sin inyección directa de repositorios.
- Prefijo: `@RequestMapping("/api/v1/recurso")`

### Services
- Toda la lógica de negocio en `service/impl/`
- Patrón: interfaz `XService` → impl `XServiceImpl`
- `@Service @Transactional` en la clase; `@Transactional(readOnly = true)` en GET
- Lanzar únicamente excepciones del paquete `exception/`

### Repositories
- Extender `JpaRepository<Entity, Long>`
- Queries JPQL con `@Query`; SQL nativo con `@Query(nativeQuery = true)`

### DTOs
- **Request**: clase normal + anotaciones de validación (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@DecimalMin`)
- **Response**: Java record inmutable
- Nunca exponer: `password_hash`, tokens, datos internos de auditoría
- **`BigDecimal` → JSON string:** todos los campos monetarios/decimales (`amount`, `salePrice`, `unitCost`, `currentStock`, etc.) se serializan como `string` en JSON, no como `number`. Aserciones en tests deben comparar como string: `assertThat(json).isEqualTo("15.00")`

### Entidades JPA
- Sin Lombok — getters/setters manuales, constructor vacío + constructor con parámetros
- `@Column(name = "snake_case")` para campos cuyo nombre Java difiere de la columna DB
- Campos booleanos con `is_active`: usar `@Column(name = "is_active")`
- Timestamps con `@PrePersist`
- **Convención setter de `isActive`:** el setter se llama `setActive(boolean)`, no `setIsActive`. Esto hace que MapStruct derive el nombre de propiedad como `active`.

### Mappers — patrón obligatorio para `isActive`
Todos los mappers que convierten una entidad con `isActive` a un Java Record con componente `boolean isActive` **deben** declarar explícitamente:
```java
@Mapping(source = "active", target = "isActive")
XxxResponse toResponse(Xxx entity);
```
Sin esto, MapStruct no puede emparejar la propiedad `active` del entity (derivada del getter `isActive()`) con el componente `isActive` del record, y el campo siempre queda `false`.

### Enums y conversores
Los enums del dominio se almacenan en **lowercase** en MySQL. Cada uno tiene su `AttributeConverter`:

```
entity/enums/          ← enum Java (UPPERCASE)
entity/converter/      ← convierte PENDING ↔ "pending" en DB
```

Enums existentes: `OrderStatus`, `OrderType`, `OrderSource`, `PaymentMethod`,
`MovementType`, `CashSessionStatus`, `PeriodType`, `PromotionType`.

`PromotionType` tiene valores DB no estándar (`"2x1"`), por eso NO usa `.name().toLowerCase()` en el mapper — usa `entity.getType().getValue()` y el converter `PromotionTypeConverter` busca por `getValue()` en lugar de `valueOf()`.

### Excepciones custom

| Clase | HTTP | Estado |
|-------|------|--------|
| `ResourceNotFoundException` | 404 | ✅ Implementada |
| `BusinessRuleException` | 422 | ✅ Implementada |
| `DuplicateResourceException` | 409 | ✅ Implementada |
| `AuthenticationException` (Spring Security) | 401 | ✅ Manejada en `GlobalExceptionHandler` |
| `UnauthorizedException` (custom) | 401 | ⏳ Pendiente |
| `ForbiddenException` | 403 | ⏳ Pendiente |

**Formato de respuesta de error** (`GlobalExceptionHandler` → `ErrorResponse` record):
```json
{ "status": 404, "error": "Not Found", "message": "...", "timestamp": "..." }
```
Para errores de validación (`400`), la estructura es diferente — usa el campo `fields` (no `message`):
```json
{ "status": 400, "error": "Validation Failed", "fields": { "fieldName": "mensaje" }, "timestamp": "..." }
```
Usar este formato al escribir asserciones en tests de integración.

---

## Base de datos — Schema: `nortcali`

`ddl-auto=validate` — Hibernate **solo valida**, nunca modifica.
El schema se gestiona con scripts SQL en `src/main/resources/db/`.

> **Advertencia — columnas extra no mapeadas:** La DB tiene columnas adicionales en
> varias tablas (`customers.last_name`, `orders.delivery_address`, `employees.hire_date`,
> etc.) que aún no están mapeadas en las entidades. Hibernate `validate` las ignora.
> Al añadir mapeos nuevos, verificar siempre contra la DB real antes de arrancar.

### Estado de implementación de módulos
Todos los módulos están implementados (entities + repos + DTOs + mappers + services + controllers):

| Módulo | Tablas principales |
|--------|--------------------|
| Geográfico | `countries`, `states`, `cities` |
| Restaurantes | `restaurants` |
| Empleados & Auth | `employees`, `sessions`, `employee_restaurants` (join table ManyToMany) |
| Roles de empleado | `employee_roles` — catálogo: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN, DELIVERY |
| Menú | `menu_categories`, `menu_items`, `menu_item_variants` |
| Inventario | `units`, `supplies`, `inventory_movements` |
| Recetas | `recipes`, `recipe_ingredients` |
| Clientes & Delivery | `customers`, `delivery_drivers` |
| Órdenes | `orders`, `order_items`, `order_item_extras`, `order_status_history`, `payments` |
| Gastos | `expense_categories`, `expenses` |
| Ingresos | `income_categories`, `incomes` |
| Ventas | `sales_sources`, `sales`, `sale_items` |
| Caja | `cash_sessions`, `cash_session_items` |
| Financiero | `financial_periods` |
| Combos | `combos`, `combo_items` |
| Promociones | `promotions`, `promotion_items` |

### Notas de alineación entidad ↔ DB conocidas

- **`Employee`**: la entidad usa `@ManyToMany` via `employee_restaurants`. La DB también tiene una columna `restaurant_id` directa en `employees` (datos legacy) — está mapeada como `@Column(name = "restaurant_id") Long restaurantId` y se asigna en `create()` junto con el ManyToMany. Usar siempre `findByRestaurantsId(Long)` para consultar empleados por restaurante.
- **`customers.total_orders`**: `INT` en DB → mapeado como `Integer` en la entidad.
- **`orders.folio`**: `VARCHAR(20)` en DB. El formato `ORD-{id}-{yyyyMMdd}-{seq}` cabe en 20 chars para IDs de restaurante ≤ 99.
- **`sales.cash_session_id`**: FK nullable a `cash_sessions`. Se asocia automáticamente al crear la venta desde una orden entregada si hay sesión abierta.

### Vistas SQL (usar con `nativeQuery = true`)
| Vista | Descripción |
|-------|-------------|
| `vw_monthly_summary` | Resumen financiero por mes |
| `vw_menu_performance` | Rendimiento de platillos |

---

## Valores enumerados (DB: lowercase)

| Campo | Valores |
|-------|---------|
| `orders.status` | `confirmed` → `preparing` → `ready` → `delivered` \| `cancelled` (las órdenes nacen en `confirmed`, no en `pending`) |
| `orders.order_type` | `dine_in` \| `takeout` \| `delivery` |
| `orders.source` | `pos` \| `whatsapp` \| `phone` \| `rappi` \| `uber_eats` \| `web` |
| `inventory_movements.movement_type` | `entrada` \| `salida` \| `merma` \| `ajuste` |
| `payments.method` | `efectivo` \| `tarjeta_credito` \| `tarjeta_debito` \| `transferencia` \| `rappi` \| `uber_eats` \| `otro` |
| `cash_sessions.status` | `open` \| `closed` |
| `financial_periods.period_type` | `daily` \| `weekly` \| `monthly` |

---

## Reglas de negocio críticas

1. **Multi-restaurante:** Siempre filtrar por `restaurant_id` — nunca devolver datos de todos los restaurantes sin filtro explícito
2. **Soft-delete:** Tablas con `is_active` nunca se borran físicamente — `entity.setActive(false); repo.save(entity)`
3. **Stock:** Las órdenes nacen en `CONFIRMED` y los insumos se descuentan al crearse (`deductInventory` en `OrderServiceImpl.create()`). Si `currentStock < minimumStock`, `log.warn(...)` (no bloquear)
4. **Folio de orden:** `FolioGenerator.generateOrderFolio(restaurantId, date, sequence)` → `ORD-{id}-{yyyyMMdd}-{seq4}`. La secuencia se calcula con `countByFolioPrefix` (LIKE en el folio) — NO con COUNT por fecha para evitar problemas de zona horaria con MVCC. `OrderServiceImpl.create()` usa `Isolation.READ_COMMITTED` para evitar snapshots obsoletos en creaciones concurrentes.
5. **Historial de estado:** Cada cambio de `orders.status` inserta en `order_status_history`. El primer registro va con `fromStatus = null`
6. **Transiciones de estado:** Solo se permiten las definidas en `OrderServiceImpl.ALLOWED_TRANSITIONS`
7. **Corte de caja:** Solo una `cash_session` con `status = 'open'` por restaurante → lanza `BusinessRuleException`
8. **Costo de receta:** `calculatedCost = quantity * supply.unitCost` — recalculado en `RecipeServiceImpl` en cada upsert
9. **Comisión de venta:** `commission = total * commissionPct / 100` con `RoundingMode.HALF_UP`
10. **Ganancia neta período:** `grossIncome + totalIncomes - totalCommissions - totalExpenses`
11. **Password:** BCrypt en `EmployeeServiceImpl`. Nunca en controllers. Nunca en response DTOs.
12. **Venta auto-creada al entregar orden:** Al transicionar a `DELIVERED`, `OrderServiceImpl` llama `saleService.createFromOrder(orderId, employeeId)` en un bloque try/catch (fallo no revierte el estado). `createFromOrder` usa `@Transactional(propagation = REQUIRES_NEW)`. La `SalesSource` se resuelve con fallback: nombre exacto del `OrderSource` → `"pos"` → primera activa → `BusinessRuleException`. La venta se asocia a la `CashSession` activa si existe (`cash_session_id` nullable).
13. **`totalSales` en sesión de caja abierta:** `CashSessionServiceImpl.getCurrent()` calcula `totalSales` dinámicamente con `saleRepo.sumTotalByCashSessionId(session.getId())` — el campo almacenado en `cash_sessions.total_sales` solo se persiste al cerrar la sesión.

---

## Endpoints implementados

### Auth (`/api/v1/auth`)
- `POST /login` — JWT + registra sesión en `sessions` con `expiresAt`
- `POST /logout` — soft-delete de sesión activa (`permitAll`)
- `GET  /me` — empleado autenticado (del `SecurityContext`)
- `POST /refresh` — rota el token: invalida sesión actual, emite nuevo JWT con nueva sesión

### Geográfico
- `GET/POST/PUT/DELETE /api/v1/countries`
- `GET/POST/PUT/DELETE /api/v1/states?countryId={id}`
- `GET/POST/PUT/DELETE /api/v1/cities?stateId={id}`

### Restaurantes
- `GET/POST/PUT/DELETE /api/v1/restaurants`

### Empleados
- `GET/POST /api/v1/restaurants/{restaurantId}/employees`
- `GET/PUT  /api/v1/employees/{id}`
- `PUT      /api/v1/employees/{id}/status`

### Menú
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/menu/categories`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/menu/items`
- `GET/POST/PUT/DELETE /api/v1/menu-items/{itemId}/variants`
- `GET/POST            /api/v1/menu-items/{itemId}/recipe`

### Unidades de medida
- `GET/POST/PUT        /api/v1/units`
- `GET/PUT             /api/v1/units/{id}`
- Sin DELETE — referenciada como FK por `supplies` y `recipe_ingredients`

### Inventario
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/supplies`
- `GET/PUT/DELETE      /api/v1/restaurants/{restaurantId}/supplies/{id}`
- `GET                 /api/v1/restaurants/{restaurantId}/supplies/low-stock`
- `GET/POST            /api/v1/supplies/{supplyId}/movements`

### Clientes
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/customers`

### Delivery
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/drivers`
- `GET                 /api/v1/restaurants/{restaurantId}/drivers/available`

### Órdenes
- `GET/POST            /api/v1/restaurants/{restaurantId}/orders` (paginado; `?status=confirmed&status=preparing` multi-valor; `?date=YYYY-MM-DD` filtro por día)
- `GET                 /api/v1/restaurants/{restaurantId}/orders/{id}`
- `PUT                 /api/v1/orders/{id}/status`
- `GET                 /api/v1/orders/{id}/history`
- `POST                /api/v1/orders/{id}/payments`

### Gastos
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/expense-categories`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/expenses` (paginado — `?page=0&size=20&sort=expenseDate,desc`)

### Ingresos
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/income-categories`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/incomes` (paginado — `?page=0&size=20&sort=incomeDate,desc`)

### Ventas
- `GET/POST/DELETE     /api/v1/restaurants/{restaurantId}/sales` (paginado)
- `GET                 /api/v1/restaurants/{restaurantId}/sales/by-source`

### Fuentes de venta
- `GET/POST/PUT/DELETE /api/v1/sales-sources`
- `GET                 /api/v1/sales-sources/{id}`

### Roles de empleado
- `GET/POST/PUT/DELETE /api/v1/employee-roles`
- `GET                 /api/v1/employee-roles/{id}`

### Combos
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/combos`
- `GET/PUT/DELETE      /api/v1/restaurants/{restaurantId}/combos/{id}`

### Promociones
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/promotions`
- `GET/PUT/DELETE      /api/v1/restaurants/{restaurantId}/promotions/{id}`

### Caja
- `POST /api/v1/restaurants/{restaurantId}/cash-sessions/open`
- `POST /api/v1/cash-sessions/{id}/close`
- `GET  /api/v1/restaurants/{restaurantId}/cash-sessions/current`

### Financiero
- `GET  /api/v1/restaurants/{restaurantId}/financial/summary?period=daily&date=YYYY-MM-DD`
- `GET  /api/v1/restaurants/{restaurantId}/financial/summary?period=monthly&month=YYYY-MM`
- `GET  /api/v1/restaurants/{restaurantId}/financial/periods`
- `POST /api/v1/restaurants/{restaurantId}/financial/periods`
- `POST /api/v1/restaurants/{restaurantId}/financial/periods/{id}/close`

---

## Configuración de la aplicación

La configuración está dividida en perfiles. El perfil `dev` es el predeterminado.

| Archivo | Perfil | Propósito |
|---|---|---|
| `application.properties` | base | Config compartida: nombre, puerto, driver, `ddl-auto` |
| `application-dev.properties` | `dev` | Desarrollo local — importa `env.properties` |
| `application-prod.properties` | `prod` | Producción — ⏳ pendiente (placeholders con env vars) |
| `application-test.properties` | `test` | Tests — H2 en memoria |

```bash
# Desarrollo (por defecto)
mvn spring-boot:run

# Producción
java -jar nortcali-api.jar --spring.profiles.active=prod
# o
SPRING_PROFILES_ACTIVE=prod java -jar nortcali-api.jar
```

`env.properties` (no versionado, solo dev):
```properties
APP_NAME=nortcali-api
DB_DATABASE=nortcali
DB_USER=root
DB_PASSWORD=...
```

**Diferencias clave entre perfiles:**
| Propiedad | dev | prod |
|---|---|---|
| `show-sql` | `true` | `false` |
| `jwt.expiration` | 86400000 (24 h) | 28800000 (8 h) |
| `jwt.secret` | hardcoded en archivo | `${JWT_SECRET}` env var |
| `logging.level.com.nortcali` | `DEBUG` | `INFO` |
| `datasource.url` | `localhost:3306` | `${DB_HOST}:${DB_PORT}` |

---

## Schema SQL

Scripts en `src/main/resources/db/`:
- `V2__new_modules.sql` — crea todas las tablas de los módulos nuevos (`CREATE TABLE IF NOT EXISTS`)
- `V3__employee_roles.sql` — crea `employee_roles` e inserta 6 roles iniciales
- `V4__expense_category_isactive.sql` — agrega `is_active TINYINT(1) NOT NULL DEFAULT 1` a `expense_categories`
- `V5__combos_and_promotions.sql` — crea `combos`, `combo_items`, `promotions`, `promotion_items`
- `V6__sale_cash_session.sql` — agrega `cash_session_id BIGINT NULL FK` a `sales`

Para aplicar manualmente:
```bash
mysql -u root -p nortcali < src/main/resources/db/V2__new_modules.sql
mysql -u root -p nortcali < src/main/resources/db/V3__employee_roles.sql
mysql -u root -p nortcali < src/main/resources/db/V4__expense_category_isactive.sql
mysql -u root -p nortcali < src/main/resources/db/V5__combos_and_promotions.sql
mysql -u root -p nortcali < src/main/resources/db/V6__sale_cash_session.sql
```

Al agregar nuevas entidades: crear `V{N}__descripcion.sql` con los `ALTER TABLE` o `CREATE TABLE` necesarios y ejecutarlo **antes** de arrancar la app (Hibernate fallará en `validate` si las tablas no existen).

### Docker

```bash
# Primera vez
cp .env.example .env      # Editar .env con valores reales
docker compose up -d      # Levanta MySQL + API (perfil prod)
docker compose logs -f    # Ver logs en tiempo real

# Rebuildar imagen tras cambios de código
docker compose up -d --build

# Parar y eliminar contenedores (datos de MySQL persisten en volumen)
docker compose down

# Eliminar también los volúmenes (BORRA DATOS)
docker compose down -v
```

**Variables requeridas en `.env`** (ver `.env.example`):
`DB_DATABASE`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mínimo 64 chars — `openssl rand -base64 64`)

Los scripts SQL de `src/main/resources/db/` se ejecutan automáticamente al crear el contenedor MySQL por primera vez (montados en `/docker-entrypoint-initdb.d/`).

---

## Documentación y herramientas de desarrollo

- `docs/FRONTEND_CONTEXT.md` — guía de integración para el frontend: interfaces TypeScript completas para todos los DTOs, reglas de negocio desde la perspectiva del consumidor, y detalles de serialización. Útil como referencia rápida de shapes de request/response sin leer el código Java.
- `docs/API_COLLECTION.md` — colección de todos los endpoints con ejemplos de request/response, iconos de rol requerido (🔒 autenticado, 👑 ADMIN, 🏢 ADMIN o MANAGER). Base URL: `http://localhost:8082`.
- `docs/nortcali.postman_collection.json` — colección Postman importable para pruebas manuales.
- `TASKS.md` — seguimiento activo de tareas de desarrollo: ✅ completado, 🔴 prioritario, 🟡 pendiente calidad, 🟢 pruebas, 🔵 futuro.

---

## Lo que Claude NO debe hacer

- ❌ Usar `ddl-auto=create` o `ddl-auto=update`
- ❌ Exponer entidades JPA directamente en controllers
- ❌ Retornar `password_hash` ni tokens en ningún response DTO
- ❌ Poner lógica de negocio en un Controller
- ❌ Inyectar repositorios directamente en Controllers (excepción: `AuthController` — es el único por diseño explícito)
- ❌ Borrar registros físicamente en tablas con `is_active`
- ❌ Crear queries que devuelvan datos de múltiples restaurantes sin filtro `restaurant_id`
- ❌ Usar `System.out.println` — solo `@Slf4j`
- ❌ Dejar `TODO` sin implementar en código entregado
- ❌ Usar Lombok (`@Data`, `@Builder`, etc.) en entidades JPA — las entidades usan getters/setters manuales
