# Frontend Context — NortCali API

Guía de integración para el equipo de frontend. Todo lo que necesitas para conectarte al backend sin leer el código fuente.

---

## 1. Base URL y estructura de rutas

```
http://localhost:8082
```

Todos los endpoints tienen el prefijo `/api/v1/`. El servidor corre en el puerto **8082** tanto en desarrollo local como en el contenedor Docker (mapeado al mismo puerto).

**Estructura general de rutas:**

```
/api/v1/auth/**                     — públicos (sin JWT)
/api/v1/restaurants/{id}/orders     — recursos anidados bajo restaurante
/api/v1/orders/{id}/status          — acciones sobre un recurso individual
```

Los recursos operativos (órdenes, inventario, menú, clientes, etc.) siempre están anidados bajo un `restaurantId`. Los catálogos globales (`/units`, `/sales-sources`, `/employee-roles`, `/countries`, etc.) no lo están.

---

## 2. Autenticación

### 2.1 Obtener el JWT

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "tu-contraseña"
}
```

**Respuesta 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

### 2.2 Enviar el token en cada request

Incluir en todas las peticiones protegidas:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 2.3 Expiración

| Entorno | Duración |
|---------|----------|
| Desarrollo | 24 horas |
| Producción | 8 horas |

El backend valida dos cosas en cada request: la firma del JWT **y** que la sesión exista en base de datos con `is_active = true`. Si haces logout, el token queda inválido aunque no haya expirado.

### 2.4 Renovar el token (refresh)

Llama a refresh **antes** de que expire para obtener un nuevo token sin que el usuario tenga que hacer login de nuevo. El token anterior queda invalidado.

```
POST /api/v1/auth/refresh
Authorization: Bearer <token-actual>
```

**Respuesta 200:** mismo shape que `/login`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...<nuevo>",
  "username": "admin",
  "role": "ADMIN"
}
```

### 2.5 Cerrar sesión

```
POST /api/v1/auth/logout
Authorization: Bearer <token>
```

Respuesta: `204 No Content`. El token queda invalidado en la base de datos.

### 2.6 Empleado autenticado

```
GET /api/v1/auth/me
Authorization: Bearer <token>
```

Respuesta: `EmployeeResponse` (ver sección 4 — Empleados).

---

## 3. Estructura estándar de responses

### 3.1 Éxito — recurso único

El body es el objeto directamente (sin wrapper):

```json
{
  "id": 1,
  "name": "Tacos El Güero",
  ...
}
```

### 3.2 Éxito — lista simple

Array JSON plano:

```json
[
  { "id": 1, ... },
  { "id": 2, ... }
]
```

### 3.3 Éxito — lista paginada

Los endpoints paginados (`orders`, `sales`, `expenses`, `incomes`) devuelven el formato estándar de Spring:

```typescript
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;        // página actual (0-based)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

**Parámetros de paginación en query string:**

```
?page=0&size=20&sort=createdAt,desc
?page=1&size=10&sort=expenseDate,desc
```

### 3.4 Error estándar

```typescript
interface ErrorResponse {
  status: number;      // 404 | 409 | 422 | 401 | 500
  error: string;       // "Not Found" | "Conflict" | "Business Rule Violation" | ...
  message: string;     // descripción legible
  timestamp: string;   // ISO 8601
}
```

Ejemplo:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Restaurant with id: 99",
  "timestamp": "2026-04-16T15:30:00.123Z"
}
```

### 3.5 Error de validación (400)

Shape diferente — incluye el campo que falló:

```typescript
interface ValidationErrorResponse {
  status: 400;
  error: "Validation Failed";
  fields: Record<string, string>;  // { "username": "El username es obligatorio" }
  timestamp: string;
}
```

### 3.6 Tabla de códigos HTTP usados

| Código | Cuándo |
|--------|--------|
| 200 | GET / PUT exitosos |
| 201 | POST que crea un recurso nuevo |
| 204 | DELETE / logout (sin body) |
| 400 | Validación fallida (`@Valid`) |
| 401 | Sin token, token expirado, sesión inactiva |
| 403 | Autenticado pero sin el rol requerido |
| 404 | Recurso no encontrado |
| 409 | Duplicado (username, nombre único, etc.) |
| 422 | Regla de negocio violada (estado de orden inválido, caja ya abierta, etc.) |
| 500 | Error interno inesperado |

---

## 4. Endpoints por módulo con interfaces TypeScript

> **Convención de tipos:**
> - Todos los decimales monetarios o de cantidad llegan como `string` en JSON (Java `BigDecimal` → JSON string para evitar pérdida de precisión). Parsea con `parseFloat()` o usa una librería decimal.
> - Fechas `LocalDate` → `"YYYY-MM-DD"`. Fechas `LocalDateTime` → `"YYYY-MM-DDTHH:mm:ss"` (UTC).
> - `isActive` es un `boolean` en todos los recursos.

---

### Auth

```typescript
interface LoginRequest {
  username: string;    // requerido
  password: string;    // requerido
}

interface LoginResponse {
  token: string;
  username: string;
  role: string;        // "ADMIN" | "MANAGER" | "CASHIER" | "WAITER" | "KITCHEN" | "DELIVERY"
}

interface EmployeeResponse {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  phone: string | null;
  email: string | null;
  role: string;
  status: string;      // "ACTIVE" | "INACTIVE"
  locked: boolean;
  hireDate: string;    // "YYYY-MM-DD"
  lastLogin: string | null;  // "YYYY-MM-DDTHH:mm:ss"
}
```

---

### Restaurantes

> Requiere rol `ADMIN` o `MANAGER`.

```
GET    /api/v1/restaurants
POST   /api/v1/restaurants
GET    /api/v1/restaurants/{id}
PUT    /api/v1/restaurants/{id}
DELETE /api/v1/restaurants/{id}
```

```typescript
interface RestaurantRequest {
  name: string;           // requerido, max 120
  phone?: string;         // max 20
  whatsapp?: string;      // max 20
  addressLine?: string;   // max 255
  cityId: number;         // requerido
  isActive?: boolean;     // default true
}

interface RestaurantResponse {
  id: number;
  name: string;
  phone: string | null;
  whatsapp: string | null;
  addressLine: string | null;
  isActive: boolean;
  cityId: number;
  cityName: string;
}
```

---

### Empleados

```
GET  /api/v1/restaurants/{restaurantId}/employees
POST /api/v1/restaurants/{restaurantId}/employees
GET  /api/v1/employees/{id}
PUT  /api/v1/employees/{id}
PUT  /api/v1/employees/{id}/status
```

```typescript
interface EmployeeRequest {
  firstName: string;    // requerido, max 100
  lastName: string;     // requerido, max 100
  username: string;     // requerido, max 50
  password: string;     // requerido en POST, min 6 chars
  phone?: string;       // max 20
  email?: string;       // formato email válido, max 150
  role: string;         // requerido: "ADMIN" | "MANAGER" | "CASHIER" | "WAITER" | "KITCHEN" | "DELIVERY"
  status?: string;      // default "ACTIVE"
  hireDate: string;     // requerido: "YYYY-MM-DD"
}

// Para cambiar solo el status:
interface EmployeeStatusRequest {
  status: string;       // requerido
}

// EmployeeResponse — ver sección Auth
```

---

### Roles de empleado

> Requiere rol `ADMIN`.

```
GET    /api/v1/employee-roles
POST   /api/v1/employee-roles
GET    /api/v1/employee-roles/{id}
PUT    /api/v1/employee-roles/{id}
DELETE /api/v1/employee-roles/{id}
```

```typescript
interface EmployeeRoleRequest {
  name: string;           // requerido, max 50
  description?: string;   // max 255
}

interface EmployeeRoleResponse {
  id: number;
  name: string;
  description: string | null;
  isActive: boolean;
}
```

---

### Menú — Categorías

```
GET    /api/v1/restaurants/{restaurantId}/menu/categories
POST   /api/v1/restaurants/{restaurantId}/menu/categories
PUT    /api/v1/restaurants/{restaurantId}/menu/categories/{id}
DELETE /api/v1/restaurants/{restaurantId}/menu/categories/{id}
```

```typescript
interface MenuCategoryRequest {
  name: string;           // requerido, max 80
  displayOrder: number;   // requerido, >= 0
  isActive?: boolean;     // default true
}

interface MenuCategoryResponse {
  id: number;
  restaurantId: number;
  name: string;
  displayOrder: number;
  isActive: boolean;
}
```

### Menú — Platillos

```
GET    /api/v1/restaurants/{restaurantId}/menu/items
POST   /api/v1/restaurants/{restaurantId}/menu/items
PUT    /api/v1/restaurants/{restaurantId}/menu/items/{id}
DELETE /api/v1/restaurants/{restaurantId}/menu/items/{id}
```

```typescript
interface MenuItemRequest {
  name: string;         // requerido, max 120
  description?: string;
  categoryId: number;   // requerido
  isActive?: boolean;   // default true
}

interface MenuItemResponse {
  id: number;
  restaurantId: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string | null;
  isActive: boolean;
}
```

### Menú — Variantes

```
GET    /api/v1/menu-items/{itemId}/variants
POST   /api/v1/menu-items/{itemId}/variants
PUT    /api/v1/menu-items/{itemId}/variants/{id}
DELETE /api/v1/menu-items/{itemId}/variants/{id}
```

```typescript
interface MenuItemVariantRequest {
  name: string;           // requerido, max 60
  salePrice: string;      // requerido, >= 0 (decimal como string)
  isActive?: boolean;     // default true
}

interface MenuItemVariantResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  name: string;
  salePrice: string;      // decimal
  isActive: boolean;
}
```

### Menú — Receta de un platillo

```
GET  /api/v1/menu-items/{itemId}/recipe
POST /api/v1/menu-items/{itemId}/recipe
```

El `POST` hace upsert — crea la receta si no existe, la reemplaza si ya existe.

```typescript
interface RecipeIngredientRequest {
  supplyId: number;     // requerido
  quantity: string;     // requerido, > 0 (decimal)
}

interface RecipeRequest {
  variantId?: number;                       // null = receta base del platillo
  portions: number;                         // requerido, >= 1
  isActive?: boolean;
  ingredients: RecipeIngredientRequest[];   // requerido, mínimo 1
}

interface RecipeIngredientResponse {
  id: number;
  supplyId: number;
  supplyName: string;
  quantity: string;
  unitId: number;
  unitAbbreviation: string;
  calculatedCost: string;   // quantity * supply.unitCost
}

interface RecipeResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  variantId: number | null;
  variantName: string | null;
  portions: number;
  isActive: boolean;
  ingredients: RecipeIngredientResponse[];
  totalCost: string;        // suma de calculatedCost de todos los ingredientes
}
```

---

### Unidades de medida

> Requiere rol `ADMIN` o `MANAGER`. Sin DELETE (FK en insumos y recetas).

```
GET  /api/v1/units
POST /api/v1/units
GET  /api/v1/units/{id}
PUT  /api/v1/units/{id}
```

```typescript
interface UnitRequest {
  name: string;           // requerido, max 40
  abbreviation: string;   // requerido, max 10
}

interface UnitResponse {
  id: number;
  name: string;
  abbreviation: string;
}
```

---

### Inventario — Insumos

```
GET    /api/v1/restaurants/{restaurantId}/supplies
POST   /api/v1/restaurants/{restaurantId}/supplies
PUT    /api/v1/restaurants/{restaurantId}/supplies/{id}
DELETE /api/v1/restaurants/{restaurantId}/supplies/{id}
GET    /api/v1/restaurants/{restaurantId}/supplies/low-stock
```

```typescript
interface SupplyRequest {
  name: string;           // requerido, max 100
  unitId: number;         // requerido
  currentStock: string;   // requerido, >= 0
  minimumStock: string;   // requerido, >= 0
  unitCost: string;       // requerido, >= 0
  isActive?: boolean;     // default true
}

interface SupplyResponse {
  id: number;
  restaurantId: number;
  name: string;
  unitId: number;
  unitName: string;
  unitAbbreviation: string;
  currentStock: string;
  minimumStock: string;
  unitCost: string;
  isActive: boolean;
  isBelowMinimum: boolean;   // currentStock < minimumStock
}
```

### Inventario — Movimientos

```
GET  /api/v1/supplies/{supplyId}/movements
POST /api/v1/supplies/{supplyId}/movements
```

```typescript
interface InventoryMovementRequest {
  movementType: MovementType;   // requerido
  quantity: string;             // requerido, > 0
  employeeId: number;           // requerido
}

interface InventoryMovementResponse {
  id: number;
  supplyId: number;
  supplyName: string;
  movementType: string;
  quantity: string;
  employeeId: number;
  employeeUsername: string;
  createdAt: string;    // "YYYY-MM-DDTHH:mm:ss"
}
```

---

### Clientes

```
GET    /api/v1/restaurants/{restaurantId}/customers
POST   /api/v1/restaurants/{restaurantId}/customers
PUT    /api/v1/restaurants/{restaurantId}/customers/{id}
DELETE /api/v1/restaurants/{restaurantId}/customers/{id}
```

```typescript
interface CustomerRequest {
  firstName: string;   // requerido, max 80
  phone: string;       // requerido, max 20
  address?: string;
  isActive?: boolean;  // default true
}

interface CustomerResponse {
  id: number;
  restaurantId: number;
  firstName: string;
  phone: string;
  address: string | null;
  totalOrders: number;
  isActive: boolean;
}
```

---

### Delivery — Repartidores

```
GET    /api/v1/restaurants/{restaurantId}/drivers
POST   /api/v1/restaurants/{restaurantId}/drivers
PUT    /api/v1/restaurants/{restaurantId}/drivers/{id}
DELETE /api/v1/restaurants/{restaurantId}/drivers/{id}
GET    /api/v1/restaurants/{restaurantId}/drivers/available
```

`/available` devuelve solo los drivers con `isActive = true`.

```typescript
interface DeliveryDriverRequest {
  firstName: string;   // requerido, max 80
  phone: string;       // requerido, max 20
  vehicle?: string;    // max 60
  isActive?: boolean;  // default true
}

interface DeliveryDriverResponse {
  id: number;
  restaurantId: number;
  firstName: string;
  phone: string;
  vehicle: string | null;
  isActive: boolean;
}
```

---

### Órdenes

```
GET  /api/v1/restaurants/{restaurantId}/orders          — paginado, ?status=pending (opcional)
POST /api/v1/restaurants/{restaurantId}/orders
GET  /api/v1/restaurants/{restaurantId}/orders/{id}
PUT  /api/v1/orders/{id}/status
GET  /api/v1/orders/{id}/history
POST /api/v1/orders/{id}/payments
```

```typescript
interface OrderItemExtraRequest {
  menuItemId: number;   // requerido
  unitPrice: string;    // requerido, >= 0
}

interface OrderItemRequest {
  menuItemId: number;                     // requerido
  variantId?: number;
  quantity: number;                       // requerido, >= 1
  unitPrice: string;                      // requerido, >= 0
  extras?: OrderItemExtraRequest[];
}

interface OrderRequest {
  orderType: OrderType;                   // requerido
  source: OrderSource;                    // requerido
  employeeId: number;                     // requerido
  customerId?: number;
  driverId?: number;                      // requerido si orderType = "delivery"
  paymentMethod?: PaymentMethod;
  items: OrderItemRequest[];              // requerido, mínimo 1
}

interface OrderStatusUpdateRequest {
  toStatus: OrderStatus;   // requerido
  employeeId: number;      // requerido
}

interface PaymentRequest {
  method: PaymentMethod;   // requerido
  amount: string;          // requerido, > 0
  reference?: string;      // opcional (número de autorización, etc.)
  registeredBy: number;    // requerido — id del empleado que registra
}

// ─── Responses ────────────────────────────────────────────────────────

interface OrderItemExtraResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  unitPrice: string;
}

interface OrderItemResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  variantId: number | null;
  variantName: string | null;
  quantity: number;
  unitPrice: string;
  subtotal: string;
  extras: OrderItemExtraResponse[];
}

interface OrderResponse {
  id: number;
  restaurantId: number;
  folio: string;             // "ORD-1-20260416-0001"
  orderType: string;
  source: string;
  status: string;
  total: string;
  paymentMethod: string | null;
  customerId: number | null;
  customerFirstName: string | null;
  employeeId: number;
  employeeUsername: string;
  driverId: number | null;
  driverFirstName: string | null;
  createdAt: string;         // "YYYY-MM-DDTHH:mm:ss"
  items: OrderItemResponse[];
}

interface OrderStatusHistoryResponse {
  id: number;
  fromStatus: string | null;   // null en el primer registro
  toStatus: string;
  employeeId: number;
  employeeUsername: string;
  changedAt: string;
}

interface PaymentResponse {
  id: number;
  method: string;
  amount: string;
  reference: string | null;
  registeredBy: number;
  createdAt: string;
}
```

---

### Gastos

```
GET    /api/v1/restaurants/{restaurantId}/expense-categories
POST   /api/v1/restaurants/{restaurantId}/expense-categories
PUT    /api/v1/restaurants/{restaurantId}/expense-categories/{id}
DELETE /api/v1/restaurants/{restaurantId}/expense-categories/{id}

GET    /api/v1/restaurants/{restaurantId}/expenses   — paginado: ?page=0&size=20&sort=expenseDate,desc
POST   /api/v1/restaurants/{restaurantId}/expenses
PUT    /api/v1/restaurants/{restaurantId}/expenses/{id}
DELETE /api/v1/restaurants/{restaurantId}/expenses/{id}
```

```typescript
interface ExpenseCategoryRequest {
  name: string;       // requerido
  isActive?: boolean; // default true
}

interface ExpenseCategoryResponse {
  id: number;
  restaurantId: number;
  name: string;
  type: string | null;
  isActive: boolean;
}

interface ExpenseRequest {
  concept: string;       // requerido, max 200
  amount: string;        // requerido, > 0
  expenseDate: string;   // requerido: "YYYY-MM-DD"
  categoryId: number;    // requerido
  employeeId: number;    // requerido
  isActive?: boolean;
}

interface ExpenseResponse {
  id: number;
  restaurantId: number;
  categoryId: number;
  categoryName: string;
  concept: string;
  amount: string;
  expenseDate: string;   // "YYYY-MM-DD"
  employeeId: number;
  isActive: boolean;
}
```

---

### Ingresos

```
GET    /api/v1/restaurants/{restaurantId}/income-categories
POST   /api/v1/restaurants/{restaurantId}/income-categories
PUT    /api/v1/restaurants/{restaurantId}/income-categories/{id}
DELETE /api/v1/restaurants/{restaurantId}/income-categories/{id}

GET    /api/v1/restaurants/{restaurantId}/incomes   — paginado: ?page=0&size=20&sort=incomeDate,desc
POST   /api/v1/restaurants/{restaurantId}/incomes
PUT    /api/v1/restaurants/{restaurantId}/incomes/{id}
DELETE /api/v1/restaurants/{restaurantId}/incomes/{id}
```

```typescript
interface IncomeCategoryRequest {
  name: string;   // requerido
}

interface IncomeCategoryResponse {
  id: number;
  restaurantId: number;
  name: string;
  isActive: boolean;
}

interface IncomeRequest {
  concept: string;         // requerido, max 200
  amount: string;          // requerido, > 0
  incomeDate: string;      // requerido: "YYYY-MM-DD"
  paymentMethod?: PaymentMethod;
  categoryId: number;      // requerido
  employeeId: number;      // requerido
  isActive?: boolean;
}

interface IncomeResponse {
  id: number;
  restaurantId: number;
  categoryId: number;
  categoryName: string;
  concept: string;
  amount: string;
  incomeDate: string;
  paymentMethod: string | null;
  employeeId: number;
  isActive: boolean;
}
```

---

### Ventas

```
GET    /api/v1/restaurants/{restaurantId}/sales           — paginado
POST   /api/v1/restaurants/{restaurantId}/sales
DELETE /api/v1/restaurants/{restaurantId}/sales/{id}
GET    /api/v1/restaurants/{restaurantId}/sales/by-source
```

```typescript
interface SaleItemRequest {
  menuItemId: number;   // requerido
  variantId?: number;
  quantity: number;     // requerido, >= 1
  subtotal: string;     // requerido, >= 0
}

interface SaleRequest {
  sourceId: number;              // requerido — id de SalesSource
  saleDate: string;              // requerido: "YYYY-MM-DD"
  employeeId: number;            // requerido
  items: SaleItemRequest[];      // requerido, mínimo 1
}

interface SaleItemResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  variantId: number | null;
  variantName: string | null;
  quantity: number;
  subtotal: string;
}

interface SaleResponse {
  id: number;
  restaurantId: number;
  sourceId: number;
  sourceName: string;
  folio: string;
  total: string;
  commission: string;     // calculada automáticamente: total * commissionPct / 100
  saleDate: string;
  employeeId: number;
  isActive: boolean;
  items: SaleItemResponse[];
}

interface SalesBySourceResponse {
  sourceName: string;
  saleCount: number;
  totalAmount: string;
}
```

### Fuentes de venta

> Requiere rol `ADMIN` o `MANAGER`.

```
GET    /api/v1/sales-sources
POST   /api/v1/sales-sources
GET    /api/v1/sales-sources/{id}
PUT    /api/v1/sales-sources/{id}
DELETE /api/v1/sales-sources/{id}
```

```typescript
interface SalesSourceRequest {
  name: string;             // requerido, max 60
  commissionPct: string;    // requerido, >= 0 (ej: "15.00" = 15%)
}

interface SalesSourceResponse {
  id: number;
  name: string;
  commissionPct: string;
  isActive: boolean;
}
```

---

### Combos

```
GET    /api/v1/restaurants/{restaurantId}/combos
POST   /api/v1/restaurants/{restaurantId}/combos
PUT    /api/v1/restaurants/{restaurantId}/combos/{id}
DELETE /api/v1/restaurants/{restaurantId}/combos/{id}
```

```typescript
interface ComboItemRequest {
  menuItemId: number;   // requerido
  variantId?: number;
  quantity?: number;    // default 1, min 1
}

interface ComboRequest {
  name: string;                   // requerido, max 150
  description?: string;           // max 400
  salePrice: string;              // requerido, > 0 (decimal como string)
  isActive?: boolean;             // default true
  items: ComboItemRequest[];      // requerido, mínimo 2
}

interface ComboItemResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  variantId: number | null;
  variantName: string | null;
  quantity: number;
}

interface ComboResponse {
  id: number;
  restaurantId: number;
  name: string;
  description: string | null;
  salePrice: string;
  isActive: boolean;
  items: ComboItemResponse[];
}
```

El `PUT` reemplaza la lista `items` completamente.

---

### Promociones

```
GET    /api/v1/restaurants/{restaurantId}/promotions
POST   /api/v1/restaurants/{restaurantId}/promotions
PUT    /api/v1/restaurants/{restaurantId}/promotions/{id}
DELETE /api/v1/restaurants/{restaurantId}/promotions/{id}
```

```typescript
type PromotionType = 'porcentaje' | 'precio_fijo' | '2x1';

interface PromotionItemRequest {
  menuItemId: number;   // requerido
  variantId?: number;
}

interface PromotionRequest {
  name: string;                         // requerido, max 150
  description?: string;                 // max 400
  type: PromotionType;                  // requerido
  discountValue?: string;               // requerido para porcentaje/precio_fijo; null para 2x1
  startDate: string;                    // requerido: "YYYY-MM-DD"
  endDate: string;                      // requerido: "YYYY-MM-DD"
  isActive?: boolean;                   // default true
  items: PromotionItemRequest[];        // requerido, mínimo 1
}

interface PromotionItemResponse {
  id: number;
  menuItemId: number;
  menuItemName: string;
  variantId: number | null;
  variantName: string | null;
}

interface PromotionResponse {
  id: number;
  restaurantId: number;
  name: string;
  description: string | null;
  type: string;
  discountValue: string | null;
  startDate: string;
  endDate: string;
  isActive: boolean;
  items: PromotionItemResponse[];
}
```

El `PUT` reemplaza la lista `items` completamente. El `DELETE` hace soft-delete (`isActive = false`).

---

### Caja

```
POST /api/v1/restaurants/{restaurantId}/cash-sessions/open
POST /api/v1/cash-sessions/{id}/close
GET  /api/v1/restaurants/{restaurantId}/cash-sessions/current
```

```typescript
interface OpenCashSessionRequest {
  openingAmount: string;   // requerido, >= 0
  openedBy: number;        // requerido — id del empleado
}

interface CashSessionItemCountRequest {
  method: PaymentMethod;   // requerido
  countedAmount: string;   // requerido, >= 0
}

interface CloseCashSessionRequest {
  closedBy: number;                                 // requerido — id del empleado
  countedAmounts: CashSessionItemCountRequest[];    // requerido, mínimo 1 por método
}

interface CashSessionItemResponse {
  id: number;
  method: string;
  expectedAmount: string;
  countedAmount: string;
  difference: string;       // countedAmount - expectedAmount
}

interface CashSessionResponse {
  id: number;
  restaurantId: number;
  openedBy: number;
  closedBy: number | null;
  openingAmount: string;
  expectedCash: string;
  countedCash: string;
  difference: string;
  totalSales: string;
  totalExpenses: string;
  totalIncomes: string;
  status: CashSessionStatus;
  openedAt: string;
  closedAt: string | null;
  items: CashSessionItemResponse[];
}
```

---

### Financiero

```
GET  /api/v1/restaurants/{restaurantId}/financial/summary?period=daily&date=YYYY-MM-DD
GET  /api/v1/restaurants/{restaurantId}/financial/summary?period=monthly&month=YYYY-MM
GET  /api/v1/restaurants/{restaurantId}/financial/periods
POST /api/v1/restaurants/{restaurantId}/financial/periods
POST /api/v1/restaurants/{restaurantId}/financial/periods/{id}/close
```

```typescript
interface FinancialPeriodRequest {
  periodType: PeriodType;    // requerido: "daily" | "weekly" | "monthly"
  periodLabel: string;       // requerido — ej: "Semana 15 / 2026"
  startDate: string;         // requerido: "YYYY-MM-DD"
  endDate: string;           // requerido: "YYYY-MM-DD"
}

interface FinancialSummaryResponse {
  startDate: string;
  endDate: string;
  period: string;
  grossIncome: string;
  totalCommissions: string;
  totalExpenses: string;
  totalIncomes: string;
  netProfit: string;         // grossIncome + totalIncomes - totalCommissions - totalExpenses
}

interface FinancialPeriodResponse {
  id: number;
  restaurantId: number;
  periodType: string;
  periodLabel: string;
  startDate: string;
  endDate: string;
  grossIncome: string;
  totalCommissions: string;
  totalExpenses: string;
  netProfit: string;
  paymentBreakdown: string;  // JSON serializado con desglose por método de pago
  status: string;            // "open" | "closed"
}
```

---

### Geográfico (catálogos globales)

> Requiere rol `ADMIN`.

```
GET/POST/PUT/DELETE /api/v1/countries
GET/POST/PUT/DELETE /api/v1/states?countryId={id}
GET/POST/PUT/DELETE /api/v1/cities?stateId={id}
```

```typescript
interface CountryResponse { id: number; name: string; isoCode: string; }
interface StateResponse   { id: number; name: string; countryId: number; countryName: string; }
interface CityResponse    { id: number; name: string; stateId: number;   stateName: string;   }
```

---

## 5. Enumerados

Todos los valores se envían y reciben en **lowercase** (o el valor exacto indicado).

### OrderStatus

```typescript
type OrderStatus = 'pending' | 'confirmed' | 'preparing' | 'ready' | 'delivered' | 'cancelled';
```

### OrderType

```typescript
type OrderType = 'dine_in' | 'takeout' | 'delivery';
```

### OrderSource

```typescript
type OrderSource = 'pos' | 'whatsapp' | 'phone' | 'rappi' | 'uber_eats' | 'web';
```

### PaymentMethod

```typescript
type PaymentMethod =
  | 'efectivo'
  | 'tarjeta_credito'
  | 'tarjeta_debito'
  | 'transferencia'
  | 'rappi'
  | 'uber_eats'
  | 'otro';
```

### MovementType (inventario)

```typescript
type MovementType = 'entrada' | 'salida' | 'merma' | 'ajuste';
```

### CashSessionStatus

```typescript
type CashSessionStatus = 'open' | 'closed';
```

### PeriodType

```typescript
type PeriodType = 'daily' | 'weekly' | 'monthly';
```

---

## 6. Reglas de negocio que el frontend debe respetar

### 6.1 Flujo de estados de una orden

Las transiciones válidas son estrictas. El backend rechaza con `422` cualquier salto no permitido.

```
PENDING ──→ CONFIRMED ──→ PREPARING ──→ READY ──→ DELIVERED
   │              │              │           │
   └──────────────┴──────────────┴───────────┴──→ CANCELLED
```

| Estado actual | Transiciones permitidas |
|---------------|-------------------------|
| `pending` | `confirmed`, `cancelled` |
| `confirmed` | `preparing`, `cancelled` |
| `preparing` | `ready`, `cancelled` |
| `ready` | `delivered`, `cancelled` |
| `delivered` | — (estado final) |
| `cancelled` | — (estado final) |

`delivered` y `cancelled` son estados finales: no se pueden cambiar una vez alcanzados.

### 6.2 Endpoints que requieren `restaurantId`

Todos los recursos operativos están aislados por restaurante. Siempre incluir el `restaurantId` correcto en la URL:

- `menu/categories`, `menu/items`
- `supplies`, `supply movements`
- `customers`
- `drivers`
- `orders`
- `expense-categories`, `expenses`
- `income-categories`, `incomes`
- `sales`
- `cash-sessions`
- `financial/summary`, `financial/periods`
- `employees` (en la creación)

Los catálogos globales **no** tienen `restaurantId`: `units`, `sales-sources`, `employee-roles`, `countries`, `states`, `cities`.

### 6.3 Reglas de caja

Solo puede haber **una caja abierta por restaurante** a la vez. Si intentas abrir una segunda, el backend devuelve `422`. Consulta primero `/cash-sessions/current` para verificar si ya hay una abierta.

### 6.4 Orden de tipo delivery

Si `orderType = "delivery"`, se recomienda enviar `driverId`. El backend no lo obliga a nivel de validación, pero es necesario para el flujo operativo.

### 6.5 Campos que el backend NUNCA devuelve

Estos campos no existen en ningún response DTO, independientemente del endpoint:

- `password` / `password_hash`
- El JWT o cualquier token de sesión
- Datos internos de auditoría de sesiones (`sessions.token`, `sessions.ip_address`)

### 6.6 Soft-delete

Ningún recurso con `isActive` se elimina físicamente. `DELETE` marca `isActive = false`. Para "reactivar" un recurso, usa el `PUT` correspondiente con `isActive: true`.

El frontend debe filtrar por `isActive = true` al mostrar listas operativas (menú disponible, insumos activos, etc.) — el backend devuelve todos los registros incluyendo inactivos en los `GET` de lista.

### 6.7 Fechas

El backend opera internamente en **UTC**. Los `LocalDateTime` en responses (`createdAt`, `openedAt`, `changedAt`, etc.) están en UTC. Aplica la conversión a la zona horaria local del usuario en el frontend.

Los campos `LocalDate` (`expenseDate`, `incomeDate`, `saleDate`, `hireDate`) no tienen zona horaria — son fechas de calendario, sin conversión.
