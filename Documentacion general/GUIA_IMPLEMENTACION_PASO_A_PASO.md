# 📁 ESTRUCTURA DE DIRECTORIOS Y GUÍA DE IMPLEMENTACIÓN

## 1️⃣ ESTRUCTURA DE CARPETAS RECOMENDADA

```
tu-proyecto-ecommerce/
├── pom.xml                                           ← ACTUALIZAR
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── tuempresa/
│   │   │           └── ecommerce/
│   │   │               ├── EcommerceApplication.java
│   │   │               ├── config/
│   │   │               │   ├── JpaAuditingConfig.java         ✓ REUTILIZAR
│   │   │               │   └── StripeConfig.java             ✓ REUTILIZAR
│   │   │               ├── payment/
│   │   │               │   ├── controller/
│   │   │               │   │   └── PaymentController.java     ✨ NUEVO
│   │   │               │   ├── dto/
│   │   │               │   │   ├── PaymentRequestDTO.java     ✨ NUEVO
│   │   │               │   │   ├── PaymentResponseDTO.java    ✨ NUEVO
│   │   │               │   │   └── PaymentErrorResponseDTO.java (opcional)
│   │   │               │   ├── mapper/
│   │   │               │   │   └── PaymentMapper.java         ✨ NUEVO
│   │   │               │   ├── model/
│   │   │               │   │   ├── Payment.java              ✓ REUTILIZAR
│   │   │               │   │   ├── base/
│   │   │               │   │   │   └── BaseEntity.java       ✓ REUTILIZAR
│   │   │               │   │   └── stripe/
│   │   │               │   │       └── StripePaymentResult.java ✓ REUTILIZAR
│   │   │               │   ├── repository/
│   │   │               │   │   └── PaymentRepository.java    ✓ REUTILIZAR (mejorado)
│   │   │               │   ├── service/
│   │   │               │   │   ├── PaymentService.java       ✓ REUTILIZAR (adaptada)
│   │   │               │   │   ├── StripePaymentService.java ✓ REUTILIZAR
│   │   │               │   │   └── impl/
│   │   │               │   │       ├── PaymentServiceImpl.java ✓ REUTILIZAR (adaptada)
│   │   │               │   │       └── StripePaymentServiceImpl.java ✓ REUTILIZAR
│   │   │               │   └── utils/
│   │   │               │       └── PaymentStatus.java        ✓ REUTILIZAR
│   │   │               ├── exception/
│   │   │               │   ├── GlobalExceptionHandler.java   (opcional)
│   │   │               │   └── PaymentException.java         (opcional)
│   │   │               └── (otros packages de tu app)
│   │   └── resources/
│   │       ├── application.properties                ✓ ACTUALIZAR
│   │       ├── application-dev.properties           (opcional)
│   │       ├── db/
│   │       │   └── migration/
│   │       │       └── V1__init_payments.sql         ✓ REUTILIZAR
│   │       └── (otros archivos de configuración)
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── tuempresa/
│       │           └── ecommerce/
│       │               └── payment/
│       │                   ├── controller/
│       │                   │   └── PaymentControllerTest.java ✨ NUEVO
│       │                   ├── service/
│       │                   │   └── impl/
│       │                   │       └── PaymentServiceImplTest.java ✓ REUTILIZAR
│       │                   ├── mapper/
│       │                   │   └── PaymentMapperTest.java     (opcional)
│       │                   └── (otros tests)
│       └── resources/
│           └── application-test.properties          ✓ REUTILIZAR/ADAPT
```

---

## 2️⃣ LISTA DE VERIFICACIÓN DE IMPLEMENTACIÓN

### FASE 1: Preparación del Proyecto

- [ ] **Crear rama git**: `git checkout -b feature/payment-stripe`

- [ ] **Actualizar pom.xml**:
  - [ ] Agregar `stripe-java` (v31.1.0)
  - [ ] Agregar `spring-boot-starter-validation`
  - [ ] Agregar `spring-boot-starter-data-jpa`
  - [ ] Agregar `mysql-connector-j`
  - [ ] Agregar `flyway-core` y `flyway-mysql`
  - [ ] Agregar `lombok`
  - [ ] ELIMINAR `spring-kafka` si la tenías
  - [ ] Verificar `spring-boot-starter-web`

- [ ] **Crear estructura de carpetas** según el diagrama anterior

---

### FASE 2: Copiar/Adaptar Clases Esenciales

**Paso 1: Modelos y Entidades**

- [ ] Copiar `BaseEntity.java` a `com.tuempresa.ecommerce.payment.model.base`
- [ ] Copiar `PaymentStatus.java` a `com.tuempresa.ecommerce.payment.utils`
- [ ] Copiar `Payment.java` a `com.tuempresa.ecommerce.payment.model`
  - Actualizar imports: cambiar paquete
- [ ] Copiar `StripePaymentResult.java` a `com.tuempresa.ecommerce.payment.model.stripe`

**Paso 2: Servicios (Stripe)**

- [ ] Copiar `StripePaymentService.java` a `com.tuempresa.ecommerce.payment.service`
- [ ] Copiar `StripePaymentServiceImpl.java` a `com.tuempresa.ecommerce.payment.service.impl`
- [ ] Actualizar imports

**Paso 3: Configuración**

- [ ] Copiar `StripeConfig.java` a `com.tuempresa.ecommerce.config`
- [ ] Copiar `JpaAuditingConfig.java` a `com.tuempresa.ecommerce.config`
- [ ] Actualizar imports

**Paso 4: Persistencia**

- [ ] Copiar `PaymentRepository.java` a `com.tuempresa.ecommerce.payment.repository`
  - Agregar métodos adicionales (ver documento anterior)
- [ ] Crear `V1__init_payments.sql` en `src/main/resources/db/migration`

---

### FASE 3: Crear Nuevas Clases (SIN KAFKA)

**Paso 1: DTOs**

- [ ] Crear `PaymentRequestDTO.java`
- [ ] Crear `PaymentResponseDTO.java`
- [ ] Crear `PaymentErrorResponseDTO.java` (opcional)

**Paso 2: Mapper**

- [ ] Crear `PaymentMapper.java` (componente Spring)

**Paso 3: Service (Adaptada)**

- [ ] Crear `PaymentService.java` (interface adaptada)
  - Cambiar método de `void processPayment(PaymentRequestedEvent)` 
  - A: `PaymentResponseDTO processPayment(PaymentRequestDTO)`
  - Agregar: `getPaymentStatus(Long paymentId)`
  - Agregar: `getPaymentsByOrderId(Long orderId)`

- [ ] Crear `PaymentServiceImpl.java` (implementación sin Kafka)
  - Remover inyección de `PaymentResultProducer`
  - Remover `.sendSucceeded()` y `.sendFailed()`
  - Agregar retorno de `PaymentResponseDTO`

**Paso 4: Controller**

- [ ] Crear `PaymentController.java`
  - POST `/api/v1/payments/process` → processPayment()
  - GET `/api/v1/payments/{paymentId}` → getPaymentStatus()
  - GET `/api/v1/payments/order/{orderId}` → getOrderPayments()

---

### FASE 4: Configuración y Base de Datos

- [ ] **Actualizar application.properties**:
  ```
  stripe.api.key=YOUR_SECRET_KEY
  stripe.publish.key=YOUR_PUBLISHABLE_KEY
  spring.datasource.url=jdbc:mysql://localhost:3306/tu_db
  spring.datasource.username=root
  spring.datasource.password=123
  spring.jpa.hibernate.ddl-auto=validate
  spring.flyway.enabled=true
  ```

- [ ] **Crear migración Flyway**: `V1__init_payments.sql`

- [ ] **Ejecutar migraciones**:
  ```bash
  mvn clean install
  mvn flyway:info
  mvn flyway:migrate
  ```

---

### FASE 5: Testing

- [ ] Copiar tests esenciales:
  - [ ] `PaymentServiceImplTest.java`
  - [ ] `StripePaymentResultTest.java`
  - [ ] `PaymentStatusTest.java`
  - [ ] `JpaAuditingConfigTest.java`

- [ ] Crear nuevos tests:
  - [ ] `PaymentControllerTest.java`
  - [ ] `PaymentMapperTest.java` (opcional)

- [ ] Ejecutar tests: `mvn clean test`

- [ ] Verificar cobertura: `mvn clean test -Pcoverage`

---

### FASE 6: Prueba Manual

- [ ] **Start de la app**: `mvn spring-boot:run`

- [ ] **Obtener token de prueba de Stripe**:
  - Ir a https://stripe.com/docs/testing
  - Usar card: `4242 4242 4242 4242`
  - Generar token en Stripe

- [ ] **Test POST /api/v1/payments/process**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/payments/process \
    -H "Content-Type: application/json" \
    -d '{
      "orderId": 1,
      "amount": 99.99,
      "stripeToken": "pm_card_visa",
      "currency": "EUR"
    }'
  ```

- [ ] **Verificar BD**: Consultar tabla `payments`

- [ ] **Test GET /api/v1/payments/{paymentId}**: 
  ```bash
  curl http://localhost:8080/api/v1/payments/1
  ```

---

## 3️⃣ CAMBIOS EN pom.xml

### ✅ Dependencias a AGREGAR:

```xml
<!-- Stripe -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>31.1.0</version>
</dependency>

<!-- Base de datos -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Migraciones -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- H2 para tests -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### ❌ Dependencias a ELIMINAR (si las tienes):

```xml
<!-- NO NECESARIAS - ELIMINAR -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## 4️⃣ CAMBIOS PRINCIPALES EN LÓGICA

### Antes (Original con Kafka):
```
Cliente → Kafka Topic (payment.requested)
    ↓
PaymentEventListener (escucha)
    ↓
PaymentServiceImpl.processPayment(PaymentRequestedEvent)
    ↓
PaymentResultProducer.sendSucceeded/Failed()
    ↓
Kafka Topic (payment.result) → Cliente
```

### Después (Tu Proyecto):
```
Cliente → HTTP POST /api/v1/payments/process
    ↓
PaymentController.processPayment(PaymentRequestDTO)
    ↓
PaymentServiceImpl.processPayment(PaymentRequestDTO)
    ↓
HTTP Response ← PaymentResponseDTO
```

---

## 5️⃣ MIGRACIONES DE BASE DE DATOS

### Crear archivo: `src/main/resources/db/migration/V1__init_payments.sql`

```sql
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    order_id BIGINT NOT NULL,
    stripe_payment_id VARCHAR(255),
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6️⃣ ARCHIVOS FINALES A ENTREGAR

### ✅ Archivos que vas a tener:

```
✓ src/main/java/com/tuempresa/ecommerce/config/StripeConfig.java
✓ src/main/java/com/tuempresa/ecommerce/config/JpaAuditingConfig.java
✓ src/main/java/com/tuempresa/ecommerce/payment/controller/PaymentController.java
✓ src/main/java/com/tuempresa/ecommerce/payment/dto/PaymentRequestDTO.java
✓ src/main/java/com/tuempresa/ecommerce/payment/dto/PaymentResponseDTO.java
✓ src/main/java/com/tuempresa/ecommerce/payment/mapper/PaymentMapper.java
✓ src/main/java/com/tuempresa/ecommerce/payment/model/Payment.java
✓ src/main/java/com/tuempresa/ecommerce/payment/model/base/BaseEntity.java
✓ src/main/java/com/tuempresa/ecommerce/payment/model/stripe/StripePaymentResult.java
✓ src/main/java/com/tuempresa/ecommerce/payment/repository/PaymentRepository.java
✓ src/main/java/com/tuempresa/ecommerce/payment/service/PaymentService.java
✓ src/main/java/com/tuempresa/ecommerce/payment/service/StripePaymentService.java
✓ src/main/java/com/tuempresa/ecommerce/payment/service/impl/PaymentServiceImpl.java
✓ src/main/java/com/tuempresa/ecommerce/payment/service/impl/StripePaymentServiceImpl.java
✓ src/main/java/com/tuempresa/ecommerce/payment/utils/PaymentStatus.java
✓ src/main/resources/db/migration/V1__init_payments.sql
✓ src/test/resources/application-test.properties (adaptado)
```

---

## 7️⃣ PASOS RÁPIDOS (TL;DR)

```bash
# 1. Actualizar pom.xml con dependencias
# 2. Crear estructura de carpetas
# 3. Copiar 10 clases del payment-service original
# 4. Crear 3 DTOs nuevas
# 5. Crear PaymentMapper
# 6. Crear PaymentController
# 7. Adaptar PaymentServiceImpl (remover Kafka)
# 8. Crear tabla Flyway
# 9. Actualizar application.properties
# 10. Ejecutar: mvn clean install && mvn spring-boot:run
# 11. Test: POST localhost:8080/api/v1/payments/process
```

---


