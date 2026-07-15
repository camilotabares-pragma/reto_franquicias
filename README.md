# 🚀 API Reactiva de Franquicias - Clean Architecture

¡Hola! Bienvenido a mi solución para el reto técnico de gestión de franquicias.

Este proyecto consiste en una API RESTful 100% reactiva construida con **Spring Boot WebFlux** y **MongoDB**, estructurada bajo los principios de **Arquitectura Hexagonal (Clean Architecture)** utilizando el plugin oficial de Bancolombia. El objetivo es gestionar franquicias, sus sucursales y el stock de productos de manera asíncrona y no bloqueante.

---

## 🏗️ Decisiones de Diseño y Arquitectura (DDD)

Para este desarrollo, apliqué principios de **Domain-Driven Design (DDD)** y **Clean Architecture** con enfoque reactivo:

* **Separación por recursos de dominio:** `Franchise`, `Branch` y `Product` se manejan como recursos diferenciados, con responsabilidades separadas en capa de aplicación (`FranchiseUseCase`, `BranchUseCase`, `ProductUseCase`) para cumplir mejor SRP y facilitar mantenimiento.
* **Persistencia desacoplada por gateways:** el dominio depende de contratos (`FranchiseRepository`, `BranchRepository`, `ProductRepository`), permitiendo cambiar infraestructura sin romper reglas de negocio.
* **Modelo reactivo end-to-end:** toda la lógica se implementa con `Mono`/`Flux`, evitando bloqueos y manteniendo composición funcional para validaciones, mutaciones y persistencia.
* **Inmutabilidad en operaciones de negocio:** las actualizaciones de entidades se realizan con `toBuilder()` para evitar efectos secundarios y mantener trazabilidad de cambios.
* **Manejo global de errores en WebFlux funcional:** se usa un manejador global reactivo para traducir excepciones de dominio a respuestas HTTP consistentes (`400`, `404`, `409`, `500`).
* **Routing funcional por recurso:** la capa de entrada está organizada por handlers (`FranchiseHandler`, `BranchHandler`, `ProductHandler`) y rutas funcionales en `RouterRest`, lo que mejora claridad y escalabilidad de la API.
---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Java 21
* **Framework:** Spring Boot 3 (WebFlux)
* **Base de Datos:** MongoDB Atlas (Cloud) y Spring Data Reactive MongoDB
* **Documentación:** Swagger UI (springdoc-openapi-starter-webflux-ui)
* **Testing:** JUnit 5, Mockito, Project Reactor `StepVerifier`
* **Despliegue:** Docker & Docker Compose (Multi-stage build optimizado), Terraform y AWS

---

## 🚦 Cómo levantar el proyecto localmente (Docker & WSL)

La aplicación está dockerizada utilizando un **Multi-stage build**. Esto significa que no necesitas tener Java ni Gradle instalados localmente, ya que el contenedor se encarga de compilar el código fuente y ejecutar la aplicación en un entorno Alpine sumamente ligero.

### Requisitos previos:
* Tener instalado **Docker** y **Docker Compose** (funciona perfectamente sobre WSL / Ubuntu o Docker Desktop).
* *(La conexión a la base de datos de MongoDB Atlas ya está configurada internamente en el proyecto para facilitar su revisión directa).*

### Instrucciones de ejecución:

1. Clona este repositorio:
   ```bash
   git clone https://github.com/camilotabares-pragma/reto_franquicias.git
   cd reto_franquicias
   ```
2. Ejecuta el comando de Docker Compose para construir y levantar los servicios:
   ```bash
   docker compose up --build
   ```
   *Nota: El Dockerfile incluye la exclusión de la tarea de validación estructural (`-x validateStructure`) para garantizar una compilación fluida y agnóstica dentro de entornos Linux.*
3. Espera a que los logs indiquen que Spring Boot ha arrancado con éxito en el puerto `8080`.

---

## ☁️ Despliegue en AWS con Terraform

La infraestructura del proyecto también está definida con **Terraform** para automatizar el aprovisionamiento en **AWS**. La configuración incluye:

La infraestructura está modularizada en múltiples archivos `.tf` por responsabilidad (red, seguridad, cómputo, balanceador, logs, etc.) para mejorar legibilidad, mantenimiento y evolución del entorno.

* **ECR** para almacenar la imagen de la API.
* **VPC**, **subredes públicas**, **Internet Gateway** y **tabla de ruteo**.
* **Security Group** para el acceso a la aplicación.
* **ECS Fargate** para ejecutar el servicio.
* **Application Load Balancer** para exponer la aplicación públicamente.
* **CloudWatch Logs** para centralizar los registros.

### URL pública actual

**👉 AWS ALB:** 

---

## 📚 Documentación Interactiva (Swagger)

A pesar de utilizar un enrutador funcional (`RouterRest`), la API está completamente documentada gracias a la implementación avanzada de anotaciones `@RouterOperations` y `@RouterOperation`.

Una vez la aplicación esté corriendo, puedes ver, inspeccionar los esquemas y probar los 11 endpoints interactuando desde tu navegador en la siguiente URL:

**👉 URL de Swagger UI: http://localhost:8080/webjars/swagger-ui/index.html**

### Endpoints Disponibles:
* `POST /api/franchises` - Crear una nueva franquicia.
* `GET /api/franchises` - Obtener todas las franquicias.
* `GET /api/franchises/{franchiseId}` - Obtener una franquicia por ID.
* `GET /api/franchises/{franchiseId}/max-stock` - Obtener el producto con más stock por sucursal.
* `PUT /api/franchises/{franchiseId}/name` - Editar el nombre de una franquicia.
* `POST /api/franchises/{franchiseId}/branches` - Agregar una sucursal a una franquicia.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/name` - Editar el nombre de una sucursal.
* `POST /api/franchises/{franchiseId}/branches/{branchId}/products` - Agregar un producto a una sucursal.
* `DELETE /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}` - Eliminar un producto de una sucursal.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` - Modificar el stock de un producto.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name` - Editar el nombre de un producto.
---

## 🧪 Pruebas Unitarias (Testing Reactivo)

El código cuenta con una suite completa de pruebas unitarias enfocadas en blindar la lógica de negocio y sus validaciones, separadas por caso de uso (`FranchiseUseCase`, `BranchUseCase`, `ProductUseCase`).

Al trabajar con flujos asíncronos inmutables (`Mono`/`Flux`), implementé **`StepVerifier`** de Project Reactor para evaluar la secuencia de eventos a lo largo del tiempo, validando tanto los flujos exitosos (`expectNextMatches`, `verifyComplete`) como el disparo controlado de excepciones (`expectErrorMatches`). La persistencia fue completamente simulada utilizando **Mockito** para asegurar que los tests sean rápidos, independientes y puramente unitarios.

Si deseas ejecutar los tests de forma manual desde tu consola:
```bash
./gradlew test
```

---
*Desarrollado con dedicación por Camilo Tabares.*