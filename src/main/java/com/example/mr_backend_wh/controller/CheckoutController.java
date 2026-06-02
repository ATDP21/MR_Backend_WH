package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.CheckoutResponseDTO;
import com.example.mr_backend_wh.DTO.CheckoutSessionRequestDTO;
import com.example.mr_backend_wh.DTO.PedidoDTO;
import com.example.mr_backend_wh.service.VentaService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@Slf4j
public class CheckoutController {

    @Autowired
    private VentaService ventaService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    /**
     * Crear una sesión de pago con Stripe
     * Requiere autenticación
     */
    @PostMapping("/create-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutResponseDTO> createCheckoutSession(
            @RequestBody CheckoutSessionRequestDTO request) {
        try {
            log.info("Solicitud de crear sesión de checkout recibida");
            CheckoutResponseDTO response = ventaService.createCheckoutSession(request);
            log.info("Sesión creada exitosamente: {}", response.getSessionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al crear sesión de checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Obtener el estado de una sesión de pago
     */
    @GetMapping("/session-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutResponseDTO> getSessionStatus(
            @RequestParam String sessionId) {
        try {
            log.info("Solicitud de obtener estado de sesión: {}", sessionId);
            CheckoutResponseDTO response = ventaService.getSessionStatus(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener estado de sesión: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Confirmar el pago y crear el pedido después del checkout exitoso
     * Requiere autenticación
     */
    @PostMapping("/confirm-payment")
    public ResponseEntity<PedidoDTO> confirmPayment(@RequestBody String body) {
        try {
            // body esperado: JSON con { "sessionId": "cs_test_..." , opcionales: "email","direccionEntrega","direccionFacturacion","cartItems" }
            String sessionId = null;
            String emailOverride = null;
            String direccionEntregaOverride = null;
            String direccionFacturacionOverride = null;
            String cartItemsOverride = null;

            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> map = om.readValue(body, java.util.Map.class);
                if (map != null) {
                    Object s = map.get("sessionId"); if (s != null) sessionId = String.valueOf(s);
                    Object e = map.get("email"); if (e != null) emailOverride = String.valueOf(e);
                    Object de = map.get("direccionEntrega"); if (de != null) direccionEntregaOverride = String.valueOf(de);
                    Object df = map.get("direccionFacturacion"); if (df != null) direccionFacturacionOverride = String.valueOf(df);
                    Object ci = map.get("cartItems"); if (ci != null) cartItemsOverride = String.valueOf(ci);
                }
            } catch (Exception ex) {
                // fallback: intentar parse simple
                if (body != null && body.contains("sessionId")) {
                    int idx = body.indexOf("sessionId");
                    int colon = body.indexOf(':', idx);
                    int quote1 = body.indexOf('"', colon);
                    int quote2 = body.indexOf('"', quote1 + 1);
                    if (quote1 > 0 && quote2 > quote1) sessionId = body.substring(quote1 + 1, quote2);
                }
            }

            if (sessionId == null || sessionId.isBlank()) {
                log.warn("confirmPayment: falta sessionId en body={}", body);
                return ResponseEntity.badRequest().body(null);
            }

            log.info("confirmPayment: sessionId={}", sessionId);

            // Recuperar la sesión en Stripe
            Stripe.apiKey = stripeApiKey;
            Session session = Session.retrieve(sessionId);

            log.info("confirmPayment: session={}", session);

            // Si la metadata está ausente, usar la variante con overrides
            boolean hasMetadata = session.getMetadata() != null && !session.getMetadata().isEmpty();
            if (!hasMetadata && (emailOverride != null || direccionEntregaOverride != null || direccionFacturacionOverride != null || cartItemsOverride != null)) {
                return ResponseEntity.ok(ventaService.procesarPedidoDespuesDePago(session, emailOverride, direccionEntregaOverride, direccionFacturacionOverride, cartItemsOverride));
            }

            // Comportamiento normal
            return ResponseEntity.ok(ventaService.procesarPedidoDespuesDePago(session));


        } catch (RuntimeException e) {
            log.warn("confirmPayment: error de validación sessionId={} mensaje={}", body, e.getMessage());
            return ResponseEntity.status(400).body(null);
        } catch (Exception e) {
            log.error("confirmPayment: error inesperado sessionId={}", body, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}

