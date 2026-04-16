# TASKS.md — NortCali API

Seguimiento de tareas de desarrollo. Actualizar este archivo al iniciar o cerrar cada tarea.

---

## ✅ Completado

### Infraestructura base
- [x] Configuración de Spring Boot 4.x con MySQL, Spring Security y JWT (`jjwt 0.11.5`)
- [x] `GlobalExceptionHandler` con `ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`
- [x] `FolioGenerator` — genera folios de órdenes con formato `ORD-{restaurantId}-{yyyyMMdd}-{seq}`
- [x] MapStruct agregado al `pom.xml` (con Lombok antes en `annotationProcessorPaths`)
- [x] SpringDoc OpenAPI (`/swagger-ui.html`) agregado al `pom.xml` y configurado con Bearer JWT
- [x] `spring-boot-starter-validation` agregado al `pom.xml`

### Módulo Auth
- [x] `POST /api/v1/auth/login` — autentica, registra sesión en tabla `sessions`, devuelve JWT
- [x] `POST /api/v1/auth/logout` — invalida sesión activa (soft-delete en tabla `sessions`)
- [x] `GET  /api/v1/auth/me` — devuelve datos del empleado autenticado
- [x] `SecurityConfig` actualizado a ruta `/api/v1/auth/**`

### Módulo Empleados
- [x] `EmployeeService` + `EmployeeServiceImpl` (BCrypt en service, no en controller)
- [x] `EmployeeMapper` (MapStruct, nunca expone `password_hash`)
- [x] DTOs: `EmployeeRequest`, `EmployeeStatusRequest`, `EmployeeResponse`
- [x] `EmployeeController` refactorizado — rutas `/api/v1/restaurants/{id}/employees` y `PUT /api/v1/employees/{id}/status`
- [x] `EmployeeRepository.findByRestaurantsId(Long)` agregado

### Módulo Geográfico
- [x] `GeoService` + `GeoServiceImpl` — countries, states, cities en un solo service
- [x] `GeoMapper` (MapStruct) con `toCountryResponse`, `toStateResponse`, `toCityResponse`
- [x] DTOs: `CountryRequest/Response`, `StateRequest/Response`, `CityRequest/Response`
- [x] `StateRepository.findByCountryId(Long)` agregado
- [x] Controllers refactorizados: `CountryController`, `StateController`, `CityController` — rutas `/api/v1/`

### Módulo Restaurantes
- [x] `RestaurantService` + `RestaurantServiceImpl` (soft-delete, filtro por `cityId`)
- [x] `RestaurantMapper` (MapStruct)
- [x] DTOs: `RestaurantRequest`, `RestaurantResponse`
- [x] `RestaurantController` refactorizado — ruta `/api/v1/restaurants`

### Módulo Menú
- [x] Entities: `MenuCategory`, `MenuItem`, `MenuItemVariant`
- [x] Repos, DTOs (`request/`, `response/`), Mappers, Services + Impls
- [x] `MenuController` — categorías, ítems, variantes, receta (upsert)

### Módulo Inventario
- [x] Entities: `Unit`, `Supply`, `InventoryMovement`
- [x] Enum `MovementType` + `MovementTypeConverter` (ENTRADA/SALIDA/MERMA/AJUSTE → lowercase DB)
- [x] Lógica de movimientos: ENTRADA suma, SALIDA/MERMA restan (lanza error si stock < 0), AJUSTE reemplaza
- [x] Alerta `log.warn` si `currentStock < minimumStock` (no bloquea)
- [x] `InventoryController` — supplies, low-stock, movements, units

### Módulo Recetas
- [x] Entities: `Recipe`, `RecipeIngredient`
- [x] `calculatedCost = quantity * supply.unitCost` recalculado en cada upsert
- [x] Upsert: si ya existe receta activa para el platillo, reemplaza ingredientes en cascada
- [x] `RecipeService` + endpoints en `MenuController` (`GET/POST /menu-items/{id}/recipe`)

### Módulo Clientes y Delivery
- [x] Entities: `Customer`, `DeliveryDriver`
- [x] CRUD completo con soft-delete
- [x] `CustomerController` (`/api/v1/restaurants/{id}/customers`)
- [x] `DeliveryDriverController` (`/api/v1/restaurants/{id}/drivers` + `/available`)

### Módulo Órdenes
- [x] Entities: `Order`, `OrderItem`, `OrderItemExtra`, `OrderStatusHistory`, `Payment`
- [x] Enums con converters: `OrderStatus`, `OrderType`, `OrderSource`, `PaymentMethod`
- [x] Generación de folio `ORD-{restaurantId}-{yyyyMMdd}-{seq}`
- [x] Primer historial de estado insertado en `create` (`null → PENDING`)
- [x] Validación de transiciones de estado (mapa de transiciones permitidas)
- [x] Descuento de inventario al confirmar orden (`CONFIRMED`) via recetas; falla silenciosa con `log.warn`
- [x] `OrderController` — CRUD, `PUT /orders/{id}/status`, `GET /orders/{id}/history`, `POST /orders/{id}/payments`

### Módulo Gastos
- [x] Entities: `ExpenseCategory`, `Expense` (soft-delete)
- [x] `ExpenseRepository.sumByRestaurantAndPeriod` (usado por Financial y CashSession)
- [x] `ExpenseController` — categories y expenses bajo `/api/v1/restaurants/{id}/`

### Módulo Ingresos
- [x] Entities: `IncomeCategory`, `Income` (soft-delete)
- [x] `IncomeRepository.sumByRestaurantAndPeriod`
- [x] `IncomeController` — categories e incomes bajo `/api/v1/restaurants/{id}/`

### Módulo Ventas
- [x] Entities: `SalesSource`, `Sale`, `SaleItem`
- [x] `commission = total * commissionPct / 100` calculado con `RoundingMode.HALF_UP`
- [x] `SaleRepository.findSalesBySource` (JPQL agrupado)
- [x] `SaleController` — CRUD + `GET /by-source`

### Módulo Caja
- [x] Entities: `CashSession`, `CashSessionItem`
- [x] Enum `CashSessionStatus` + `CashSessionStatusConverter`
- [x] Regla: solo una sesión `OPEN` por restaurante (lanza `BusinessRuleException`)
- [x] `close`: calcula `totalSales`, `totalExpenses`, `totalIncomes` del día, registra conteos por método
- [x] `CashSessionController` — open, close, current

### Módulo Financiero
- [x] Entity: `FinancialPeriod` (campo `paymentBreakdown` como JSON)
- [x] Enum `PeriodType` + `PeriodTypeConverter`
- [x] `getSummary` — soporta `period=daily&date=YYYY-MM-DD` y `period=monthly&month=YYYY-MM`
- [x] Regla: `netProfit = grossIncome + totalIncomes - totalCommissions - totalExpenses`
- [x] `FinancialController` — summary, periods CRUD, close period

### Limpieza
- [x] `security/JwtFilter.java` eliminado (duplicado inactivo)
- [x] `src/main/resources/BCryptPasswordEncoder.java` eliminado (`.java` mal ubicado)

### Mappers — corrección `isActive` (2026-04-15)
- [x] **Bug en `MenuCategoryMapper`, `MenuItemMapper`, `MenuItemVariantMapper`**
  Faltaba `@Mapping(source = "active", target = "isActive")` en `toResponse`. MapStruct derivaba
  la propiedad del getter `isActive()` como `active` (convención boolean JavaBeans), pero el record
  de respuesta la declara como `isActive`. Sin el mapping explícito, el campo siempre era `false`.

### Versión Java — alineación IDE (2026-04-15)
- [x] **`pom.xml`: `java.version` 17 → 21**
  El IDE usa JDK 21 (`/usr/lib/jvm/java-21-openjdk`). Con target 17, el compilador necesitaba
  `ct.sym` para cross-compilación y fallaba al inicializarlo. Cambiando a 21 se elimina la
  necesidad de cross-compilación y el error desaparece. Maven (JDK 25) sigue compilando sin problema.

### Calidad de código (2026-04-15)
- [x] **`show-sql=false`** en `application.properties`
- [x] **`@ParameterObject`** en `OrderController` y `SaleController` para paginación correcta en Swagger
- [x] **`Employee` entity alineada con DB** — verificada contra MySQL real:
  - Agregados: `firstName`, `lastName`, `phone`, `email`, `hireDate`, `lastLogin`
  - Corregido: `@Column(name = "isLocked")` → `@Column(name = "is_locked")`
  - `@JoinTable`: añadido `inverseJoinColumns = @JoinColumn(name = "restaurants_id")` explícito
  - `EmployeeRequest` actualizado con los campos nuevos + validaciones
  - `EmployeeResponse` actualizado con los campos nuevos
  - `EmployeeServiceImpl.create` y `update` rellenan los campos nuevos
- [x] **`Restaurant.@Column(name="isActive")`** → `@Column(name = "is_active")` para consistencia

---

## 🔴 Prioritario — Bloqueante para primer arranque

- [x] **Permitir Swagger en `SecurityConfig`** — agregado `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`
- [x] **Eliminar DTOs legacy en `dto/` plano** — 12 archivos eliminados
- [x] **Verificar compilación completa** — `mvn clean install` → BUILD SUCCESS
- [x] **Aplicar schema de los módulos nuevos** — `src/main/resources/db/V2__new_modules.sql` ejecutado
- [x] **Corregir discrepancias entidad–DB detectadas por Hibernate validate**
  - `customers.total_orders`: `Long` → `Integer` (DB es `INT`)
  - `orders.driver_id`: columna faltante → `ALTER TABLE orders ADD COLUMN driver_id`
  - `expenses.is_active`: columna faltante → `ALTER TABLE expenses ADD COLUMN is_active`
  - `sales.is_active`: columna faltante → `ALTER TABLE sales ADD COLUMN is_active`
- [x] **Verificar arranque** — `Started NortcaliApiApplication in 4.421 seconds` en puerto **8082** ✓

---

## 🟡 Pendiente — Calidad de código

_(Todos los ítems de esta sección están resueltos)_

---

## 🟢 Pruebas

### Unitarias (2026-04-15) — 32 tests, BUILD SUCCESS
- [x] **`OrderServiceImplTest`** (9 tests)
  - `create`: folio (`ORD-{id}-{fecha}-{seq}`), total con varios ítems, primer historial `null→PENDING`, restaurante no encontrado
  - `updateStatus`: transición válida + historial, transición inválida (`BusinessRuleException`), estado final `CANCELLED`, descuento de inventario en `CONFIRMED`, falla silenciosa de inventario
- [x] **`InventoryMovementServiceImplTest`** (9 tests)
  - ENTRADA suma stock, SALIDA resta, MERMA resta, AJUSTE reemplaza
  - SALIDA sin stock suficiente → `BusinessRuleException`
  - Caída bajo mínimo → continúa sin excepción (solo log warn)
  - Insumo inactivo → `BusinessRuleException`
  - Tipo inválido → `BusinessRuleException`, insumo no encontrado → `ResourceNotFoundException`
- [x] **`CashSessionServiceImplTest`** (4 tests)
  - Crea sesión con `status=OPEN` y `openingAmount` correcto
  - Sesión ya abierta → `BusinessRuleException`
  - Restaurante / empleado no encontrado → `ResourceNotFoundException`
- [x] **`RecipeServiceImplTest`** (5 tests)
  - Nueva receta: `calculatedCost = qty * unitCost`
  - Receta existente: ingrediente antiguo reemplazado
  - Múltiples ingredientes: costos calculados por separado
  - MenuItem / insumo no encontrado → `ResourceNotFoundException`
- [x] **`SaleServiceImplTest`** (5 tests)
  - `commission = total * commissionPct / 100` con `HALF_UP`
  - Comisión 0% devuelve 0.00
  - Redondeo `HALF_UP` verificado con decimales impares
  - Fuente / restaurante no encontrado → `ResourceNotFoundException`

- [x] **Test de integración: flujo de login** (2026-04-15) — `AuthIntegrationTest`, 8 tests
  - Login correcto → 200 + token + username + role
  - Flujo completo: login → JWT → `/me` → datos del empleado autenticado
  - `/me` sin token → 401, token inválido → 401
  - Login con contraseña incorrecta → 401, usuario inexistente → 401
  - Campos vacíos en login → 400, logout con token → 204
  - **Bugs encontrados y corregidos:**
    - `GlobalExceptionHandler`: agregado `@ExceptionHandler(AuthenticationException.class)` → 401 (antes 500)
    - `SecurityConfig`: `/api/v1/auth/me` movido fuera de `permitAll()` para requerir JWT (antes era accesible sin token)
    - `SecurityConfig`: agregado `authenticationEntryPoint` para devolver 401 en vez de 403 para acceso anónimo
  - Usa H2 en memoria (perfil `test`) — no requiere MySQL

---

## 🔵 Pendiente — Futuro / Mejoras

- [x] **Paginación en más endpoints** (2026-04-15)
  `GET /restaurants/{id}/expenses` y `GET /restaurants/{id}/incomes` migrados a `Page<T>`.
  Orden por defecto: `expenseDate DESC` / `incomeDate DESC`. Tamaño de página: 20.

- [x] **Filtros en órdenes** (2026-04-15)
  `GET /restaurants/{id}/orders` acepta `?status=pending|confirmed|preparing|ready|delivered|cancelled` (opcional).
  Sin parámetro devuelve todas. Status inválido → 422 `BusinessRuleException`. 5 tests nuevos en `OrderServiceImplTest`.

- [x] **`SalesSource` — CRUD de administración** (2026-04-15)
  Nuevos archivos: `SalesSourceRequest`, `SalesSourceResponse`, `SalesSourceMapper`,
  `SalesSourceService`, `SalesSourceServiceImpl`, `SalesSourceController`.
  10 tests unitarios en `SalesSourceServiceImplTest`.

- [x] **`Unit` — CRUD de administración** (2026-04-15)
  Nuevos archivos: `UnitRequest`, `UnitService`, `UnitServiceImpl`, `UnitController`.
  Mapper e interfaz de repo actualizados. `GET /units` migrado de `InventoryController`
  (que inyectaba el repo directamente) al nuevo `UnitController` vía servicio.
  Sin DELETE por diseño — las unidades son referenciadas por `supplies` y `recipe_ingredients`.
  11 tests unitarios en `UnitServiceImplTest`.

- [x] **`EmployeeRole` — entidad y catálogo** (2026-04-15)
  Tabla creada en DB con `V3__employee_roles.sql` (6 roles iniciales: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN, DELIVERY).
  Nuevos archivos: `EmployeeRole`, `EmployeeRoleRequest`, `EmployeeRoleResponse`, `EmployeeRoleMapper`,
  `EmployeeRoleRepository`, `EmployeeRoleService`, `EmployeeRoleServiceImpl`, `EmployeeRoleController`.
  12 tests unitarios en `EmployeeRoleServiceImplTest`.

- [x] **Expiración de sesiones JWT** (2026-04-15)
  - `JwtUtil.getExpiration()` expuesto para que `AuthController` calcule `expiresAt`.
  - `AuthController.login()` ahora persiste `session.expiresAt = now + jwt.expiration` (columna era NOT NULL en DB pero nunca se rellenaba).
  - `JwtAuthFilter` inyecta `SessionRepository` y, por cada request autenticado, verifica que la sesión exista, esté activa (`is_active=true`) y no haya expirado en DB. Si expiró, la marca inactiva automáticamente.
  - **Diseño clave:** cuando la sesión es inválida/expirada, el filtro NO setea autenticación (no hace `return` 401 directamente). Spring Security devuelve 401 para endpoints protegidos y sigue permitiendo `permitAll` como `/logout`.
  - `AuthIntegrationTest` actualizado para mockear `findByTokenAndIsActiveTrue` con sesión activa.

- [x] **Renovación de token (refresh token)** (2026-04-15)
  `POST /api/v1/auth/refresh` — requiere JWT válido en `Authorization: Bearer`.
  Flujo: valida sesión activa en DB → genera nuevo JWT → invalida sesión anterior (rotación) → crea nueva sesión con nuevo `expiresAt`.
  3 tests en `AuthIntegrationTest`: token válido devuelve nuevo token, nuevo token da acceso a `/me`, sin token → 401.

- [x] **Perfil `dev` vs `prod` en `application.properties`** (2026-04-15)
  - `application.properties` → solo base compartida: nombre de app, perfil por defecto (`dev`), puerto, `ddl-auto`, driver.
  - `application-dev.properties` → valores actuales: `spring.config.import=file:env.properties`, datasource local, `show-sql=true`, JWT dev, logging DEBUG.
  - `application-prod.properties` → pendiente: placeholders con variables de entorno (`${DB_HOST}`, `${DB_PORT}`, `${JWT_SECRET}`), `show-sql=false`, JWT 8h, logging WARN/INFO.
  - `application-test.properties` → sin cambios (H2 + `@ActiveProfiles("test")` anula el perfil `dev` del base).

- [x] **Logging estructurado** (2026-04-15)
  `logback-spring.xml` con appenders diferenciados por perfil de Spring:
  - `dev` → consola coloreada (patrón Spring Boot estándar)
  - `prod` → JSON vía `logstash-logback-encoder:8.0`, consola (stdout para Docker/K8s) + archivo rotativo diario (`logs/{app}.log`, 30 días, máx 1 GB, comprimido `.gz`)
  - `test` → consola mínima sin color
  Dependencia `logstash-logback-encoder:8.0` añadida al `pom.xml`.
  Niveles de log siguen controlados desde `application-{profile}.properties`.

- [x] **Dockerización** (2026-04-15)
  - `Dockerfile` — build multi-etapa (builder: JDK 21 + Maven; runtime: JRE 21 alpine, usuario no-root, health check vía `/actuator/health`).
  - `docker-compose.yml` — servicios `mysql:8.4` y `api` con `depends_on: service_healthy`. Scripts SQL montados en `/docker-entrypoint-initdb.d`. Volúmenes para datos MySQL y logs de la API.
  - `.dockerignore` — excluye `target/`, `env.properties`, `.env`, `.git`, IDE files.
  - `.env.example` — plantilla de variables requeridas (`DB_DATABASE`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`).
  - `.env` añadido a `.gitignore`.
  - Usar: `cp .env.example .env` → editar valores → `docker compose up -d`.
