# 📦 ARCHIVOS LISTOS PARA COPIAR Y PEGAR (PARTE 2: DEL ORIGINAL)

## 🔄 Estos archivos se reutilizan directamente del payment-service

---

## 📄 ARCHIVO 11: BaseEntity.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.model.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base para auditoría automática.
 * Heredar de esta clase proporciona createdAt y updatedAt automáticos.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = true)
    private LocalDateTime updatedAt;
}
```

---

## 📄 ARCHIVO 12: PaymentStatus.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.utils;

/**
 * Enum para los estados posibles de un pago.
 */
public enum PaymentStatus {
    SUCCEEDED,
    FAILED
}
```

---

## 📄 ARCHIVO 13: StripePaymentResult.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.model.stripe;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Encapsula la respuesta de Stripe.
 */
@Getter
@AllArgsConstructor
public class StripePaymentResult {

    /**
     * ID del Payment Intent en Stripe.
     */
    private final String paymentIntentId;

    /**
     * Estado retornado por Stripe: succeeded, requires_action, failed
     */
    private final String status;
}
```

---

## 📄 ARCHIVO 14: Payment.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.model;

import com.tuempresa.ecommerce.payment.model.base.BaseEntity;
import com.tuempresa.ecommerce.payment.utils.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad JPA para pagos.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "payments")
@Table(name = "payments")
public class Payment extends BaseEntity {

    /**
     * ID de la orden.
     */
    private Long orderId;

    /**
     * ID del Payment Intent en Stripe.
     */
    private String stripePaymentId;

    /**
     * Monto del pago.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Estado del pago.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
}
```

---

## 📄 ARCHIVO 15: StripePaymentService.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.service;

import com.stripe.exception.StripeException;
import com.tuempresa.ecommerce.payment.model.stripe.StripePaymentResult;

import java.math.BigDecimal;

/**
 * Interface para integración con Stripe.
 */
public interface StripePaymentService {

    /**
     * Procesa un pago con Stripe.
     *
     * @param amount Monto a cobrar
     * @param currency Moneda (ej: EUR, USD)
     * @param paymentMethodId Token/ID del método de pago
     * @return Resultado del pago
     * @throws StripeException si hay error en Stripe
     */
    StripePaymentResult charge(BigDecimal amount, String currency, String paymentMethodId) 
            throws StripeException;
}
```

---

## 📄 ARCHIVO 16: StripePaymentServiceImpl.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.payment.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.tuempresa.ecommerce.payment.model.stripe.StripePaymentResult;
import com.tuempresa.ecommerce.payment.service.StripePaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementación de integración con Stripe.
 */
@Service
public class StripePaymentServiceImpl implements StripePaymentService {

    /**
     * Procesa un pago con Stripe.
     * Convertir cantidad a centavos (× 100) y crear PaymentIntent.
     */
    @Override
    public StripePaymentResult charge(
            BigDecimal amount,
            String currency,
            String paymentMethodId) throws StripeException {

        // Crear parámetros del PaymentIntent
        PaymentIntentCreateParams params =
            PaymentIntentCreateParams.builder()
                .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency(currency.toLowerCase())
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                        )
                        .build()
                )
                .build();

        // Crear el PaymentIntent
        PaymentIntent intent = PaymentIntent.create(params);

        // Retornar resultado
        return new StripePaymentResult(intent.getId(), intent.getStatus());
    }
}
```

---

## 📄 ARCHIVO 17: StripeConfig.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Stripe.
 * Inyecta la API key al iniciar la aplicación.
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
```

---

## 📄 ARCHIVO 18: JpaAuditingConfig.java ✓ REUTILIZAR

```java
package com.tuempresa.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuración de JPA Auditing.
 * Habilita auditoría automática en entidades (createdAt, updatedAt).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("SYSTEM");
    }
}
```

---

## 📄 ARCHIVO 19: application-test.properties ✓ REUTILIZAR/ADAPTAR

```properties
# Base de datos H2 en memoria para tests
spring.datasource.url=jdbc:h2:mem:test
spring.datasource.username=sa
spring.datasource.password=

# H2 Console (deshabilitada por seguridad)
spring.h2.console.enabled=false

# JPA Configuration
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.defer-datasource-initialization=true

# H2 Dialect
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# Flyway (deshabilitada para tests)
spring.sql.init.schema-locations=
spring.sql.init.data-locations=
spring.flyway.enabled=false

# Stripe
stripe.api.key=sk_test_fake_key_for_testing
```

---

## 🔍 CONVERSIÓN DE PAQUETES REQUERIDA

Todos los archivos anteriores usan: `com.yashmerino.ecommerce`

Debes cambiar a: `com.tuempresa.ecommerce`

### Cambios necesarios en CADA ARCHIVO:

**DE:**
```java
package com.yashmerino.ecommerce.payment.model;
import com.yashmerino.ecommerce.model.base.BaseEntity;
```

**A:**
```java
package com.tuempresa.ecommerce.payment.model;
import com.tuempresa.ecommerce.payment.model.base.BaseEntity;
```

### Pro Tip: Usa Find & Replace de tu IDE

En IntelliJ IDEA:
1. `Ctrl+H` (o `Cmd+H` en Mac)
2. Find: `com.yashmerino.ecommerce`
3. Replace with: `com.tuempresa.ecommerce`
4. Replace All

---

## 📋 CHECKLIST FINAL

### ARCHIVOS NUEVOS A CREAR (10):
- [ ] PaymentRequestDTO.java
- [ ] PaymentResponseDTO.java
- [ ] PaymentMapper.java
- [ ] PaymentController.java
- [ ] PaymentService.java (interface adaptada)
- [ ] PaymentServiceImpl.java (adaptada sin Kafka)
- [ ] Actualizar PaymentRepository.java
- [ ] application.properties (nuevo/actualizado)
- [ ] V1__init_payments.sql
- [ ] pom.xml (actualizar dependencias)

### ARCHIVOS A REUTILIZAR (9):
- [ ] BaseEntity.java
- [ ] PaymentStatus.java
- [ ] StripePaymentResult.java
- [ ] Payment.java
- [ ] StripePaymentService.java
- [ ] StripePaymentServiceImpl.java
- [ ] StripeConfig.java
- [ ] JpaAuditingConfig.java
- [ ] application-test.properties

### TOTAL: 19 archivos

---

## 🚀 ORDEN DE CREACIÓN RECOMENDADO

### Paso 1: Configuración Base
1. Actualizar pom.xml
2. Actualizar application.properties

### Paso 2: Modelos y Base
3. Crear carpeta `payment/model/base/`
4. Crear `BaseEntity.java`
5. Crear `PaymentStatus.java`
6. Crear `StripePaymentResult.java`
7. Crear `Payment.java`

### Paso 3: Servicios Stripe
8. Crear `payment/service/`
9. Crear `StripePaymentService.java`
10. Crear `StripePaymentServiceImpl.java`

### Paso 4: Configuración
11. Crear `config/StripeConfig.java`
12. Crear `config/JpaAuditingConfig.java`

### Paso 5: Persistencia
13. Crear `payment/repository/PaymentRepository.java`

### Paso 6: DTOs y Mappers
14. Crear `payment/dto/PaymentRequestDTO.java`
15. Crear `payment/dto/PaymentResponseDTO.java`
16. Crear `payment/mapper/PaymentMapper.java`

### Paso 7: Servicios Adaptados
17. Crear `payment/service/PaymentService.java` (ADAPTADA)
18. Crear `payment/service/impl/PaymentServiceImpl.java` (ADAPTADA)

### Paso 8: Controller REST
19. Crear `payment/controller/PaymentController.java`

### Paso 9: BD
20. Crear `src/main/resources/db/migration/V1__init_payments.sql`

### Paso 10: Testing
21. Crear `application-test.properties`
22. (Opcional) Crear tests

---

## ✅ VALIDACIÓN FINAL

Después de crear todos los archivos:

```bash
# 1. Compilar
mvn clean compile

# 2. Instalar dependencias
mvn clean install

# 3. Ejecutar tests
mvn test

# 4. Iniciar aplicación
mvn spring-boot:run
```

### Debería ver:
```
...
Hibernate: CREATE TABLE payments (...)
...
Tomcat started on port(s): 8080 (http)
Application started successfully!
```

### Prueba rápida:
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

---

## 🆘 ERRORES COMUNES

### ❌ Error: `StripeConfig could not autowire field`
**Solución**: Verifica que tienes `${stripe.api.key}` en `application.properties`

### ❌ Error: `BaseEntity not found`
**Solución**: Verifica que creaste BaseEntity en correcta carpeta: `payment/model/base/`

### ❌ Error: `Table 'payments' doesn't exist`
**Solución**: Verifica que Flyway está habilitado y tienes V1__init_payments.sql

### ❌ Error: `PaymentMapper constructor not found`
**Solución**: Asegúrate que PaymentMapper tiene anotación `@Component`

### ❌ Error: `Invalid API Key`
**Solución**: Usa clave de test de Stripe: `sk_test_...`

---

## 📞 RESUMEN DE CONTACTOS

- **Paquete base**: `com.tuempresa.ecommerce`
- **Paquete de pago**: `com.tuempresa.ecommerce.payment`
- **Puerto default**: `8080`
- **Endpoint principal**: `POST /api/v1/payments/process`
- **BD**: MySQL (ecommerce_db)
- **Stripe**: v31.1.0

---


