# Guía de Integración Stripe - MR Backend

## 📋 Descripción General

Se ha implementado la integración completa de Stripe para procesar pagos de guitarras en tu carrito. El flujo es:

1. **Cliente selecciona items y direcciones** → Enviados al backend
2. **Backend crea sesión de pago** → Stripe retorna URL de sesión
3. **Cliente paga en Stripe** → Confirma pago
4. **Webhook de Stripe** → Notifica al backend
5. **Backend crea el pedido** → Sistema registra la orden

---

## 🔧 Configuración Requerida

### 1. Variables de Entorno (application.properties)

Ya están configuradas pero debes actualizar con tus credenciales reales:

```properties
stripe.api.key=sk_live_TUCLAVEREAL
stripe.webhook.secret=whsec_TUSECRETODEWEBHOOK
stripe.front-url=https://tudominio.com
```

**IMPORTANTE:** 
- `stripe.api.key`: Obtén tu clave en https://dashboard.stripe.com/apikeys
- `stripe.webhook.secret`: Configura webhooks en https://dashboard.stripe.com/webhooks
- `stripe.front-url`: URL de tu aplicación frontend

---

## 📡 Endpoints Disponibles

### 1. Crear Sesión de Pago
**POST** `/api/checkout/create-session`

**Headers requeridos:**
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
X-XSRF-TOKEN: {CSRF_TOKEN}
```

**Request Body:**
```json
{
  "cartItems": [
    {
      "guitarraid": 1,
      "cantidad": 2
    },
    {
      "guitarraid": 3,
      "cantidad": 1
    }
  ],
  "direccionEntregaId": 1,
  "direccionFacturacionId": 2,
  "email": "cliente@example.com",
  "nombreCliente": "Juan Pérez"
}
```

**Response (200 OK):**
```json
{
  "sessionId": "cs_test_a1b2c3d4e5f6g7h8i9j0",
  "sessionUrl": "https://checkout.stripe.com/pay/cs_test_a1b2c3d4e5f6g7h8i9j0",
  "clientSecret": "pi_test_secret_xyz",
  "status": "open"
}
```

**Uso en Frontend:**
```javascript
// Después de obtener sessionUrl del backend
window.location.href = response.sessionUrl;
```

---

### 2. Obtener Estado de Sesión
**GET** `/api/checkout/session-status?sessionId=cs_test_xxxxx`

**Response:**
```json
{
  "sessionId": "cs_test_a1b2c3d4e5f6g7h8i9j0",
  "status": "complete",
  "clientSecret": "pi_test_secret_xyz"
}
```

---

### 3. Webhook de Stripe
**POST** `/api/webhook/stripe`

Este endpoint recibe notificaciones de Stripe cuando:
- ✅ Pago completado
- ❌ Pago cancelado
- ↩️ Pago reembolsado

**No requiere autenticación** (Stripe verifica la firma del webhook)

---

## 🔄 Flujo Completo de Pago

### Frontend (React/Frontend)
```javascript
// 1. Preparar datos del carrito
const checkoutData = {
  cartItems: carrito.items,
  direccionEntregaId: 1,
  direccionFacturacionId: 2,
  email: usuarioLogeado.email,
  nombreCliente: usuarioLogeado.nombre
};

// 2. Crear sesión de pago
const response = await fetch(`${API_BASE_URL}/api/checkout/create-session`, {
  method: 'POST',
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json',
    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
  },
  body: JSON.stringify(checkoutData)
});

const { sessionUrl } = await response.json();

// 3. Redirigir a Stripe
window.location.href = sessionUrl;

// 4. Después del pago, Stripe redirige a:
// - Success: https://tudominio.com/checkout/success?session_id=cs_test_xxxxx
// - Cancelled: https://tudominio.com/checkout/cancelled
```

---

## 🔐 Seguridad

### CSRF Token
El endpoint requiere el token XSRF-TOKEN en el header (ya lo estás usando):
```javascript
headers: {
  'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
}
```

### Autenticación
Solo usuarios autenticados pueden crear sesiones de pago:
```java
@PreAuthorize("isAuthenticated()")
```

### Verificación de Webhook
El webhook verifica la firma criptográfica de Stripe para evitar falsificaciones.

---

## 💾 Crear Pedido Después del Pago

Cuando el webhook confirma el pago, debes crear el pedido. Puedes hacerlo de dos formas:

### Opción 1: En el Webhook (Recomendado)
Modifica `StripeWebhookController.handleCheckoutSessionCompleted()`:

```java
private void handleCheckoutSessionCompleted(Event event) {
    try {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
        
        String email = session.getMetadata().get("email");
        String direccionEntrega = session.getMetadata().get("direccionEntrega");
        String direccionFacturacion = session.getMetadata().get("direccionFacturacion");
        
        // Crear el pedido con la información almacenada
        PedidoCrearRequestDTO pedidoRequest = new PedidoCrearRequestDTO();
        pedidoRequest.setDireccionEntregaId(Integer.parseInt(direccionEntrega));
        pedidoRequest.setDireccionFacturacionId(Integer.parseInt(direccionFacturacion));
        
        // Obtener los items del carrito (necesitarás guardarlos en la sesión)
        pedidoService.crearPedido(pedidoRequest);
        
    } catch (Exception e) {
        log.error("Error al procesar pago: {}", e.getMessage(), e);
    }
}
```

### Opción 2: En el Frontend (Después de redireccionamiento)
```javascript
// En la página de success
const sessionId = new URLSearchParams(location.search).get('session_id');

// Crear pedido con la sesión confirmada
const response = await fetch(`${API_BASE_URL}/pedido/crear`, {
  method: 'POST',
  credentials: 'include',
  header: {
    'Content-Type': 'application/json',
    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')
  },
  body: JSON.stringify({
    stripeSessionId: sessionId,
    direccionEntregaId: 1,
    direccionFacturacionId: 2,
    stockPedidos: carrito.items
  })
});
```

---

## 🧪 Pruebas en Modo Test

### Tarjetas de Prueba Stripe

| Situación | Tarjeta | CVC | Fecha |
|-----------|---------|-----|--------|
| Pago exitoso | 4242 4242 4242 4242 | 123 | 12/25 |
| Pago rechazado | 4000 0000 0000 0002 | 123 | 12/25 |
| Autenticación 3D | 4000 0025 0000 3155 | 123 | 12/25 |

---

## ❌ Errores Comunes

### 1. `Firma de Webhook inválida`
- Verifica que `stripe.webhook.secret` sea el correcto
- En desarrollo, usa `whsec_test_...`
- En producción, usa `whsec_live_...`

### 2. `Guitarra no encontrada con ID: X`
- Verifica que el ID de guitarra exista en la BD
- Comprueba que la guitarra tenga precio definido

### 3. `CSRF token missing`
- Asegúrate de enviar el header `X-XSRF-TOKEN`
- Obtén el token con: `document.querySelector('meta[name="_csrf"]').content`

### 4. `No autenticado`
- Verifica que el JWT token sea válido
- Comprueba la cookie de autenticación

---

## 📊 Monitorar Pagos

```bash
# Ver transacciones en dashbord de Stripe
https://dashboard.stripe.com/payments

# Ver eventos de webhook
https://dashboard.stripe.com/webhooks
```

---

## 📝 Próximos Pasos

1. ✅ **Implementar almacenamiento de sesión**: Guardar los datos del carrito asociados al sessionId
2. ✅ **Crear pedido automáticamente**: Confirmar pago y crear orden
3. ✅ **Email de confirmación**: Enviar email después de confirmar pago
4. ✅ **Reembolsos**: Implementar lógica de devoluciones
5. ✅ **Historial de pagos**: Mostrar transacciones pasadas al usuario

---

## 🆘 Soporte

- Documentación Stripe: https://stripe.com/docs
- Dashboard: https://dashboard.stripe.com
- API Reference: https://stripe.com/docs/api

