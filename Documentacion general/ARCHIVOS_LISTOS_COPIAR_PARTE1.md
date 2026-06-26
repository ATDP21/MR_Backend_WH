# 📦 ARCHIVOS LISTOS PARA COPIAR Y PEGAR (PARTE 1)

## ⚠️ NOTA IMPORTANTE
Asegúrate de ajustar los nombres de paquetes según tu proyecto.
Todos los ejemplos usan: `com.tuempresa.ecommerce`

---

## 📄 ARCHIVO 1: PaymentRequestDTO.java

```java
package com.tuempresa.ecommerce.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para recibir solicitudes de pago desde el cliente.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentRequestDTO {

    @NotNull(message = "El orderId no puede ser nulo")
    private Long orderId;

    @NotNull(message = "El amount no puede ser nulo")
    @Positive(message = "El amount debe ser positivo")
    private BigDecimal amount;

    @NotNull(message = "El stripeToken no puede ser nulo")
    private String stripeToken;

    private String currency = "EUR";
}
```

---

## 📄 ARCHIVO 2: PaymentResponseDTO.java

```java
package com.tuempresa.ecommerce.payment.dto;

import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO para retornar respuestas de pago.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentResponseDTO {

    private Long paymentId;
    private Long orderId;
    private PaymentStatus status;
    private String stripePaymentIntentId;
    private String errorMessage;
    private LocalDateTime processedAt;
    private String message;
}
```

---

## 📄 ARCHIVO 3: PaymentMapper.java

```java
package com.tuempresa.ecommerce.payment.mapper;

import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;
import com.tuempresa.ecommerce.payment.model.Payment;
import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversiones entre entidades y DTOs.
 */
@Component
public class PaymentMapper {

    public PaymentResponseDTO toResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .stripePaymentIntentId(payment.getStripePaymentId())
                .processedAt(payment.getCreatedAt())
                .message(buildMessage(payment.getStatus()))
                .build();
    }

    public PaymentResponseDTO toResponseDTOWithError(Payment payment, String errorMessage) {
        PaymentResponseDTO response = toResponseDTO(payment);
        response.setErrorMessage(errorMessage);
        response.setMessage("El pago no se pudo procesar: " + errorMessage);
        return response;
    }

    private String buildMessage(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED 
            ? "Pago procesado exitosamente" 
            : "El pago no fue procesado";
    }
}
```

---

## 📄 ARCHIVO 4: PaymentController.java ⭐ IMPORTANTE

```java
package com.tuempresa.ecommerce.payment.controller;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;
import com.tuempresa.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.exception.StripeException;

import java.time.LocalDateTime;

/**
 * REST Controller para procesar pagos con Stripe.
 */
@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/process
     * Procesa un pago
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request) {
        
        log.info("Procesando pago para orden: {}, monto: {}", 
                request.getOrderId(), request.getAmount());

        try {
            PaymentResponseDTO response = paymentService.processPayment(request);
            
            HttpStatus status = response.getStatus().toString().equals("SUCCEEDED") 
                ? HttpStatus.OK 
                : HttpStatus.PAYMENT_REQUIRED;
            
            return new ResponseEntity<>(response, status);

        } catch (Exception e) {
            log.error("Error al procesar pago: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * GET /api/v1/payments/{paymentId}
     * Obtiene estado de un pago
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @PathVariable Long paymentId) {
        
        log.info("Obteniendo estado de pago: {}", paymentId);
        
        try {
            PaymentResponseDTO response = paymentService.getPaymentStatus(paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Pago no encontrado: {}", paymentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/payments/order/{orderId}
     * Obtiene pagos de una orden
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderPayments(
            @PathVariable Long orderId) {
        
        log.info("Obteniendo pagos de orden: {}", orderId);
        
        try {
            var payments = paymentService.getPaymentsByOrderId(orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error al obtener pagos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
```

---

## 📄 ARCHIVO 5: PaymentService.java (Interface Adaptada)

```java
package com.tuempresa.ecommerce.payment.service;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;

/**
 * Service interface para procesamiento de pagos.
 */
public interface PaymentService {

    /**
     * Procesa un pago desde una solicitud HTTP.
     */
    PaymentResponseDTO processPayment(PaymentRequestDTO request);

    /**
     * Obtiene el estado de un pago.
     */
    PaymentResponseDTO getPaymentStatus(Long paymentId);

    /**
     * Obtiene los pagos de una orden.
     */
    Object getPaymentsByOrderId(Long orderId);
}
```

---

## 📄 ARCHIVO 6: PaymentServiceImpl.java (ADAPTADA - SIN KAFKA)

```java
package com.tuempresa.ecommerce.payment.service.impl;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;
import com.tuempresa.ecommerce.payment.mapper.PaymentMapper;
import com.tuempresa.ecommerce.payment.model.Payment;
import com.tuempresa.ecommerce.payment.repository.PaymentRepository;
import com.tuempresa.ecommerce.payment.service.PaymentService;
import com.tuempresa.ecommerce.payment.service.StripePaymentService;
import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import com.tuempresa.ecommerce.payment.model.stripe.StripePaymentResult;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio de pagos (SIN KAFKA).
 * ✨ Cambio principal: Retorna DTOs en lugar de producir eventos Kafka
 */
@Service
@AllArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final StripePaymentService stripeService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Procesa un pago.
     * Retorna DTO con resultado (en lugar de producir evento Kafka).
     */
    @Override
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        log.info("Iniciando procesamiento de pago para orden: {}", request.getOrderId());

        try {
            // 1. Procesar con Stripe
            StripePaymentResult result = stripeService.charge(
                    request.getAmount(),
                    request.getCurrency(),
                    request.getStripeToken()
            );

            // 2. Crear pago exitoso
            Payment payment = new Payment(
                    request.getOrderId(),
                    result.getPaymentIntentId(),
                    request.getAmount(),
                    PaymentStatus.SUCCEEDED
            );

            // 3. Guardar en BD
            payment = paymentRepository.save(payment);

            // 4. Retornar respuesta exitosa
            PaymentResponseDTO response = paymentMapper.toResponseDTO(payment);
            response.setMessage("Pago procesado exitosamente");
            
            log.info("✅ Pago exitoso para orden: {} (paymentId: {})", 
                    request.getOrderId(), payment.getId());
            
            return response;

        } catch (Exception e) {
            log.error("❌ Error procesando pago para orden: {}", 
                    request.getOrderId(), e);

            // Guardar pago fallido
            Payment failedPayment = new Payment(
                    request.getOrderId(),
                    null,
                    request.getAmount(),
                    PaymentStatus.FAILED
            );
            failedPayment = paymentRepository.save(failedPayment);

            // Retornar respuesta con error
            PaymentResponseDTO response = paymentMapper.toResponseDTOWithError(
                    failedPayment, 
                    e.getMessage()
            );
            
            log.info("Pago fallido guardado con ID: {}", failedPayment.getId());
            return response;
        }
    }

    /**
     * Obtiene el estado de un pago existente.
     */
    @Override
    public PaymentResponseDTO getPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));
        
        return paymentMapper.toResponseDTO(payment);
    }

    /**
     * Obtiene todos los pagos de una orden.
     */
    @Override
    public Object getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
}
```

---

## 📄 ARCHIVO 7: PaymentRepository.java (MEJORADA)

```java
package com.tuempresa.ecommerce.payment.repository;

import com.tuempresa.ecommerce.payment.model.Payment;
import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para Payment con métodos personalizados.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Métodos adicionales útiles
    List<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
```

---

## 📄 ARCHIVO 8: application.properties

```properties
# ============================================
# Stripe Configuration
# ============================================
stripe.api.key=${STRIPE_API_KEY:sk_test_YOUR_KEY_HERE}

# ============================================
# Database Configuration
# ============================================
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=root
spring.datasource.password=123

# ============================================
# JPA Configuration
# ============================================
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# ============================================
# Flyway Configuration
# ============================================
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# ============================================
# Server Configuration
# ============================================
server.port=8080
server.servlet.context-path=/
```

---

## 📄 ARCHIVO 9: V1__init_payments.sql

```sql
-- Migración inicial para tabla de pagos

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    order_id BIGINT NOT NULL,
    stripe_payment_id VARCHAR(255) UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de auditoría (opcional, para tracking más detallado)
CREATE TABLE IF NOT EXISTS payment_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    timestamp DATETIME NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_payment_id (payment_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 📄 ARCHIVO 10: pom.xml (Sección de Dependencias)

Agrega esto a tu pom.xml:

```xml
<!-- Stripe Payment -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>31.1.0</version>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- MySQL Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Flyway (Database Migrations) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 (In-memory DB for tests) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## ⚡ QUICK START

1. Copia los 10 archivos anteriores a tu proyecto
2. Reemplaza `com.tuempresa.ecommerce` con tu package
3. Copia también:
   - `Payment.java`
   - `BaseEntity.java`
   - `PaymentStatus.java`
   - `StripePaymentResult.java`
   - `StripePaymentService.java`
   - `StripePaymentServiceImpl.java`
   - `StripeConfig.java`
   - `JpaAuditingConfig.java`
4. Ejecuta: `mvn clean install && mvn spring-boot:run`
5. Test: `POST localhost:8080/api/v1/payments/process`

---


