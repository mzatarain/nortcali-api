# NortCali API — Colección de Endpoints

**Base URL:** `http://localhost:8082`  
**Formato:** JSON (`Content-Type: application/json`)  
**Autenticación:** JWT Bearer Token — `Authorization: Bearer <token>`

> Los endpoints marcados con 🔒 requieren autenticación.  
> Los marcados con 👑 requieren rol `ADMIN`.  
> Los marcados con 🏢 requieren rol `ADMIN` o `MANAGER`.

---

## Índice

1. [Auth](#1-auth)
2. [Roles de Empleado](#2-roles-de-empleado)
3. [Geográfico](#3-geográfico)
4. [Restaurantes](#4-restaurantes)
5. [Empleados](#5-empleados)
6. [Unidades de Medida](#6-unidades-de-medida)
7. [Inventario](#7-inventario)
8. [Menú](#8-menú)
9. [Clientes](#9-clientes)
10. [Delivery](#10-delivery)
11. [Órdenes](#11-órdenes)
12. [Gastos](#12-gastos)
13. [Ingresos](#13-ingresos)
14. [Fuentes de Venta](#14-fuentes-de-venta)
15. [Ventas](#15-ventas)
16. [Caja](#16-caja)
17. [Financiero](#17-financiero)
18. [Modificadores](#18-modificadores)

---

## 1. Auth

### POST /api/v1/auth/login

Autentica un empleado y devuelve un JWT. Registra la sesión en la tabla `sessions` con `expiresAt`.

**Headers**
```
Content-Type: application/json
```

**Request**
```json
{
  "username": "admin01",
  "password": "Secure#2026"
}
```

**Response 200**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjAxIn0...",
  "username": "admin01",
  "role": "ADMIN"
}
```

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `username` o `password` vacíos |
| 401 | Credenciales incorrectas o usuario deshabilitado |

---

### POST /api/v1/auth/logout

Invalida la sesión activa del token enviado (soft-delete en `sessions`). No requiere autenticación válida — funciona aunque el token esté próximo a expirar.

**Headers**
```
Authorization: Bearer <token>
```

**Response 204** — Sin cuerpo.

---

### GET /api/v1/auth/me 🔒

Devuelve los datos del empleado autenticado.

**Headers**
```
Authorization: Bearer <token>
```

**Response 200**
```json
{
  "id": 1,
  "firstName": "Carlos",
  "lastName": "Reyes",
  "username": "admin01",
  "phone": "555-1234",
  "email": "carlos@nortcali.com",
  "role": "ADMIN",
  "status": "ACTIVE",
  "locked": false,
  "hireDate": "2024-01-15",
  "lastLogin": "2026-04-15T17:00:00"
}
```

**Errores**
| Código | Motivo |
|--------|--------|
| 401 | Token ausente, inválido o sesión expirada |

---

### POST /api/v1/auth/refresh 🔒

Rota el JWT: invalida la sesión actual y emite un nuevo token con `expiresAt` renovado. Requiere que el token vigente sea válido y la sesión esté activa en DB.

**Headers**
```
Authorization: Bearer <token>
```

**Response 200**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjAxIn0...",
  "username": "admin01",
  "role": "ADMIN"
}
```

**Errores**
| Código | Motivo |
|--------|--------|
| 401 | Token ausente, expirado o sesión inactiva |

---

## 2. Roles de Empleado

> Requiere rol **ADMIN** 👑

### GET /api/v1/employee-roles 👑

Lista los roles activos ordenados alfabéticamente.

**Headers**
```
Authorization: Bearer <token>
```

**Response 200**
```json
[
  { "id": 1, "name": "ADMIN",    "description": "Administrador del sistema",     "isActive": true },
  { "id": 3, "name": "CASHIER",  "description": "Cajero — gestiona pagos",       "isActive": true },
  { "id": 6, "name": "DELIVERY", "description": "Repartidor",                    "isActive": true },
  { "id": 5, "name": "KITCHEN",  "description": "Personal de cocina",            "isActive": true },
  { "id": 2, "name": "MANAGER",  "description": "Gerente de restaurante",        "isActive": true },
  { "id": 4, "name": "WAITER",   "description": "Mesero — gestiona órdenes",     "isActive": true }
]
```

---

### GET /api/v1/employee-roles/{id} 👑

**Response 200**
```json
{ "id": 1, "name": "ADMIN", "description": "Administrador del sistema", "isActive": true }
```

**Errores** — `404` si no existe.

---

### POST /api/v1/employee-roles 👑

**Request**
```json
{
  "name": "SUPERVISOR",
  "description": "Supervisor de turno"
}
```

**Response 201**
```json
{ "id": 7, "name": "SUPERVISOR", "description": "Supervisor de turno", "isActive": true }
```

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `name` vacío o supera 50 chars |
| 409 | Nombre duplicado |

---

### PUT /api/v1/employee-roles/{id} 👑

**Request** — Misma estructura que POST.

**Response 200** — Objeto actualizado.

**Errores** — `404` si no existe · `409` si el nuevo nombre está en uso.

---

### DELETE /api/v1/employee-roles/{id} 👑

Soft-delete (`is_active = false`).

**Response 204** — Sin cuerpo.

**Errores** — `404` si no existe.

---

## 3. Geográfico

> Requiere rol **ADMIN** 👑

### Países

#### GET /api/v1/countries 👑

**Response 200**
```json
[
  { "id": 1, "name": "México", "code": "MX" }
]
```

#### POST /api/v1/countries 👑

**Request**
```json
{ "name": "México", "code": "MX" }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/countries/{id} 👑 — Actualiza. **Response 200**.

#### DELETE /api/v1/countries/{id} 👑 — Elimina físicamente. **Response 204**.

---

### Estados

#### GET /api/v1/states?countryId={id} 👑

**Response 200**
```json
[
  { "id": 1, "name": "Jalisco", "countryId": 1 }
]
```

#### POST /api/v1/states 👑

**Request**
```json
{ "name": "Jalisco", "countryId": 1 }
```

#### PUT /api/v1/states/{id} 👑 · DELETE /api/v1/states/{id} 👑

---

### Ciudades

#### GET /api/v1/cities?stateId={id} 👑

**Response 200**
```json
[
  { "id": 1, "name": "Guadalajara", "stateId": 1 }
]
```

#### POST /api/v1/cities 👑

**Request**
```json
{ "name": "Guadalajara", "stateId": 1 }
```

#### PUT /api/v1/cities/{id} 👑 · DELETE /api/v1/cities/{id} 👑

**Errores comunes Geográfico**
| Código | Motivo |
|--------|--------|
| 400 | Campos requeridos vacíos |
| 401 | Sin token |
| 403 | Rol insuficiente |
| 404 | Recurso no encontrado |

---

## 4. Restaurantes

> Requiere rol **ADMIN** o **MANAGER** 🏢

### GET /api/v1/restaurants 🏢

**Response 200**
```json
[
  {
    "id": 1,
    "name": "NortCali Sucursal Centro",
    "phone": "33-1234-5678",
    "whatsapp": "33-9876-5432",
    "addressLine": "Av. Juárez 100, Col. Centro",
    "isActive": true,
    "cityId": 1,
    "cityName": "Guadalajara"
  }
]
```

### POST /api/v1/restaurants 🏢

**Request**
```json
{
  "name": "NortCali Sucursal Centro",
  "phone": "33-1234-5678",
  "whatsapp": "33-9876-5432",
  "addressLine": "Av. Juárez 100, Col. Centro",
  "cityId": 1
}
```

**Response 201** — Objeto creado.

### GET /api/v1/restaurants/{id} 🏢

**Response 200** — Objeto restaurante.

### PUT /api/v1/restaurants/{id} 🏢

**Request** — Misma estructura que POST.  
**Response 200** — Objeto actualizado.

### DELETE /api/v1/restaurants/{id} 🏢

Soft-delete. **Response 204**.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `name` vacío · `cityId` nulo |
| 403 | Rol insuficiente |
| 404 | Restaurante no encontrado |

---

## 5. Empleados

### GET /api/v1/restaurants/{restaurantId}/employees 🔒

Lista empleados del restaurante.

**Response 200**
```json
[
  {
    "id": 1,
    "firstName": "Carlos",
    "lastName": "Reyes",
    "username": "admin01",
    "phone": "555-1234",
    "email": "carlos@nortcali.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "locked": false,
    "hireDate": "2024-01-15",
    "lastLogin": "2026-04-15T17:00:00"
  }
]
```

### POST /api/v1/restaurants/{restaurantId}/employees 🔒

**Request**
```json
{
  "firstName": "María",
  "lastName": "López",
  "username": "mesero01",
  "password": "TempPass#123",
  "phone": "555-5678",
  "email": "maria@nortcali.com",
  "role": "WAITER",
  "status": "ACTIVE",
  "hireDate": "2026-04-15"
}
```

**Response 201** — Objeto empleado (sin `password`).

### GET /api/v1/employees/{id} 🔒

**Response 200** — Objeto empleado.

### PUT /api/v1/employees/{id} 🔒

**Request** — Misma estructura que POST. `password` es opcional (si se omite, no cambia).  
**Response 200** — Objeto actualizado.

### PUT /api/v1/employees/{id}/status 🔒

**Request**
```json
{ "status": "INACTIVE" }
```

**Response 200** — Objeto actualizado.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | Campos obligatorios vacíos |
| 404 | Empleado o restaurante no encontrado |
| 409 | `username` ya existe |

---

## 6. Unidades de Medida

> Requiere rol **ADMIN** o **MANAGER** 🏢

### GET /api/v1/units 🏢

**Response 200**
```json
[
  { "id": 1, "name": "Kilogramo", "abbreviation": "kg" },
  { "id": 2, "name": "Litro",     "abbreviation": "l"  },
  { "id": 3, "name": "Pieza",     "abbreviation": "pza" }
]
```

### GET /api/v1/units/{id} 🏢

**Response 200** — Objeto unidad.

### POST /api/v1/units 🏢

**Request**
```json
{ "name": "Gramo", "abbreviation": "g" }
```

**Response 201** — Objeto creado.

### PUT /api/v1/units/{id} 🏢

**Request** — Misma estructura que POST.  
**Response 200** — Objeto actualizado.

> No existe DELETE — las unidades son referenciadas por insumos y recetas.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `name` vacío · `abbreviation` supera 10 chars |
| 409 | Nombre duplicado |

---

## 7. Inventario

### GET /api/v1/restaurants/{restaurantId}/supplies 🔒

**Response 200**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "name": "Harina de trigo",
    "unitId": 1,
    "unitName": "Kilogramo",
    "unitAbbreviation": "kg",
    "currentStock": 25.500,
    "minimumStock": 5.000,
    "unitCost": 18.5000,
    "isActive": true,
    "isBelowMinimum": false
  }
]
```

### GET /api/v1/restaurants/{restaurantId}/supplies/low-stock 🔒

Devuelve insumos con `currentStock < minimumStock`.

**Response 200** — Lista con la misma estructura de arriba, `isBelowMinimum: true`.

### GET /api/v1/supplies/{id} 🔒

**Response 200** — Objeto insumo.

### POST /api/v1/restaurants/{restaurantId}/supplies 🔒

**Request**
```json
{
  "name": "Harina de trigo",
  "unitId": 1,
  "currentStock": 25.5,
  "minimumStock": 5.0,
  "unitCost": 18.50
}
```

**Response 201** — Objeto creado.

### PUT /api/v1/supplies/{id} 🔒

**Request** — Misma estructura que POST.  
**Response 200** — Objeto actualizado.

### DELETE /api/v1/supplies/{id} 🔒

Soft-delete. **Response 204**.

---

### GET /api/v1/supplies/{supplyId}/movements 🔒

Lista movimientos de inventario del insumo.

**Response 200**
```json
[
  {
    "id": 1,
    "supplyId": 1,
    "supplyName": "Harina de trigo",
    "movementType": "entrada",
    "quantity": 10.000,
    "employeeId": 1,
    "employeeUsername": "admin01",
    "createdAt": "2026-04-15T10:00:00"
  }
]
```

### POST /api/v1/supplies/{supplyId}/movements 🔒

**Request**
```json
{
  "movementType": "salida",
  "quantity": 2.5,
  "employeeId": 1
}
```

> `movementType`: `entrada` | `salida` | `merma` | `ajuste`

**Response 201** — Objeto movimiento.

**Errores Inventario**
| Código | Motivo |
|--------|--------|
| 404 | Insumo no encontrado |
| 422 | Stock insuficiente · insumo inactivo · tipo de movimiento inválido |

---

## 8. Menú

### Categorías

#### GET /api/v1/restaurants/{restaurantId}/menu/categories 🔒

**Response 200**
```json
[
  { "id": 1, "restaurantId": 1, "name": "Entradas", "displayOrder": 1, "isActive": true },
  { "id": 2, "restaurantId": 1, "name": "Platos fuertes", "displayOrder": 2, "isActive": true }
]
```

#### POST /api/v1/restaurants/{restaurantId}/menu/categories 🔒

**Request**
```json
{ "name": "Bebidas", "displayOrder": 3 }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/restaurants/{restaurantId}/menu/categories/{id} 🔒

**Request** — Misma estructura que POST.  
**Response 200** — Actualizado.

#### DELETE /api/v1/restaurants/{restaurantId}/menu/categories/{id} 🔒

Soft-delete. **Response 204**.

---

### Platillos

#### GET /api/v1/restaurants/{restaurantId}/menu/items 🔒

**Response 200**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "categoryId": 2,
    "categoryName": "Platos fuertes",
    "name": "Tacos de Birria",
    "description": "3 tacos con consomé",
    "isActive": true
  }
]
```

#### POST /api/v1/restaurants/{restaurantId}/menu/items 🔒

**Request**
```json
{
  "name": "Tacos de Birria",
  "description": "3 tacos con consomé",
  "categoryId": 2
}
```

**Response 201** — Objeto creado.

#### PUT /api/v1/restaurants/{restaurantId}/menu/items/{id} 🔒 · DELETE 🔒

---

### Variantes

#### GET /api/v1/menu-items/{itemId}/variants 🔒

**Response 200**
```json
[
  { "id": 1, "menuItemId": 1, "menuItemName": "Tacos de Birria", "name": "Orden de 3", "salePrice": 75.00, "isActive": true },
  { "id": 2, "menuItemId": 1, "menuItemName": "Tacos de Birria", "name": "Orden de 6", "salePrice": 140.00, "isActive": true }
]
```

#### POST /api/v1/menu-items/{itemId}/variants 🔒

**Request**
```json
{ "name": "Orden de 3", "salePrice": 75.00 }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/menu-items/{itemId}/variants/{id} 🔒 · DELETE 🔒

---

### Receta

#### GET /api/v1/menu-items/{itemId}/recipe 🔒

**Response 200**
```json
{
  "id": 1,
  "menuItemId": 1,
  "menuItemName": "Tacos de Birria",
  "variantId": null,
  "variantName": null,
  "portions": 1,
  "isActive": true,
  "ingredients": [
    {
      "supplyId": 1,
      "supplyName": "Carne de res",
      "unitId": 1,
      "unitAbbreviation": "kg",
      "quantity": 0.2500,
      "calculatedCost": 45.0000
    }
  ],
  "totalCost": 45.00
}
```

#### POST /api/v1/menu-items/{itemId}/recipe 🔒

Upsert — crea o reemplaza la receta activa del platillo.

**Request**
```json
{
  "portions": 1,
  "variantId": null,
  "ingredients": [
    { "supplyId": 1, "quantity": 0.25, "unitId": 1 },
    { "supplyId": 2, "quantity": 2,    "unitId": 3 }
  ]
}
```

**Response 200/201** — Objeto receta actualizado con `totalCost` calculado.

**Errores Menú**
| Código | Motivo |
|--------|--------|
| 400 | Campos obligatorios vacíos |
| 404 | Platillo, categoría, insumo o unidad no encontrados |

---

## 9. Clientes

### GET /api/v1/restaurants/{restaurantId}/customers 🔒

**Response 200**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "firstName": "Juan Pérez",
    "phone": "331-234-5678",
    "address": "Calle Reforma 45",
    "totalOrders": 12,
    "isActive": true
  }
]
```

### POST /api/v1/restaurants/{restaurantId}/customers 🔒

**Request**
```json
{
  "firstName": "Juan Pérez",
  "phone": "331-234-5678",
  "address": "Calle Reforma 45"
}
```

**Response 201** — Objeto creado.

### GET /api/v1/customers/{id} 🔒 · PUT /api/v1/customers/{id} 🔒

### DELETE /api/v1/customers/{id} 🔒

Soft-delete. **Response 204**.

---

## 10. Delivery

### GET /api/v1/restaurants/{restaurantId}/drivers 🔒

**Response 200**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "firstName": "Pedro Ruiz",
    "phone": "331-999-0000",
    "vehicle": "Moto Honda CB250 — Roja",
    "isActive": true
  }
]
```

### GET /api/v1/restaurants/{restaurantId}/drivers/available 🔒

Devuelve solo repartidores activos (`isActive: true`). Misma estructura que arriba.

### POST /api/v1/restaurants/{restaurantId}/drivers 🔒

**Request**
```json
{
  "firstName": "Pedro Ruiz",
  "phone": "331-999-0000",
  "vehicle": "Moto Honda CB250 — Roja"
}
```

**Response 201** — Objeto creado.

### PUT /api/v1/drivers/{id} 🔒 · DELETE /api/v1/drivers/{id} 🔒

Soft-delete en DELETE. **Response 204**.

---

## 11. Órdenes

### GET /api/v1/restaurants/{restaurantId}/orders 🔒

Paginado. Acepta `?status=` y `?date=` para filtrar.

**Query params**
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `status` | string (opcional, multi-valor) | `confirmed` · `preparing` · `ready` · `delivered` · `cancelled` |
| `date` | YYYY-MM-DD (opcional) | Filtra órdenes del día |
| `page` | int (default 0) | |
| `size` | int (default 20) | |

**Response 200**
```json
{
  "content": [
    {
      "id": 1,
      "restaurantId": 1,
      "folio": "ORD-1-20260415-0001",
      "orderType": "dine_in",
      "source": "pos",
      "status": "confirmed",
      "total": 225.00,
      "paymentMethod": null,
      "customerId": null,
      "customerFirstName": null,
      "employeeId": 1,
      "employeeUsername": "mesero01",
      "driverId": null,
      "driverFirstName": null,
      "createdAt": "2026-04-15T18:30:00",
      "preparingAt": null,
      "readyAt": null,
      "preparationTimeSeconds": null,
      "items": [
        {
          "id": 1,
          "menuItemId": 1,
          "menuItemName": "Tacos de Birria",
          "variantId": 1,
          "variantName": "Orden de 3",
          "quantity": 3,
          "unitPrice": 75.00,
          "subtotal": 225.00,
          "modifiers": []
        }
      ]
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### POST /api/v1/restaurants/{restaurantId}/orders 🔒

**Request**
```json
{
  "orderType": "dine_in",
  "source": "pos",
  "employeeId": 1,
  "customerId": null,
  "driverId": null,
  "paymentMethod": null,
  "items": [
    {
      "menuItemId": 1,
      "variantId": 1,
      "quantity": 3,
      "unitPrice": 75.00,
      "modifiers": [
        { "modifierId": 5, "price": 10.00 }
      ]
    }
  ]
}
```

> `orderType`: `dine_in` | `takeout` | `delivery`  
> `source`: `pos` | `whatsapp` | `phone` | `rappi` | `uber_eats` | `web`  
> `paymentMethod`: `efectivo` | `tarjeta_credito` | `tarjeta_debito` | `transferencia` | `rappi` | `uber_eats` | `otro`  
> `modifiers`: lista de modificadores del catálogo. Cada uno requiere `modifierId` y `price`. El servicio resuelve el nombre.

**Response 201** — Objeto orden con folio generado y `status: "confirmed"`. El inventario se descuenta automáticamente al crear la orden.

### GET /api/v1/restaurants/{restaurantId}/orders/{id} 🔒

**Response 200** — Objeto orden completo.

---

### PUT /api/v1/orders/{id}/status 🔒

Cambia el estado de la orden. Al pasar a `delivered` se crea automáticamente una venta.

**Transiciones permitidas:**
`confirmed → preparing | cancelled`  
`preparing → ready | cancelled`  
`ready → delivered | cancelled`

> Las órdenes nacen en `confirmed`, no en `pending`. El estado `pending` existe pero no se usa en el flujo actual.

**Request**
```json
{
  "toStatus": "confirmed",
  "employeeId": 1
}
```

**Response 200** — Orden actualizada.

**Errores**
| Código | Motivo |
|--------|--------|
| 404 | Orden o empleado no encontrados |
| 422 | Transición de estado no permitida · valor de status inválido |

---

### GET /api/v1/orders/{id}/history 🔒

**Response 200**
```json
[
  {
    "id": 1,
    "fromStatus": null,
    "toStatus": "confirmed",
    "employeeId": 1,
    "employeeUsername": "mesero01",
    "changedAt": "2026-04-15T18:30:00"
  },
  {
    "id": 2,
    "fromStatus": "confirmed",
    "toStatus": "preparing",
    "employeeId": 1,
    "employeeUsername": "mesero01",
    "changedAt": "2026-04-15T18:45:00"
  }
]
```

---

### POST /api/v1/orders/{id}/payments 🔒

Registra un pago para la orden.

**Request**
```json
{
  "method": "efectivo",
  "amount": 225.00,
  "reference": null,
  "registeredBy": 1
}
```

**Response 201**
```json
{
  "id": 1,
  "method": "efectivo",
  "amount": 225.00,
  "reference": null,
  "registeredBy": 1
}
```

---

## 12. Gastos

### Categorías de gasto

#### GET /api/v1/restaurants/{restaurantId}/expense-categories 🔒

**Response 200**
```json
[
  { "id": 1, "restaurantId": 1, "name": "Insumos y materias primas", "type": "operativo" }
]
```

#### POST /api/v1/restaurants/{restaurantId}/expense-categories 🔒

**Request**
```json
{ "name": "Insumos y materias primas", "type": "operativo" }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/restaurants/{restaurantId}/expense-categories/{id} 🔒 · DELETE 🔒

---

### Gastos

#### GET /api/v1/restaurants/{restaurantId}/expenses 🔒

Paginado. Query params: `page`, `size` (default 20, orden `expenseDate DESC`). Filtros opcionales de fecha.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `startDate` | YYYY-MM-DD (opcional) | Fecha desde (inclusive) |
| `endDate` | YYYY-MM-DD (opcional) | Fecha hasta (inclusive) |

**Response 200**
```json
{
  "content": [
    {
      "id": 1,
      "restaurantId": 1,
      "categoryId": 1,
      "categoryName": "Insumos y materias primas",
      "concept": "Compra semanal de harina",
      "amount": 850.00,
      "expenseDate": "2026-04-15",
      "employeeId": 1,
      "isActive": true,
      "isPaid": false
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

#### POST /api/v1/restaurants/{restaurantId}/expenses 🔒

**Request**
```json
{
  "concept": "Compra semanal de harina",
  "amount": 850.00,
  "expenseDate": "2026-04-15",
  "categoryId": 1,
  "employeeId": 1,
  "isPaid": false
}
```

> `isPaid` es opcional — si se omite, se guarda como `false`.

**Response 201** — Objeto creado.

#### GET /api/v1/expenses/{id} 🔒 · PUT /api/v1/expenses/{id} 🔒

#### PATCH /api/v1/restaurants/{restaurantId}/expenses/{id}/paid 🏢

Actualiza únicamente el campo `isPaid` del gasto. Requiere rol `ADMIN` o `MANAGER`.

**Request**
```json
{ "isPaid": true }
```

**Response 200** — Objeto gasto actualizado.

#### DELETE /api/v1/expenses/{id} 🔒

Soft-delete. **Response 204**.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `concept` vacío · `amount` ≤ 0 · fecha nula |
| 403 | Rol insuficiente en PATCH `/paid` |
| 404 | Gasto, categoría o empleado no encontrados |

---

## 13. Ingresos

### Categorías de ingreso

#### GET /api/v1/restaurants/{restaurantId}/income-categories 🔒

**Response 200**
```json
[
  { "id": 1, "restaurantId": 1, "name": "Servicio a domicilio", "description": "Recargos por delivery", "isActive": true }
]
```

#### POST /api/v1/restaurants/{restaurantId}/income-categories 🔒

**Request**
```json
{ "name": "Servicio a domicilio", "description": "Recargos por delivery" }
```

#### PUT /api/v1/restaurants/{restaurantId}/income-categories/{id} 🔒 · DELETE 🔒

---

### Ingresos

#### GET /api/v1/restaurants/{restaurantId}/incomes 🔒

Paginado (`incomeDate DESC`). Misma estructura de `Page<T>` que gastos.

**Response 200 (ejemplo ítem)**
```json
{
  "id": 1,
  "restaurantId": 1,
  "categoryId": 1,
  "categoryName": "Servicio a domicilio",
  "concept": "Recargo delivery zona norte",
  "amount": 150.00,
  "incomeDate": "2026-04-15",
  "paymentMethod": "efectivo",
  "employeeId": 1,
  "isActive": true
}
```

#### POST /api/v1/restaurants/{restaurantId}/incomes 🔒

**Request**
```json
{
  "concept": "Recargo delivery zona norte",
  "amount": 150.00,
  "incomeDate": "2026-04-15",
  "paymentMethod": "efectivo",
  "categoryId": 1,
  "employeeId": 1
}
```

> `paymentMethod`: `efectivo` | `tarjeta_credito` | `tarjeta_debito` | `transferencia` | `rappi` | `uber_eats` | `otro`

**Response 201** — Objeto creado.

#### GET /api/v1/incomes/{id} 🔒 · PUT /api/v1/incomes/{id} 🔒 · DELETE /api/v1/incomes/{id} 🔒

---

## 14. Fuentes de Venta

> Requiere rol **ADMIN** o **MANAGER** 🏢

### GET /api/v1/sales-sources 🏢

**Response 200**
```json
[
  { "id": 1, "name": "POS",       "commissionPct": 0.00,  "isActive": true },
  { "id": 2, "name": "Rappi",     "commissionPct": 15.00, "isActive": true },
  { "id": 3, "name": "Uber Eats", "commissionPct": 20.00, "isActive": true }
]
```

### GET /api/v1/sales-sources/{id} 🏢

**Response 200** — Objeto fuente.

### POST /api/v1/sales-sources 🏢

**Request**
```json
{ "name": "Didi Food", "commissionPct": 12.00 }
```

**Response 201** — Objeto creado.

### PUT /api/v1/sales-sources/{id} 🏢

**Request** — Misma estructura que POST.  
**Response 200** — Actualizado.

### DELETE /api/v1/sales-sources/{id} 🏢

Soft-delete. **Response 204**.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `name` vacío · `commissionPct` negativo |
| 409 | Nombre duplicado |

---

## 15. Ventas

### GET /api/v1/restaurants/{restaurantId}/sales 🔒

Paginado. Filtros de fecha opcionales.

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `startDate` | YYYY-MM-DD (opcional) | Fecha desde |
| `endDate` | YYYY-MM-DD (opcional) | Fecha hasta |

**Response 200**
```json
{
  "content": [
    {
      "id": 1,
      "restaurantId": 1,
      "sourceId": 2,
      "sourceName": "Rappi",
      "folio": "VTA-1-20260415-0001",
      "total": 350.00,
      "commission": 52.50,
      "saleDate": "2026-04-15",
      "employeeId": 1,
      "customerName": null,
      "isActive": true,
      "items": [
        {
          "id": 1,
          "menuItemId": 1,
          "menuItemName": "Tacos de Birria",
          "variantId": 1,
          "variantName": "Orden de 3",
          "quantity": 2,
          "subtotal": 150.00
        }
      ]
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### POST /api/v1/restaurants/{restaurantId}/sales 🔒

**Request**
```json
{
  "sourceId": 2,
  "saleDate": "2026-04-15",
  "employeeId": 1,
  "paymentMethod": "efectivo",
  "notes": null,
  "items": [
    { "menuItemId": 1, "variantId": 1, "quantity": 2, "subtotal": 150.00 }
  ]
}
```

> `paymentMethod` y `notes` son opcionales.  
> La comisión se calcula automáticamente: `total × commissionPct ÷ 100` (HALF_UP).  
> El folio se genera con formato `VTA-{restaurantId}-{yyyyMMdd}-{seq4}`.

**Response 201** — Objeto venta con `commission` calculado.

### DELETE /api/v1/sales/{id} 🔒

Soft-delete. **Response 204**.

---

### GET /api/v1/restaurants/{restaurantId}/sales/by-source 🔒

Agrupa el total de ventas y comisiones por fuente. Filtros de fecha opcionales: `?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`.

**Response 200**
```json
[
  { "sourceName": "POS",   "saleCount": 5, "totalAmount": 1250.00 },
  { "sourceName": "Rappi", "saleCount": 2, "totalAmount": 350.00  }
]
```

---

## 16. Caja

### POST /api/v1/restaurants/{restaurantId}/cash-sessions/open 🔒

Abre una sesión de caja. Solo se permite una sesión abierta por restaurante.

**Request**
```json
{
  "openingAmount": 500.00,
  "openedBy": 1
}
```

**Response 201**
```json
{
  "id": 1,
  "restaurantId": 1,
  "openedBy": 1,
  "closedBy": null,
  "openingAmount": 500.00,
  "expectedCash": 0.00,
  "countedCash": 0.00,
  "difference": 0.00,
  "totalSales": 0.00,
  "totalExpenses": 0.00,
  "totalIncomes": 0.00,
  "status": "open",
  "openedAt": "2026-04-15T09:00:00",
  "closedAt": null,
  "items": []
}
```

**Errores**
| Código | Motivo |
|--------|--------|
| 404 | Restaurante o empleado no encontrado |
| 422 | Ya existe una sesión abierta para el restaurante |

---

### POST /api/v1/cash-sessions/{id}/close 🔒

Cierra la sesión. Calcula `totalSales`, `totalExpenses` e `totalIncomes` del día.

**Request**
```json
{
  "closedBy": 1,
  "countedAmounts": [
    { "method": "efectivo",       "countedAmount": 1350.00 },
    { "method": "tarjeta_debito", "countedAmount": 450.00  }
  ]
}
```

> `method`: `efectivo` | `tarjeta_credito` | `tarjeta_debito` | `transferencia` | `rappi` | `uber_eats` | `otro`

**Response 200** — Objeto sesión cerrada con totales calculados y diferencia.

**Errores**
| Código | Motivo |
|--------|--------|
| 404 | Sesión no encontrada |
| 422 | La sesión ya estaba cerrada |

---

### GET /api/v1/restaurants/{restaurantId}/cash-sessions/current 🔒

Devuelve la sesión actualmente abierta del restaurante.

**Response 200** — Objeto sesión con `status: "open"`.

**Errores** — `404` si no hay sesión abierta.

---

## 17. Financiero

### GET /api/v1/restaurants/{restaurantId}/financial/summary 🔒

Resumen financiero por período.

**Query params**
| Parámetro | Ejemplo | Descripción |
|-----------|---------|-------------|
| `period=daily&date=YYYY-MM-DD` | `period=daily&date=2026-04-15` | Resumen del día |
| `period=monthly&month=YYYY-MM` | `period=monthly&month=2026-04`  | Resumen del mes |

**Response 200**
```json
{
  "startDate": "2026-04-15",
  "endDate": "2026-04-15",
  "period": "daily",
  "grossIncome": 3250.00,
  "totalCommissions": 487.50,
  "totalExpenses": 850.00,
  "totalIncomes": 150.00,
  "netProfit": 2062.50
}
```

> `netProfit = grossIncome + totalIncomes - totalCommissions - totalExpenses`

---

### GET /api/v1/restaurants/{restaurantId}/financial/periods 🔒

Lista los períodos financieros del restaurante.

**Response 200**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "periodType": "monthly",
    "periodLabel": "Abril 2026",
    "startDate": "2026-04-01",
    "endDate": "2026-04-30",
    "grossIncome": 48500.00,
    "totalCommissions": 7275.00,
    "totalExpenses": 12000.00,
    "netProfit": 29225.00,
    "paymentBreakdown": "{\"efectivo\":25000,\"tarjeta_debito\":15000,\"rappi\":8500}",
    "status": "open"
  }
]
```

---

### POST /api/v1/restaurants/{restaurantId}/financial/periods 🔒

**Request**
```json
{
  "periodType": "monthly",
  "periodLabel": "Abril 2026",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30"
}
```

> `periodType`: `daily` | `weekly` | `monthly`

**Response 201** — Objeto período creado.

---

### POST /api/v1/restaurants/{restaurantId}/financial/periods/{id}/close 🔒

Cierra un período financiero (cambia `status` a `closed`).

**Response 200** — Objeto período cerrado.

**Errores** — `404` si no existe.

---

## 18. Modificadores

Los modificadores permiten personalizar variantes de platillos (e.g. "Sin cebolla", "Extra queso"). Se organizan en grupos.

### Grupos de modificadores

#### GET /api/v1/restaurants/{restaurantId}/modifier-groups 🔒

**Response 200**
```json
[
  { "id": 1, "restaurantId": 1, "name": "Extras", "isActive": true },
  { "id": 2, "restaurantId": 1, "name": "Sin ingrediente", "isActive": true }
]
```

#### POST /api/v1/restaurants/{restaurantId}/modifier-groups 🔒

**Request**
```json
{ "name": "Extras" }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/restaurants/{restaurantId}/modifier-groups/{id} 🔒

**Request** — Misma estructura que POST. **Response 200**.

#### DELETE /api/v1/restaurants/{restaurantId}/modifier-groups/{id} 🔒

Soft-delete. **Response 204**.

---

### Modificadores

#### GET /api/v1/restaurants/{restaurantId}/modifier-groups/{groupId}/modifiers 🔒

**Response 200**
```json
[
  { "id": 1, "groupId": 1, "groupName": "Extras", "name": "Extra queso", "isActive": true },
  { "id": 2, "groupId": 1, "groupName": "Extras", "name": "Extra carne",  "isActive": true }
]
```

#### POST /api/v1/restaurants/{restaurantId}/modifier-groups/{groupId}/modifiers 🔒

**Request**
```json
{ "name": "Extra queso" }
```

**Response 201** — Objeto creado.

#### PUT /api/v1/restaurants/{restaurantId}/modifier-groups/{groupId}/modifiers/{id} 🔒

**Request** — Misma estructura que POST. **Response 200**.

#### DELETE /api/v1/restaurants/{restaurantId}/modifier-groups/{groupId}/modifiers/{id} 🔒

Soft-delete. **Response 204**.

---

### Modificadores de variante (precios)

Asocia un modificador del catálogo a una variante de menú con un precio específico.

#### GET /api/v1/menu-item-variants/{variantId}/modifiers 🔒

**Response 200**
```json
[
  {
    "modifierId": 1,
    "modifierName": "Extra queso",
    "groupId": 1,
    "groupName": "Extras",
    "price": 15.00
  }
]
```

#### POST /api/v1/menu-item-variants/{variantId}/modifiers 🔒

**Request**
```json
{ "modifierId": 1, "price": 15.00 }
```

**Response 201** — Objeto creado.

#### DELETE /api/v1/menu-item-variants/{variantId}/modifiers/{modifierId} 🔒

Elimina la asociación. **Response 204**.

**Errores**
| Código | Motivo |
|--------|--------|
| 400 | `name` vacío · `price` negativo |
| 404 | Grupo, modificador o variante no encontrados |

---

## Códigos de error globales

| Código | Descripción | Ejemplo de body |
|--------|-------------|-----------------|
| 400 | Validación fallida | `{"status":400,"error":"Validation Failed","fields":{"username":"El username es obligatorio"}}` |
| 401 | No autenticado (token ausente, inválido o sesión expirada) | `{"status":401,"error":"Unauthorized","message":"..."}` |
| 403 | Autenticado pero sin el rol requerido | `{"status":403,"error":"Forbidden","message":"..."}` |
| 404 | Recurso no encontrado | `{"status":404,"error":"Not Found","message":"Order with id 99 not found"}` |
| 409 | Recurso duplicado | `{"status":409,"error":"Conflict","message":"Ya existe una fuente de venta con el nombre 'Rappi'"}` |
| 422 | Regla de negocio violada | `{"status":422,"error":"Business Rule Violation","message":"Transición de estado no permitida: delivered → pending"}` |
| 500 | Error interno inesperado | `{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}` |

---

## Notas generales

- **Multi-restaurante:** todos los endpoints operativos filtran por `restaurantId` — nunca se mezclan datos entre restaurantes.
- **Soft-delete:** los recursos con `isActive` nunca se borran físicamente. `DELETE` marca `isActive = false`.
- **Paginación:** los endpoints paginados devuelven el envelope estándar de Spring: `content`, `totalElements`, `totalPages`, `number`, `size`.
- **Fechas:** siempre en formato ISO 8601. Las fechas sin hora (`LocalDate`) en `YYYY-MM-DD`; las con hora (`LocalDateTime`) en `YYYY-MM-DDTHH:mm:ss`.
- **Montos:** todos los valores monetarios en `BigDecimal` con 2 decimales.
- **Swagger UI:** disponible en `http://localhost:8082/swagger-ui.html` con autenticación Bearer JWT integrada.
