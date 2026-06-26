# 🔄 COMPARATIVA VISUAL Y DIAGRAMA DE INTEGRACIÓN

## 1️⃣ FLUJO ANTES (Payment Service Original con Kafka)

```
┌─────────────────────────────────────┐
│       CLIENTE (OTRO SISTEMA)        │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   KAFKA BROKER                      │
│   Topic: payment.requested          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   PAYMENT-SERVICE                   │
│   ┌─────────────────────────────┐   │
│   │ PaymentEventListener        │   │
│   │ @KafkaListener              │   │
│   │ onPaymentRequested()        │   │
│   └────────────┬────────────────┘   │
│                │                    │
│                ▼                    │
│   ┌─────────────────────────────┐   │
│   │ PaymentServiceImpl           │   │
│   │ processPayment()            │   │
│   │  - Call Stripe             │   │
│   │  - Save to DB              │   │
│   │  - Send Result             │   │
│   └────────────┬────────────────┘   │
│                │                    │
│   ┌────────────▼────────────────┐   │
│   │ PaymentResultProducer       │   │
│   │ sendSucceeded/Failed()      │   │
│   └─────────────┬───────────────┘   │
└──────────────┬─────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   KAFKA BROKER                      │
│   Topic: payment.result             │
└─────────────────────────────────────┘
               │
               ▼
         Cliente Consumidor
```

---

## 2️⃣ FLUJO DESPUÉS (Tu Proyecto - REST/HTTP)

```
┌─────────────────────────────────────┐
│    CLIENTE (React Web/Mobile)       │
│                                     │
│  fetch('/api/v1/payments/process',  │
│    { method: 'POST',                │
│      body: { orderId, amount,       │
│               stripeToken }         │
│    })                               │
└──────────────┬──────────────────────┘
               │ HTTP POST
               │ JSON Body
               ▼
┌─────────────────────────────────────┐
│   TU APLICACION SPRING BOOT         │
│   ┌───────────────────────────────┐ │
│   │ PaymentController             │ │
│   │ POST /api/v1/payments/process │ │
│   │ processPayment(RequestDTO)    │ │
│   └────────────┬──────────────────┘ │
│                │                    │
│                ▼                    │
│   ┌───────────────────────────────┐ │
│   │ PaymentServiceImpl             │ │
│   │ processPayment(RequestDTO)    │ │
│   │  - Call Stripe               │ │
│   │  - Save to DB                │ │
│   │  - Build Response            │ │
│   └────────────┬──────────────────┘ │
│                │                    │
│                ▼                    │
│   ┌───────────────────────────────┐ │
│   │ PaymentMapper                 │ │
│   │ toResponseDTO(payment)        │ │
│   └────────────┬──────────────────┘ │
└──────────────┬─────────────────────┘
               │ HTTP 200/400
               │ JSON Response
               ▼
┌─────────────────────────────────────┐
│    CLIENTE (React)                  │
│    Recibe: {                        │
│      paymentId: 1,                  │
│      orderId: 123,                  │
│      status: "SUCCEEDED",           │
│      stripePaymentIntentId: "...",  │
│      message: "...",                │
│      processedAt: "..."             │
│    }                                │
└─────────────────────────────────────┘
```

---

## 3️⃣ TABLA COMPARATIVA DETALLADA

| Aspecto | Payment Service (Original) | Tu Proyecto |
|--------|---------------------------|------------|
| **Trigger** | Evento Kafka | HTTP POST |
| **Entrada** | `PaymentRequestedEvent` | `PaymentRequestDTO` |
| **Listener** | `@KafkaListener` | `@PostMapping` |
| **Usuario** | Servicio de Order Server | Cliente (React) |
| **Respuesta** | Evento Kafka en otro tópico | HTTP Response 200/400 |
| **Formato respuesta** | `PaymentResultEvent` (Kafka) | `PaymentResponseDTO` (JSON) |
| **Acoplamiento** | Desacoplado (async/event-driven) | Acoplado REST (sync) |
| **Latencia** | Media (async) | Baja (sync/HTTP) |
| **Persistencia** | Sí (BD) | Sí (BD) |
| **Transacciones** | @Transactional | @Transactional |
| **Logging** | Sí (@Slf4j) | Sí (@Slf4j) |

---

## 4️⃣ DIFERENCIAS CLAVE EN CÓDIGO

### A) PaymentService Interface

#### ANTES (Original):
```java
public interface PaymentService {
    void processPayment(PaymentRequestedEvent event);
}
```

#### DESPUÉS (Tu proyecto):
```java
public interface PaymentService {
    PaymentResponseDTO processPayment(PaymentRequestDTO request);
    PaymentResponseDTO getPaymentStatus(Long paymentId);
    Object getPaymentsByOrderId(Long orderId);
}
```

---

### B) PaymentServiceImpl

#### ANTES (Original):
```java
@Override
@Transactional
public void processPayment(PaymentRequestedEvent event) {
    try {
        StripePaymentResult result = stripeService.charge(
            event.amount(),
            "EUR",
            event.stripeToken()
        );
        
        Payment payment = new Payment(...);
        paymentRepository.save(payment);
        
        // ❌ Envía a Kafka
        resultProducer.sendSucceeded(event.orderId(), event.paymentId());
        
    } catch (Exception e) {
        paymentRepository.save(...);
        // ❌ Envía a Kafka
        resultProducer.sendFailed(event.orderId(), event.paymentId(), e.getMessage());
    }
}
```

#### DESPUÉS (Tu proyecto):
```java
@Override
@Transactional
public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
    try {
        StripePaymentResult result = stripeService.charge(
            request.getAmount(),
            request.getCurrency(),
            request.getStripeToken()
        );
        
        Payment payment = new Payment(...);
        payment = paymentRepository.save(payment);
        
        // ✅ Retorna DTO directamente
        PaymentResponseDTO response = paymentMapper.toResponseDTO(payment);
        return response;
        
    } catch (Exception e) {
        Payment failedPayment = new Payment(...);
        failedPayment = paymentRepository.save(failedPayment);
        
        // ✅ Retorna DTO con error
        return paymentMapper.toResponseDTOWithError(failedPayment, e.getMessage());
    }
}
```

---

### C) Controller HTTP (NUEVO)

```java
@PostMapping("/process")
public ResponseEntity<PaymentResponseDTO> processPayment(
        @Valid @RequestBody PaymentRequestDTO request) {
    
    try {
        PaymentResponseDTO response = paymentService.processPayment(request);
        
        HttpStatus status = response.getStatus().name().equals("SUCCEEDED") 
            ? HttpStatus.OK 
            : HttpStatus.PAYMENT_REQUIRED;
        
        return new ResponseEntity<>(response, status);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
```

---

## 5️⃣ ESTRUCTURA DE PAYLOADS

### Entrada (HTTP POST Body)

```json
{
  "orderId": 123,
  "amount": 99.99,
  "stripeToken": "pm_card_visa",
  "currency": "EUR"
}
```

### Salida Exitosa (HTTP 200)

```json
{
  "paymentId": 5,
  "orderId": 123,
  "status": "SUCCEEDED",
  "stripePaymentIntentId": "pi_1234567890abcdef",
  "errorMessage": null,
  "processedAt": "2024-05-27T10:30:45.123456",
  "message": "Pago procesado exitosamente"
}
```

### Salida Fallida (HTTP 402)

```json
{
  "paymentId": 6,
  "orderId": 123,
  "status": "FAILED",
  "stripePaymentIntentId": null,
  "errorMessage": "Your card has insufficient funds",
  "processedAt": "2024-05-27T10:31:15.654321",
  "message": "El pago no se pudo procesar: Your card has insufficient funds"
}
```

### Error de Validación (HTTP 400)

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Error de validación en la solicitud",
  "details": "The field 'amount' is required and must be positive",
  "timestamp": "2024-05-27T10:32:00.000000",
  "path": "/api/v1/payments/process"
}
```

---

## 6️⃣ INTEGRACIÓN CON REACT (Preview para Fase 2)

### Headers HTTP obligatorios

```javascript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer ' + authToken // Si tienes seguridad
};
```

### Ejemplo de llamada desde React

```javascript
// En un componente React
const processPayment = async (orderId, amount, stripeToken) => {
  try {
    const response = await fetch('/api/v1/payments/process', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        orderId,
        amount,
        stripeToken,
        currency: 'EUR'
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    
    if (data.status === 'SUCCEEDED') {
      console.log('✅ Pago exitoso:', data);
      // Redirigir a página de confirmación
      window.location.href = '/order/confirmation?orderId=' + data.orderId;
    } else {
      console.log('❌ Pago fallido:', data.errorMessage);
      // Mostrar error al usuario
      setError(data.errorMessage);
    }
  } catch (error) {
    console.error('Error:', error);
    setError('Error al procesar pago');
  }
};
```

---

## 7️⃣ DIFERENCIAS EN TESTING

### ANTES (con Kafka)

```java
@Test
void testProcessPaymentSuccess() throws StripeException {
    PaymentRequestedEvent event = new PaymentRequestedEvent(...);
    
    when(stripePaymentService.charge(...)).thenReturn(stripeResult);
    
    paymentService.processPayment(event);
    
    verify(resultProducer).sendSucceeded(...);  // ← Verifica envío a Kafka
    verify(paymentRepository).save(any(Payment.class));
}
```

### DESPUÉS (HTTP Response)

```java
@Test
void testProcessPaymentSuccess() throws StripeException {
    PaymentRequestDTO request = PaymentRequestDTO.builder()
        .orderId(1L)
        .amount(BigDecimal.valueOf(99.99))
        .stripeToken("pm_card_visa")
        .currency("EUR")
        .build();
    
    when(stripePaymentService.charge(...)).thenReturn(stripeResult);
    
    PaymentResponseDTO response = paymentService.processPayment(request);
    
    // ✅ Verifica respuesta directa (sin Kafka)
    assertEquals(PaymentStatus.SUCCEEDED, response.getStatus());
    assertEquals(1L, response.getOrderId());
    assertNotNull(response.getMessage());
}
```

---

## 8️⃣ MIGRATION CHECKLIST

- [ ] **Actualizar BaseEntity** (auditoría)
- [ ] **Actualizar Payment** (entidad JPA)
- [ ] **Crear PaymentStatus** (enum)
- [ ] **Crear StripePaymentResult** (DTO)
- [ ] **Crear StripePaymentService** (interface)
- [ ] **Crear StripePaymentServiceImpl** (implementation)
- [ ] **Crear PaymentRepository** (data access)
- [ ] **Crear PaymentService** (interface - adaptada)
- [ ] **Crear PaymentServiceImpl** (implementation - sin Kafka)
- [ ] **Crear PaymentRequestDTO** (nuevo)
- [ ] **Crear PaymentResponseDTO** (nuevo)
- [ ] **Crear PaymentMapper** (nuevo)
- [ ] **Crear PaymentController** (nuevo REST endpoint)
- [ ] **Crear StripeConfig** (configuración)
- [ ] **Crear JpaAuditingConfig** (configuración)
- [ ] **Crear V1__init_payments.sql** (migración Flyway)
- [ ] **Actualizar pom.xml** (dependencias)
- [ ] **Actualizar application.properties** (config Stripe)
- [ ] **Escribir tests unitarios**
- [ ] **Realizar prueba manual con Stripe**

---

## 9️⃣ NOTAS IMPORTANTES

### Security Considerations
- 🔐 El `stripeToken` debe generarse desde el CLIENTE (React) con Stripe.js
- 🔐 NUNCA guardes tokens completos en BD, solo Intent ID
- 🔐 Usa HTTPS en producción
- 🔐 Implementa CORS si React está en dominio diferente

### Database Considerations
- 💾 La tabla `payments` almacena histórico completo
- 💾 Indices en `order_id` y `status` para queries rápidas
- 💾 `created_at` se genera automáticamente (auditoría)
- 💾 `updated_at` se actualiza automáticamente

### Performance Considerations
- ⚡ Las llamadas a Stripe pueden tardar 1-3 segundos
- ⚡ Usa timeout en cliente React (ej: 30 segundos)
- ⚡ Considera rollback de orden si pago falla
- ⚡ Guarda información de error para debugging

### Error Handling
- 🚨 Captura `StripeException` específicamente
- 🚨 Log detallado de errores para auditoría
- 🚨 Mensajes amigables al usuario (no técnicos)
- 🚨 Implementa reintentos si es necesario

---

## 🔟 CONTACTO CON TU ARQUITECTURA EXISTENTE

### Si tu app tiene AuthService:
```java
@PostMapping("/process")
public ResponseEntity<PaymentResponseDTO> processPayment(
        @Valid @RequestBody PaymentRequestDTO request,
        @AuthenticationPrincipal UserDetails userDetails) {  // ← Agregar si tienes Spring Security
    
    Long userId = authService.getUserIdFromDetails(userDetails);
    request.setUserId(userId);  // ← Si necesitas asociar usuario
    
    // ... resto del código
}
```

### Si tu app tiene OrderService:
```java
@Override
public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
    // Validar que la orden existe
    Order order = orderService.getOrderById(request.getOrderId());
    if (order == null) {
        throw new OrderNotFoundException("Orden no encontrada");
    }
    
    // Validar que el monto coincide
    if (order.getTotalAmount().compareTo(request.getAmount()) != 0) {
        throw new PaymentAmountMismatchException("Montos no coinciden");
    }
    
    // ... procesar pago
}
```

### Si tu app tiene NotificationService:
```java
@Override
public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
    // ... procesar pago
    
    if (response.getStatus() == PaymentStatus.SUCCEEDED) {
        notificationService.sendOrderConfirmation(request.getOrderId());
    } else {
        notificationService.sendPaymentFailedNotification(
            request.getOrderId(),
            response.getErrorMessage()
        );
    }
    
    return response;
}
```

---


