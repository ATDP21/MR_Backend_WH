# Configuración de Webhooks - Stripe

## 📡 ¿Qué son los Webhooks?

Los webhooks son notificaciones que Stripe envía a tu servidor cuando ocurren eventos en los pagos. Permiten que tu backend sepa cuándo:
- ✅ Un pago se completa
- ❌ Un pago falla
- ↩️ Un pago es reembolsado
- 🔄 Un pago es disputado

---

## 🔧 Configuración en Dashboard de Stripe

### Paso 1: Ir al Panel de Webhooks

1. Accede a https://dashboard.stripe.com
2. Ve a **Developers** → **Webhooks**
3. Haz clic en **Add endpoint**

### Paso 2: Configurar el Endpoint

**URL del Endpoint:**
```
https://tudominio.com/api/webhook/stripe
```

⚠️ **En desarrollo:**
```
http://localhost:8080/api/webhook/stripe
```

### Paso 3: Seleccionar Eventos

Haz clic en **Select events** y marca:

**Eventos Recomendados:**
- ✅ `checkout.session.completed` - Pago completado
- ✅ `checkout.session.async_payment_succeeded` - Pago asincrónico exitoso
- ✅ `charge.refunded` - Reembolso procesado
- ✅ `charge.dispute.created` - Disputa iniciada
- ✅ `charge.failed` - Cobro fallido

### Paso 4: Obtener el Signing Secret

Después de crear el endpoint:
1. Haz clic en el endpoint que creaste
2. Busca **Signing secret**
3. Copia el valor (comienza con `whsec_`)
4. Actualiza en `application.properties`:

```properties
stripe.webhook.secret=whsec_test_xxxxxxxxxxxxx
```

---

## 🧪 Testear Webhooks en Local

### Opción 1: Usar Stripe CLI (Recomendado)

#### 1. Instalar Stripe CLI
```bash
# Windows (usando Chocolatey)
choco install stripe-cli

# macOS
brew install stripe-cli

# Linux
curl https://files.stripe.com/stripe-cli/install.sh -o install.sh && sudo bash install.sh
```

#### 2. Autenticarse
```bash
stripe login
```
Te abrirá el navegador para que inicies sesión con tu cuenta de Stripe.

#### 3. Escuchar webhooks locales
```bash
stripe listen --forward-to localhost:8080/api/webhook/stripe
```

Esto te dará un **signing secret**, cópialo y actualiza:
```properties
stripe.webhook.secret=whsec_test_xxxxx  # El que te da stripe listen
```

#### 4. Simular eventos
En otra terminal:
```bash
# Simular pago completado
stripe trigger checkout.session.completed

# Simular pago rechazado
stripe trigger checkout.session.async_payment_failed

# Simular reembolso
stripe trigger charge.refunded
```

---

### Opción 2: Usar Postman

#### Simular manualmente un webhook:

**POST** `http://localhost:8080/api/webhook/stripe`

**Headers:**
```
Content-Type: application/json
Stripe-Signature: t=timestamp,v1=signature
```

**Body (ejemplo de pago completado):**
```json
{
  "id": "evt_test",
  "object": "event",
  "api_version": "2023-03-02",
  "created": 1649852864,
  "data": {
    "object": {
      "id": "cs_test_a1b2c3d4e5f6g7h8i9j0",
      "object": "checkout.session",
      "billing_address_collection": null,
      "cancel_url": "http://localhost:3000/checkout/cancelled",
      "client_secret": "cs_test_secret",
      "customer": null,
      "customer_creation": "if_required",
      "customer_email": "cliente@example.com",
      "expires_at": 1649939264,
      "mode": "payment",
      "payment_intent": "pi_test_1234567890",
      "payment_method_collection": "if_required",
      "payment_status": "paid",
      "recovery": null,
      "status": "complete",
      "success_url": "http://localhost:3000/checkout/success?session_id=cs_test_a1b2c3d4e5f6g7h8i9j0",
      "url": null,
      "metadata": {
        "email": "cliente@example.com",
        "nombreCliente": "María García",
        "direccionEntrega": "1",
        "direccionFacturacion": "2"
      }
    }
  },
  "livemode": false,
  "pending_webhooks": 1,
  "type": "checkout.session.completed"
}
```

⚠️ **Problema:** Sin firma válida, el webhook será rechazado. Para testear sin validación, coméntalo temporalmente:

En `StripeWebhookController.java`:
```java
// Comentar temporalmente para pruebas
// Event event = Webhook.constructEvent(payload, signature, webhookSecret);

// Usar en su lugar
Event event = new Event();
// ... configurar manualmente el evento ...
```

---

## 📝 Registros de Webhooks

### Ver en Dashboard de Stripe

1. Ir a **Developers** → **Webhooks**
2. Haz clic en tu endpoint
3. Verás todos los eventos enviados:
   - ✅ Exitosos (green checkmark)
   - ⚠️ Con errores (red X)

### Logs en tu Servidor

Los logs se guardan en el archivo de Spring Boot:

```java
// En StripeWebhookController
log.info("Sesión de pago completada: {} - Cliente: {}", 
    session.getId(), 
    session.getCustomerEmail());
```

Ver logs:
```bash
tail -f logs/application.log | grep -i webhook
```

---

## 🔐 Validar Firma de Webhook

El webhook incluye una firma para validar que viene de Stripe:

```
Stripe-Signature: t=1649852864,v1=abcdef123456...
```

**Validación automática en Spring:**
```java
Event event = Webhook.constructEvent(payload, signature, webhookSecret);
// Si la firma es inválida, lanza SignatureVerificationException
```

---

## ✅ Respuestas Correctas del Webhook

### Éxito
```
HTTP 200 OK
Content: "Webhook recibido correctamente"
```

### Error
```
HTTP 400 Bad Request
Content: "Error procesando webhook"

HTTP 403 Forbidden
Content: "Firma inválida"
```

---

## 🚨 Problemas Comunes

### 1. "Firma inválida"
**Causa:** El `stripe.webhook.secret` es incorrecto

**Solución:**
```properties
# Verificar que sea del formato correcto
stripe.webhook.secret=whsec_test_xxxxx  # Empieza con whsec_live_ o whsec_test_

# En desarrollo: usa lo que te da stripe listen
stripe listen --forward-to localhost:8080/api/webhook/stripe
```

### 2. Webhook no recibe eventos
**Causa:** El endpoint no es accesible

**Solución:**
- En desarrollo: usar `stripe listen`
- En producción: verificar que la URL sea pública y HTTPS
- Verificar que el endpoint no requiera autenticación

### 3. El webhook se ejecuta pero no crea pedido
**Causa:** La lógica no está implementada

**Solución:** 
Descomentar en `StripeWebhookController`:
```java
// TODO: Crear o actualizar el pedido en la base de datos
pedidoService.crearPedidoDesdeWebhook(session);
```

---

## 📊 Flujo de Webhook en Detalle

```
1. Cliente completa pago en Stripe
   ↓
2. Stripe crea evento: checkout.session.completed
   ↓
3. Stripe envía POST a: /api/webhook/stripe
   - Headers incluyen firma (Stripe-Signature)
   - Body contiene detalles de la sesión
   ↓
4. Tu servidor recibe webhook:
   - Valida firma
   - Verifica que sea un evento conocido
   - Procesa la acción (crear pedido, enviar email, etc.)
   - Retorna 200 OK
   ↓
5. Stripe marca evento como exitoso ✅
   Si no retorna 200, reintentará varias veces
```

---

## 🔄 Reintentos Automáticos de Stripe

Si tu webhook retorna error (no 2xx), Stripe reintentará:
- Después de 5 segundos
- Después de 5 minutos  
- Después de 30 minutos
- Después de 2 horas
- Después de 5 horas
- Después de 10 horas

Máximo 5 reintentos en 24 horas.

---

## 💾 Guardar Datos de Sesión

Para relacionar la sesión de Stripe con tu pedido, guarda los datos:

### Opción 1: En Metadatos (Recomendado)
```java
// Al crear la sesión
paramsBuilder.putMetadata("email", request.getEmail());
paramsBuilder.putMetadata("userId", request.getUserId());
paramsBuilder.putMetadata("cartId", request.getCartId());

// En el webhook
Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
String userId = session.getMetadata().get("userId");
```

### Opción 2: En Base de Datos
```java
// Al crear sesión, guardar:
// - sessionId de Stripe
// - userId
// - cartItems
// - estado = "pending"

// En webhook, cuando se completa:
// - Buscar por sessionId
// - Cambiar estado a "paid"
// - Crear pedido
```

---

## 🎯 Checklist de Implementación

- [ ] Stripe API key en `application.properties`
- [ ] Webhook secret en `application.properties`
- [ ] Endpoint webhook configurado en dashboard de Stripe
- [ ] `StripeWebhookController` implementado
- [ ] Validación de firma en webhook
- [ ] Logging de eventos
- [ ] Lógica de creación de pedido
- [ ] Testear con Stripe CLI
- [ ] Testear pago completo
- [ ] Verificar en dashboard de Stripe
- [ ] Implementar reinventos
- [ ] Email de confirmación
- [ ] Manejo de reembolsos

---

## 📚 Recursos Adicionales

- Documentación de Webhooks: https://stripe.com/docs/webhooks
- Stripe CLI: https://stripe.com/docs/stripe-cli
- Event Types: https://stripe.com/docs/api/events/types
- Testing: https://stripe.com/docs/testing

