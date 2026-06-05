package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.PedidoDTO;
import com.example.mr_backend_wh.service.VentaService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class StripeWebhookController {

    @Autowired
    private VentaService ventaService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    /**
     * Endpoint para recibir webhooks de Stripe
     * Este endpoint se ejecuta cuando Stripe notifica cambios en los pagos
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            Stripe.apiKey = stripeApiKey;
            
            // Verificar que la firma del webhook sea válida
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            // Procesar diferentes tipos de eventos
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "charge.refunded":
                    log.info("Reembolso procesado: {}", event.getId());
                    break;
                default:
                    log.info("Evento no manejado: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook recibido correctamente");

        } catch (SignatureVerificationException e) {
            log.error("Firma de Webhook inválida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma inválida");
        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error procesando webhook");
        }
    }

    /**
     * Endpoint de depuración: procesar manualmente una sesión de Stripe por sessionId.
     * Útil para desarrollo cuando se quiere forzar la ejecución del flujo de creación de pedidos.
     * No requiere firma y solo está pensado para pruebas locales.
     */
    @PostMapping("/process-session")
    public ResponseEntity<String> processSessionById(@RequestBody String body) {
        try {
            // body esperado: JSON con { "sessionId": "cs_test_...", optional overrides: "email","direccionEntrega","direccionFacturacion","cartItems" }
            String sessionId = null;
            String emailOverride = null;
            String direccionEntregaOverride = null;
            String direccionFacturacionOverride = null;
            String cartItemsOverride = null;

            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> map = om.readValue(body, java.util.Map.class);
                if (map != null) {
                    Object s = map.get("sessionId");
                    if (s != null) sessionId = String.valueOf(s);
                    Object e = map.get("email"); if (e != null) emailOverride = String.valueOf(e);
                    Object de = map.get("direccionEntrega"); if (de != null) direccionEntregaOverride = String.valueOf(de);
                    Object df = map.get("direccionFacturacion"); if (df != null) direccionFacturacionOverride = String.valueOf(df);
                    Object ci = map.get("cartItems"); if (ci != null) cartItemsOverride = String.valueOf(ci);
                }
            } catch (Exception ex) {
                if (body != null && body.contains("sessionId")) {
                    int idx = body.indexOf("sessionId");
                    int colon = body.indexOf(':', idx);
                    int quote1 = body.indexOf('"', colon);
                    int quote2 = body.indexOf('"', quote1 + 1);
                    if (quote1 > 0 && quote2 > quote1) sessionId = body.substring(quote1 + 1, quote2);
                }
            }

            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body("Falta sessionId en body");
            }

            // Llamar al flujo de depuración recuperando la sesión completa
            Stripe.apiKey = stripeApiKey;
            Session session = Session.retrieve(sessionId);

            // Usar la variante que permite overrides si la metadata estuviera vacía
            ventaService.procesarPedidoDespuesDePago(session, emailOverride, direccionEntregaOverride, direccionFacturacionOverride, cartItemsOverride);

            return ResponseEntity.ok("Procesado OK: " + sessionId);
        } catch (Exception e) {
            log.error("Error procesando sesión manualmente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();

        log.info("Sesión de pago completada: {} - Cliente: {}",
                session.getId(),
                session.getCustomerEmail());

        String email = session.getMetadata().get("email");
        String nombreCliente = session.getMetadata().get("nombreCliente");
        log.info("Información del cliente - Email: {}, Nombre: {}", email, nombreCliente);

        PedidoDTO pedidoCreado = ventaService.procesarPedidoDespuesDePago(session);
        log.info("Pedido confirmado desde webhook. Pedido ID: {}, sesión Stripe: {}",
                pedidoCreado.getId(),
                session.getId());
    }
}



