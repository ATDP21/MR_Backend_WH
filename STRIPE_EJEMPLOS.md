# Ejemplos de Uso - Stripe Integration

## 📚 Ejemplos en Postman

### 1. Obtener CSRF Token y Auth Token
```
GET http://localhost:8080/usuario/loggeado
```
- Guarda la cookie `XSRF-TOKEN`
- Guarda la cookie `X-Auth-Token`

---

### 2. Crear Sesión de Pago

**URL:** `POST http://localhost:8080/api/checkout/create-session`

**Headers:**
```
Content-Type: application/json
X-XSRF-TOKEN: {{XSRF-TOKEN}}
Cookie: X-Auth-Token={{AUTH-TOKEN}}; XSRF-TOKEN={{XSRF-TOKEN}}
```

**Body (JSON):**
```json
{
  "cartItems": [
    {
      "guitarraid": 2,
      "cantidad": 1
    },
    {
      "guitarraid": 3,
      "cantidad": 2
    }
  ],
  "direccionEntregaId": 1,
  "direccionFacturacionId": 2,
  "email": "cliente@example.com",
  "nombreCliente": "María García López"
}
```

**Response (copiar sessionUrl):**
```json
{
  "sessionId": "cs_test_a1b2c3d4e5f6...",
  "sessionUrl": "https://checkout.stripe.com/pay/cs_test_...",
  "clientSecret": "pi_test_...",
  "status": "open"
}
```

---

### 3. Verificar Estado de Sesión

**URL:** `GET http://localhost:8080/api/checkout/session-status?sessionId=cs_test_a1b2c3d4...`

**Headers:**
```
X-XSRF-TOKEN: {{XSRF-TOKEN}}
Cookie: X-Auth-Token={{AUTH-TOKEN}}
```

**Response:**
```json
{
  "sessionId": "cs_test_a1b2c3d4...",
  "status": "complete",
  "clientSecret": "pi_test_..."
}
```

---

## 🎯 Flujo en Frontend (React)

### 1. Componente de Checkout

```typescript
// CheckoutPage.tsx
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { config } from '../config/enviroment';

interface CartItem {
  guitarraid: number;
  cantidad: number;
}

export const CheckoutPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCheckout = async () => {
    setLoading(true);
    setError(null);

    try {
      // 1. Obtener datos del carrito y direcciones
      const cartItems: CartItem[] = [
        { guitarraid: 2, cantidad: 1 },
        { guitarraid: 3, cantidad: 2 }
      ];

      const checkoutData = {
        cartItems,
        direccionEntregaId: 1,
        direccionFacturacionId: 2,
        email: 'cliente@example.com',
        nombreCliente: 'María García'
      };

      // 2. Crear sesión de pago
      const response = await fetch(
        `${config.url.API_BASE_URL}/api/checkout/create-session`,
        {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''
          },
          body: JSON.stringify(checkoutData)
        }
      );

      if (!response.ok) {
        throw new Error('Error al crear sesión de pago');
      }

      const data = await response.json();

      // 3. Redirigir a Stripe Checkout
      if (data.sessionUrl) {
        window.location.href = data.sessionUrl;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="checkout-container">
      <h1>Resumen del Pedido</h1>
      {error && <div className="error">{error}</div>}
      <button 
        onClick={handleCheckout} 
        disabled={loading}
        className="btn-checkout"
      >
        {loading ? 'Procesando...' : 'Ir a Pagar'}
      </button>
    </div>
  );
};

function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}
```

---

### 2. Página de Éxito

```typescript
// CheckoutSuccessPage.tsx
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { config } from '../config/enviroment';

export const CheckoutSuccessPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [pedido, setPedido] = useState(null);

  useEffect(() => {
    const verificarPago = async () => {
      const sessionId = searchParams.get('session_id');

      if (!sessionId) {
        navigate('/checkout/cancelled');
        return;
      }

      try {
        // 1. Verificar estado de sesión
        const response = await fetch(
          `${config.url.API_BASE_URL}/api/checkout/session-status?sessionId=${sessionId}`,
          {
            credentials: 'include',
            headers: {
              'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''
            }
          }
        );

        const data = await response.json();

        if (data.status === 'complete') {
          // 2. Crear pedido en la base de datos
          const pedidoResponse = await fetch(
            `${config.url.API_BASE_URL}/pedido/crear`,
            {
              method: 'POST',
              credentials: 'include',
              headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''
              },
              body: JSON.stringify({
                stripeSessionId: sessionId,
                direccionEntregaId: 1,
                direccionFacturacionId: 2,
                stockPedidos: [] // Deberías tener esto del carrito
              })
            }
          );

          if (pedidoResponse.ok) {
            const pedidoData = await pedidoResponse.json();
            setPedido(pedidoData);
          }
        } else {
          navigate('/checkout/cancelled');
        }
      } catch (err) {
        console.error('Error:', err);
        navigate('/checkout/cancelled');
      } finally {
        setLoading(false);
      }
    };

    verificarPago();
  }, [searchParams, navigate]);

  if (loading) return <div>Verificando pago...</div>;

  if (!pedido) return <div>Error al procesar el pedido</div>;

  return (
    <div className="success-container">
      <h1>✅ ¡Pago Exitoso!</h1>
      <p>Tu pedido ha sido creado correctamente.</p>
      <div className="pedido-info">
        <p><strong>Número de Pedido:</strong> {pedido.id}</p>
        <p><strong>Total:</strong> €{pedido.total}</p>
        <p><strong>Estado:</strong> {pedido.estado}</p>
      </div>
      <button onClick={() => navigate('/')}>Volver al inicio</button>
    </div>
  );
};

function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}
```

---

### 3. Página de Cancelación

```typescript
// CheckoutCancelledPage.tsx
import { useNavigate } from 'react-router-dom';

export const CheckoutCancelledPage = () => {
  const navigate = useNavigate();

  return (
    <div className="cancelled-container">
      <h1>❌ Pago Cancelado</h1>
      <p>Tu pago ha sido cancelado. Puedes intentar de nuevo.</p>
      <button onClick={() => navigate('/cart')}>Volver al Carrito</button>
      <button onClick={() => navigate('/')}>Ir al Inicio</button>
    </div>
  );
};
```

---

## 🔧 Helper: Función para Hacer Checkout

```typescript
// utils/stripeCheckout.ts
import { config } from '../config/enviroment';

interface CheckoutParams {
  cartItems: Array<{ guitarraid: number; cantidad: number }>;
  direccionEntregaId: number;
  direccionFacturacionId: number;
  email: string;
  nombreCliente: string;
}

export const iniciarCheckoutStripe = async (params: CheckoutParams) => {
  try {
    const response = await fetch(
      `${config.url.API_BASE_URL}/api/checkout/create-session`,
      {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest',
          'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''
        },
        body: JSON.stringify(params)
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Error al crear sesión de pago');
    }

    const data = await response.json();
    
    if (!data.sessionUrl) {
      throw new Error('No se recibió URL de sesión de Stripe');
    }

    // Redirigir a Stripe
    window.location.href = data.sessionUrl;
    
  } catch (error) {
    console.error('Error en checkout:', error);
    throw error;
  }
};

function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}
```

---

## 🚀 Flujo Completo Paso a Paso

### Backend (Spring Boot)

1. **Cliente realiza POST a `/api/checkout/create-session`**
   - Se verifica autenticación (@PreAuthorize)
   - Se validan los datos (IDs de dirección, items disponibles)
   
2. **VentaService procesa la solicitud**
   - Obtiene datos de guitarras de BD
   - Calcula total
   - Crea sesión de Stripe
   - Retorna sessionUrl
   
3. **WebhookController recibe evento de Stripe**
   - Verifica firma (seguridad)
   - Cuando status = "complete"
   - Crea pedido en la BD
   
4. **Cliente recibe notificación de éxito**
   - Redireccionado a página de confirmación
   - Ve detalles del pedido

---

## 📊 Tabla de Estados de Sesión

| Status | Significado | Acción |
|--------|-------------|--------|
| `open` | Sesión creada, esperando pago | Redirigir a Stripe |
| `complete` | Pago confirmado | Crear pedido |
| `expired` | Sesión expirada | Crear nueva sesión |

---

## 🔐 Seguridad - Lista de Verificación

- ✅ Verificar XSRF-TOKEN en cada POST
- ✅ Usar credentials='include' para cookies
- ✅ Validar email del usuario
- ✅ Verificar disponibilidad de guitarras
- ✅ Validar direcciones pertenecen al usuario
- ✅ Verificar firmas de webhooks
- ✅ Usar HTTPS en producción

---

## 🐛 Debugging

### Ver logs de Spring
```bash
tail -f target/spring-boot-application.log | grep -i stripe
```

### Verificar en Dashboard de Stripe
- https://dashboard.stripe.com/payments
- https://dashboard.stripe.com/webhooks

### Testear webhook localmente
```bash
# Usar Stripe CLI
stripe listen --forward-to localhost:8080/api/webhook/stripe

# En otra terminal, simular evento
stripe trigger payment_intent.succeeded
```

