-- Fase 0 (parche de seguridad, ver ../../../../../../SAAS-TASKS.md) — cierre de gap crítico.
--
-- RestaurantAccessInterceptor y (desde Fase 1) RestaurantServiceImpl dependen de
-- `employee.getRestaurant()` (el @ManyToMany employee_restaurants) para saber a qué
-- restaurante(s)/tenant pertenece un empleado. Empleados creados antes de que existiera este
-- ManyToMany (o insertados directo por SQL) solo tienen la columna legacy
-- `employees.restaurant_id` poblada — employee_restaurants nunca se les backfilleó.
--
-- Sin este backfill, cualquier empleado en ese estado (ADMIN incluido — verificado en local:
-- 2 de 4 empleados de prueba están en este caso) recibe 403 de RestaurantAccessInterceptor en
-- TODOS los endpoints con restaurantId en el path en cuanto Fase 0 se despliegue — el
-- interceptor ya está escrito y probado (unit), pero nunca se ejecutó contra datos reales
-- hasta ahora. Bloqueante para el deploy de Fase 0.

INSERT INTO employee_restaurants (restaurant_id, restaurants_id)
SELECT e.id, e.restaurant_id
FROM employees e
WHERE e.restaurant_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM employee_restaurants er
      WHERE er.restaurant_id = e.id AND er.restaurants_id = e.restaurant_id
  );
