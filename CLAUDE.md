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
| Framework | Spring Boot **4.0.6** |
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
| Thymeleaf | En `pom.xml` pero **no se usa** — la API es REST puro. Dependencia vestigial; ignorar. |

**Puerto del servidor:** `8082` (configurado en `env.properties` vía `spring.config.import`)

**CORS:** Orígenes permitidos: `http://localhost:5173` (Vite frontend dev) y `http://your-frontend-domain.com`. Configurado en `CorsConfig.java`.

**Scheduling:** `@EnableScheduling` activo en `NortcaliApiApplication`. `EndOfDayScheduler` (en `config/`) corre a las 23:55 todos los días — llama `orderService.closeDay()` por cada restaurante activo. Errores por restaurante se loggean y se continúa con el siguiente.

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

# Test específico (clase)
mvn test -Dtest=NombreDelTest

# Test específico (método)
mvn test -Dtest=NombreDelTest#nombreDelMetodo

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

- **Validación de sesión en `JwtAuthFilter`:** Dos caminos distintos:
  1. **Firma JWT inválida** (jjwt lanza excepción en `getUsername`): el filtro devuelve `401` directamente con `response.setStatus(401); return`.
  2. **Sesión inválida o expirada** (firma válida pero `is_active=false` o `expires_at < now()`): NO se setea autenticación pero tampoco se retorna directamente — Spring Security resuelve 401 para endpoints protegidos y deja pasar a `permitAll` como `/logout`. Las sesiones expiradas se marcan `is_active=false` automáticamente en este paso.
- **Paginación:** Los endpoints paginados usan `@ParameterObject @PageableDefault(size=20) Pageable` y devuelven `Page<T>`. Endpoints paginados: `orders`, `sales`, `expenses`, `incomes`.
- **CORS — métodos permitidos:** `CorsConfig` lista explícitamente los métodos HTTP: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Al añadir un endpoint con un método nuevo, verificar que esté en esta lista — su ausencia produce 403 en el DispatcherServlet sin ninguna traza en Spring Security (el filtro CORS de Spring MVC corre después del filter chain).
- **Restricción de rol en sub-recursos:** Para endpoints dentro de `/restaurants/{id}/...` que requieren rol específico, usar `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")` en el método del controller en lugar de `requestMatchers` en `SecurityConfig`. Los `requestMatchers` en SecurityConfig solo cubren paths de nivel superior (sin sub-recursos de restaurante).
- **GET listas incluyen inactivos:** Los endpoints de lista devuelven todos los registros incluyendo `isActive = false`. El filtrado por activos es responsabilidad del consumidor.
- **Filtros de fecha parciales — fechas centinela:** Cuando un endpoint acepta `startDate` y `endDate` opcionales y el repo solo expone un método `findBy...BetweenOrderBy...`, el service usa fechas centinela para el límite ausente: `LocalDate.of(1970, 1, 1)` si solo hay `endDate`, y `LocalDate.of(9999, 12, 31)` si solo hay `startDate`. Seguir este patrón al agregar filtros de fecha a nuevos servicios (ver `ExpenseServiceImpl.getByRestaurant` y `SaleServiceImpl.getByRestaurant`).
- **Logging:** `logback-spring.xml` en `src/main/resources/` — el appender activo se selecciona por `<springProfile>`. Dev: consola coloreada. Prod: JSON a stdout + archivo rotativo (`logs/{app}.log`). Test: consola mínima. Los niveles (`logging.level.*`) se controlan desde los `application-{profile}.properties`.
- **Cadena de modificadores (Modifier chain):** El flujo es `ModifierGroup → Modifier → VariantModifier → OrderItemModifier`. `ModifierGroup` es el catálogo de grupos por restaurante; `Modifier` son las opciones dentro del grupo; `VariantModifier` vincula un `Modifier` a una `MenuItemVariant` con un precio concreto; `OrderItemModifier` es una **snapshot** — copia `modifierName` y `groupName` del catálogo en el momento de crear la orden, por lo que renombrar o borrar el modificador no afecta órdenes históricas. El campo `modifier_id` en `order_item_modifiers` es nullable por este mismo motivo.

---

## Convenciones de código

### General
- Entidades **NUNCA** se exponen en endpoints — siempre DTOs
- Toda respuesta en `ResponseEntity<?>`
- `@ControllerAdvice` → `GlobalExceptionHandler` en `exception/`
- Logging con `@Slf4j` — nunca `System.out.println`
- Fechas en UTC — `LocalDateTime.now()` es seguro porque la JVM se fija en UTC vía `@PostConstruct` en `NortcaliApiApplication`. Jackson y Hibernate también forzados a UTC en `application.properties`. No agregar zona explícita en `now()`.
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
- Campos booleanos `is*`: la columna DB lleva el prefijo `is_` en snake_case (`@Column(name = "is_active")`, `@Column(name = "is_paid")`). El campo Java se declara con el mismo nombre (`private boolean isActive`, `private boolean isPaid`).
- Timestamps con `@PrePersist`
- **Convención setter de campos `is*`:** el setter omite el prefijo `is` — `setActive(boolean)`, `setPaid(boolean)`, no `setIsActive`/`setIsPaid`. MapStruct deriva la propiedad del setter, no del campo: `active`, `paid`, etc.

### Mappers — patrón obligatorio para campos booleanos `is*`
Cualquier campo boolean `is*` en una entidad (p.ej. `isActive`, `isPaid`) produce una asimetría: MapStruct deriva el nombre de propiedad del setter (`active`, `paid`) pero el record usa el nombre del componente (`isActive`, `isPaid`). El mapping explícito es obligatorio:
```java
@Mapping(source = "active", target = "isActive")  // isActive / setActive
@Mapping(source = "paid",   target = "isPaid")    // isPaid / setPaid
XxxResponse toResponse(Xxx entity);
```
Sin esto el campo queda siempre `false`. Al agregar cualquier nuevo boolean `is*` a una entidad, verificar que el mapper declare su `@Mapping` correspondiente.

Al ignorar `is*` en mapeos de entrada (`toEntity`, `updateEntity`), usar el nombre de propiedad del setter:
```java
@Mapping(target = "paid", ignore = true)  // no "isPaid"
```

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
> varias tablas (p.ej. `orders.delivery_address`) que aún no están mapeadas en las
> entidades. Hibernate `validate` las ignora.
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
| Modificadores | `modifier_groups`, `modifiers`, `variant_modifiers`, `order_item_modifiers` |
| Órdenes | `orders`, `order_items`, `order_item_modifiers`, `order_status_history`, `payments` |
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
- **`customers.last_name`**: `VARCHAR(80)` nullable — ya mapeado en la entidad.
- **`orders.folio`**: `VARCHAR(40)` en DB y en la entidad (`length = 40`). El formato es `ORD-{restaurantId}-{yyyyMMdd}-{seq4}`.
- **`restaurants`**: columna `timezone VARCHAR(50) NOT NULL DEFAULT 'America/Tijuana'` — mapeada en la entidad. Los registros existentes reciben el default al aplicar V13. Se usa en `SaleServiceImpl.createFromOrder()` para derivar la fecha local correcta del negocio al crear ventas auto-generadas.
- **`sales`**: columnas `subtotal`, `payment_method`, `notes`, `customer_id`, `cash_session_id`, `order_id` — todas mapeadas en la entidad `Sale`. `folio` usa el mismo valor que la orden de origen (auto-creación) o `VTA-{id}-{yyyyMMdd}-{seq4}` (creación manual). `order_id` es nullable y tiene índice UNIQUE — `null` en ventas manuales; apunta a la orden que la generó en ventas auto-creadas. FK con `ON DELETE SET NULL` (la lógica de borrado en cascada la maneja la capa de servicio).
- **`sale_items.unit_price`**: `DECIMAL NOT NULL` — mapeado en `SaleItem`. En auto-creación se copia de `orderItem.unitPrice`; en creación manual se deriva de `subtotal / quantity`.
- **`order_items.group_label` / `sale_items.group_label`**: `VARCHAR(100) NULL` — `null` para ítems individuales; contiene el nombre del combo (ej. `"Combo Familiar"`) cuando el ítem proviene de un combo. El frontend lo envía en `OrderItemRequest.groupLabel`; `SaleServiceImpl.createFromOrder` lo propaga automáticamente al crear la venta. No hay FK al catálogo de combos — es un snapshot de texto.
- **`order_item_extras` — código muerto:** La tabla existe en DB (creada en V2) y los archivos `OrderItemExtra.java`, `OrderItemExtraRequest.java`, `OrderItemExtraResponse.java` aún están en el repo, pero ningún servicio ni controller los referencia. Fueron reemplazados por el sistema de modificadores (`order_item_modifiers`). No extender ni usar estos archivos.

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
6. **Transiciones de estado:** Solo se permiten las definidas en `OrderServiceImpl.ALLOWED_TRANSITIONS`. El mapa incluye `PENDING → CONFIRMED/CANCELLED` por retrocompatibilidad, pero `PENDING` nunca es el estado inicial de una orden nueva — `create()` siempre asigna `CONFIRMED`.
7. **Corte de caja:** Solo una `cash_session` con `status = 'open'` por restaurante → lanza `BusinessRuleException`
8. **Costo de receta:** `calculatedCost = quantity * supply.unitCost` — recalculado en `RecipeServiceImpl` en cada upsert
9. **Comisión de venta:** `commission = total * commissionPct / 100` con `RoundingMode.HALF_UP`
10. **Ganancia neta período:** `grossIncome + totalIncomes - totalCommissions - totalExpenses`
11. **Password:** BCrypt en `EmployeeServiceImpl`. Nunca en controllers. Nunca en response DTOs.
12. **Venta auto-creada al entregar orden:** Al transicionar a `DELIVERED`, `OrderServiceImpl` llama `saleService.createFromOrder(orderId, employeeId)` en un bloque try/catch (fallo no revierte el estado). `createFromOrder` usa `@Transactional(propagation = REQUIRES_NEW)`. La `SalesSource` se resuelve con fallback: nombre exacto del `OrderSource` → `"pos"` → primera activa → `BusinessRuleException`. El folio de la venta es igual al folio de la orden. Se copian `paymentMethod` y `customer` de la orden. Se asocia a la `CashSession` activa si existe.
    - **Folio ventas manuales:** `FolioGenerator.generateSaleFolio(restaurantId, date, sequence)` → `VTA-{id}-{yyyyMMdd}-{seq4}`
13. **`totalSales` en sesión de caja abierta:** `CashSessionServiceImpl.getCurrent()` calcula `totalSales` dinámicamente con `saleRepo.sumTotalByCashSessionId(session.getId())` — el campo almacenado en `cash_sessions.total_sales` solo se persiste al cerrar la sesión.
14. **Tiempo de preparación de orden:** Al transicionar a `PREPARING` se registra `preparingAt = now(UTC)`. Al transicionar a `READY`, si `preparingAt != null`, se calcula `preparationTimeSeconds = ChronoUnit.SECONDS.between(preparingAt, now)` y se guarda `readyAt`. Si la orden pasa a `CANCELLED` tras `PREPARING`, `preparingAt` queda guardado pero `readyAt` y `preparationTimeSeconds` quedan `null`. Si llega a `READY` sin haber pasado por `PREPARING`, `preparationTimeSeconds` queda `null`.
15. **`isPaid` en gastos:** Los gastos tienen `is_paid BOOLEAN NOT NULL DEFAULT FALSE`. El endpoint `PATCH /restaurants/{restaurantId}/expenses/{id}/paid` (requiere rol `ADMIN` o `MANAGER`) actualiza solo ese campo. En `create` y `update`, `isPaid` es opcional en el request (null se interpreta como false).
17. **Eliminación de orden (hard-delete):** `orders` no tiene `is_active` — el `DELETE` es físico. `OrderServiceImpl.delete()` elimina en este orden: (1) venta vinculada si existe (`saleService.deleteLinkedSale(orderId)` — hard-delete de la `Sale` y sus `SaleItem` por cascade JPA), (2) `order_status_history` (sin cascade JPA desde `Order`), (3) la orden; JPA cascadea a `order_items`, sus `order_item_modifiers`, y `payments`. Las ventas manuales (`order_id = null`) nunca se ven afectadas.

16. **Subtotal de ítem con modificadores:** `item.subtotal = (unitPrice + sum(modifierPricePerUnit)) × quantity`. El precio de los modificadores se acumula por unidad y se multiplica por la cantidad — no se suman al total de la orden por separado. `OrderItemModifierRequest` requiere `modifierId` (Long) y `price` (BigDecimal); el servicio resuelve `modifierName` y `groupName` del catálogo.

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
- `POST                /api/v1/restaurants/{restaurantId}/orders/close-day` — CASHIER, MANAGER o ADMIN · cierra en masa todas las órdenes activas del día (confirmed/preparing/ready → delivered) · 200 `{ "closedCount": N }`
- `DELETE              /api/v1/restaurants/{restaurantId}/orders/{orderId}` — solo ADMIN (`@PreAuthorize`) · 204 No Content
- `PUT                 /api/v1/orders/{id}/status`
- `GET                 /api/v1/orders/{id}/history`
- `POST                /api/v1/orders/{id}/payments`

### Gastos
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/expense-categories`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/expenses` (paginado — `?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` opcionales; sin filtro devuelve todos)
- `PATCH               /api/v1/restaurants/{restaurantId}/expenses/{id}/paid` — body `{ "isPaid": true }` · requiere ADMIN o MANAGER (`@PreAuthorize`)

### Ingresos
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/income-categories`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/incomes` (paginado — `?page=0&size=20&sort=incomeDate,desc`)

### Ventas
- `GET/POST/DELETE     /api/v1/restaurants/{restaurantId}/sales` (paginado — `?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` opcionales)
- `GET                 /api/v1/restaurants/{restaurantId}/sales/by-source` (`?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` opcionales)

### Fuentes de venta
- `GET/POST/PUT/DELETE /api/v1/sales-sources`
- `GET                 /api/v1/sales-sources/{id}`

### Roles de empleado
- `GET/POST/PUT/DELETE /api/v1/employee-roles`
- `GET                 /api/v1/employee-roles/{id}`

### Modificadores
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/modifier-groups`
- `GET/POST/PUT/DELETE /api/v1/restaurants/{restaurantId}/modifier-groups/{groupId}/modifiers`
- `GET/POST            /api/v1/menu-item-variants/{variantId}/modifiers`
- `DELETE              /api/v1/menu-item-variants/{variantId}/modifiers/{modifierId}`

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
| `application-prod.properties` | `prod` | Producción — env vars (`${DB_HOST}`, `${JWT_SECRET}`, etc.) |
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
- `V7__sale_customer.sql` — agrega `customer_id BIGINT NULL FK` a `sales`
- `V8__order_preparation_times.sql` — agrega `preparing_at DATETIME NULL`, `ready_at DATETIME NULL`, `preparation_time_seconds INT NULL` a `orders`
- `V9__expense_is_paid.sql` — agrega `is_paid BOOLEAN NOT NULL DEFAULT FALSE` a `expenses`
- `V10__modifier_groups.sql` — crea `modifier_groups`, `modifiers`, `variant_modifiers`, `order_item_modifiers`
- `V11__group_label.sql` — agrega `group_label VARCHAR(100) NULL` a `order_items` y `sale_items`
- `V12__sale_order_id.sql` — agrega `order_id BIGINT NULL UNIQUE` a `sales` con FK a `orders(id) ON DELETE SET NULL`
- `V13__restaurant_timezone.sql` — agrega `timezone VARCHAR(50) NOT NULL DEFAULT 'America/Tijuana'` a `restaurants`
- `V14__order_notes.sql` — agrega `notes TEXT NULL` a `orders` para notas o solicitudes especiales del cliente

Para aplicar manualmente:
```bash
mysql -u root -p nortcali < src/main/resources/db/V2__new_modules.sql
mysql -u root -p nortcali < src/main/resources/db/V3__employee_roles.sql
mysql -u root -p nortcali < src/main/resources/db/V4__expense_category_isactive.sql
mysql -u root -p nortcali < src/main/resources/db/V5__combos_and_promotions.sql
mysql -u root -p nortcali < src/main/resources/db/V6__sale_cash_session.sql
mysql -u root -p nortcali < src/main/resources/db/V7__sale_customer.sql
mysql -u root -p nortcali < src/main/resources/db/V8__order_preparation_times.sql
mysql -u root -p nortcali < src/main/resources/db/V9__expense_is_paid.sql
mysql -u root -p nortcali < src/main/resources/db/V10__modifier_groups.sql
mysql -u root -p nortcali < src/main/resources/db/V11__group_label.sql
mysql -u root -p nortcali < src/main/resources/db/V12__sale_order_id.sql
mysql -u root -p nortcali < src/main/resources/db/V13__restaurant_timezone.sql
mysql -u root -p nortcali < src/main/resources/db/V14__order_notes.sql
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
- ❌ Usar o extender `OrderItemExtra` / `order_item_extras` — código muerto reemplazado por el sistema de modificadores
- ❌ Editar o consultar `CLAUDE.md.bakup` — es un backup obsoleto en la raíz del repo; puede eliminarse con `rm CLAUDE.md.bakup`
