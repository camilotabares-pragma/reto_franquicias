# 🚀 API Reactiva de Franquicias - Clean Architecture

¡Hola! Bienvenido a mi solución para el reto técnico de gestión de franquicias.

Este proyecto consiste en una API RESTful 100% reactiva construida con **Spring Boot WebFlux** y **MongoDB**, estructurada bajo los principios de **Arquitectura Hexagonal (Clean Architecture)** utilizando el plugin oficial de Bancolombia. El objetivo es gestionar franquicias, sus sucursales y el stock de productos de manera asíncrona y no bloqueante.

---

## 🏗️ Decisiones de Diseño y Arquitectura (DDD)

Para este desarrollo, apliqué principios de **Domain-Driven Design (DDD)** adaptados a la naturaleza orientada a documentos de bases de datos NoSQL como MongoDB:

* **Franchise como Aggregate Root:** En una base de datos relacional, tendríamos tablas separadas. Sin embargo, en MongoDB, para garantizar la consistencia atómica y evitar problemas de concurrencia, modelé `Franchise` como el **Aggregate Root (Raíz de Agregado)**. Las sucursales (`Branches`) y los productos (`Products`) son sub-recursos anidados dentro del mismo documento de la franquicia. Por esta razón, todas las operaciones de mutación se centralizan de forma segura en el `FranchiseUseCase`.
* **Inmutabilidad y Programación Funcional:** Las actualizaciones del árbol de datos (por ejemplo, modificar stock o editar nombres de entidades) se realizan de forma inmutable utilizando la API de `Streams` de Java y el patrón Builder (`.toBuilder()`), evitando efectos secundarios indeseados en ambientes concurrentes.
* **Manejo de Errores Global (Resiliencia):** Implementé un interceptor `@ControllerAdvice` en la capa de infraestructura. Este interceptor captura excepciones personalizadas del dominio y las traduce en respuestas HTTP estandarizadas:
    * **400 Bad Request:** A través de `InvalidDataException` para reglas de negocio infringidas (como enviar un stock con valor negativo o nombres vacíos).
    * **404 Not Found:** A través de `BusinessException` para recursos inexistentes o IDs no encontrados en la base de datos.
* **Endpoints Funcionales (Functional Routing):** En lugar de los controladores tradicionales (`@RestController`), utilicé el modelo funcional de WebFlux con `RouterFunction` y `Handler`, optimizando el rendimiento y adoptando por completo el paradigma reactivo.

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

* **ECR** para almacenar la imagen de la API.
* **VPC**, **subredes públicas**, **Internet Gateway** y **tabla de ruteo**.
* **Security Group** para el acceso a la aplicación.
* **ECS Fargate** para ejecutar el servicio.
* **Application Load Balancer** para exponer la aplicación públicamente.
* **CloudWatch Logs** para centralizar los registros.

### URL pública actual

**👉 AWS ALB:** http://franquicias-alb-1278485183.us-east-1.elb.amazonaws.com

---

## 📚 Documentación Interactiva (Swagger)

A pesar de utilizar un enrutador funcional (`RouterRest`), la API está completamente documentada gracias a la implementación avanzada de anotaciones `@RouterOperations` y `@RouterOperation`.

Una vez la aplicación esté corriendo, puedes ver, inspeccionar los esquemas y probar los 10 endpoints interactuando desde tu navegador en la siguiente URL:

**👉 URL de Swagger UI: http://localhost:8080/webjars/swagger-ui/index.html**

### Endpoints Disponibles:
* `POST /api/franchises` - Crear una nueva franquicia.
* `POST /api/franchises/{franchiseId}/branches` - Agregar una sucursal a una franquicia.
* `GET /api/franchises/{franchiseId}` - Obtener la franquicia completa por ID.
* `POST /api/franchises/{franchiseId}/branches/{branchId}/products` - Agregar un producto a una sucursal.
* `DELETE /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}` - Eliminar un producto de una sucursal.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` - Modificar el stock de un producto.
* `GET /api/franchises/{franchiseId}/max-stock` - Obtener el producto con más stock por sucursal.
* `PUT /api/franchises/{franchiseId}/name` - Editar el nombre de una franquicia.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/name` - Editar el nombre de una sucursal.
* `PUT /api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name` - Editar el nombre de un producto.

---

## 🧪 Pruebas Unitarias (Testing Reactivo)

El código cuenta con una suite completa de **21 pruebas unitarias** enfocadas en blindar toda la lógica de negocio y las restricciones dentro del `FranchiseUseCase`.

Al trabajar con flujos asíncronos inmutables (`Mono`/`Flux`), implementé **`StepVerifier`** de Project Reactor para evaluar la secuencia de eventos a lo largo del tiempo, validando tanto los flujos exitosos (`expectNextMatches`, `verifyComplete`) como el disparo controlado de excepciones (`expectErrorMatches`). La persistencia fue completamente simulada utilizando **Mockito** para asegurar que los tests sean rápidos, independientes y puramente unitarios.

Si deseas ejecutar los tests de forma manual desde tu consola:
```bash
./gradlew test
```

---
*Desarrollado con dedicación por Camilo Tabares.*