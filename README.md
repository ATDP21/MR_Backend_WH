# MR Backend WH

API REST para la tienda web de guitarras MR. Este repositorio contiene el backend encargado de gestionar usuarios, catálogo, imágenes, direcciones, pedidos y pagos en línea.

<img width="1895" height="862" alt="image" src="https://github.com/user-attachments/assets/03af803d-7c04-4e46-bbfc-f251f710aa9d" />


## Tecnologías

- Java 17
- Spring Boot 4.0.6
- Spring Web, Spring Security y Spring Data JPA
- PostgreSQL y Hibernate
- Stripe Java SDK 32.1.0
- JWT y Google OAuth 2.0
- Maven Wrapper

## Funcionalidades

- Registro e inicio de sesión con credenciales, más autenticación mediante Google.
- Autenticación basada en JWT y cookies; protección CSRF mediante cookie XSRF.
- Consulta y gestión del catálogo de guitarras e imágenes.
- Gestión de direcciones de entrega y facturación.
- Creación y consulta de pedidos.
- Creación de sesiones de Stripe, consulta de estado y recepción de webhooks.

## Requisitos

- JDK 17.
- PostgreSQL.
- Credenciales de Google OAuth 2.0 y Stripe para habilitar esas integraciones.

El repositorio incluye Maven Wrapper (`mvnw` y `mvnw.cmd`), por lo que no hace falta instalar Maven por separado.

## Configuración

La configuración principal está en `src/main/resources/application.properties`. Define estas variables de entorno antes de arrancar:

| Variable | Descripción | Valor predeterminado |
| --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/postgres` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | Sin valor predeterminado |
| `JWT_SECRET` | Secreto de firma de tokens JWT | Obligatorio |
| `GOOGLE_CLIENT_ID` | ID de cliente OAuth de Google | Obligatorio |
| `GOOGLE_CLIENT_SECRET` | Secreto de cliente OAuth de Google | Obligatorio |
| `STRIPE_API_KEY` | Clave secreta de Stripe | Obligatorio |
| `STRIPE_WEBHOOK_SECRET` | Secreto para verificar la firma de webhooks | Obligatorio |
| `STRIPE_FRONT_URL` | URL del frontend para el flujo de checkout | `http://localhost:3000` |
| `GUITARRAS_DIR` | Directorio donde se guardan imágenes | `uploads/guitarras` |

Hibernate utiliza el esquema PostgreSQL `manuel_romero` y `ddl-auto=update`. Crea ese esquema en la base de datos antes de iniciar la aplicación.

### Ejemplo en Windows PowerShell

Sustituye los valores de ejemplo por tus credenciales locales. No guardes secretos reales en el repositorio.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "tu-clave-local"
$env:JWT_SECRET = "un-secreto-local-de-al-menos-32-bytes"
$env:GOOGLE_CLIENT_ID = "tu-client-id"
$env:GOOGLE_CLIENT_SECRET = "tu-client-secret"
$env:STRIPE_API_KEY = "sk_test_..."
$env:STRIPE_WEBHOOK_SECRET = "whsec_..."
$env:STRIPE_FRONT_URL = "http://localhost:3000"

.\mvnw.cmd spring-boot:run
```

En Linux o macOS, exporta las mismas variables con `export VARIABLE="valor"` y ejecuta:

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

## Pruebas

En Windows:

```powershell
.\mvnw.cmd test
```

En Linux o macOS:

```bash
./mvnw test
```

## Endpoints

Los endpoints REST están organizados por recurso:

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/auth/registro` | Registrar usuario |
| `POST` | `/auth/login` | Iniciar sesión |
| `POST` | `/auth/logout` | Cerrar sesión |
| `GET` | `/auth/credencialDisponible?usuario=...` | Comprobar disponibilidad de una credencial |
| `GET` | `/usuario/loggeado` | Consultar el perfil autenticado |
| `GET` | `/guitarra/verStock` | Consultar guitarras en stock |
| `GET` | `/guitarra/ver/{id}` | Consultar una guitarra por ID |
| `GET` | `/guitarra/verCarrito` | Consultar guitarras por IDs enviados en el body |
| `POST` | `/guitarra/crear` | Crear guitarra |
| `GET` | `/guitarra/{id}/imagenes` | Consultar imágenes de una guitarra |
| `POST` | `/guitarra/{id}/imagenes` | Subir imágenes (`multipart/form-data`, campo `files`) |
| `PUT` | `/guitarra/{guitarraId}/imagenes/{imagenId}/principal` | Marcar una imagen como principal |
| `DELETE` | `/guitarra/{guitarraId}/imagenes/{imagenId}` | Eliminar una imagen |
| `POST` | `/direccion/crear` | Crear dirección |
| `GET` | `/direccion/ver` | Consultar direcciones propias |
| `PUT` | `/direccion/editar/{id}` | Editar dirección |
| `DELETE` | `/direccion/eliminar/{id}` | Eliminar dirección |
| `POST` | `/pedido/crear` | Crear pedido |
| `GET` | `/pedido/mios` | Consultar pedidos propios |
| `GET` | `/pedido/{id}` | Consultar un pedido |
| `GET` | `/pedido/todos` | Consultar todos los pedidos (requiere autoridad `ADMIN`) |
| `POST` | `/api/checkout/create-session` | Crear sesión de pago (requiere autenticación) |
| `GET` | `/api/checkout/session-status?sessionId=...` | Consultar sesión de pago (requiere autenticación) |
| `POST` | `/api/checkout/confirm-payment` | Confirmar el pago y procesar el pedido |
| `POST` | `/api/webhook/stripe` | Recibir eventos firmados de Stripe |
| `POST` | `/api/webhook/process-session` | Procesar manualmente una sesión de Stripe |

La configuración de seguridad se define en `SecurityConfig` y en las anotaciones de los controladores. Antes de publicar el servicio, revisa los permisos de cada endpoint, especialmente los endpoints de procesamiento manual y confirmación de pago.

## Pagos con Stripe

El flujo habitual es:

1. El cliente autenticado envía los artículos y las direcciones a `POST /api/checkout/create-session`.
2. El backend crea una sesión de Stripe y devuelve sus datos para redirigir al cliente al checkout.
3. El cliente completa el pago en Stripe.
4. Stripe envía el evento firmado a `POST /api/webhook/stripe`.
5. El backend verifica la firma y procesa la sesión completada para crear el pedido.

Ejemplo del cuerpo para crear una sesión:

```json
{
  "cartItems": [
    {
      "guitarraid": 1,
      "cantidad": 1
    }
  ],
  "direccionEntregaId": 1,
  "direccionFacturacionId": 1,
  "email": "cliente@example.com",
  "nombreCliente": "Nombre del cliente"
}
```

Configura el webhook de Stripe con la URL pública del backend seguida de `/api/webhook/stripe`. Para desarrollo local se puede usar Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/api/webhook/stripe
```

Usa el secreto `whsec_...` que entrega Stripe CLI como `STRIPE_WEBHOOK_SECRET`. Las claves de prueba y las tarjetas de prueba están disponibles en el [Dashboard y la documentación de Stripe](https://docs.stripe.com/testing).

## Imágenes de guitarras

Las imágenes se guardan en `GUITARRAS_DIR` (por defecto, `uploads/guitarras`) y se sirven desde `/media/guitarras/**`. En producción configura un directorio persistente y accesible por la aplicación.

## Estructura del proyecto

```text
src/
├── main/
│   ├── java/com/example/mr_backend_wh/
│   │   ├── controller/   # Controladores REST
│   │   ├── service/      # Lógica de negocio
│   │   ├── repository/   # Acceso a datos JPA
│   │   ├── model/        # Entidades
│   │   ├── DTO/          # Objetos de transferencia
│   │   ├── security/     # JWT, OAuth y configuración de seguridad
│   │   └── config/       # Configuración de recursos
│   └── resources/        # Configuración de Spring
└── test/                 # Pruebas
```

## Despliegue y seguridad

- No subas claves de Stripe, credenciales OAuth, contraseñas ni secretos JWT al repositorio.
- En producción, utiliza HTTPS y habilita cookies seguras.
- La configuración CORS actual permite `http://localhost:3000`; ajústala al origen del frontend desplegado.
- Protege los endpoints según el rol y la operación antes de exponer el backend públicamente.
- Usa las claves de Stripe correspondientes al entorno (pruebas o producción) y conserva persistentes los datos de imágenes.

## Documentación adicional

- [Inicio rápido de Stripe](README_INICIO_RAPIDO.md)
- [Guía de integración con Stripe](STRIPE_GUIDE.md)
- [Ejemplos de Stripe](STRIPE_EJEMPLOS.md)
- [Implementación de Stripe](STRIPE_IMPLEMENTACION.md)
- [Webhooks de Stripe](STRIPE_WEBHOOKS.md)

