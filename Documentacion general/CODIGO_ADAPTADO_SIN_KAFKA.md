# 💾 CÓDIGO ADAPTADO PARA TU PROYECTO (SIN KAFKA)

## PARTE 1: CLASES DE DATOS (DTOs)

### 1. PaymentRequestDTO.java
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
 * Reemplaza a PaymentRequestedEvent (que venía de Kafka).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentRequestDTO {

    /**
     * ID de la orden a la cual se asocia el pago.
     */
    @NotNull(message = "El orderId no puede ser nulo")
    private Long orderId;

    /**
     * Monto a cobrar.
     */
    @NotNull(message = "El amount no puede ser nulo")
    @Positive(message = "El amount debe ser positivo")
    private BigDecimal amount;

    /**
     * Token de Stripe generado en el frontend.
     */
    @NotNull(message = "El stripeToken no puede ser nulo")
    private String stripeToken;

    /**
     * Moneda de pago (default: EUR).
     */
    private String currency = "EUR";
}
```

---

### 2. PaymentResponseDTO.java
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
 * DTO para retornar respuestas de pago hacia el cliente.
 * Reemplaza a PaymentResultEvent (que venía de Kafka).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentResponseDTO {

    /**
     * ID del pago en la BD local (generado por tu aplicación).
     */
    private Long paymentId;

    /**
     * ID de la orden asociada.
     */
    private Long orderId;

    /**
     * Estado del pago (SUCCEEDED o FAILED).
     */
    private PaymentStatus status;

    /**
     * ID del intent de pago en Stripe (si fue exitoso).
     */
    private String stripePaymentIntentId;

    /**
     * Mensaje de error (si el pago falló).
     */
    private String errorMessage;

    /**
     * Timestamp cuando se procesó el pago.
     */
    private LocalDateTime processedAt;

    /**
     * Mensaje amigable para el usuario.
     */
    private String message;
}
```

---

### 3. PaymentErrorResponseDTO.java (Opcional)
```java
package com.tuempresa.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de error en pagos.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentErrorResponseDTO {

    /**
     * Código de error.
     */
    private String errorCode;

    /**
     * Mensaje de error detallado.
     */
    private String message;

    /**
     * Descripción de qué salió mal.
     */
    private String details;

    /**
     * Timestamp del error.
     */
    private LocalDateTime timestamp;

    /**
     * Path del endpoint donde ocurrió el error.
     */
    private String path;
}
```

---

## PARTE 2: ADAPTER/CONVERTER

### 4. PaymentMapper.java (Utility)
```java
package com.tuempresa.ecommerce.payment.mapper;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;
import com.tuempresa.ecommerce.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper para convertir entre entidades y DTOs.
 */
@Component
public class PaymentMapper {

    /**
     * Convierte una entidad Payment a DTO de respuesta.
     */
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

    /**
     * Convierte una entidad Payment a DTO con error.
     */
    public PaymentResponseDTO toResponseDTOWithError(Payment payment, String errorMessage) {
        PaymentResponseDTO response = toResponseDTO(payment);
        response.setErrorMessage(errorMessage);
        response.setMessage("El pago no se pudo procesar: " + errorMessage);
        return response;
    }

    /**
     * Construye mensaje amigable según el estado.
     */
    private String buildMessage(Payment.PaymentStatus status) {
        return status.name().equals("SUCCEEDED") 
            ? "Pago procesado exitosamente" 
            : "El pago no fue procesado";
    }
}
```

**NOTA**: Si tu Payment usa enum como `Payment.PaymentStatus`, ajusta el mapper accordingly.

---

## PARTE 3: REST CONTROLLER

### 5. PaymentController.java ⭐ NUEVO PRINCIPAL
```java
package com.tuempresa.ecommerce.payment.controller;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentErrorResponseDTO;
import com.tuempresa.ecommerce.payment.service.PaymentService;
import com.tuempresa.ecommerce.payment.mapper.PaymentMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.exception.StripeException;

import java.time.LocalDateTime;

/**
 * REST Controller para procesar pagos.
 * Reemplaza a PaymentEventListener (que escuchaba Kafka).
 */
@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    /**
     * Endpoint para procesar un pago.
     * 
     * POST /api/v1/payments/process
     * Body: PaymentRequestDTO
     * Response: PaymentResponseDTO
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request) {
        
        log.info("Procesando pago para orden: {}, monto: {}", 
                request.getOrderId(), request.getAmount());

        try {
            PaymentResponseDTO response = paymentService.processPayment(request);
            
            HttpStatus status = response.getStatus().name().equals("SUCCEEDED") 
                ? HttpStatus.OK 
                : HttpStatus.PAYMENT_REQUIRED;
            
            return new ResponseEntity<>(response, status);

        } catch (StripeException e) {
            log.error("Error de Stripe al procesar pago: {}", e.getMessage());
            PaymentErrorResponseDTO error = PaymentErrorResponseDTO.builder()
                    .errorCode("STRIPE_ERROR")
                    .message("Error al procesar pago con Stripe")
                    .details(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/payments/process")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        } catch (Exception e) {
            log.error("Error inesperado al procesar pago: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint para obtener estado de un pago.
     * 
     * GET /api/v1/payments/{paymentId}
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
     * Endpoint para obtener pagos de una orden.
     * 
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Object> getOrderPayments(
            @PathVariable Long orderId) {
        
        log.info("Obteniendo pagos de orden: {}", orderId);
        
        try {
            Object payments = paymentService.getPaymentsByOrderId(orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error al obtener pagos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
```

---

## PARTE 4: SERVICIO ADAPTADO

### 6. PaymentService.java (Interface - ADAPTADA)
```java
package com.tuempresa.ecommerce.payment.service;

import com.tuempresa.ecommerce.payment.dto.PaymentRequestDTO;
import com.tuempresa.ecommerce.payment.dto.PaymentResponseDTO;

/**
 * Interface adaptada para procesamiento de pagos sin Kafka.
 */
public interface PaymentService {

    /**
     * Procesa un pago desde una solicitud HTTP.
     *
     * @param request DTO con datos del pago
     * @return DTO con resultado del pago
     */
    PaymentResponseDTO processPayment(PaymentRequestDTO request);

    /**
     * Obtiene el estado de un pago existente.
     *
     * @param paymentId ID del pago
     * @return DTO con datos del pago
     */
    PaymentResponseDTO getPaymentStatus(Long paymentId);

    /**
     * Obtiene los pagos de una orden.
     *
     * @param orderId ID de la orden
     * @return Lista de pagos
     */
    Object getPaymentsByOrderId(Long orderId);
}
```

---

### 7. PaymentServiceImpl.java (ADAPTADA - SIN KAFKA)
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación adaptada del servicio de pagos (SIN ProductPaymentResultProducer).
 * ProductPaymentResultProducer fue reemplazado por ResponseEntity HTTP.
 */
@Service
@AllArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final StripePaymentService stripeService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Procesa un pago desde solicitud HTTP (sin Kafka).
     * 
     * ✓ CAMBIO PRINCIPAL: Retorna PaymentResponseDTO en lugar de producir evento Kafka
     */
    @Override
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        log.info("Iniciando procesamiento de pago para orden: {}", request.getOrderId());

        try {
            // 1️⃣ Procesar con Stripe
            StripePaymentResult result = stripeService.charge(
                    request.getAmount(),
                    request.getCurrency(),
                    request.getStripeToken()
            );

            // 2️⃣ Crear entidad de pago exitoso
            Payment payment = new Payment(
                    request.getOrderId(),
                    result.getPaymentIntentId(),
                    request.getAmount(),
                    PaymentStatus.SUCCEEDED
            );

            // 3️⃣ Guardar en BD
            payment = paymentRepository.save(payment);

            // 4️⃣ Retornar respuesta (en lugar de producir evento Kafka)
            PaymentResponseDTO response = paymentMapper.toResponseDTO(payment);
            response.setMessage("Pago procesado exitosamente");
            
            log.info("Pago exitoso para orden: {} (ID: {})", 
                    request.getOrderId(), payment.getId());
            
            return response;

        } catch (Exception e) {
            log.error("Error procesando pago para orden: {}", 
                    request.getOrderId(), e);

            // 🔴 Caso de error: Guardar pago fallido
            Payment failedPayment = new Payment(
                    request.getOrderId(),
                    null,
                    request.getAmount(),
                    PaymentStatus.FAILED
            );
            failedPayment = paymentRepository.save(failedPayment);

            // ⚠️ Retornar respuesta de error (en lugar de producir evento Kafka)
            PaymentResponseDTO response = paymentMapper.toResponseDTOWithError(
                    failedPayment, 
                    e.getMessage()
            );
            
            return response;
        }
    }

    /**
     * Obtiene el estado actualizado de un pago.
     */
    @Override
    public PaymentResponseDTO getPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));
        
        return paymentMapper.toResponseDTO(payment);
    }

    /**
     * Obtiene todos los pagos asociados a una orden.
     */
    @Override
    public Object getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
```

**CAMBIOS PRINCIPALES**:
- ❌ Eliminamos `PaymentResultProducer`
- ✅ Retornamos `PaymentResponseDTO` directamente
- ✅ Los métodos lanzan excepciones que el Controller maneja
- ✅ Mismo `@Transactional` para garantizar integridad

---

## PARTE 5: ACTUALIZACIÓN DEL REPOSITORY

### 8. PaymentRepository.java (MEJORADA)
```java
package com.tuempresa.ecommerce.payment.repository;

import com.tuempresa.ecommerce.payment.model.Payment;
import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para Payment con métodos personalizados útiles.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Encuentra pagos por ID de orden.
     */
    List<Payment> findByOrderId(Long orderId);

    /**
     * Encuentra el pago más reciente de una orden.
     */
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Encuentra pagos por estado.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Encuentra pagos exitosos por orden.
     */
    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
```

---

## PARTE 6: CONFIGURACIÓN

### 9. Actualizar application.properties
```properties
# Stripe API Key
stripe.api.key=${STRIPE_API_KEY:YOUR_STRIPE_KEY_HERE}

# Datos de Stripe (TEST MODE)
stripe.publish.key=${STRIPE_PUBLISH_KEY:pk_test_...}

# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/tu_db_ecommerce
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## PARTE 7: RESUMEN DE CAMBIOS

### ✅ Lo que CONSERVAS del original:
```
- Payment.java
- BaseEntity.java
- PaymentStatus.java
- StripePaymentResult.java
- PaymentRepository.java (con mejoras)
- StripePaymentService.java
- StripePaymentServiceImpl.java
- StripeConfig.java
- JpaAuditingConfig.java
- V1__init.sql
```

### 🚀 Lo que AÑADES:
```
- PaymentController.java (NUEVO)
- PaymentRequestDTO.java (NUEVO)
- PaymentResponseDTO.java (NUEVO)
- PaymentErrorResponseDTO.java (NUEVO - Optional)
- PaymentMapper.java (NUEVO)
- PaymentService.java (ADAPTADA - Interface)
- PaymentServiceImpl.java (ADAPTADA - sin Kafka)
```

### ❌ Lo que ELIMINAS:
```
- PaymentEventListener.java
- PaymentResultProducer.java
- PaymentRequestedEvent.java
- PaymentResultEvent.java
- spring-kafka (dependencia)
```

---

